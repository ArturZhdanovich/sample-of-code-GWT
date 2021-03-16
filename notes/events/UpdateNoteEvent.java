package app.m8.web.client.view.calendarmodule.notes.events;

import app.m8.web.shared.core.note.NoteChangeStatus;

import com.google.gwt.event.shared.GwtEvent;

import java.util.Date;
import java.util.List;

public class UpdateNoteEvent extends GwtEvent<UpdateNoteHandler> {

	public static final Type<UpdateNoteHandler> TYPE = new Type<>();

	private final List<Integer> noteIds;
	private final NoteChangeStatus noteChangeStatus;
	private final Integer orderNumber;
	private final Date newDate;
	private final Boolean favorite;

	public UpdateNoteEvent(List<Integer> noteIds, NoteChangeStatus noteChangeStatus) {
		this(noteIds, noteChangeStatus, null, null, null);
	}

	public UpdateNoteEvent(List<Integer> noteIds, NoteChangeStatus noteChangeStatus, Integer orderNumber, Date newDate, Boolean favorite) {
		this.noteIds = noteIds;
		this.noteChangeStatus = noteChangeStatus;
		this.orderNumber = orderNumber;
		this.newDate = newDate;
		this.favorite = favorite;
	}

	public static Type<UpdateNoteHandler> getType() {
		return TYPE;
	}

	@Override
	public Type<UpdateNoteHandler> getAssociatedType() {
		return getType();
	}

	@Override
	protected void dispatch(UpdateNoteHandler handler) {
		handler.onCreateOrUpdate(this);
	}

	public NoteChangeStatus getNoteChangeStatus() {
		return noteChangeStatus;
	}

	public Integer getOrderNumber() {
		return orderNumber;
	}

	public Date getNewDate() {
		return newDate;
	}

	public List<Integer> getNoteIds() {
		return noteIds;
	}

	public Boolean getFavorite() {
		return favorite;
	}

	public Integer getSingleNoteId() {
		return noteIds != null && noteIds.size() > 0 ? noteIds.get(0) : null;
	}
}