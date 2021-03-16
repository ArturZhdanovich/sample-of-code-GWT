package app.m8.web.client.view.calendarmodule.notes.events;

import app.m8.web.shared.core.note.Note;

import com.google.gwt.event.shared.GwtEvent;

import java.util.Collections;
import java.util.List;

public class RemoveNotesEvent extends GwtEvent<RemoveNotesHandler> {

	public static final Type<RemoveNotesHandler> TYPE = new Type<>();

	private final List<Note> notes;

	public RemoveNotesEvent(Note note) {
		this.notes = Collections.singletonList(note);
	}

	public RemoveNotesEvent(List<Note> notes) {
		this.notes = notes;
	}

	@Override
	public Type<RemoveNotesHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(RemoveNotesHandler handler) {
		handler.onRemoveNotes(this);
	}

	public List<Note> getNotes() {
		return notes;
	}
}
