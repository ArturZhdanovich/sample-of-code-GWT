package app.m8.web.client.view.calendarmodule.notes.group;

import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;

import java.util.Date;

public interface NotesGroupModelFactory {

	NotesGroupModel createNotesGroupModel(Date groupDate, boolean synchGroup);

}
