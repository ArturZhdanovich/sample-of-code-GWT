package app.m8.web.client.view.calendarmodule.notes.events;

import app.m8.web.shared.core.note.Note;

import com.google.gwt.event.shared.GwtEvent;

import java.util.Collections;
import java.util.List;

public class AddNotesEvent extends GwtEvent<AddNoteHandler> {

public static final Type<AddNoteHandler> TYPE = new Type<>();
	private final List<AddNoteEventData> addNoteEventDataList;

	public AddNotesEvent(Note note, int toIndex) {
		this(Collections.singletonList(new AddNoteEventData(note, toIndex)));
	}

	public AddNotesEvent(List<AddNoteEventData> addNoteEventDataList) {
		this.addNoteEventDataList = addNoteEventDataList;
	}

	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<AddNoteHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(AddNoteHandler handler) {
		handler.onAddNote(this);
	}

	public List<AddNoteEventData> getAddNoteEventDataList() {
		return addNoteEventDataList;
	}

	public static class AddNoteEventData {
		private final Note note;
		private final int toIndex;

		public AddNoteEventData(Note note, int toIndex) {
			this.note = note;
			this.toIndex = toIndex;
		}

		public Note getNote() {
			return note;
		}

		public int getToIndex() {
			return toIndex;
		}
	}
}
