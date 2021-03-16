package app.m8.web.client.view.calendarmodule.notes.events;

import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.shared.core.note.Note;

import com.google.gwt.event.shared.GwtEvent;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class UpdateNotesDateChangedEvent extends GwtEvent<UpdateNotesDateChangedHandler> {

	public static final Type<UpdateNotesDateChangedHandler> TYPE = new Type<>();

	private final List<UpdateNotesDateChangedEventData> dataList;

	public UpdateNotesDateChangedEvent(UpdateNotesDateChangedEventData data) {
		this.dataList = Collections.singletonList(data);
	}

	public UpdateNotesDateChangedEvent(List<UpdateNotesDateChangedEventData> dataList) {
		this.dataList = dataList;
	}
	
	@Override
	public Type<UpdateNotesDateChangedHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(UpdateNotesDateChangedHandler handler) {
		handler.onUpdateNoteWithoutDateChanged(this);
	}

	public List<UpdateNotesDateChangedEventData> getDataList() {
		return dataList;
	}

	public static class UpdateNotesDateChangedEventData {
		private final Note note;
		private final NotesGroupModel fromGroupModel;
		private final int fromIndex;
		private final NotesGroupModel toGroupModel;
		private final int toIndex;
		private final Date oldDate;

		public UpdateNotesDateChangedEventData(Note note, NotesGroupModel fromGroupModel, int fromIndex, NotesGroupModel toGroupModel, int toIndex, Date oldDate) {
			this.note = note;
			this.fromGroupModel = fromGroupModel;
			this.fromIndex = fromIndex;
			this.toGroupModel = toGroupModel;
			this.toIndex = toIndex;
			this.oldDate = oldDate;
		}

		public Note getNote() {
			return note;
		}

		public NotesGroupModel getFromGroupModel() {
			return fromGroupModel;
		}

		public int getFromIndex() {
			return fromIndex;
		}

		public NotesGroupModel getToGroupModel() {
			return toGroupModel;
		}

		public int getToIndex() {
			return toIndex;
		}

		public Date getOldDate() {
			return oldDate;
		}
	}
}
