package app.m8.web.client.view.calendarmodule.notes.note;

import app.components.client.DomUtils;
import app.components.client.JQueryUtils;
import app.components.client.ScrollUtils;
import app.components.client.animation.AnimationUtils;
import app.components.client.base.DateField;
import app.components.client.base.SimpleTimeWidget;
import app.components.client.contenteditable.CursorPosition;
import app.components.client.contenteditable.SimpleLinksTextArea;
import app.components.client.dotpopup.DotAction;
import app.components.client.dotpopup.DotActionItemModel;
import app.components.client.dotpopup.DotActionPresenter;
import app.components.client.dotpopup.DotActionView;
import app.components.client.dotpopup.UniformDotActionEventHandler;
import app.components.client.mvp.AbstractCommonPresenter;
import app.components.client.mvp.promise.FulfillablePromise;
import app.components.client.mvp.promise.Promise;
import app.components.client.mvp.promise.Promises;
import app.components.client.plusobject.DataTransferBuilder;
import app.components.client.plusobject.PlusItemType;
import app.components.client.plusobject.PlusObjectService;
import app.components.client.upload.event.DropAgmFileEvent;
import app.components.client.upload.event.DropAgmFilesEvent;
import app.components.client.upload.event.DropFileEventHandler;
import app.components.client.upload.event.DropNativeFileEvent;
import app.components.client.util.DateUtils;
import app.components.client.util.UIUtils;
import app.m8.web.client.attachment.AttachmentFactory;
import app.m8.web.client.attachment.AttachmentPanelFileSupervisor;
import app.m8.web.client.attachment.AttachmentPanelPresenter;
import app.m8.web.client.attachment.AttachmentPanelSupervisor;
import app.m8.web.client.attachment.DefaultAttachmentPanelSupervisor;
import app.m8.web.client.attachment.file.FileAttachmentPresenter;
import app.m8.web.client.attachment.file.FileAttachmentSupervisor;
import app.m8.web.client.command.AGMDispatcher;
import app.m8.web.client.view.calendarmodule.notes.events.UpdateNoteEvent;
import app.m8.web.client.view.calendarmodule.notes.note.components.NoteKeyDownEventListener;
import app.m8.web.client.view.calendarmodule.notes.note.components.NoteKeyDownEventListenerSupervisor;
import app.m8.web.client.view.calendarmodule.notes.storage.NotesDescriptionVisibilityStorage;
import app.m8.web.client.view.calendarmodule.notes.table.NotesTableSupervisor;
import app.m8.web.shared.CommonUtils;
import app.m8.web.shared.GlobalConstants;
import app.m8.web.shared.command.actions.calendar.note.UpdateNoteStatusAction;
import app.m8.web.shared.command.actions.pretenders.result.ExportVacancyToTaskResult;
import app.m8.web.shared.core.TextBodyContent;
import app.m8.web.shared.core.calendar.Time;
import app.m8.web.shared.core.file.AGMFile;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.note.NoteChangeStatus;
import app.m8.web.shared.core.note.NoteStatus;
import app.m8.web.shared.core.task.TypeProtocol;
import app.m8.web.shared.core.texteditor.Document;
import app.m8.web.shared.richeditor.RteContent;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

public class NotePresenter extends AbstractCommonPresenter<NoteModel, NoteView> implements NoteKeyDownEventListenerSupervisor {

	private static final String DOT_POPUP_ITEM_STYLE = DotActionView.DOT_POPUP_LIST_ITEM_WITH_ICON_STYLE + " dot-popup-treetable__list-item";

	private final NotesTableSupervisor supervisor;
	private final DotActionPresenter dotActionPresenter;
	private final PlusObjectService plusObjectService;
	private final AttachmentPanelPresenter attachmentPanelPresenter;
	private final NotesDescriptionVisibilityStorage notesDescriptionVisibilityStorage;

	@AssistedInject
	public NotePresenter(@Assisted NoteModel model,
						 NoteView view,
						 @Assisted NotesTableSupervisor supervisor,
						 PlusObjectService plusObjectService,
						 AttachmentFactory attachmentFactory,
						 NotesDescriptionVisibilityStorage notesDescriptionVisibilityStorage) {
		super(model, view);
		this.supervisor = supervisor;
		this.plusObjectService = plusObjectService;
		this.dotActionPresenter = createDotActionPresenter();
		this.notesDescriptionVisibilityStorage = notesDescriptionVisibilityStorage;
		this.attachmentPanelPresenter = attachmentFactory.createAttachmentPanelPresenter(model.getAttachmentModel(), view.getAttachmentView(), createAttachmentPanelSupervisor());
	}

	@Override
	protected void attachHandlers() {
		registerHandler(view.addClickHandler(createNoteClickHandler()));
		registerHandler(view.addFavoriteIconClickHandler(event -> supervisor.setFavorite(this)));
		registerHandler(view.addCompleteBtnClickHandler(clickEvent -> onComplete()));
		registerHandler(view.addCompleteBtnMouseOverHandler(event -> view.setRemoveHoveredStyle(true)));
		registerHandler(view.addCompleteBtnMouseOutHandler(event -> view.setRemoveHoveredStyle(false)));
		registerHandler(view.addCopyIconClickHandler(event -> copyToClipboard()));
		registerHandler(view.addRestoreNodelabelClickHandler(event -> {
			onComplete();
			supervisor.restoreOrgStructure(model.getNote().getRestoredOrgId());
		}));
		registerHandler(view.addNameBoxKeyDownHandler(event -> {
			if (UIUtils.isCmdEnterPressed(event)) {
				showDescription(true, true);
				String transferText = view.getName().cutTextAfterCursor();
				view.getName().validate();
				view.getDescription().setText(transferText + view.getDescription().getText());
				view.getDescription().setCursorPositionBegin();
				view.refreshCopyIconVisibility();
			} else if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
				saveNote();
			}
		}));
		registerHandler(view.addDescriptionInputHandler(event -> view.refreshCopyIconVisibility()));
		registerHandler(view.addDescriptionBoxKeyDownHandler(new NoteKeyDownEventListener(this, () -> {
			boolean isDescriptionEmpty = getView().getDescription().getText().isEmpty();
			if (isDescriptionEmpty || view.getDescription().isCursorAtTheEndOfTextNode()) {
				saveNote();
			} else {
				view.insertContentEditableLineBreak();
			}
			view.refreshCopyIconVisibility();
		})));
		registerHandler(view.addNameBoxValidationHandler(event -> refreshNamePlaceholder()));
		registerHandler(view.addNameBoxFocusHandler(event -> view.setNameBoxIncorrectFieldStyle(false)));
		registerHandler(view.addNameBoxBlurHandler(event -> {
			refreshNamePlaceholder();
		}));
		registerHandler(view.addDateValueChangeHandler(event -> {
			Date eventValue = event.getValue();
			Date viewTime = getViewTime();
			if (viewTime != null) {
				DateUtils.setMH(eventValue, viewTime);
				view.setTimeValue(eventValue);
				view.setDateValue(eventValue);
			}
			handleDateChange(eventValue);
		}));
		registerHandler(view.addTimeValueChangeHandler(event -> {
			Date date;
			if (event.getValue() != null) {
				date = getViewDate();
				DateUtils.setMH(date, event.getValue());
				view.setDateValue(date);
				view.setTimeValue(date);
			} else {
				date = getViewDate();
				DateUtils.setMH(date, new Time(0, 0));
				view.setDateValue(date);
			}
			handleDateChange(date);
		}));
		registerHandler(view.getExpandIcon().addClickHandler(event -> {
			if (view.isOpened()) {
				setNoteState(NoteUIState.NORMAL);
				showDescription(false, true);
			} else {
				setNoteState(NoteUIState.EDIT, Optional.of(new CursorPosition(event.getClientX(), event.getClientY())));
				showDescription(true, true);
				view.setDescriptionFocus(true);
				ScrollUtils.scrollIntoViewIfNeededNative(view.getAttachmentView().getElement());
			}
		}));
		registerHandler(view.getDropContainer().addDropFileHandler(new DropFileEventHandler() {

			@Override
			public void onLowDropAgmFile(DropAgmFileEvent event) {
				attachAgmFile(event.getFile());
			}

			@Override
			public void onLowDropAgmFiles(DropAgmFilesEvent event) {
				event.getFiles().forEach(this::attachAgmFile);
			}

			private void attachAgmFile(AGMFile agmFile) {
				attachmentPanelPresenter.attachAgmFile(agmFile).done(() -> attachmentPanelPresenter.setAttachmentsEditMode(false));
			}

			@Override
			public void onDropNativeFile(DropNativeFileEvent event) {
				attachmentPanelPresenter.initDroppedFileUploading(event);
			}
		}));
		registerHandler(view.getDropContainer().addDropLinkEventHandler(event -> attachmentPanelPresenter.attachLink(event.getLinkTarget(), event.getLinkName())));
	}


	private native void copyToClipboard() /*-{
		var self = this;
		var text = self.@app.m8.web.client.view.calendarmodule.notes.note.NotePresenter::getClipboardText()();
		if ($wnd.clipboardData && $wnd.clipboardData.setData) {
			clipboardData.setData("Text", text);
		} else if ($doc.queryCommandSupported && $doc.queryCommandSupported("copy")) {
			var textarea = $doc.createElement("textarea");
			textarea.textContent = text;
			$doc.body.appendChild(textarea);
			textarea.select();
			try {
				$doc.execCommand("copy");
			} catch (ex) {
				console.warn("Copy to clipboard failed.", ex);
			} finally {
				$doc.body.removeChild(textarea);
			}
		}
	}-*/;

	private String getClipboardText() {
		return view.getTrimmedNameText() + "\n" + view.getTrimmedDescriptionText();
	}


	/**
	 * Отобразить/скрыть описание с анимацией
	 */
	public void showDescription(boolean show, boolean withAnimation) {
		Promises.releaseException(refreshOpenStatus(show, withAnimation));
	}

	/**
	 * Отобразить/скрыть описание
	 */
	public Promise<Void> refreshOpenStatus(boolean show, boolean withAnimation) {
		FulfillablePromise<Void> promise = FulfillablePromise.create();
		if (show == view.isOpened()) {
			return Promises.fulfilled();
		}
		if (withAnimation) {
			if (show) {
				AnimationUtils.showAnimationHeight(view.getDescription().getElement(), elements -> {
					refreshStates(true);
					promise.fulfill(null);
				});
			} else {
				AnimationUtils.hideAnimationHeight(view.getDescription().getElement(), elements -> {
					refreshStates(false);
					promise.fulfill(null);
				});
			}
		} else {
			refreshStates(show);
			promise.fulfill(null);
		}
		return promise;
	}

	private void refreshStates(boolean isVisible) {
		view.setOpenedStyle(isVisible);
		view.setDescriptionVisible(isVisible);
		notesDescriptionVisibilityStorage.setDescriptionOpen(getNote().getId(), isVisible);
		view.refreshCopyIconVisibility();
	}

	@Override
	public void refresh() {
		refreshEditState(Optional.empty());
		refreshNameBoxName();
		refreshDescriptionBox();
		refreshDateBox();
		refreshAttachmentPanel();
		refreshFavoriteIcon();
		refreshDotActionButton();
		view.getRestoreNodelabel().setVisible(model.getNote().getRestoredOrgId() != null);
	}

	private void refreshFavoriteIcon() {
		view.setFavorite(model.getNote().isFavorite());
	}

	private void refreshDotActionButton() {
		boolean dotActionButtonVisible = model.isDotActionButtonVisible();
		dotActionPresenter.getView().setVisible(dotActionButtonVisible);
		if (dotActionButtonVisible) {
			dotActionPresenter.refreshAsync();
		}
	}

	private Optional<CursorPosition> getDescriptionCursorPosition(ClickEvent event) {
		if (SimpleLinksTextArea.is(event.getNativeEvent().getEventTarget())) {
			return Optional.of(new CursorPosition(event.getClientX(), event.getClientY()));
		}
		return Optional.empty();
	}

	private ClickHandler createNoteClickHandler() {
		return event -> {
			if (!supervisor.isPreventNoteNativeClick(event.getNativeEvent()) && !preventClick(event.getNativeEvent())) {
				if (isNormalState()) {
					supervisor.setSelectedNote(NotePresenter.this);
				} else if (isSelectedState()) {
					setEditNode(event);
				}
			}
		};
	}

	private boolean isNormalState() {
		return model.isNormalState();
	}

	private boolean preventClick(NativeEvent nativeEvent) {
		return isNoteAreaDateField(nativeEvent)
				|| isNoteAreaTimeField(nativeEvent)
				|| isFavoriteIcon(nativeEvent)
				|| isNoteAreaDatePopupElement(nativeEvent)
				|| isNoteAreaCopyIcon(nativeEvent)
				|| isFile(nativeEvent)
				|| (isExpandIcon(nativeEvent))
				|| isTimeDatePopupElement(getTargetElement(nativeEvent));
	}

	private boolean isNoteAreaCopyIcon(NativeEvent nativeEvent) {
		return view.getCopyIcon().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private boolean isExpandIcon(NativeEvent nativeEvent) {
		return view.getExpandIcon().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private boolean isFile(NativeEvent nativeEvent) {
		return attachmentPanelPresenter.getFileAttachmentPresenterMap().entrySet().stream()
				.anyMatch(agmFileFileAttachmentPresenterEntry -> agmFileFileAttachmentPresenterEntry.getValue().getView().getElement().isOrHasChild(getTargetElement(nativeEvent)));
	}

	private boolean isNoteAreaDateField(NativeEvent nativeEvent) {
		return view.getDateField().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private boolean isNoteAreaTimeField(NativeEvent nativeEvent) {
		return view.getTimeField().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private boolean isFavoriteIcon(NativeEvent nativeEvent) {
		return view.getFavoriteIcon().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private boolean isNoteAreaDatePopupElement(NativeEvent nativeEvent) {
		return isNoteAreaDatePopupElement(getTargetElement(nativeEvent));
	}

	private boolean isNoteAreaDatePopupElement(Element targetElement) {
		return targetElement != null && JQueryUtils.hasClosest(targetElement, GlobalConstants.DOT + DateField.TRIPPLE_DATE_PICKER_STATIC_STYLE);
	}

	private boolean isTimeDatePopupElement(Element targetElement) {
		return targetElement != null
				&& JQueryUtils.getClosest(targetElement, GlobalConstants.DOT + SimpleTimeWidget.SIMPLE_TIME_RANGE_PANEL) != null;
	}

	private Element getTargetElement(NativeEvent nativeEvent) {
		final EventTarget eventTarget = nativeEvent.getEventTarget();
		if (Element.is(eventTarget)) {
			return Element.as(eventTarget);
		}
		return null;
	}

	private void setEditNode(ClickEvent event) {
		EventTarget eventTarget = event.getNativeEvent().getEventTarget();
		if (!AnchorElement.is(eventTarget) && !DomUtils.hasExpandedSelection()) {
			setNoteState(NoteUIState.EDIT, getDescriptionCursorPosition(event));
		}
	}

	public void deleteNoteFromStorage() {
		notesDescriptionVisibilityStorage.deleteNote(getNote().getId());
	}

	private void onComplete() {
		supervisor.onNoteCompleteBtnClick(this);
	}

	private void handleDateChange(Date date) {
		supervisor.setEditNotePresenter(this);
		view.refreshDateExpired(date);
		view.setFocusedStyle(false);
		supervisor.saveAllEditedNotesAndUpdate().done();
	}

	private void refreshEditState(Optional<CursorPosition> cursorPosition) {
		refreshDescriptionBoxEditable(cursorPosition);
		view.setFocusedStyle(isEditState());
		attachmentPanelPresenter.setAttachmentsEditMode(false);
	}

	private void refreshNameBoxName() {
		view.setNoteName(model.getNoteName());
	}

	private void refreshDescriptionBox() {
		refreshDescriptionBoxName();
		refreshDescriptionBoxVisible();
	}

	private void refreshDescriptionBoxName() {
		view.updateDescriptionEditModeContent(isEditState(), model.getNoteDescription());
	}

	private void refreshDescriptionBoxEditable(Optional<CursorPosition> cursorPosition) {
		view.refreshDescriptionEditModeContent(isEditState(), cursorPosition);
	}

	private void refreshDescriptionBoxVisible() {
		showDescription(notesDescriptionVisibilityStorage.isDescriptionOpen(getNote().getId()) && !view.isDescriptionEmpty(), false);
	}

	public void refreshDateBox() {
		view.refreshDateValue(model.getDateNote());
		view.refreshDateExpired(model.getDateNote());
	}

	private void refreshAttachmentPanel() {
		attachmentPanelPresenter.refresh();
	}

	public boolean isTextFieldsFocus() {
		boolean isNameFocus = getView().isNameFocus();
		boolean isDescriptionFocus = getView().isDescriptionFocus();
		return isNameFocus || isDescriptionFocus;
	}

	private void refreshNamePlaceholder() {
		view.refreshNamePlaceholder();
	}

	/**
	 * Выбрать заметку
	 */
	public void setSelected(boolean isSelected) {
		if (!isEditState()) {
			if (isSelected) {
				if (!isSelectedState() || !getView().isSelected()) {
					setNoteState(NoteUIState.SELECTED);
				}
			} else if (isSelectedState()) {
				setNoteState(NoteUIState.NORMAL);
			}
		} else if (!isSelected) {
			supervisor.collapseEditedNoteAndUpdate().done(() -> setNoteState(NoteUIState.NORMAL));
		}
	}

	private DotActionPresenter createDotActionPresenter() {
		return new DotActionPresenter(model.getDotActionModel(), view.getDotActionView(), new UniformDotActionEventHandler() {

			@Override
			public void onDotAction(DotActionPresenter dotActionPresenter, DotAction dotActionType) {
				switch (dotActionType) {
					case SEND_TO_IDEAS:
						saveNotePromise().done(value -> {
							DataTransferBuilder dataTransferBuilder = new DataTransferBuilder();
							dataTransferBuilder.setName(model.getNoteName());
							dataTransferBuilder.setDescription(CommonUtils.convertToHtml(model.getNoteDescription()));
							dataTransferBuilder.setAgmFiles(model.getNote().getFiles());
							dataTransferBuilder.setReminderDate(model.getDateNote());
							setNoteIdToRemove(dataTransferBuilder, model.getNote().getId());
							plusObjectService.show(PlusItemType.IDEA, dataTransferBuilder.build()).done();
						});
						break;
					case PLUS_WINDOW_TASK:
						saveNotePromise().done(value -> {
							DataTransferBuilder dataTransferBuilder = new DataTransferBuilder();
							Document circumstancesTb = new Document();
							if (!CommonUtils.isNullOrEmptyString(model.getNoteDescription())) {
								circumstancesTb.getBodyContents().add(new TextBodyContent(CommonUtils.convertToHtml(model.getNoteDescription())));
							}
							dataTransferBuilder.setExportVacancyToTaskResult(new ExportVacancyToTaskResult(model.getNoteName(),
									TypeProtocol.ACTION_PROTOCOL_ID, null, model.getDateNote(), null,
									new RteContent(circumstancesTb)));
							dataTransferBuilder.setAgmFiles(model.getNote().getFiles());
							setNoteIdToRemove(dataTransferBuilder, model.getNote().getId());
							plusObjectService.show(PlusItemType.TASK, dataTransferBuilder.build()).done();
						});
						break;
					default:
						break;
				}
			}

			@Override
			public DotActionView.DotActionItem createDotActionItem(DotActionItemModel dotActionItemModel) {
				DotActionView.DotActionItem createDotActionItem = super.createDotActionItem(dotActionItemModel);
				String iconStyle = dotActionItemModel.getDotActionCallback().getType().getIconStyle();
				createDotActionItem.addStyleName(DOT_POPUP_ITEM_STYLE + GlobalConstants.SPACE + iconStyle);
				return createDotActionItem;
			}
		});
	}

	protected void setNoteIdToRemove(DataTransferBuilder dataTransferBuilder, Integer id) {
		dataTransferBuilder.setAfterSuccessContentCreateAction(() -> {
			AGMDispatcher.getInstance().execute(new UpdateNoteStatusAction(id, NoteStatus.COMPLETED))
					.done(value -> {
						UpdateNoteEvent updateNoteEvent = new UpdateNoteEvent(Collections.singletonList(id), NoteChangeStatus.COMPLETE);
						// берем глобальную шину событий
						model.getAttachmentModel().getEventBus().fireEvent(updateNoteEvent);
					});
		});

	}

	private AttachmentPanelSupervisor createAttachmentPanelSupervisor() {
		return new DefaultAttachmentPanelSupervisor(model.getFileService()) {

			@Override
			public FileAttachmentSupervisor createFileAttachmentSupervisor(AttachmentPanelPresenter attachmentPanelPresenter) {
				return new AttachmentPanelFileSupervisor(getFileService(), this, attachmentPanelPresenter, attachmentPanelPresenter.getModel()) {

					@Override
					public void renameFile(FileAttachmentPresenter fileAttachmentPresenter) {
						super.renameFile(fileAttachmentPresenter);
						setNoteState(NoteUIState.SELECTED);
					}
				};
			}

			@Override
			public void afterAttachFile(AttachmentPanelPresenter attachmentPanelPresenter, Boolean mainAccount) {
				resetAutomaticState();
			}
		};
	}

	private void resetAutomaticState() {
		getModel().resetAutomaticState();
	}

	private boolean isEditState() {
		return getModel().isEditState();
	}

	private boolean isSelectedState() {
		return model.isSelectedState();
	}

	public boolean setNoteState(NoteUIState state) {
		return setNoteState(state, Optional.empty());
	}

	/**
	 * Установить режим отображения для напоминания
	 */
	public boolean setNoteState(NoteUIState state, Optional<CursorPosition> cursorPosition) {
		boolean isStateChanged = model.setState(state);
		if (isStateChanged) {
			switch (state) {
				case NORMAL:
					view.setNoteNormalState();
					refreshEditState(Optional.empty());
					break;
				case EDIT:
					supervisor.setEditNotePresenter(this);
					view.setNoteSelectedState();
					refreshEditState(cursorPosition);
					break;
				case SELECTED:
					refreshEditState(Optional.empty());
					view.setNoteSelectedState();
					break;
				default:
					break;
			}
		}
		return isStateChanged;
	}

	public void updateDatePopupPosition() {
		view.updateDatePopupPosition();
	}

	public Note getNote() {
		return model.getNote();
	}

	public boolean isSelected() {
		return isSelectedState() && getView().isSelected();
	}

	public boolean isNameBoxEmpty() {
		return view.isNameBoxEmpty();
	}

	public String getTrimmedNameText() {
		return view.getTrimmedNameText();
	}

	public String getTrimmedDescriptionText() {
		return view.getTrimmedDescriptionText();
	}

	public Date getViewDate() {
		return view.getViewDate();
	}

	private Date getViewTime() {
		return view.getViewTime();
	}

	public boolean isSameEditedNoteName() {
		final String trimmedNameText = getTrimmedNameText();
		return trimmedNameText.equals(CommonUtils.preventNullString(model.getNoteName()));
	}

	public boolean isSameEditedNoteDescription() {
		return view.isSameDescriptionText(model.getNoteDescription());
	}

	public boolean isSameEditedNoteDate() {
		return getNote().isSameDate(getViewDate());
	}

	public boolean isOnlyTimeChanged() {
		// TODO очень странное сравнение, дублирование одного и того же метода
		// нет явного сравнения даты с учетом времени, нет проверки на null, дата из вьюхи не объязана быть заполненной
		return !isSameEditedNoteDate() && DateUtils.compareDate(getNote().getDateNote(), getViewDate()) == 0;
	}

	public void setFavorite(boolean setFavorite) {
		getNote().setFavorite(setFavorite);
		refreshFavoriteIcon();
	}

	@Override
	public void onDoubleClick(Event event) {
		if (isEditState()) {
			saveNote();
			DomUtils.shutUpEvent(event);
		}
	}

	@Override
	public void onCmdEnter(Event event) {
		saveNote();
		DomUtils.shutUpEvent(event);
	}

	private void saveNote() {
		saveNotePromise().done();
	}

	private Promise<Void> saveNotePromise() {
		view.setDescriptionFocus(false);
		view.setNameFocus(false);
		return supervisor.collapseEditedNoteAndUpdate();
	}

	/**
	 * Обработка долгов анимации перед азменением следующего стейта нотификации.
	 */
	public Promise<Void> ensureNoteStateChange(NoteUIState nextState) {
		boolean needCollapseAnimationFirst = isNeedCollapseNormalState(nextState);

		setNoteState(nextState);

		if (needCollapseAnimationFirst) {
			return refreshOpenStatus(false, true);
		}
		return Promises.fulfilled();
	}

	private boolean isNeedCollapseNormalState(NoteUIState state) {
		return !model.isNormalState() && state.equals(NoteUIState.NORMAL) && view.isDescriptionEmpty() && view.isDescriptionVisible();
	}
}
