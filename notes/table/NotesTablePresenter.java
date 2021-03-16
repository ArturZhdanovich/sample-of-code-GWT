package app.m8.web.client.view.calendarmodule.notes.table;

import app.components.client.DomUtils;
import app.components.client.JQueryUtils;
import app.components.client.RootPanelProvider;
import app.components.client.RootPanelProviderImpl;
import app.components.client.animation.AnimationUtils;
import app.components.client.animation.css.AnimationType;
import app.components.client.animation.css.SelfCleaningCSSAnimation;
import app.components.client.animation.slide.SlideAnimation;
import app.components.client.animation.slide.SlideType;
import app.components.client.animation.velocity.BeginAnimationCallback;
import app.components.client.animation.velocity.CompleteAnimationCallback;
import app.components.client.base.DateField;
import app.components.client.base.SimpleTimeWidget;
import app.components.client.dotpopup.DotActionView;
import app.components.client.event.MouseOverOutHandlerAdapter;
import app.components.client.mvp.AbstractCommonPresenter;
import app.components.client.mvp.promise.Callback;
import app.components.client.mvp.promise.FulfillablePromise;
import app.components.client.mvp.promise.Promise;
import app.components.client.mvp.promise.Promises;
import app.components.client.notifications.NotificationEvent;
import app.components.client.util.DateUtils;
import app.m8.web.client.callback.NotifyingDoneCallback;
import app.m8.web.client.message.MessageFactory;
import app.m8.web.client.view.calendarmodule.calendar.EventDraggingInNotesSupervisor;
import app.m8.web.client.view.calendarmodule.calendar.NoteToGridPostProcessor;
import app.m8.web.client.view.calendarmodule.calendar.controllers.ClickControllerAdapter;
import app.m8.web.client.view.calendarmodule.calendar.util.CalendarConstants;
import app.m8.web.client.view.calendarmodule.notes.animation.NotesAnimationUtils;
import app.m8.web.client.view.calendarmodule.notes.dnd.EventInNotesSortContext;
import app.m8.web.client.view.calendarmodule.notes.dnd.NotesSortContext;
import app.m8.web.client.view.calendarmodule.notes.events.AddNotesEvent;
import app.m8.web.client.view.calendarmodule.notes.events.MoveNoteAfterSortEvent;
import app.m8.web.client.view.calendarmodule.notes.events.RemoveNotesEvent;
import app.m8.web.client.view.calendarmodule.notes.events.UpdateNotesDateChangedEvent;
import app.m8.web.client.view.calendarmodule.notes.events.UpdateNotesWithoutDateChangedEvent;
import app.m8.web.client.view.calendarmodule.notes.group.NotesGroupPresenter;
import app.m8.web.client.view.calendarmodule.notes.group.NotesGroupSupervisor;
import app.m8.web.client.view.calendarmodule.notes.note.NotePresenter;
import app.m8.web.client.view.calendarmodule.notes.note.NoteUIState;
import app.m8.web.client.view.calendarmodule.notes.note.area.NoteAreaPresenter;
import app.m8.web.client.view.calendarmodule.notes.note.area.NoteAreaView;
import app.m8.web.client.view.calendarmodule.notes.selection.SingleNoteSelector;
import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.client.view.calendarmodule.notes.table.footer.FooterNotesView;
import app.m8.web.shared.GlobalConstants;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.util.Noop;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Представление таблицы заметок
 */
public class NotesTablePresenter extends AbstractCommonPresenter<NotesTableModel, NotesTableView> implements NotesTableSupervisor, EventFromNoteSupervisor {

	@Inject
	private SingleNoteSelector noteSelector;

	private final NotesTableFactory notesTableFactory;
	private final RootPanelProvider rootPanelProvider;
	private final FooterNotesView footerNotesView = new FooterNotesView();
	/**
	 * Карта для представлений групп заметок, ключом является дата группы
	 */
	private final SortedMap<Date, NotesGroupPresenter> groupPresentersMap;
	/**
	 * Презентер группы в которую попадаеют заметки после создания.
	 */
	private final NotesGroupPresenter syncGroupPresenter;
	/**
	 * Редактируемая заметка
	 */
	private NotePresenter editedNotePresenter;
	/**
	 * Интерфейс для регистрации обработчиков ДнД заметки на календарную сетку
	 */
	private NoteToGridPostProcessor noteToGridPostProcessor = (notePresenter, eventFromNoteSupervisor) -> {
		//Заглушка
	};
	/**
	 * Интерфейс для обработки встечи после дропа в напоминания
	 */
	private EventDraggingInNotesSupervisor eventDraggingInNotesSupervisor;
	/**
	 * Интерфейс для определенния используется ли сейчас данный инстанс таблицы напоминаний
	 * */
	private final NoteTableSupervisor noteTableSupervisor;

	@AssistedInject
	public NotesTablePresenter(NotesTableFactory notesTableFactory,
							   NotesTableModel model,
							   NotesTableView view,
							   @Assisted NoteTableSupervisor noteTableSupervisor) {
		super(model, view);
		this.noteTableSupervisor = noteTableSupervisor;
		this.notesTableFactory = notesTableFactory;
		this.rootPanelProvider = new RootPanelProviderImpl();
		this.groupPresentersMap = new TreeMap<>();
		this.syncGroupPresenter = createNoteGroupPresenter(getSynchNotesGroupModel());
	}

	@Override
	protected void initialize() {
		view.getNotesHeaderPanel().clear();
		view.getNotesHeaderPanel().add(syncGroupPresenter.getView());
		view.getNotesHeaderPanel().add(footerNotesView);
	}

	@Override
	protected void onAttachView() {
		distributeCreatedNotes(false);
	}

	@Override
	public void refresh() {
		clearGroups();
		syncGroupPresenter.refresh();
		createNotesGroups();
		refreshNotesGroupsVisibility();
		refreshSyncBtnVisibility();
	}

	protected void refreshSyncBtnVisibility() {
		footerNotesView.setSyncBtnVisible(!syncGroupPresenter.isEmpty());
	}

	@Override
	protected void attachHandlers() {
 		registerHandler(rootPanelProvider.addBodyPanelClickHandler(createRootPanelClickHandler()));
		registerHandler(rootPanelProvider.addBodyPanelKeyDownHandler(createRootPanelKeyDownHandler()));
		registerHandler(model.getGlobalEventBus().addHandler(AddNotesEvent.TYPE, event -> {
			if (!event.getSource().equals(model) && noteTableSupervisor.isTableVisible()) {
				clearSelection();
				insertNotesAfterCreate(event);
				fireRefreshCountersEvent();
			}
		}));
		registerHandler(model.getGlobalEventBus().addHandler(MoveNoteAfterSortEvent.TYPE, event -> {
			if (!event.getSource().equals(model) && noteTableSupervisor.isTableVisible()) {
				clearSelection();
				Promises.releaseException(moveNoteAfterSort(event).then(runSuccessCallback()));
			}
		}));
		MouseOverHandler printMouseOverHandler = mouseOverEvent -> getSyncGroupNoteAreaView().setStyleName(GlobalConstants.HOVERED_STYLE,
				!model.isEmptyTable());
		MouseOutHandler printMouseOutHandler = mouseOutEvent -> getSyncGroupNoteAreaView().removeStyleName(GlobalConstants.HOVERED_STYLE);
		registerHandler(getSyncGroupNoteAreaView().addPrintMouseOverHandler(printMouseOverHandler));
		registerHandler(getSyncGroupNoteAreaView().addPrintMouseOutHandler(printMouseOutHandler));
		registerHandler(getFooterNotesView().addSyncBtnClickHandler(event -> {
			DomUtils.shutUpEvent(event);
			distributeCreatedNotes(true);
		}));
		final MouseOverOutHandlerAdapter mouseOverOutHandlerAdapter = new MouseOverOutHandlerAdapter() {

			@Override
			public void onMouseOverOut(boolean over) {
				syncGroupPresenter.getView().setStyleName(GlobalConstants.HOVERED_STYLE, over);
			}
		};
		registerHandler(getFooterNotesView().getSyncBtn().addDomHandler(mouseOverOutHandlerAdapter, MouseOverEvent.getType()));
		registerHandler(getFooterNotesView().getSyncBtn().addDomHandler(mouseOverOutHandlerAdapter, MouseOutEvent.getType()));
		registerHandler(getView().addDomHandler(printMouseOverHandler, MouseOverEvent.getType()));
		registerHandler(getView().addDomHandler(printMouseOutHandler, MouseOutEvent.getType()));
		registerHandler(model.getGlobalEventBus().addHandler(UpdateNotesDateChangedEvent.TYPE, event -> {
			if (!event.getSource().equals(model) && noteTableSupervisor.isTableVisible()) {
				clearSelection();
				Promises.releaseException(updateNotesDateChanged(event).then(runSuccessCallback()));
			}
		}));
		registerHandler(model.getGlobalEventBus().addHandler(UpdateNotesWithoutDateChangedEvent.TYPE, event -> {
			if (!event.getSource().equals(model) && noteTableSupervisor.isTableVisible()) {
				clearSelection();
				Promises.releaseException(updateNotesWithoutDateChanged(event).then(runSuccessCallback()));
			}
		}));
		registerHandler(model.getGlobalEventBus().addHandler(RemoveNotesEvent.TYPE, event -> {
			if (!event.getSource().equals(model) && noteTableSupervisor.isTableVisible()) {
				clearSelection();
				Promises.releaseException(removeNotes(event).then(runSuccessCallback()));
			}
		}));
	}

	@Override
	public boolean isSorting() {
		return model.isSorting();
	}

	public boolean isEditedNoteExists() {
		return isEditedNotePresenterExists() || syncGroupPresenter.isCreateAreaEdited();
	}

	@Override
	public boolean isAllowDropInSyncGroup() {
		return model.isAllowDropInSyncGroup();
	}

	@Override
	public boolean isPreventNoteNativeClick(NativeEvent nativeEvent) {
		return isTableBlockedState();
	}

	@Override
	public void setSelectedNote(NotePresenter notePresenter) {
		selectNote(notePresenter);
		Scheduler.get().scheduleDeferred(notePresenter::updateDatePopupPosition);
	}

	@Override
	public void setEditNotePresenter(NotePresenter notePresenter) {
		if (editedNotePresenter != null && !editedNotePresenter.getNote().getId().equals(notePresenter.getNote().getId())) {
			cancelEditedNoteUpdate();
		}
		this.editedNotePresenter = notePresenter;
	}

	@Override
	public void onNoteCompleteBtnClick(NotePresenter notePresenter) {
		Promises.releaseException(awaitNoteCompleted(notePresenter).then(runSuccessCallback()));
	}

	@Override
	public Promise<Void> collapseEditedNoteAndUpdate() {
		if (isEditedNotePresenterExists()) {
			return awaitCollapseEditedNoteAndUpdate().then(runSuccessCallback());
		}
		return Promises.fulfilled();
	}

	@Override
	public Promise<Void> saveAllEditedNotesAndUpdate() {
		return collapseEditedNoteAndUpdate().then(aVoid -> syncGroupPresenter.saveAllNotes());
	}

	@Override
	public void moveNoteOnSort(NotesSortContext context) {
		Promises.releaseException(awaitMoveNoteOnSort(context).then(aVoid -> moveNoteAfterSort(model.moveNoteAfterSort(context))).then(runSuccessCallback()));
	}

	private void selectNote(NotePresenter notePresenter) {
		if (notePresenter.getView().isAttached()
				&& notePresenter.getView().isVisible()) {
			noteSelector.select(notePresenter);
		}
	}

	@Override
	public void clearSelection() {
		noteSelector.clearSelection();
	}

	@Override
	public void setSorting(boolean isSorting) {
		model.setSorting(isSorting);
	}

	@Override
	public void setAllowDropInCreatedGroup(boolean isAllow) {
		model.setAllowDropInCreatedGroup(isAllow);
	}

	@Override
	public void setNoteToGridPostProcess(NotePresenter notePresenter) {
		noteToGridPostProcessor.postProcessNote(notePresenter, this);
	}

	@Override
	public void fireRefreshCountersEvent() {
		model.fireRefreshCountersEvent();
	}

	@Override
	public Promise<Void> insertNewNoteAfterTaskDrop(NotesSortContext context) {
		return awaitMoveNoteOnSort(context)
				.then(aVoid -> moveNoteAfterEventDrop(model.moveNoteAfterSort(context)))
				.then(runSuccessCallback());
	}

	@Override
	public Promise<Void> insertNewNoteAfterEventDrop(EventInNotesSortContext context) {
		return awaitMoveNoteOnSort(context)
				.then(aVoid -> {
					eventDraggingInNotesSupervisor.postProcessDropEventInNotes(context.getEventId(), context.getEventDate());
					return moveNoteAfterEventDrop(model.moveNoteAfterSort(context));
				})
				.then(runSuccessCallback());
	}


	@Override
	public Promise<Note> createNoteFromEvent(final Note note, Integer eventId, Integer staffId) {
		return model.getNoteRegistry().createNoteFromEvent(note, eventId, staffId);
	}

	@Override
	public boolean isEventDraggingInProcess() {
		return eventDraggingInNotesSupervisor != null && eventDraggingInNotesSupervisor.isEventDraggingInProcess();
	}

	private Promise<Void> awaitMoveNoteOnSort(final NotesSortContext context) {
		setNotesTableBlockingState(true);
		clearSelection();

		final Note sortedNote = context.getSortedNote();
		final Note relativeNote = context.getRelativeNote();
		return model.moveNote(sortedNote, relativeNote.getDateNote(), relativeNote.getId(), context.isDown());
	}

	@Override
	public void moveOnDate(Date groupDate, List<Note> notesInGroup, Date dateTo) {
		model.moveOnDate(groupDate, notesInGroup, dateTo)
				.thenConsume(aVoid -> {
					if (DateUtils.compareDate(groupDate, dateTo) != 0) {
						animateMoveNotesToDate(groupDate, dateTo);
					}
				})
				.then(runSuccessCallback());
	}

	@Override
	public void restoreOrgStructure(Integer restoredOrgId) {
		final String failureMessage = MessageFactory.getErrMessages().restoreDeletedNode(false, null);
		model.restoreOrgStructure(restoredOrgId).done(new NotifyingDoneCallback<Void>(model.getOrgTreeModel().getEventBus(), null, failureMessage) {

			@Override
			protected NotificationEvent afterFulfilled(Void value) {
				final String successMessage = MessageFactory.getErrMessages().restoreDeletedNode(true, model.getSelectedStructure().getName());
				return new NotificationEvent(true, successMessage);
			}
		});
	}

	@Override
	public void setFavorite(NotePresenter notePresenter) {
		final boolean setFavorite = !notePresenter.getNote().isFavorite();
		model.setFavorite(notePresenter.getNote(), setFavorite).thenConsume(aVoid -> notePresenter.setFavorite(setFavorite)).then(runSuccessCallback());
	}

	@Override
	public Promise<Void> createNote(Note note) {
		return model.createNote(note);
	}

	private void animateMoveNotesToDate(Date groupDate, Date dateTo) {
		setNotesTableBlockingState(true);
		final NotesGroupPresenter fromGroupPresenter = getNotesGroupPresenterByDate(groupDate);
		final NotesGroupPresenter toGroupPresenter = getNotesGroupPresenterByDate(dateTo);

		final Element noteElement = fromGroupPresenter.getView().getNotesContainer().getElement();
		final Element noteElementClone = Element.as(noteElement.cloneNode(true));
		getView().getElement().getStyle().setWidth(getView().getOffsetWidth(), Style.Unit.PX);
		AnimationUtils.hideAnimationHeight(fromGroupPresenter.getView().getNotesContainer().getElement(), elements -> {
			fromGroupPresenter.refresh();
			refreshNotesGroupsVisibility();
			noteElementClone.getStyle().setOpacity(0);
			toGroupPresenter.getView().getNotesContainer().getElement().appendChild(noteElementClone);
			AnimationUtils.fadeInElement(noteElementClone, GlobalConstants.COUNT_ANIMATION_DURATION_250, elements1 -> {
				noteElementClone.removeFromParent();
				toGroupPresenter.refresh();
				refreshSyncBtnVisibility();
				refreshNotesGroupsVisibility();
				setNotesTableBlockingState(false);
				getView().getElement().getStyle().clearWidth();
			}, GlobalConstants.BLOCK_STYLE);
		});
	}

	private Promise<Void> moveNoteAfterEventDrop(MoveNoteAfterSortEvent event) {
		final int toIndex = event.getContext().getToIndex();

		final NotesGroupPresenter toGroupPresenter = getNotesGroupPresenterByNote(event.getContext().getRelativeNote());
		final NotePresenter notePresenter = toGroupPresenter.createNotePresenter(event.getContext().getSortedNote());
		notePresenter.refresh();
		toGroupPresenter.insertNotePresenter(toIndex, notePresenter);
		toGroupPresenter.setNotePresenterVisible(notePresenter);
		toGroupPresenter.getView().insertNoteView(notePresenter.getView(), toIndex);

		refreshNotesGroupsVisibility();
		final FulfillablePromise<Void> promise = FulfillablePromise.create();
		final Element noteElement = notePresenter.getView().asWidget().getElement();
		final JsArray<Element> insertedElements = JavaScriptObject.createArray().cast();
		final JsArrayInteger elementHeights = JavaScriptObject.createArray().cast();

		insertedElements.push(noteElement);
		elementHeights.push(noteElement.getOffsetHeight());

		Collection<Promise<Void>> promises = new ArrayList<>();
		for (int i = 0; i < insertedElements.length(); i++) {
			SlideAnimation slideAnimation = new SlideAnimation(insertedElements.get(i));
			promises.add(slideAnimation.run(SlideType.SLIDE_IN, GlobalConstants.COUNT_ANIMATION_DURATION_250).thenConsume(Noop::noop));
		}

		Promises.wait(promises).thenConsume(r -> {
			noteElement.getStyle().clearHeight();
			animateYellowBlink(notePresenter);
			focusNoteArea();
			setNotesTableBlockingState(false);
			promise.fulfill(null);
		});
		return promise;
	}

	protected void setNotesTableBlockingState(boolean isBlock) {
		final boolean isStateChanged = model.setNotesTableBlockedState(isBlock);
		if (isStateChanged) {
			refreshNotesTableBlocked();
		}
	}

	@Override
	public NoteAreaPresenter createNoteAreaPresenter(NotesGroupSupervisor groupSupervisor) {
		return notesTableFactory.createNoteAreaPresenter(model, this, groupSupervisor);
	}

	private NotesGroupPresenter getNotesGroupPresenterByNote(Note note) {
		return getNotesGroupPresenterByNote(note, note.getDateNote());
	}

	private NotesGroupPresenter getNotesGroupPresenterByNote(Note note, Date groupDate) {
		return note.isCreated() ? syncGroupPresenter : getNotesGroupPresenterByDate(groupDate);
	}

	@Override
	public void insertNotesAfterCreate(AddNotesEvent event) {
		setNotesTableBlockingState(true);
		final List<NotePresenter> insertedNotePresenterList = new ArrayList<>();
		event.getAddNoteEventDataList().forEach(addNoteEventData -> {
			final Note insertedNote = addNoteEventData.getNote();
			final int toIndex = addNoteEventData.getToIndex();
			final NotesGroupPresenter toGroupPresenter = getNotesGroupPresenterByNote(insertedNote);
			final NotePresenter notePresenter = toGroupPresenter.createNotePresenter(insertedNote);
			notePresenter.refresh();
			toGroupPresenter.insertNotePresenter(toIndex, notePresenter);
			toGroupPresenter.getView().insertNoteView(notePresenter.getView(), toIndex);
			insertedNotePresenterList.add(notePresenter);
		});
		refreshNotesGroupsVisibility();
		insertedNotePresenterList.forEach(notePresenter -> {
			if (!notePresenter.getNote().isCreated()) {
				animateYellowBlink(notePresenter);
			}
		});
		clearSelection();
		setNotesTableBlockingState(false);
	}

	@Override
	public NotesGroupModel getNotesGroupModelByDate(Date date) {
		return model.getNotesGroupModelByDate(date);
	}

	@Override
	public NotesGroupModel getSynchNotesGroupModel() {
		return model.getSynchNotesGroupModel();
	}

	public NotesGroupPresenter getSyncGroupPresenter() {
		return syncGroupPresenter;
	}

	private ClickControllerAdapter createRootPanelClickHandler() {
		return new ClickControllerAdapter() {

			@Override
			public boolean isPreventNativeClick(NativeEvent nativeEvent) {
				return isPreventClick(nativeEvent);
			}

			@Override
			public void doOnClick(ClickEvent event) {
				if (isEditedNotePresenterExists()) {
					awaitCollapseEditedNoteAndUpdate().done(() -> runSuccessCallback());
				} else {
					clearSelection();
				}
			}

			@Override
			public boolean isOpenLinkAndPreventNativeClick(NativeEvent nativeEvent) {
				return isNoteElement(nativeEvent);
			}

			private boolean isPreventClick(NativeEvent nativeEvent) {
				return isNoteElement(nativeEvent) && !AnchorElement.is(nativeEvent.getEventTarget())
						|| isNoteDatePopupElement(nativeEvent)
						|| isNotesAttachmentFilesPopupElement(nativeEvent)
						|| isTableBlockedState();
			}
		};
	}

	private KeyDownHandler createRootPanelKeyDownHandler() {
		return event -> {
			if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
				if (isEditedNotePresenterExists() && !isTableBlockedState()) {
					cancelEditedNoteUpdate();
				}
			}
		};
	}


	protected void clearGroups() {
		groupPresentersMap.clear();
		view.clear();
	}

	private void createNotesGroups() {
		model.getNoteRegistry().getNotesGroupModelCollection().forEach(notesGroupModel -> {
			final NotesGroupPresenter groupPresenter = createNoteGroupPresenter(notesGroupModel);
			groupPresenter.refresh();
			refreshSyncBtnVisibility();
			groupPresentersMap.put(notesGroupModel.getGroupDate(), groupPresenter);
			view.addNotesGroup(groupPresenter.getView());
		});
	}

	private NotesGroupPresenter createNoteGroupPresenter(NotesGroupModel groupModel) {
		return notesTableFactory.createNotesGroupPresenter(groupModel, this);
	}

	private void refreshNotesTableBlocked() {
		if (model.isTableBlockedState()) {
			setNotesTableBlock();
		} else {
			removeNotesTableBlock();
		}
	}

	protected void refreshNotesGroupsVisibility() {
		refreshSyncBtnVisibility();
		getNotesGroupPresenterCollection().forEach(NotesGroupPresenter::refreshVisibility);
	}

	private void removeNotesTableBlock() {
		getView().getNotesTableContainer().removeStyleName(GlobalConstants.DISABLE_EVENTS_STYLE);
		getNotePresenters().forEach(notePresenter -> notePresenter.getView().removeStyleName(GlobalConstants.DISABLE_EVENTS_STYLE));
	}

	private void setNotesTableBlock() {
		removeNotesTableBlock();
		getView().getNotesTableContainer().addStyleName(GlobalConstants.DISABLE_EVENTS_STYLE);
	}

	/**
	 * Обработчик обновления заметки при выходе из режима редактирования
	 *
	 * @return Promise - промис
	 */
	private Promise<Void> awaitCollapseEditedNoteAndUpdate() {

		String nameToUpdate = null;
		String descriptionToUpdate = null;
		Date dateToUpdate = null;

		final boolean isNameBoxEmpty = editedNotePresenter.isNameBoxEmpty();
		final boolean isSameEditedNoteDate = editedNotePresenter.isSameEditedNoteDate();
		final boolean isOnlyTimeChanged = editedNotePresenter.isOnlyTimeChanged();

		if (!editedNotePresenter.isSameEditedNoteName()) {
			nameToUpdate = editedNotePresenter.getTrimmedNameText();
		}
		if (!editedNotePresenter.isSameEditedNoteDescription()) {
			descriptionToUpdate = editedNotePresenter.getTrimmedDescriptionText();
		}
		if (!isSameEditedNoteDate) {
			dateToUpdate = editedNotePresenter.getViewDate();
		}
		final boolean isPreventUpdate = nameToUpdate == null
				&& descriptionToUpdate == null
				&& (dateToUpdate == null && isSameEditedNoteDate);

		if (isNameBoxEmpty || isPreventUpdate) {
			cancelEditedNoteUpdate();
			return Promises.fulfilled();
		}

		final Note note = editedNotePresenter.getNote();
		final Date date = note.getDateNote();
		final String nameToUpdateFinal = nameToUpdate;
		final String descriptionToUpdateFinal = descriptionToUpdate;
		final Date dateToUpdateFinal = isSameEditedNoteDate && dateToUpdate == null ? date : dateToUpdate;
		editedNotePresenter.setNoteState(NoteUIState.NORMAL);
		clearEditedNotePresenter();
		return model.updateNote(note, nameToUpdateFinal, descriptionToUpdateFinal, dateToUpdateFinal).then(result ->
				!isSameEditedNoteDate ? moveNoteAfterDateChanged(result, date, isOnlyTimeChanged) : updateNoteWithoutDateChanged(result));
	}

	private Promise<Void> moveNoteAfterDateChanged(Note note, Date oldDate, boolean isOnlyTimeChanged) {
		return updateNotesDateChanged(model.moveNoteAfterDateChanged(note, oldDate, isOnlyTimeChanged));
	}

	private Promise<Void> updateNoteWithoutDateChanged(Note note) {
		return updateNotesWithoutDateChanged(model.updateNoteWithoutDateChanged(note));
	}

	private Promise<Void> moveNoteAfterSort(MoveNoteAfterSortEvent event) {
		setNotesTableBlockingState(true);
		clearSelection();

		NotesSortContext context = event.getContext();
		final int fromIndex = context.getFromIndex();
		final int toIndex = context.getToIndex();

		final NotesGroupPresenter fromGroupPresenter = getNotesGroupPresenterByNote(context.getSortedNote(), context.getFromGroupModel().getGroupDate());
		final NotesGroupPresenter toGroupPresenter = getNotesGroupPresenterByNote(context.getRelativeNote());
		final NotePresenter notePresenter = fromGroupPresenter.getNotePresenter(fromIndex);
		if (!context.getRelativeNote().isCreated() && context.getSortedNote().isCreated()) {
			context.getSortedNote().setStatus(context.getRelativeNote().getStatus());
		}
		notePresenter.refresh();
		fromGroupPresenter.removeNotePresenter(fromIndex);
		toGroupPresenter.insertNotePresenter(toIndex, notePresenter);
		toGroupPresenter.setNotePresenterVisible(notePresenter);
		toGroupPresenter.getView().insertNoteView(notePresenter.getView(), toIndex);

		refreshNotesGroupsVisibility();
		return Promises.fulfilled();
	}

	public Promise<Void> awaitNoteRemove(Note note) {
		final NotesGroupPresenter groupPresenter = getNotesGroupPresenterByNote(note);
		final NotePresenter removedNotePresenter = groupPresenter.findNotePresenterByNote(note);
		return removeNote(removedNotePresenter).then(runSuccessCallback());
	}

	/**
	 * Смена статуса на "выполнено"
	 */
	private Promise<Void> awaitNoteCompleted(final NotePresenter notePresenter) {
		setNotesTableBlockingState(true);
		clearSelection();
		return model.completeNote(notePresenter.getNote().getId()).then(aVoid -> removeNote(notePresenter));
	}

	private Promise<Void> removeNote(NotePresenter notePresenter) {
		return removeNotes(model.removeNote(notePresenter.getNote()));
	}

	protected Callback<Void, Void> runSuccessCallback() {
		return new Callback<Void, Void>() {

			@Override
			public Promise<Void> onFulfilled(Void result) {
				setNotesTableBlockingState(false);
				fireRefreshCountersEvent();
				return Promises.fulfilled();
			}
		};
	}

	private void clearEditedNotePresenter() {
		editedNotePresenter = null;
	}

	private void cancelEditedNoteUpdate() {
		if (!editedNotePresenter.getModel().isNormalState()) {
			setNotesTableBlockingState(true);
			editedNotePresenter.ensureNoteStateChange(NoteUIState.NORMAL).thenConsume(t -> editedNotePresenter.refresh());
			clearEditedNotePresenter();
			setNotesTableBlockingState(false);
		}
	}

	public void setNoteToGridPostProcessor(NoteToGridPostProcessor noteToGridPostProcessor) {
		this.noteToGridPostProcessor = noteToGridPostProcessor;
	}

	public void setEventDraggingInNotesSupervisor(EventDraggingInNotesSupervisor eventDraggingInNotesSupervisor) {
		this.eventDraggingInNotesSupervisor = eventDraggingInNotesSupervisor;
	}

	private boolean isTableBlockedState() {
		return model.isTableBlockedState();
	}

	private boolean isEditedNotePresenterExists() {
		return editedNotePresenter != null && editedNotePresenter.getView().isAttached();
	}

	private boolean isEmptyTable() {
		return model.isEmptyTable();
	}

	private boolean isNoteElement(NativeEvent nativeEvent) {
		return isNoteElement(getTargetElement(nativeEvent));
	}

	private boolean isNoteElement(Element targetElement) {
		return targetElement != null
				&& JQueryUtils.getClosest(targetElement, GlobalConstants.DOT + CalendarConstants.NOTE_CONTAINER_JS_STYLE) != null;
	}

	private boolean isNoteDatePopupElement(NativeEvent nativeEvent) {
		final Element targetElement = getTargetElement(nativeEvent);
		return isNoteDatePopupElement(targetElement) || isTimeDatePopupElement(targetElement);
	}

	private boolean isNoteDatePopupElement(Element targetElement) {
		return targetElement != null
				&& JQueryUtils.getClosest(targetElement, GlobalConstants.DOT + DateField.TRIPPLE_DATE_PICKER_STATIC_STYLE) != null;
	}

	private boolean isTimeDatePopupElement(Element targetElement) {
		return targetElement != null
				&& JQueryUtils.getClosest(targetElement, GlobalConstants.DOT + SimpleTimeWidget.SIMPLE_TIME_RANGE_PANEL) != null;
	}

	private boolean isNotesAttachmentFilesPopupElement(NativeEvent nativeEvent) {
		return isNotesAttachmentFilesPopupElement(getTargetElement(nativeEvent));
	}

	private boolean isNotesAttachmentFilesPopupElement(Element targetElement) {
		return targetElement != null
				&& JQueryUtils.getClosest(targetElement, GlobalConstants.DOT + DotActionView.DOT_POPUP_STYLE) != null;
	}

	private Element getTargetElement(NativeEvent nativeEvent) {
		final EventTarget eventTarget = nativeEvent.getEventTarget();
		if (Element.is(eventTarget)) {
			return Element.as(eventTarget);
		}
		return null;
	}

	public List<NotePresenter> getNotePresenters() {
		if (isEmptyTable()) {
			return Collections.emptyList();
		}
		return getGroupsNotePresenters();
	}

	private List<NotePresenter> getGroupsNotePresenters() {
		final LinkedList<NotePresenter> groupsNotePresenters = new LinkedList<>();
		for (NotesGroupPresenter groupPresenter : getNotesGroupPresenterCollection()) {
			groupsNotePresenters.addAll(groupPresenter.getNotePresenters());
		}
		return groupsNotePresenters;
	}

	private NotesGroupPresenter getNotesGroupPresenterByDate(Date noteDate) {
		Date roundDate = DateUtils.roundToDay(new Date(noteDate.getTime()));
		if (DateUtils.isToday(roundDate)) {
			roundDate = DateUtils.getToday();
		}
		if (DateUtils.compareDate(roundDate, DateUtils.getDate()) < 0) {
			roundDate = DateUtils.getYesterday();
		}
		if (!groupPresentersMap.containsKey(roundDate)) {
			NotesGroupPresenter noteGroupPresenter = createNoteGroupPresenter(model.getNotesGroupModelByDate(noteDate));
			groupPresentersMap.put(roundDate, noteGroupPresenter);
			int index = groupPresentersMap.headMap(roundDate).size();
			noteGroupPresenter.refreshHeader();
			if (noteGroupPresenter.getModel().isSynchGroup()) {
				refreshSyncBtnVisibility();
			}
			view.insertNotesGroup(noteGroupPresenter.getView(), index);
		}
		return groupPresentersMap.get(roundDate);
	}

	private Collection<NotesGroupPresenter> getNotesGroupPresenterCollection() {
		return groupPresentersMap.values();
	}

	public NoteAreaView getSyncGroupNoteAreaView() {
		return syncGroupPresenter.getNoteAreaView();
	}

	public void focusNoteArea() {
		Promises.releaseException(syncGroupPresenter.focusNoteArea());
	}

	/**
	 * Распределить созденные напоминания по группам
	 *
	 * @return Promise - промис
	 */
	public Promise<Void> distributeCreatedNotes(boolean withAnimation) {
		setNotesTableBlockingState(true);
		return saveAllEditedNotesAndUpdate().thenConsume(aVoid -> {
			List<Note> synchroniseGroupNoteModels = getModel().getNoteRegistry().getSynchNotesGroupModel().getNotes();

			synchroniseGroupNoteModels.forEach(note -> {
				model.distributeCreatedNote(note);
				insertNoteAfterDistribute(note, withAnimation);
				syncGroupPresenter.removeNotePresenter(note);
			});
			refreshNotesGroupsVisibility();
			setNotesTableBlockingState(false);
			Scheduler.get().scheduleDeferred(this::focusNoteArea);
		});
	}

	private void insertNoteAfterDistribute(Note insertedNote, boolean withAnimation) {
		final NotesGroupModel toGroupModel = getNotesGroupModelByDate(insertedNote.getDateNote());
		final NotesGroupPresenter toGroupPresenter = getNotesGroupPresenterByDate(toGroupModel.getGroupDate());
		final int toIndex = toGroupModel.getNoteIndex(insertedNote);
		final NotePresenter notePresenter = toGroupPresenter.createNotePresenter(insertedNote);
		notePresenter.refresh();
		toGroupPresenter.insertNotePresenter(toIndex, notePresenter);
		toGroupPresenter.getView().insertNoteView(notePresenter.getView(), toIndex);
		refreshNotesGroupsVisibility();
		if (withAnimation) {
			animateYellowBlink(notePresenter);
		}
	}

	public void animateYellowBlink(NotePresenter notePresenter) {
		SelfCleaningCSSAnimation.apply(notePresenter.getView().getContentWrapper(), AnimationType.YELLOW_BLINKING);
	}

	public Promise<Void> insertNoteAfterEventDrop(Note note) {
		return syncGroupPresenter.insertNoteAfterCreate(note);
	}

	public void setNewNote(String nameText, Date date) {
		getSyncGroupNoteAreaView().setNameText(nameText);
		getSyncGroupNoteAreaView().setDateValue(date);
		focusNoteArea();
	}

	private Promise<Void> updateNotesDateChanged(UpdateNotesDateChangedEvent event) {
		final JsArray<Element> insertedElements = JavaScriptObject.createArray().cast();
		final JsArray<Element> clonedElements = JavaScriptObject.createArray().cast();
		final JsArrayInteger elementHeights = JavaScriptObject.createArray().cast();
		List<NotePresenter> notePresenters = new ArrayList<>();
		List<Element> clonedElementList = new ArrayList<>();
		Set<NotesGroupPresenter> notesGroupPresenterSet = new HashSet<>();
		final FulfillablePromise<Void> promise = FulfillablePromise.create();

		event.getDataList().forEach(data -> {
			final NotesGroupModel fromGroupModel = data.getFromGroupModel();
			final int fromIndex = data.getFromIndex();
			final NotesGroupModel toGroupModel = data.getToGroupModel();
			final int toIndex = data.getToIndex();
			if (!fromGroupModel.equals(toGroupModel) || fromIndex != toIndex) {

				final NotesGroupPresenter fromGroupPresenter = getNotesGroupPresenterByNote(data.getNote(), data.getOldDate());
				final NotePresenter notePresenter = fromGroupPresenter.getNotePresenter(fromIndex);
				notePresenter.getModel().updateNote(data.getNote());
				notePresenter.refresh();

				final NotesGroupPresenter toGroupPresenter = getNotesGroupPresenterByNote(data.getNote(), toGroupModel.getGroupDate());
				// Позиция заметки после редактирования даты изменилась, нужна анимация
				// Если в таблице отображаются заметки одной даты, в данной таблице позиция гарантированно меняется
				final boolean isPositionChanged = !(fromGroupModel.equals(toGroupModel) && fromIndex == toIndex);
				if (isPositionChanged && notePresenter.getView().isAttached()) {
					final FlowPanel fromNotesContainer = fromGroupPresenter.getView().getNotesContainer();

					// Создание заглушки заметки
					final Element noteElement = notePresenter.getView().asWidget().getElement();
					final Element noteElementClone = (Element) noteElement.cloneNode(true);

					// Поиск относительного для вставки элемента заметки
					final Element relativeNextElement = noteElement.getNextSiblingElement();
					final Element relativePreviousElement = noteElement.getPreviousSiblingElement();
					final Boolean isInsertBefore = relativeNextElement != null
							? Boolean.TRUE : (relativePreviousElement != null ? Boolean.FALSE : null);

					// Удаление представления заметки из группы (+вью)
					fromGroupPresenter.removeNotePresenter(fromIndex);
					// Добавление представления заметки в группу (-вью)
					toGroupPresenter.insertNotePresenter(toIndex, notePresenter);
					// Для вставки вью определяем тип таблицы заметок
					// Берем необходимый контейнер и преобразованный индекс
					toGroupPresenter.getView().insertNoteView(notePresenter.getView(), toIndex);

					// Вставка заглушки заметки в контейнер,
					// если заметка не отображалась, значит заглушка не требуется
					if (notePresenter.getView().isVisible()) {
						if (isInsertBefore == null) {
							fromNotesContainer.getElement().insertFirst(noteElementClone);
						} else if (isInsertBefore) {
							fromNotesContainer.getElement().insertBefore(noteElementClone, relativeNextElement);
						} else {
							fromNotesContainer.getElement().insertAfter(noteElementClone, relativePreviousElement);
						}
					}
					toGroupPresenter.setNotePresenterVisible(notePresenter);

					// Актуальная высота после вставки
					final int noteElementHeight = noteElement.getOffsetHeight() != 0
							? noteElement.getOffsetHeight() : noteElementClone.getOffsetHeight();
					insertedElements.push(noteElement);
					clonedElements.push(noteElementClone);
					elementHeights.push(noteElementHeight);
					notePresenters.add(notePresenter);
					clonedElementList.add(noteElementClone);
					notesGroupPresenterSet.add(fromGroupPresenter);
					notesGroupPresenterSet.add(toGroupPresenter);
				}
			} else if (DateUtils.compareDate(data.getNote().getDateNote(), data.getOldDate()) == 0) {
				final NotesGroupPresenter fromGroupPresenter = getNotesGroupPresenterByNote(data.getNote(), data.getOldDate());
				final NotePresenter notePresenter = fromGroupPresenter.getNotePresenter(fromIndex);
				notePresenter.getModel().getNote().setDateNote(data.getNote().getDateNote());
				notePresenter.refreshDateBox();
			}
		});

		final BeginAnimationCallback beginAnimationCallback = elements -> {
			for (NotesGroupPresenter groupPresenter : notesGroupPresenterSet) {
				groupPresenter.refreshVisibility();
			}
		};

		final CompleteAnimationCallback completeAnimationCallback = elements -> {
			clonedElementList.forEach(Node::removeFromParent);
			notePresenters.forEach(notePresenter -> notePresenter.getView().asWidget().getElement().getStyle().clearHeight());
			refreshNotesGroupsVisibility();
			clearEditedNotePresenter();
			clearSelection();
			setNotesTableBlockingState(false);
			NotesAnimationUtils.runBlinkAnimation(notePresenters, blinkElements -> promise.fulfill(null));
		};

		if (insertedElements.length() == 0) {
			setNotesTableBlockingState(false);
			promise.fulfill(null);
		} else {
			// Запускаем анимацию перемещения заметки
			NotesAnimationUtils.runMoveAnimation(insertedElements, clonedElements, elementHeights, beginAnimationCallback, completeAnimationCallback);
		}

		return promise;
	}

	private Promise<Void> updateNotesWithoutDateChanged(UpdateNotesWithoutDateChangedEvent event) {
		final FulfillablePromise<Void> promise = FulfillablePromise.create();
		List<NotePresenter> notePresenters = new ArrayList<>();
		setNotesTableBlockingState(true);

		event.getNotes().forEach(note -> {
			final NotesGroupPresenter groupPresenter = getNotesGroupPresenterByNote(note);
			final NotePresenter notePresenter = groupPresenter.findNotePresenterByNote(note);
			int index = groupPresenter.getNotePresenters().indexOf(notePresenter);
			groupPresenter.removeNotePresenter(note);
			NotePresenter newNotePresenter = groupPresenter.createNotePresenter(note);
			groupPresenter.insertNotePresenter(index, newNotePresenter);
			groupPresenter.getView().insertNoteView(newNotePresenter.getView(), index);
			newNotePresenter.refresh();
			notePresenters.add(newNotePresenter);
		});

		NotesAnimationUtils.runBlinkAnimation(notePresenters, blinkElements -> {
			setNotesTableBlockingState(false);
			promise.fulfill(null);
		});

		return promise;
	}

	/**
	 * Удаление напоминаний
	 */
	public Promise<Void> removeNotes(RemoveNotesEvent event) {
		setNotesTableBlockingState(true);

		Map<NotesGroupPresenter, List<NotePresenter>> map = new HashMap<>();
		event.getNotes().forEach(note -> {
			final NotesGroupPresenter groupPresenter = getNotesGroupPresenterByNote(note);
			final NotePresenter removedNotePresenter = groupPresenter.findNotePresenterByNote(note);
			if (removedNotePresenter != null && removedNotePresenter.getView().isAttached()) {
				removedNotePresenter.deleteNoteFromStorage();
				if (isEditedNotePresenterExists()) {
					if (editedNotePresenter.setNoteState(NoteUIState.NORMAL)) {
						editedNotePresenter.refresh();
						clearEditedNotePresenter();
					}
				}
				final List<NotePresenter> notePresenters = map.computeIfAbsent(groupPresenter, presenter -> new ArrayList<>());
				notePresenters.add(removedNotePresenter);
			}
		});

		return removeNotesFromView(map);
	}

	private Promise<Void> removeNotesFromView(Map<NotesGroupPresenter, List<NotePresenter>> map) {
		setNotesTableBlockingState(true);

		final FulfillablePromise<Void> promise = FulfillablePromise.create();
		final JsArray<Element> animatedElement = JavaScriptObject.createArray().cast();

		map.forEach((key, value) -> value.forEach(notePresenter -> animatedElement.push(notePresenter.getView().getElement())));

		NotesAnimationUtils.runRemoveAnimation(animatedElement, elements -> {
			map.forEach((key, value) -> value.forEach(key::removeNotePresenter));
			refreshNotesGroupsVisibility();
			promise.fulfill(null);
		});
		return promise;
	}

	public FooterNotesView getFooterNotesView() {
		return footerNotesView;
	}
}