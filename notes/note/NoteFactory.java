package app.m8.web.client.view.calendarmodule.notes.note;

import app.m8.web.client.view.calendarmodule.notes.table.NotesTableSupervisor;
import app.m8.web.shared.core.note.Note;

public interface NoteFactory {

	NoteModel createNoteModel(Note note);

	NotePresenter createNotePresenter(NoteModel noteModel, NotesTableSupervisor supervisor);
}
