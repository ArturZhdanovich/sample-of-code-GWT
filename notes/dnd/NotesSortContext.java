package app.m8.web.client.view.calendarmodule.notes.dnd;

import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.shared.core.note.Note;

public class NotesSortContext {

	private Note sortedNote;
	private Note relativeNote;
	
	private NotesGroupModel fromGroupModel;
	private NotesGroupModel toGroupModel;
	
	private Integer fromIndex;
	private Integer toIndex;
	
	private boolean isDown;
	
	public NotesSortContext(Note sortedNote, Note relativeNote, NotesGroupModel fromGroupModel, NotesGroupModel toGroupModel,
                            Integer fromIndex, Integer toIndex, boolean isDown) {
		this.sortedNote = sortedNote;
		this.relativeNote = relativeNote;
		this.fromGroupModel = fromGroupModel;
		this.toGroupModel = toGroupModel;
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
		this.isDown = isDown;
	}

	public Note getSortedNote() {
		return sortedNote;
	}

	public Note getRelativeNote() {
		return relativeNote;
	}

	public NotesGroupModel getFromGroupModel() {
		return fromGroupModel;
	}

	public NotesGroupModel getToGroupModel() {
		return toGroupModel;
	}

	public Integer getFromIndex() {
		return fromIndex;
	}

	public Integer getToIndex() {
		return toIndex;
	}

	public boolean isDown() {
		return isDown;
	}
}
