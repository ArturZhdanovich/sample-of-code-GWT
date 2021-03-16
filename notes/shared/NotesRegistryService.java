package app.m8.web.client.view.calendarmodule.notes.shared;

import app.components.client.mvp.promise.Promise;
import app.m8.web.client.view.calendarmodule.notes.dnd.NotesSortContext;
import app.m8.web.client.view.calendarmodule.notes.events.AddNotesEvent;
import app.m8.web.client.view.calendarmodule.notes.events.MoveNoteAfterSortEvent;
import app.m8.web.client.view.calendarmodule.notes.events.RemoveNotesEvent;
import app.m8.web.client.view.calendarmodule.notes.events.UpdateNotesDateChangedEvent;
import app.m8.web.shared.core.note.Note;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface NotesRegistryService extends NotesInitService, NoteCRUDService {

    /**
     * распределить созданную заметку в группу согласно времени
     */
    void distributeCreatedNote(Note note);

    /**
     * Получить группу по дате или создать группу для переданной даты, если такой нет.
     * Добавит группу в список и вернет созданную группу
     * */
    NotesGroupModel getNoteGroupOrCreateIfNotExist(Date date);

    /**
     * Пустой ли список напоминаний
     * */
    boolean isEmpty();

    /**
     * Вставить заметку после создания и уведомить остальные таблицы заметок
     * @param note - заметка
     * @return AddNoteEvent
     */
    AddNotesEvent insertNoteAfterCreate(Note note, Object eventSource);

    /**
     * Сдвинуть заметку после изменения даты и уведомить остальные таблицы заметок
     * @param note - заметка
     * @param oldDate - старая дата
     * @param isOnlyTimeChanged изменлось ли только время или еще и дата
     * @param orderNumber новый индекс в группе
     * @return MoveNoteAfterDateChangedEvent
     */
    UpdateNotesDateChangedEvent moveNoteAfterDateChanged(Note note, Date oldDate, Object eventSource, boolean isOnlyTimeChanged, Integer orderNumber);

    /**
     * Сдвинуть заметки после ручной сортировки и уведомить остальные таблицы заметок
     * @param context - контекст сортировки
     * @return MoveNoteAfterSortEvent
     */
    MoveNoteAfterSortEvent moveNoteAfterSort(NotesSortContext context, Object eventSource);

    RemoveNotesEvent removeNote(Note note, Object eventSource);

    Promise<Note> createNoteFromEvent(Note note, Integer eventId, Integer staffId);

    NotesGroupModel getSynchNotesGroupModel();

    Collection<NotesGroupModel> getNotesGroupModelCollection();

    List<Note> getNotes();

    Promise<Void> setFavorite(Note note, boolean setFavorite);

    long getSizeByDate(Date date);

    int getExpiredNotesSize();

    List<Note> getExpiredNotesForNotification();
}
