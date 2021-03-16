package app.m8.web.client.view.calendarmodule.notes.shared;

import app.components.client.mvp.promise.Promise;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.note.NoteCreationContext;

import java.util.Date;
import java.util.List;

public interface NoteCRUDService {

    /**
     * создать напоминание, с параметрами сортировки
     * */
    Promise<Void> createNote(Note note, NoteCreationContext context);

    /**
     * Получить напоминание
     * */
    Promise<Note> getNote(Integer noteId);

    /**
     * Получить список напоминаний
     * */
    Promise<List<Note>> getNote();

    /**
     * Обновить напоминание
     * */
    Promise<Note> updateNote(Note note, String name, String description, Date date);

    /**
     * Переместить напоминание
     * */
    Promise<Void> moveNote(Note note, Date date, Integer noteTo, boolean isDown);

    /**
     * Переместить группу напоминаний на другую дату
     * */
    Promise<Void> moveOnDate(Date groupDate, List<Note> notesInGroup, Date dateTo);

    /**
     * Завершить напоминание
     * */
    Promise<Void> completeNote(Integer noteId);
}
