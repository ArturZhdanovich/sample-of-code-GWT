package app.m8.web.client.view.calendarmodule.notes.dnd;

import app.components.client.DataTransferJS;
import app.components.client.dndsorting.AgmSortingDndController;
import app.m8.web.client.view.calendarmodule.notes.note.NotePresenter;
import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.client.view.calendarmodule.notes.table.NotesTableSupervisor;
import app.m8.web.client.view.goalortask.dnd.GoalOrTaskDropHandler;
import app.m8.web.client.view.goalortask.dnd.GoalOrTaskDropListener;
import app.m8.web.shared.DNDConstants;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.note.NoteStatus;
import app.m8.web.shared.core.task.GoalOrTask;

import com.google.gwt.dom.client.DataTransfer.DropEffect;
import com.google.gwt.user.client.Event;

import java.util.Date;

/**
 * Контроллер обрабатывающий драг задач и целей в списке напоминаний
 * */
public class TaskInNotesSortingController extends AgmSortingDndController<NotePresenter, NotesTableSupervisor> implements GoalOrTaskDropListener {

	public TaskInNotesSortingController(NotePresenter presenter, NotesTableSupervisor supervisor) {
		super(presenter, supervisor, null, presenter.getView().getElement());
		new GoalOrTaskDropHandler(presenter.getView(), this);
	}

	@Override
	public void doOnGoalOrTaskDrop(Event event, GoalOrTask draggedItem) {
		final Note note = new Note();
		note.setName(draggedItem.getName());

		final Note receivingNote = presenter.getNote();
		if (receivingNote != null) {
			final NotesGroupModel toGroupModel =  publicApi.getNotesGroupModelByDate(receivingNote.getDateNote());
			final int receivingNoteIndex = toGroupModel.getNoteIndex(receivingNote);
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
					final int relativeNoteIndex = toGroupModel.getNoteIndex(relativeNote);

					final int toIndex;
					if (isDown) {
						toIndex = relativeNoteIndex + 1;
					} else {
						toIndex = relativeNoteIndex;
					}
					note.setDateNote(new Date(relativeNote.getDateNote().getTime()));

					publicApi.createNote(note).then(aVoid -> {
						note.setStatus(NoteStatus.IN_WORK);
						final NotesSortContext context = new NotesSortContext(note, relativeNote, null,
								toGroupModel, null, toIndex, isDown);
						return publicApi.insertNewNoteAfterTaskDrop(context);
					});
				}
			}
		}
	}

	@Override
	public void doOnGoalOrTaskDragOver(Event event, GoalOrTask draggedItem) {
		if (!presenter.getNote().getStatus().equals(NoteStatus.CREATED)) {
			event.getDataTransfer().setDropEffect(DropEffect.COPY);
		} else {
			event.getDataTransfer().setDropEffect(DropEffect.NONE);
		}
	}

	@Override
	public void doOnGoalOrTaskDragEnter(Event event, GoalOrTask draggedItem) {
	}

	@Override
	public void doOnGoalOrTaskDragLeave(Event event, GoalOrTask draggedItem) {
	}

	@Override
	protected boolean isAllowDropHandling(Event event) {
		DataTransferJS dt = event.getDataTransfer().cast();
		return dt.hasType(DNDConstants.DRAGGED_GOAL_OR_TASK_ID);
	}
}