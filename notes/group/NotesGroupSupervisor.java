package app.m8.web.client.view.calendarmodule.notes.group;

import app.m8.web.shared.core.note.NoteCreationContext;

import java.util.Date;

public interface NotesGroupSupervisor {

	Date getNoteCreationTime();

	void refreshValidNoteArea();

	void refreshInvalidNoteArea();

	NoteCreationContext getCreationContextFromNote();

}
