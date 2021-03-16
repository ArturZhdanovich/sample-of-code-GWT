package app.m8.web.client.view.calendarmodule.notes.dnd;

import app.components.client.mvp.AbstractCommonController;
import app.m8.web.client.view.calendarmodule.notes.note.area.NoteAreaPresenter;
import app.m8.web.client.view.goalortask.dnd.GoalOrTaskDropHandler;
import app.m8.web.client.view.goalortask.dnd.GoalOrTaskDropListener;
import app.m8.web.shared.core.task.GoalOrTask;

import com.google.gwt.dom.client.DataTransfer.DropEffect;
import com.google.gwt.user.client.Event;

/**
 * Контроллер обрабатывающий драг задач и целей в области создания напоминания
 * */
public class TaskInNotesAreaController extends AbstractCommonController<NoteAreaPresenter, Void> implements GoalOrTaskDropListener {

	public TaskInNotesAreaController(NoteAreaPresenter presenter) {
		initController(presenter);
		new GoalOrTaskDropHandler(presenter.getView(), this);
	}

	@Override
	public void doOnGoalOrTaskDrop(Event event, GoalOrTask draggedItem) {
		presenter.focusNoteArea().done(aVoid -> {
			presenter.getView().setNameText(draggedItem.getName());
			presenter.getView().refreshIconsVisibility();
		});
	}

	@Override
	public void doOnGoalOrTaskDragOver(Event event, GoalOrTask draggedItem) {
		event.getDataTransfer().setDropEffect(DropEffect.COPY);
	}

	@Override
	public void doOnGoalOrTaskDragEnter(Event event, GoalOrTask draggedItem) {
	}

	@Override
	public void doOnGoalOrTaskDragLeave(Event event, GoalOrTask draggedItem) {
	}
}