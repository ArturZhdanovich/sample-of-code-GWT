package app.m8.web.client.view.calendarmodule.notes.table;

import app.m8.web.client.view.calendarmodule.notes.group.NotesGroupFactory;
import app.m8.web.client.view.calendarmodule.notes.group.NotesGroupSupervisor;
import app.m8.web.client.view.calendarmodule.notes.note.area.NoteAreaPresenter;
import app.m8.web.client.view.calendarmodule.notespopup.NotesPopupSupervisor;
import app.m8.web.client.view.calendarmodule.notespopup.NotesTablePopupPresenter;

public interface NotesTableFactory extends NotesGroupFactory {

	NoteAreaPresenter createNoteAreaPresenter(NotesTableModel model, NotesTableSupervisor notesTableSupervisor, NotesGroupSupervisor notesGroupSupervisor);

	NotesTablePresenter createNotesTablePresenter(NoteTableSupervisor noteTableSupervisor);

	NotesTablePopupPresenter createNotesTablePopupPresenter(NotesPopupSupervisor notesPopupSupervisor, NoteTableSupervisor noteTableSupervisor);
}
