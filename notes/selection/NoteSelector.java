package app.m8.web.client.view.calendarmodule.notes.selection;

import app.m8.web.client.view.calendarmodule.notes.note.NotePresenter;

public interface NoteSelector {
	
	void select(NotePresenter element);

	void unSelect(NotePresenter element);

	void clearSelection();
}
