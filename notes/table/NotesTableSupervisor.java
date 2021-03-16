package app.m8.web.client.view.calendarmodule.notes.table;

import app.components.client.mvp.promise.Promise;
import app.m8.web.client.view.calendarmodule.notes.dnd.EventInNotesSortContext;
import app.m8.web.client.view.calendarmodule.notes.dnd.NotesSortContext;
import app.m8.web.client.view.calendarmodule.notes.events.AddNotesEvent;
import app.m8.web.client.view.calendarmodule.notes.group.NotesGroupSupervisor;
import app.m8.web.client.view.calendarmodule.notes.note.NotePresenter;
import app.m8.web.client.view.calendarmodule.notes.note.area.NoteAreaPresenter;
import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.shared.core.note.Note;

import com.google.gwt.dom.client.NativeEvent;

import java.util.Date;
import java.util.List;

public interface NotesTableSupervisor {

	boolean isSorting();

	boolean isAllowDropInSyncGroup();

	boolean isPreventNoteNativeClick(NativeEvent nativeEvent);

	void setSelectedNote(NotePresenter notePresenter);

	void setEditNotePresenter(NotePresenter notePresenter);

	Promise<Void> collapseEditedNoteAndUpdate();

	Promise<Void> saveAllEditedNotesAndUpdate();

	void moveNoteOnSort(NotesSortContext context);

	void clearSelection();

	void setSorting(boolean isSorting);

	void setAllowDropInCreatedGroup(boolean equals);

	void setNoteToGridPostProcess(NotePresenter notePresenter);

	void fireRefreshCountersEvent();

	void insertNotesAfterCreate(AddNotesEvent event);

	Promise<Void> insertNewNoteAfterEventDrop(EventInNotesSortContext context);

	NotesGroupModel getNotesGroupModelByDate(Date date);

	NotesGroupModel getSynchNotesGroupModel();

	NoteAreaPresenter createNoteAreaPresenter(NotesGroupSupervisor groupSupervisor);

	void onNoteCompleteBtnClick(NotePresenter notePresenter);

	Promise<Note> createNoteFromEvent(Note note, Integer eventId, Integer staffId);

	boolean isEventDraggingInProcess();

	/**
	 * перенести напоминания на указанную дату
	 *
	 * @param groupDate Дата группы напоминаний, которые переносятся
	 * @param dateTo    дата на которую переносятся напоминания
	 */
	void moveOnDate(Date groupDate, List<Note> notesInGroup, Date dateTo);

	/**
	 * Восстановить узел при напоминании типа AutoNoteType.RESTORE_ORG
	 */
	void restoreOrgStructure(Integer restoredOrgId);

	/**
	 * Установко значения метки "Важное"
	 */
	void setFavorite(NotePresenter notePresenter);

	Promise<Void> createNote(Note note);

	/**
	 * Обработка дропа Цели/Задачи в список напоминаний
	 */
	Promise<Void> insertNewNoteAfterTaskDrop(NotesSortContext context);

}