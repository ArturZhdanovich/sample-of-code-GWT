package app.m8.web.client.view.calendarmodule.notes.group;

import app.components.client.animation.AnimationUtils;
import app.components.client.mvp.AbstractCommonPresenter;
import app.components.client.mvp.promise.FulfillablePromise;
import app.components.client.mvp.promise.Promise;
import app.components.client.util.DateUtils;
import app.m8.web.client.util.AppData;
import app.m8.web.client.view.calendarmodule.notes.dnd.EventsInNotesSortingDropController;
import app.m8.web.client.view.calendarmodule.notes.dnd.NotesSortingController;
import app.m8.web.client.view.calendarmodule.notes.dnd.TaskInNotesSortingController;
import app.m8.web.client.view.calendarmodule.notes.note.NoteFactory;
import app.m8.web.client.view.calendarmodule.notes.note.NoteModel;
import app.m8.web.client.view.calendarmodule.notes.note.NotePresenter;
import app.m8.web.client.view.calendarmodule.notes.note.area.NoteAreaPresenter;
import app.m8.web.client.view.calendarmodule.notes.note.area.NoteAreaView;
import app.m8.web.client.view.calendarmodule.notes.shared.NotesGroupModel;
import app.m8.web.client.view.calendarmodule.notes.table.NotesTableSupervisor;
import app.m8.web.client.view.calendarmodule.print.CalendarPrintSupervisor;
import app.m8.web.shared.GlobalConstants;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.note.NoteCreationContext;
import app.m8.web.shared.core.note.NoteCreationDirection;
import app.m8.web.shared.core.note.NoteCreationGroup;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class NotesGroupPresenter extends AbstractCommonPresenter<NotesGroupModel, NotesGroupView> {

	private final NotesTableSupervisor supervisor;
	private final List<NotePresenter> notePresenters;
	private final NoteFactory noteFactory;
	private final CalendarPrintSupervisor calendarPrintSupervisor;
	private final NoteAreaPresenter noteAreaPresenter;
	private FulfillablePromise<Void> collapseAnimation;

	@AssistedInject
	public NotesGroupPresenter(@Assisted NotesGroupModel groupModel,
							   @Assisted NotesTableSupervisor supervisor,
							   NotesGroupView view,
							   NoteFactory noteFactory,
							   CalendarPrintSupervisor calendarPrintSupervisor) {
		super(groupModel, view);
		this.supervisor = supervisor;
		this.noteFactory = noteFactory;
		this.calendarPrintSupervisor = calendarPrintSupervisor;
		this.notePresenters = new LinkedList<>();
		this.noteAreaPresenter = supervisor.createNoteAreaPresenter(createNotesGroupSupervisor());

		view.addNoteArea(noteAreaPresenter.getView());
	}

	private NotesGroupSupervisor createNotesGroupSupervisor() {
		return new NotesGroupSupervisor() {

			@Override
			public Date getNoteCreationTime() {
				return DateUtils.roundToDay(model.getGroupDate());
			}

			@Override
			public void refreshValidNoteArea() {
				NoteAreaView areaView = noteAreaPresenter.getView();
				areaView.setNoteAreaNormalState();
				NotesGroupPresenter.this.refreshNoteAreaPanel(false);
			}

			@Override
			public void refreshInvalidNoteArea() {
				NoteAreaView areaView = noteAreaPresenter.getView();
				areaView.setNoteAreaNormalState();
				if (!areaView.getTrimmedDescriptionText().isEmpty() && areaView.getTrimmedNameText().isEmpty()) {
					areaView.setNoteAreaFocusState();
				}
			}

			@Override
			public NoteCreationContext getCreationContextFromNote() {
				if (model.isSynchGroup()) {
					return NotesGroupPresenter.this.getNoteSyncContext();
				}
				return NotesGroupPresenter.this.getNoteContext();
			}
		};
	}

	private NoteCreationContext getNoteContext() {
		if (model.getNotes().size() > 0) {
			return new NoteCreationContext(NoteCreationGroup.REGULAR, model.getNotes().get(0).getId(), NoteCreationDirection.UP);
		}
		return new NoteCreationContext(NoteCreationGroup.REGULAR);
	}

	private NoteCreationContext getNoteSyncContext() {
		return new NoteCreationContext(NoteCreationGroup.SYNC);
	}

	@Override
	public void refresh() {
		noteAreaPresenter.refresh();
		refreshHeader();
		refreshContent();
		refreshNoteAreaPanel(true);
	}

	@Override
	protected void attachHandlers() {
		registerHandler(view.addPlusBtnClickHandler(event -> showNoteAreaPanel(true, true, true)));
		registerHandler(view.addDateValueChangeHandler(event -> supervisor.moveOnDate(model.getGroupDate(), model.getNotes(), event.getValue())));
		registerHandler(getView().addPrintBtnClickHandler(clickEvent -> calendarPrintSupervisor.printEventsAndNotes(
				AppData.getStaffId(), getModel().getGroupDate(), false, true, GlobalConstants.EMPTY_STRING)));
	}

	private void refreshNoteAreaPanel(boolean withFocus) {
		showNoteAreaPanel(model.isSynchGroup(), withFocus, false);
	}

	public void refreshHeader() {
		view.refreshHeader(model.getGroupDate(), model.isSynchGroup());
	}

	private void showNoteAreaPanel(boolean show, boolean withFocus, boolean withAnimation) {
		FlowPanel noteAreaContainer = view.getNoteAreaContainer();
		if (show == noteAreaContainer.isVisible() || collapseAnimation != null && collapseAnimation.isPending()) {
			return;
		}
		if (withAnimation) {
			collapseAnimation = FulfillablePromise.create();
			Element element = noteAreaContainer.getElement();
			if (show) {
				AnimationUtils.showAnimationHeight(element, e -> {
					collapseAnimation.fulfill(null);
					if (withFocus) {
						noteAreaPresenter.getView().setNoteAreaFocusState();
					}
				});
			} else {
				AnimationUtils.hideAnimationHeight(element, e -> {
					collapseAnimation.fulfill(null);
					noteAreaContainer.setVisible(false);
				});
			}
		} else {
			noteAreaContainer.setVisible(show);
			if (show && withFocus) {
				noteAreaPresenter.getView().setNoteAreaFocusState();
			}
		}
	}

	private void refreshContent() {
		clear();
		model.getNotes().forEach(note -> {
			final NotePresenter notePresenter = createNotePresenter(note);
			notePresenter.refresh();
			notePresenters.add(notePresenter);
			view.addNoteView(notePresenter.getView());
		});
	}

	private void clear() {
		view.clear();
		notePresenters.clear();
	}

	/**
	 * Создание представления заметки
	 *
	 * @param note - заметка
	 * @return NotePresenter
	 */
	public NotePresenter createNotePresenter(Note note) {
		final NoteModel noteModel = noteFactory.createNoteModel(note);
		final NotePresenter notePresenter = noteFactory.createNotePresenter(noteModel, supervisor);
		// регистрация обработчиков сортировки
		new NotesSortingController(notePresenter, supervisor);
		new EventsInNotesSortingDropController(notePresenter, supervisor);
		new TaskInNotesSortingController(notePresenter, supervisor);
		// регистрация обработчиков дропа заметки на календарную сетку
		supervisor.setNoteToGridPostProcess(notePresenter);
		setNotePresenterVisible(notePresenter);
		return notePresenter;
	}

	/**
	 * Обновление видимости представления заметки в таблице
	 *
	 * @param notePresenter - представление заметки
	 */
	public void setNotePresenterVisible(NotePresenter notePresenter) {
		notePresenter.getView().setVisible(true);
	}

	public void insertNotePresenter(int toIndex, NotePresenter notePresenter) {
		notePresenters.add(toIndex, notePresenter);
	}

	/**
	 * Удалить представление заметки из группы
	 *
	 * @param index - индекс(положение) заметки в группе
	 */
	public void removeNotePresenter(int index) {
		final NotePresenter notePresenter = notePresenters.remove(index);
		notePresenter.getView().removeFromParent();
	}

	/**
	 * Удалить представление заметки из группы
	 *
	 * @param notePresenter - представление заметки
	 */
	public void removeNotePresenter(NotePresenter notePresenter) {
		if (notePresenter != null) {
			final int index = notePresenters.indexOf(notePresenter);
			if (index != -1) {
				removeNotePresenter(index);
			}
		}
	}

	/**
	 * Удалить представление заметки из группы
	 *
	 * @param note - заметка презентер которой требуется удалить
	 */
	public void removeNotePresenter(Note note) {
		final NotePresenter notePresenter = findNotePresenterByNote(note);
		if (notePresenter != null) {
			removeNotePresenter(notePresenter);
		}
	}

	/**
	 * Найти представление заметки по заметке
	 *
	 * @param note - заметка
	 * @return NotePresenter
	 */
	public NotePresenter findNotePresenterByNote(Note note) {
		if (note != null) {
			for (NotePresenter notePresenter : notePresenters) {
				if (notePresenter.getNote().equals(note)) {
					return notePresenter;
				}
			}
		}
		return null;
	}

	public boolean isEmpty() {
		return model.isEmpty() || isAllNotesHidden();
	}

	private boolean isAllNotesHidden() {
		return notePresenters.size() == getHiddenNotesSize();
	}

	public NotePresenter getNotePresenter(int index) {
		return notePresenters.get(index);
	}

	public List<NotePresenter> getNotePresenters() {
		return notePresenters;
	}

	private List<NotePresenter> getHiddenNotePresenters() {
		final List<NotePresenter> hiddenNotePresenters = new LinkedList<>();
		notePresenters.forEach(presenter -> {
			if (!presenter.getView().isVisible()) {
				hiddenNotePresenters.add(presenter);
			}
		});
		return hiddenNotePresenters;
	}

	private int getHiddenNotesSize() {
		return getHiddenNotePresenters().size();
	}

	public int getSize() {
		return model.getNotes().size();
	}

	public void refreshVisibility() {
		refreshNoteAreaPanel(false);
		view.setVisible(!isEmpty());
	}

	public boolean isCreateAreaEdited() {
		return noteAreaPresenter.isEdited();
	}

	public Promise<Void> saveAllNotes() {
		return noteAreaPresenter.saveNote();
	}

	public NoteAreaView getNoteAreaView() {
		return noteAreaPresenter.getView();
	}

	public Promise<Void> focusNoteArea() {
		return noteAreaPresenter.focusNoteArea();
	}

	public Promise<Void> insertNoteAfterCreate(Note note) {
		return noteAreaPresenter.insertNoteAfterCreate(note);
	}
}
