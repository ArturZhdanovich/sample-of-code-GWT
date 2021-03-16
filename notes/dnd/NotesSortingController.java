package app.m8.web.client.view.calendarmodule.notes.dnd;

import app.components.client.DndUtils;
import app.components.client.dndsorting.AgmSortingDndController;
import app.m8.web.client.view.calendarmodule.notes.note.NotePresenter;
import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.client.view.calendarmodule.notes.table.NotesTableSupervisor;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.note.NoteStatus;

import com.google.gwt.dom.client.DataTransfer;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

import java.util.Date;

public class NotesSortingController extends AgmSortingDndController<NotePresenter, NotesTableSupervisor> {

	private static final String IS_CREATED = "isCreated";

	public NotesSortingController(NotePresenter presenter, NotesTableSupervisor supervisor) {
		super(presenter, supervisor, presenter.getView().getDragElement(), presenter.getView().getElement());
	}

	@Override
	protected boolean isAllowDrag(Event event) {
		return !presenter.isTextFieldsFocus();
	}

	@Override
	protected void setDraggedItemClass(boolean set) {
	}

	@Override
	protected void setDragImage(Event event, Element element) {
		event.getDataTransfer().setDragImage(DndUtils.createDragImage(element, "note-sorting"), 0, 19);
	}

	@Override
	protected void onDragStart(Event event) {
		publicApi.setSorting(true);
		publicApi.setAllowDropInCreatedGroup(presenter.getNote().getStatus().equals(NoteStatus.CREATED));
	}
	
	@Override
	protected void fillData(DataTransfer dataTransfer) {
		final Note note = presenter.getNote();
		final NotesGroupModel fromGroupModel = note.isCreated() ? publicApi.getSynchNotesGroupModel()
				: publicApi.getNotesGroupModelByDate(note.getDateNote());
		final Integer fromIndex = fromGroupModel.getNoteIndex(note);
		dataTransfer.setData(ITEM_ID, note.getId().toString());
		dataTransfer.setData(IS_CREATED, Boolean.toString(note.isCreated()));
		dataTransfer.setData(ORDER_IDX, fromIndex.toString());
		dataTransfer.setData(GROUP_ID, String.valueOf(fromGroupModel.getGroupDate().getTime()));
	}

	@Override
	protected void onDrop(Event event) {
		final Integer fromIndex = Integer.parseInt(event.getDataTransfer().getData(ORDER_IDX));
		if (fromIndex != -1) {
			final Integer sortedNoteId = Integer.parseInt(event.getDataTransfer().getData(ITEM_ID));
			final Date fromGroupDate = new Date(Long.parseLong(event.getDataTransfer().getData(GROUP_ID)));

			boolean isCreated = Boolean.parseBoolean(event.getDataTransfer().getData(IS_CREATED));
			final NotesGroupModel fromGroupModel = isCreated ? publicApi.getSynchNotesGroupModel() : publicApi.getNotesGroupModelByDate(fromGroupDate);
			final Note sortedNote = fromGroupModel.findNoteById(sortedNoteId);
			final Note receivingNote = presenter.getNote();
			if (sortedNote != null && receivingNote != null && !sortedNoteId.equals(receivingNote.getId())) {
				final NotesGroupModel toGroupModel =  receivingNote.isCreated() ? publicApi.getSynchNotesGroupModel()
						: publicApi.getNotesGroupModelByDate(receivingNote.getDateNote());
				final Integer receivingNoteIndex = toGroupModel.getNoteIndex(receivingNote);
				if (receivingNoteIndex != -1) {
					final Note relativeNote = toGroupModel.getNotes().get(receivingNoteIndex);
					boolean isLast = receivingNoteIndex == toGroupModel.getNotes().size() - 1;
					if (relativeNote != null && !sortedNoteId.equals(relativeNote.getId())) {
						final boolean isSameGroup = fromGroupModel.equals(toGroupModel);
						final Integer relativeNoteIndex = toGroupModel.getNoteIndex(relativeNote);
						final Integer toIndex;
						final boolean isDown = !isInsertBeforeV();
						if (isSameGroup) {
							if (fromIndex < relativeNoteIndex) {
								toIndex = isDown ? relativeNoteIndex : relativeNoteIndex - 1;
							} else {
								toIndex = isDown ? relativeNoteIndex + 1 : relativeNoteIndex;
							}
						} else {
							if (isDown) {
								toIndex = relativeNoteIndex + 1;
							} else {
								toIndex = relativeNoteIndex;
							}
						}
						if (!isSameGroup || !fromIndex.equals(toIndex)) {
							final NotesSortContext context = new NotesSortContext(sortedNote, relativeNote, fromGroupModel,
									toGroupModel, fromIndex, toIndex, isDown);
							publicApi.moveNoteOnSort(context);
						}
					}
				}
			}
		}
	}
	
	@Override
	protected void onDragEnd(Event event) {
		publicApi.setSorting(false);
		publicApi.setAllowDropInCreatedGroup(false);
	}
	
	@Override
	protected boolean isAllowDropHandling(Event event) {
		return publicApi.isSorting() && (!presenter.getNote().getStatus().equals(NoteStatus.CREATED) || publicApi.isAllowDropInSyncGroup());
	}
	
	@Override
	protected boolean isDragLeaveHandlingOnlyOnBorders() {
		return true;
	}
}
