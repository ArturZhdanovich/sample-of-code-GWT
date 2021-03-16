package app.m8.web.client.view.calendarmodule.notes.events;

import app.m8.web.shared.core.note.Note;

import com.google.gwt.event.shared.GwtEvent;

import java.util.Collections;
import java.util.List;

public class UpdateNotesWithoutDateChangedEvent extends GwtEvent<UpdateNotesWithoutDateChangedHandler> {

	public static final Type<UpdateNotesWithoutDateChangedHandler> TYPE = new Type<>();
	
	private final List<Note> notes;

	public UpdateNotesWithoutDateChangedEvent(Note note) {
		this.notes = Collections.singletonList(note);
	}
	
	public UpdateNotesWithoutDateChangedEvent(List<Note> notes) {
		this.notes = notes;
	}
	
	@Override
	public Type<UpdateNotesWithoutDateChangedHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(UpdateNotesWithoutDateChangedHandler handler) {
		handler.onUpdateNoteWithoutDateChanged(this);
	}

	public List<Note> getNotes() {
		return notes;
	}
}
