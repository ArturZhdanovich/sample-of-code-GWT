package app.m8.web.client.view.calendarmodule.notes.group;

import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.client.view.calendarmodule.notes.table.NotesTableSupervisor;

public interface NotesGroupFactory extends NotesGroupModelFactory {

	NotesGroupPresenter createNotesGroupPresenter(NotesGroupModel groupModel, NotesTableSupervisor supervisor);

}
