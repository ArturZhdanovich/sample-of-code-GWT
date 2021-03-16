package app.m8.web.client.view.calendarmodule.notes.events;

import com.google.gwt.event.shared.GwtEvent;

public class ShowNotesPanelEvent extends GwtEvent<ShowNotesPanelHandler> {

public static final Type<ShowNotesPanelHandler> TYPE = new Type<>();

	private Integer noteId;

	public ShowNotesPanelEvent(Integer noteId) {
		this.noteId = noteId;
	}

	@Override
	public Type<ShowNotesPanelHandler> getAssociatedType() {
		return TYPE;
	}

	public static Type<ShowNotesPanelHandler> getType() {
		return TYPE;
	}

	@Override
	protected void dispatch(ShowNotesPanelHandler handler) {
		handler.onShowNote(this);
	}

	public Integer getNoteId() {
		return noteId;
	}
}
