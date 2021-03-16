package app.m8.web.client.view.calendarmodule.notes.storage;

/**
 * Интерфейс сервиса по доступу к данным о состоянии описания напоминания(открыто/закрыто)
 * */
public interface NotesDescriptionVisibilityStorage {

    boolean isDescriptionOpen(Integer noteId);

    void setDescriptionOpen(Integer noteId, boolean isOpen);

    void deleteNote(Integer noteId);
}