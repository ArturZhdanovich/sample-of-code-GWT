package app.m8.web.client.view.calendarmodule.notes.table;

import app.components.client.mvp.model.ModelWithEvents;
import app.components.client.mvp.promise.Promise;
import app.m8.web.client.OrgTreeModel;
import app.m8.web.client.view.calendarmodule.calendarpopup.CalendarPopupEventBus;
import app.m8.web.client.view.calendarmodule.notes.dnd.NotesSortContext;
import app.m8.web.client.view.calendarmodule.notes.events.AddNotesEvent;
import app.m8.web.client.view.calendarmodule.notes.events.MoveNoteAfterSortEvent;
import app.m8.web.client.view.calendarmodule.notes.events.RefreshCountersEvent;
import app.m8.web.client.view.calendarmodule.notes.events.RemoveNotesEvent;
import app.m8.web.client.view.calendarmodule.notes.events.UpdateNotesDateChangedEvent;
import app.m8.web.client.view.calendarmodule.notes.events.UpdateNotesWithoutDateChangedEvent;
import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.client.view.calendarmodule.notes.shared.NotesRegistryService;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.note.NoteCreationContext;
import app.m8.web.shared.core.note.NoteCreationGroup;
import app.m8.web.shared.core.structure.OrgStructure;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.inject.Inject;

import java.util.Date;
import java.util.List;


public class NotesTableModel implements ModelWithEvents, HasHandlers {

	@Inject
	private NotesRegistryService notesRegistryService;
	@Inject
	private OrgTreeModel orgTreeModel;

	private final CalendarPopupEventBus calendarPopupEventBus;
	private final EventBus globalEventBus;

	/**
	 * Состояние блокировки таблицы заметок
	 */
	private boolean notesTableBlockedState;

	/**
	 * Осуществляется ли в данный момент ручная сортировка заметок
	 */
	private boolean isSorting;

	private boolean allowDropInCreatedGroup;

	@Inject
	public NotesTableModel(CalendarPopupEventBus calendarPopupEventBus, EventBus globalEventBus) {
		this.calendarPopupEventBus = calendarPopupEventBus;
		this.globalEventBus = globalEventBus;
	}

	@Override
	public CalendarPopupEventBus getEventBus() {
		return calendarPopupEventBus;
	}

	public EventBus getGlobalEventBus() {
		return globalEventBus;
	}

	@Override
	public void fireEvent(GwtEvent<?> event) {
		getEventBus().fireEventFromSource(event, this);
	}

	/**
	 * Установка состояния таблицы напоминаний.
	 * @return Изменилось ли состояние блокировки
	 */
	public boolean setNotesTableBlockedState(boolean newState) {
		final boolean isStateChanged = newState != notesTableBlockedState;
		if (isStateChanged) {
			notesTableBlockedState = newState;
		}
		return isStateChanged;
	}

	public void setSorting(boolean isSorting) {
		this.isSorting = isSorting;
	}

	public void setAllowDropInCreatedGroup(boolean isAllow) {
		this.allowDropInCreatedGroup = isAllow;
	}

	public void fireRefreshCountersEvent() {
		fireEvent(new RefreshCountersEvent());
	}

	public boolean isEmptyTable() {
		return notesRegistryService.isEmpty();
	}

	public boolean isTableBlockedState() {
		return notesTableBlockedState;
	}

	public boolean isSorting() {
		return isSorting;
	}

	public AddNotesEvent insertNoteAfterCreate(Note note) {
		return notesRegistryService.insertNoteAfterCreate(note, this);
	}

	public UpdateNotesDateChangedEvent moveNoteAfterDateChanged(Note note, Date oldDate, boolean isOnlyTimeChanged) {
		return notesRegistryService.moveNoteAfterDateChanged(note, oldDate, this, isOnlyTimeChanged, null);
	}

	public MoveNoteAfterSortEvent moveNoteAfterSort(NotesSortContext context) {
		return notesRegistryService.moveNoteAfterSort(context, this);
	}

	public UpdateNotesWithoutDateChangedEvent updateNoteWithoutDateChanged(Note note) {
		final UpdateNotesWithoutDateChangedEvent event = new UpdateNotesWithoutDateChangedEvent(note);
		calendarPopupEventBus.fireEventFromSource(event, this);
		return event;
	}

	public RemoveNotesEvent removeNote(Note note) {
		return notesRegistryService.removeNote(note, this);
	}

	public NotesGroupModel getNotesGroupModelByDate(Date date) {
		return notesRegistryService.getNoteGroupOrCreateIfNotExist(date);
	}

	public NotesGroupModel getSynchNotesGroupModel() {
		return notesRegistryService.getSynchNotesGroupModel();
	}

	public Promise<Void> createNote(Note note) {
		return createNote(note, new NoteCreationContext(NoteCreationGroup.REGULAR));
	}

	public Promise<Void> createNote(Note note, NoteCreationContext sortingContext) {
		return notesRegistryService.createNote(note, sortingContext);
	}

	public Promise<Note> updateNote(Note note, String name, String description, Date date) {
		return notesRegistryService.updateNote(note, name, description, date);
	}

	public Promise<Void> moveNote(final Note note, final Date date, Integer noteTo, boolean isDown) {
		return notesRegistryService.moveNote(note, date, noteTo, isDown);
	}

	public Promise<Void> moveOnDate(Date groupDate, List<Note> notesInGroup, Date dateTo) {
		return notesRegistryService.moveOnDate(groupDate, notesInGroup, dateTo);
	}

	public Promise<Void> completeNote(final Integer noteId) {
		return notesRegistryService.completeNote(noteId);
	}

	public NotesRegistryService getNoteRegistry() {
		return notesRegistryService;
	}

	public void distributeCreatedNote(final Note note) {
		notesRegistryService.distributeCreatedNote(note);
	}

	public boolean isAllowDropInSyncGroup() {
		return allowDropInCreatedGroup;
	}

	public OrgStructure getSelectedStructure() {
		return orgTreeModel.getOrgTree().getSelected();
	}

	public OrgTreeModel getOrgTreeModel() {
		return orgTreeModel;
	}

	public Promise<Void> restoreOrgStructure(Integer restoredOrgId) {
		return orgTreeModel.restoreOrgStructure(restoredOrgId);
	}

	public Promise<Void> setFavorite(Note note, boolean setFavorite) {
		return notesRegistryService.setFavorite(note, setFavorite).thenConsume(aVoid -> updateNoteWithoutDateChanged(note));
	}
}