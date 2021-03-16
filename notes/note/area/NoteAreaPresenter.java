package app.m8.web.client.view.calendarmodule.notes.note.area;

import app.components.client.DomUtils;
import app.components.client.JQueryUtils;
import app.components.client.RootPanelProvider;
import app.components.client.RootPanelProviderImpl;
import app.components.client.base.DateField;
import app.components.client.base.SimpleTimeWidget;
import app.components.client.base.event.NativePreviewWatcher;
import app.components.client.dotpopup.DotActionView;
import app.components.client.mvp.AbstractCommonPresenter;
import app.components.client.mvp.promise.FulfillablePromise;
import app.components.client.mvp.promise.Promise;
import app.components.client.mvp.promise.Promises;
import app.components.client.upload.DropAgmFileResult;
import app.components.client.upload.event.DropFileEventHandler;
import app.components.client.upload.event.DropNativeFileEvent;
import app.components.client.util.DateUtils;
import app.components.client.util.UIUtils;
import app.m8.web.client.attachment.AttachmentFactory;
import app.m8.web.client.attachment.AttachmentPanelPresenter;
import app.m8.web.client.attachment.DefaultAttachmentPanelSupervisor;
import app.m8.web.client.attachment.file.FileService;
import app.m8.web.client.view.calendarmodule.calendar.controllers.ClickControllerAdapter;
import app.m8.web.client.view.calendarmodule.notes.dnd.TaskInNotesAreaController;
import app.m8.web.client.view.calendarmodule.notes.group.NotesGroupSupervisor;
import app.m8.web.client.view.calendarmodule.notes.note.components.NoteKeyDownEventListener;
import app.m8.web.client.view.calendarmodule.notes.note.components.NoteKeyDownEventListenerSupervisor;
import app.m8.web.client.view.calendarmodule.notes.table.NotesTableModel;
import app.m8.web.client.view.calendarmodule.notes.table.NotesTableSupervisor;
import app.m8.web.client.view.goalortask.dnd.GoalOrTaskDropHandler;
import app.m8.web.client.view.goalortask.dnd.GoalOrTaskDropListener;
import app.m8.web.shared.CommonUtils;
import app.m8.web.shared.GlobalConstants;
import app.m8.web.shared.core.calendar.Time;
import app.m8.web.shared.core.file.AGMFile;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.note.NoteStatus;
import app.m8.web.shared.core.task.GoalOrTask;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Event;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import java.util.ArrayList;
import java.util.Date;

import static app.components.client.upload.RootHighlightDropFilesPanel.ROOT_DRAGOVER_CLASS;


public class NoteAreaPresenter extends AbstractCommonPresenter<NoteAreaModel, NoteAreaView> {

	private final NotesTableSupervisor tableSupervisor;
	private final NotesGroupSupervisor groupSupervisor;
	private final RootPanelProvider rootPanelProvider;
	private final FileService fileService;
	private AttachmentPanelPresenter attachmentPanelPresenter;
	private FulfillablePromise<Void> savePromise;

	@AssistedInject
	public NoteAreaPresenter(@Assisted NotesTableModel notesTableModel,
							 @Assisted NotesTableSupervisor notesTableSupervisor,
							 @Assisted NotesGroupSupervisor notesGroupSupervisor,
							 NoteAreaModel model,
							 NoteAreaView view,
							 FileService fileService,
							 AttachmentFactory attachmentFactory) {
		super(model, view);
		this.tableSupervisor = notesTableSupervisor;
		this.groupSupervisor = notesGroupSupervisor;
		this.fileService = fileService;
		this.rootPanelProvider = new RootPanelProviderImpl();
		model.setNotesTableModel(notesTableModel);
		createAttachmentPanel(attachmentFactory);
		new NativePreviewWatcher(getPreviewHandler(), view.getDatePopupCalendar(), true);
		new TaskInNotesAreaController(this);
		initTaskDropController();
	}

	private void initTaskDropController() {
		new GoalOrTaskDropHandler(getView().getDropContainer(), new GoalOrTaskDropListener() {

			@Override
			public void doOnGoalOrTaskDrop(Event event, GoalOrTask draggedItem) {
				refreshDropPanelStyles(false);
			}

			@Override
			public void doOnGoalOrTaskDragOver(Event event, GoalOrTask draggedItem) {
				refreshDropPanelStyles(true);
			}

			@Override
			public void doOnGoalOrTaskDragEnter(Event event, GoalOrTask draggedItem) {
				refreshDropPanelStyles(true);
			}

			@Override
			public void doOnGoalOrTaskDragLeave(Event event, GoalOrTask draggedItem) {
				refreshDropPanelStyles(false);
			}

			private void refreshDropPanelStyles(boolean dragover) {
				DomUtils.setClassName(getView().getDropContainer().getReadyStatePanel().getElement(), GlobalConstants.DROP_FILES_OVER_STYLE, dragover);
				if (dragover) {
					getView().getDropContainer().getReadyStatePanel().getElement().getStyle().setProperty(GlobalConstants.POINTER_EVENTS_PROP, "none");
				} else {
					getView().getDropContainer().getReadyStatePanel().getElement().getStyle().clearProperty(GlobalConstants.POINTER_EVENTS_PROP);
				}
				DomUtils.setClassName(getView().getElement(), ROOT_DRAGOVER_CLASS, dragover);
			}
		});
	}

	private Event.NativePreviewHandler getPreviewHandler() {
		return event -> {
			if (event.getTypeInt() == Event.ONKEYDOWN && event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
				handleDateField(event.getNativeEvent());
			}
		};
	}

	@Override
	protected void onAttachView() {
		setDefaultDateToDateBox();
	}

	@Override
	protected void onDetachView() {
		setNoteAreaNormalState();
	}

	@Override
	protected void attachHandlers() {
		registerHandler(rootPanelProvider.addBodyPanelClickHandler(createRootPanelClickHandler()));
		registerHandler(rootPanelProvider.addBodyPanelKeyDownHandler(createRootPanelKeyDownHandler()));
		registerHandler(view.addDateValueChangeHandler(event -> {
			final Date eventValue = event.getValue();
			Date viewTime = getViewTime();
			if (viewTime != null) {
				DateUtils.setMH(eventValue, viewTime);
				view.setTimeValue(eventValue);
				view.setDateValue(eventValue);
			}
			view.refreshDateExpired(eventValue);
			tryToSaveOnDateCalendarHide();
		}));
		registerHandler(view.addTimeValueChangeHandler(event -> {
			final Date date;
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

			view.refreshDateExpired(date);
		}));
		registerHandler(view.addNameBoxFocusHandler(event -> Promises.releaseException(focusNoteArea())));
		registerHandler(view.addValidationHandler(event -> view.validatePlaceholder()));
		registerHandler(view.addNameBoxKeyDownHandler(event -> {
			if (UIUtils.isCmdEnterPressed(event)) {
				showDescription();
			} else if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
				saveNote();
			}
		}));
		registerHandler(view.addNameBoxChangeHandler(event -> view.refreshIconsVisibility()));
		registerHandler(view.addDescriptionBoxChangeHandler(event -> view.refreshIconsVisibility()));
		registerHandler(view.addDescriptionBoxKeyDownHandler(new NoteKeyDownEventListener(new NoteKeyDownEventListenerSupervisor() {

			@Override
			public void onDoubleClick(Event event) {
				saveNote();
			}

			@Override
			public void onCmdEnter(Event event) {
				saveNote();
			}
		}, () -> {
			final String descriptionValue = view.getDescription().getValue();
			final int cursorPos = getView().getDescription().getCursorPos();
			String textBeforeCursor = descriptionValue.substring(0, cursorPos);
			boolean isEndsWithNewLine = textBeforeCursor.endsWith("\n");
			final int descriptionLength = descriptionValue.length();
			String textAfterCursor = descriptionValue.substring(cursorPos, descriptionLength);
			boolean isStartsWithNewLine = textAfterCursor.startsWith("\n");
			boolean isEmptyLine = isEndsWithNewLine && isStartsWithNewLine;
			boolean isDescriptionEmpty = getView().getDescription().isEmptyValue();
			boolean isNewLineAtTheBegining = isStartsWithNewLine && cursorPos == 0;
			boolean isNewLineAtTheEnd = isEndsWithNewLine && cursorPos == descriptionLength;
			if (isDescriptionEmpty || isEmptyLine || isNewLineAtTheBegining || isNewLineAtTheEnd) {
				saveNote();
			} else {
				int cursorPosition = view.getDescription().getCursorPos();
				String transferText = view.getDescription().cutTextAfterCursor();
				view.getDescription().setText(view.getDescription().getText() + "\n" + transferText);
				view.getDescription().setCursorPos(cursorPosition + "\n".length());
			}
		})));
		registerHandler(view.getClearBtn().addClickHandler(event -> {
			refreshNoteAreaNormalState();
			focusNoteArea();
		}));
		registerHandler(view.getExpandIcon().addClickHandler(event -> {
			if (!view.isDescriptionVisible()) {
				view.showDescription();
				view.setDescriptionFocus(true);
			} else {
				view.hideDescription();
				view.setNameFocus(true);
			}
		}));
		registerHandler(view.getDropContainer().addDropFileHandler(new DropFileEventHandler() {

			@Override
			public Promise<DropAgmFileResult> onDropAgmFile(AGMFile agmFile) {
				view.showDescription();
				return attachmentPanelPresenter.attachAgmFile(agmFile);
			}

			@Override
			public void onDropNativeFile(DropNativeFileEvent event) {
				view.showDescription();
				attachmentPanelPresenter.initDroppedFileUploading(event);
			}
		}));
		registerHandler(view.getDropContainer().addDropLinkEventHandler(event -> {
			view.showDescription();
			attachmentPanelPresenter.attachLink(event.getLinkTarget(), event.getLinkName());
		}));
	}

	@Override
	public void refresh() {
		cancelUploads();
		refreshAttachmentPanelPresenter();
		refreshNameBox();
		refreshDescriptionBox();
		setNoteAreaNormalState();
		view.refreshOpenedStile();
		view.refreshIconsVisibility();
	}

	private void showDescription() {
		if (!view.isDescriptionVisible()) {
			view.showDescription();
		}
		view.setDescriptionFocus(true);
		DomUtils.transferTextAfterCursor(view.getName(), view.getDescription());
	}

	private ClickControllerAdapter createRootPanelClickHandler() {
		return new ClickControllerAdapter() {

			@Override
			public void doOnClick(ClickEvent event) {
				NativeEvent nativeEvent = event.getNativeEvent();
				boolean notesAttachmentFiles = isNotesAttachmentFilesPopupElement(nativeEvent);
				if (!(isPreventClick(nativeEvent) || notesAttachmentFiles)) {
					saveNote();
				}
			}

			private boolean isPreventClick(NativeEvent nativeEvent) {
				Element targetElement = getTargetElement(nativeEvent);
				return isNoteAreaNameBoxElement(nativeEvent)
						|| isNoteAreaDescriptionBoxElement(nativeEvent)
						|| isNoteAreaDateField(nativeEvent)
						|| isNoteAreaTimeField(nativeEvent)
						|| isNoteAreaHideIcon(nativeEvent)
						|| isExpandIcon(nativeEvent)
						|| isNoteAreaDatePopupElement(nativeEvent)
						|| isTimeDatePopupElement(targetElement)
						|| isNotesViewerPopupElement(targetElement)
						|| isNoteAttachmentPanelView(nativeEvent);
			}
		};
	}

	/**
	 * Сохранение напоминания
	 */
	public Promise<Void> saveNote() {
		return trySaveNoteArea();
	}

	private KeyDownHandler createRootPanelKeyDownHandler() {
		return event -> {
			if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
				if (!view.getDatePopupCalendar().isAttached() && !view.getTimePopupCalendar().isAttached()) {
					view.setNameFocus(false);
					view.setDescriptionFocus(false);
					refreshNoteAreaNormalState();
					tableSupervisor.fireRefreshCountersEvent();
				}
			}
		};
	}

	private void refreshNoteAreaNormalState() {
		setNoteAreaNormalState();
		refresh();
	}

	private void handleDateField(NativeEvent event) {
		final int keyCode = event.getKeyCode();
		if (keyCode == KeyCodes.KEY_ENTER && view.getDatePopupCalendar().isShowing()) {
			view.getDateField().getPopupCalendar().hide();
			event.stopPropagation();
			event.preventDefault();
			tryToSaveOnDateCalendarHide();
		}
	}

	private void tryToSaveOnDateCalendarHide() {
		Promises.releaseException(trySaveNoteArea());
	}

	private void refreshAttachmentPanelPresenter() {
		model.getAttachmentModel().getFiles().clear();
		model.getAttachmentModel().refreshModel(true);
		attachmentPanelPresenter.refresh();
	}

	private void refreshNameBox() {
		view.refreshNameBox();
	}

	private void refreshDescriptionBox() {
		view.refreshDescriptionBox();
	}

	private void createAttachmentPanel(AttachmentFactory attachmentFactory) {
		model.createAttachmentModel();
		attachmentPanelPresenter = attachmentFactory.createAttachmentPanelPresenter(model.getAttachmentModel(), view.createAttachmentView(),
				new DefaultAttachmentPanelSupervisor(fileService));
	}

	private void cancelUploads() {
		if (attachmentPanelPresenter != null && attachmentPanelPresenter.isUploading()) {
			attachmentPanelPresenter.cancelUploads();
		}
	}

	public void setNoteAreaNormalState() {
		setDefaultDateToDateBox();
		view.setNoteAreaNormalState();
	}

	/**
	 * Установить фокус в область ввода напоминания и перейти врежим редактирования
	 */
	public Promise<Void> focusNoteArea() {
		return tableSupervisor.collapseEditedNoteAndUpdate().thenConsume(aVoid -> {
			tableSupervisor.clearSelection();
			view.setNoteAreaFocusState();
		});
	}

	private Promise<Void> trySaveNoteArea() {
		if (savePromise != null && savePromise.isPending()) {
			return savePromise;
		}

		if (isNoteAreaValid()) {
			savePromise = FulfillablePromise.create();
			awaitCollapseNoteAreaWithSave().thenConsume(aVoid -> {
				tableSupervisor.fireRefreshCountersEvent();
				groupSupervisor.refreshValidNoteArea();
				view.setNoteAreaFocusState();
				savePromise.fulfill(null);
			}, throwable -> savePromise.reject(throwable));
			return savePromise;
		} else {
			groupSupervisor.refreshInvalidNoteArea();
		}

		return Promises.fulfilled();
	}

	private Promise<Void> awaitCollapseNoteAreaWithSave() {
		final String noteName = CommonUtils.preventNullString(view.getTrimmedNameText());
		final String noteDescription = CommonUtils.preventNullString(view.getTrimmedDescriptionText());

		final Note note = new Note();
		note.setStatus(NoteStatus.CREATED);
		note.setName(noteName);
		note.setDescription(noteDescription.trim());
		Date noteAreaCreationDate = getNoteAreaCreationDate();
		note.setDateNote(noteAreaCreationDate);
		note.setFiles(new ArrayList<>(model.getFiles()));

		return model.createNote(note, groupSupervisor.getCreationContextFromNote()).then(aVoid -> insertNoteAfterCreate(note));
	}

	private Date getNoteAreaCreationDate() {
		Date viewDate = view.getViewDate();
		if (viewDate != null) {
			return DateUtils.isMidnight(viewDate) ? DateUtils.roundToDay(viewDate) : viewDate;
		}
		return groupSupervisor.getNoteCreationTime();
	}

	/**
	 * вставит напоминание после создания
	 */
	public Promise<Void> insertNoteAfterCreate(Note note) {
		return model.insertNoteAfterCreate(note).thenConsume(addNotesEvent -> {
			refreshAfterNoteCreate();
			tableSupervisor.insertNotesAfterCreate(addNotesEvent);
		});
	}

	private boolean isNoteAreaValid() {
		return !view.getTrimmedNameText().isEmpty();
	}

	private boolean isNoteAreaNameBoxElement(NativeEvent nativeEvent) {
		return view.getName().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private boolean isNoteAreaDescriptionBoxElement(NativeEvent nativeEvent) {
		return view.getDescription().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private boolean isNoteAreaDateField(NativeEvent nativeEvent) {
		return view.getDateField().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private boolean isNoteAreaTimeField(NativeEvent nativeEvent) {
		return view.getTimeField().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private boolean isNoteAreaHideIcon(NativeEvent nativeEvent) {
		return view.getClearBtn().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private boolean isExpandIcon(NativeEvent nativeEvent) {
		return view.getExpandIcon().getElement().isOrHasChild(getTargetElement(nativeEvent));
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

	private boolean isNotesAttachmentFilesPopupElement(NativeEvent nativeEvent) {
		return isNotesAttachmentFilesPopupElement(getTargetElement(nativeEvent));
	}

	private boolean isNotesAttachmentFilesPopupElement(Element targetElement) {
		return targetElement != null && JQueryUtils.hasClosest(targetElement, GlobalConstants.DOT + DotActionView.DOT_POPUP_STYLE);
	}

	private boolean isNotesViewerPopupElement(Element targetElement) {
		return targetElement != null && JQueryUtils.hasClosest(targetElement, GlobalConstants.DOT + GlobalConstants.VIEWER_STYLE_NAME);
	}

	private boolean isNoteAttachmentPanelView(NativeEvent nativeEvent) {
		return view.getAttachmentView().getElement().isOrHasChild(getTargetElement(nativeEvent));
	}

	private Element getTargetElement(NativeEvent nativeEvent) {
		final EventTarget eventTarget = nativeEvent.getEventTarget();
		if (Element.is(eventTarget)) {
			return Element.as(eventTarget);
		}
		return null;
	}

	public boolean isEdited() {
		return view.isFocusedStyle();
	}

	private Date getViewDate() {
		return view.getViewDate();
	}

	private Date getViewTime() {
		return view.getViewTime();
	}

	/**
	 * Обновление после создания напоминаия, написано отдельное, для корректной работы с фокусом и порядком очистки полей
	 */
	public void refreshAfterNoteCreate() {
		setDefaultDateToDateBox();
		view.setDescriptionVisible(false);
		cancelUploads();
		refreshAttachmentPanelPresenter();
		refreshDescriptionBox();
		view.refreshOpenedStile();
		view.updatePlaceholder(true);
		refreshNameBox();
		view.refreshIconsVisibility();
	}

	void setDefaultDateToDateBox() {
		Date defaultTime = groupSupervisor.getNoteCreationTime();
		view.setDateValue(defaultTime);
		view.setTimeValue(defaultTime);
		view.refreshDateExpired(defaultTime);
	}
}
