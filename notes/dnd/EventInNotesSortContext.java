package app.m8.web.client.view.calendarmodule.notes.dnd;

import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.shared.core.note.Note;

import java.util.Date;

public class EventInNotesSortContext extends NotesSortContext {

	private Integer eventId;
	private Date eventDate;

	public EventInNotesSortContext(Note sortedNote, Note relativeNote, NotesGroupModel fromGroupModel, NotesGroupModel toGroupModel,
								   Integer fromIndex, Integer toIndex, boolean isDown, Integer eventId, Date eventDate) {
		super(sortedNote, relativeNote, fromGroupModel, toGroupModel, fromIndex, toIndex, isDown);
		this.eventId = eventId;
		this.eventDate = eventDate;
	}

	public Integer getEventId() {
		return eventId;
	}

	public void setEventId(Integer eventId) {
		this.eventId = eventId;
	}

	public Date getEventDate() {
		return eventDate;
	}

	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}
}
