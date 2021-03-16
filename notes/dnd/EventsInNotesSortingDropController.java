package app.m8.web.client.view.calendarmodule.notes.dnd;

import app.components.client.dndsorting.AgmSortingDndController;
import app.components.client.util.DateUtils;
import app.m8.web.client.view.calendarmodule.calendar.calendartype.adaptive.common.grid.EventDNDConstants;
import app.m8.web.client.view.calendarmodule.notes.note.NotePresenter;
import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.client.view.calendarmodule.notes.table.NotesTableSupervisor;
import app.m8.web.shared.CommonUtils;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.note.NoteStatus;

import com.google.gwt.user.client.Event;

import java.util.Date;

public class EventsInNotesSortingDropController extends AgmSortingDndController<NotePresenter, NotesTableSupervisor>  {

	public EventsInNotesSortingDropController(NotePresenter presenter, NotesTableSupervisor supervisor) {
		super(presenter, supervisor, null, presenter.getView().getElement());
	}

	@Override
	protected void onDrop(Event event) {
		final Note note = new Note();
		final Integer eventId = Integer.parseInt(event.getDataTransfer().getData(EventDNDConstants.DRAGGED_EVENT_ID));

		final String noteName = event.getDataTransfer().getData(EventDNDConstants.ITEM_NAME);
		note.setName(noteName);
		final String sortedNoteDescription = event.getDataTransfer().getData(EventDNDConstants.ITEM_DESCRIPTION);
		if (!sortedNoteDescription.equals("null") && !CommonUtils.isNullOrEmptyString(sortedNoteDescription)) {
			note.setDescription(sortedNoteDescription);
		}
		final Date eventDate = new Date(Long.parseLong(event.getDataTransfer().getData(EventDNDConstants.ITEM_DATE_START)));
		note.setDateNote(DateUtils.isPastDate(eventDate) ? DateUtils.getToday() : new Date(eventDate.getTime()));

		final Integer staffId = Integer.parseInt(event.getDataTransfer().getData(EventDNDConstants.STAFF_ID));
		final Note receivingNote = presenter.getNote();
		if (receivingNote != null) {
			final NotesGroupModel toGroupModel =  publicApi.getNotesGroupModelByDate(receivingNote.getDateNote());
			final Integer receivingNoteIndex = toGroupModel.getNoteIndex(receivingNote);
			if (receivingNoteIndex != -1) {
				final Note relativeNote;
				boolean isLast = receivingNoteIndex == toGroupModel.getNotes().size() - 1;
				final boolean isDown = isLast && !isInsertBeforeV();
				if (isLast || isInsertBeforeV()) {
					relativeNote = toGroupModel.getNotes().get(receivingNoteIndex);
				} else {
					relativeNote = toGroupModel.getNotes().get(receivingNoteIndex + 1);
				}
				if (relativeNote != null) {
					final Integer relativeNoteIndex = toGroupModel.getNoteIndex(relativeNote);

					final Integer toIndex;
					if (isDown) {
						toIndex = relativeNoteIndex + 1;
					} else {
						toIndex = relativeNoteIndex;
					}
					publicApi.createNoteFromEvent(note, eventId, staffId).then(note1 -> {
						final EventInNotesSortContext context = new EventInNotesSortContext(note1, relativeNote, null,
								toGroupModel, null, toIndex, isDown, eventId, eventDate);
						return publicApi.insertNewNoteAfterEventDrop(context);
					});
				}
			}
		}
	}

	@Override
	protected boolean isDragLeaveHandlingOnlyOnBorders() {
		return true;
	}

	@Override
	protected boolean isAllowDropHandling(Event event) {
		return !presenter.getNote().getStatus().equals(NoteStatus.CREATED)
				&& publicApi.isEventDraggingInProcess();
	}
}