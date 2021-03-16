package app.m8.web.client.view.calendarmodule.notes.events;

import app.m8.web.client.view.calendarmodule.notes.dnd.NotesSortContext;

import com.google.gwt.event.shared.GwtEvent;

public class MoveNoteAfterSortEvent extends GwtEvent<MoveNoteAfterSortHandler> {
	
	public static final Type<MoveNoteAfterSortHandler> TYPE = new Type<>();
	
	private NotesSortContext context;

	public MoveNoteAfterSortEvent(NotesSortContext context) {
		this.context = context;
	}
	
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<MoveNoteAfterSortHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(MoveNoteAfterSortHandler handler) {
		handler.onMoveNoteAfterSort(this);
	}

	public NotesSortContext getContext() {
		return context;
	}
}
