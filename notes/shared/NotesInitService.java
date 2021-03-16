package app.m8.web.client.view.calendarmodule.notes.shared;

import app.m8.web.shared.core.note.Note;

import java.util.List;

public interface NotesInitService {

    /**
     * Инициализация сервиса напоминаний
     * */
    void setNotes(List<Note> notes);
}
