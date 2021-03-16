package app.m8.web.client.view.calendarmodule.notes.shared;

import app.components.client.mvp.promise.Callback;
import app.components.client.mvp.promise.Promise;
import app.components.client.mvp.promise.Promises;
import app.components.client.util.DateUtils;
import app.m8.web.client.command.AGMDispatcher;
import app.m8.web.client.view.calendarmodule.notes.dnd.NotesSortContext;
import app.m8.web.client.view.calendarmodule.notes.events.AddNotesEvent;
import app.m8.web.client.view.calendarmodule.notes.events.MoveNoteAfterSortEvent;
import app.m8.web.client.view.calendarmodule.notes.events.RefreshNotesExpiredEvent;
import app.m8.web.client.view.calendarmodule.notes.events.RemoveNotesEvent;
import app.m8.web.client.view.calendarmodule.notes.events.UpdateNoteEvent;
import app.m8.web.client.view.calendarmodule.notes.events.UpdateNotesDateChangedEvent;
import app.m8.web.client.view.calendarmodule.notes.events.UpdateNotesWithoutDateChangedEvent;
import app.m8.web.client.view.calendarmodule.notes.group.NotesGroupModelFactory;
import app.m8.web.client.websocket.event.NetworkGoneOnlineEvent;
import app.m8.web.shared.CommonUtils;
import app.m8.web.shared.command.actions.calendar.note.CreateNoteFromEventAction;
import app.m8.web.shared.command.actions.calendar.note.CreateNoteQuicklyAction;
import app.m8.web.shared.command.actions.calendar.note.GetNotesAction;
import app.m8.web.shared.command.actions.calendar.note.MoveNoteAction;
import app.m8.web.shared.command.actions.calendar.note.MoveOnDateAction;
import app.m8.web.shared.command.actions.calendar.note.SetNoteFavoriteAction;
import app.m8.web.shared.command.actions.calendar.note.UpdateNoteAction;
import app.m8.web.shared.command.actions.calendar.note.UpdateNoteStatusAction;
import app.m8.web.shared.command.results.ReloadStatisticResult;
import app.m8.web.shared.command.results.calendar.note.CreateNoteFromEventResult;
import app.m8.web.shared.command.results.calendar.note.GetNotesResult;
import app.m8.web.shared.core.calendar.Time;
import app.m8.web.shared.core.file.FileType;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.note.NoteChangeStatus;
import app.m8.web.shared.core.note.NoteCreationContext;
import app.m8.web.shared.core.note.NoteCreationGroup;
import app.m8.web.shared.core.note.NoteStatus;
import app.m8.web.shared.util.Noop;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Singleton
public class NotesRegistryServiceImpl implements NotesRegistryService {

	private final EventBus globalEventBus;
	private final SortedMap<Date, NotesGroupModel> groupsMap;
	private final NotesGroupModel synchNotesGroupModel;
	private final NotesGroupModelFactory groupModelFactory;
	private List<Note> expiredNotesForNotification = new LinkedList<>();

	@Inject
	public NotesRegistryServiceImpl(EventBus globalEventBus, NotesGroupModelFactory groupModelFactory) {
		this.globalEventBus = globalEventBus;
		this.groupModelFactory = groupModelFactory;
		this.groupsMap = new TreeMap<>();
		this.synchNotesGroupModel = groupModelFactory.createNotesGroupModel(DateUtils.getToday(), true);
		registerSocketEventsHandler();
		globalEventBus.addHandler(NetworkGoneOnlineEvent.getType(), event -> reloadRegistryAndNotifyListeners());
	}

	private void reloadRegistryAndNotifyListeners() {
		AGMDispatcher.getInstance().execute(new GetNotesAction(NoteStatus.IN_WORK, null))
				.thenConsume(getNotesResult -> setNotes(getNotesResult.getNotes()))
				.thenConsume(aVoid -> fireRefreshExpiredAfterOnline());
	}

	private void registerSocketEventsHandler() {
		// Если создали, обновили или удалили напоминание на одном клиенте сделаем тоже самое
		globalEventBus.addHandler(UpdateNoteEvent.getType(), event -> {
			NoteChangeStatus noteChangeStatus = event.getNoteChangeStatus();
			if (NoteChangeStatus.COMPLETE.equals(noteChangeStatus) || NoteChangeStatus.DELETE.equals(noteChangeStatus)) {
				Optional<Note> noteForDelete = getNoteById(event.getSingleNoteId());
				noteForDelete.ifPresent(note -> removeNote(note, this));
			} else if (NoteChangeStatus.CREATE.equals(noteChangeStatus)) {
				getNote(event.getSingleNoteId()).thenConsume(note -> insertNoteAfterCreate(note, this));
			} else if (NoteChangeStatus.UPDATE.equals(noteChangeStatus)) {
				Integer orderNumber = event.getOrderNumber();
				getNote(event.getSingleNoteId()).thenConsume(note -> {
					if (note != null) {
						final Note oldNote = getNoteById(event.getSingleNoteId()).orElse(null);
						if (oldNote == null) {
							insertNoteAfterCreate(note, this);
						} else {
							final boolean isSameEditedNoteDate = oldNote.isSameDate(note.getDateNote());
							if (!isSameEditedNoteDate) {
								note.setStatus(oldNote.getStatus());
								boolean isOnlyTimeChanged = DateUtils.compareDate(note.getDateNote(), oldNote.getDateNote()) == 0;
								moveNoteAfterDateChanged(note, oldNote.getDateNote(), this, isOnlyTimeChanged, orderNumber);
							} else if (orderNumber != null) {
								NotesGroupModel homeGroup = getNoteGroupOrCreateIfNotExist(note.getDateNote());
								final int oldIndex = homeGroup.getNoteIndex(note);
								final int newIndex = orderNumber - 1;
								boolean isDown = newIndex > 0;
								Note relativeNote = homeGroup.getNotes().get(newIndex);
								NotesSortContext context = new NotesSortContext(note, relativeNote, homeGroup, homeGroup, oldIndex, newIndex, isDown);
								moveNoteAfterSort(context, this);
							} else {
								oldNote.setName(note.getName());
								oldNote.setDescription(note.getDescription());
								oldNote.setFiles(note.getFiles());
								globalEventBus.fireEventFromSource(new UpdateNotesWithoutDateChangedEvent(note), this);
							}
						}
					}
				});
			} else if (NoteChangeStatus.MULTIPLE_DATE_CHANGED.equals(noteChangeStatus)) {
				final Date newDate = event.getNewDate();
				final List<UpdateNotesDateChangedEvent.UpdateNotesDateChangedEventData> dataForUpdate = new ArrayList<>();

				event.getNoteIds().forEach(id -> getNoteById(id).ifPresent(noteFromView -> {
					final Date oldDateNote = noteFromView.getDateNote();
					final Date newDateNote = DateUtils.mergeTime(newDate, oldDateNote);

					final NotesGroupModel fromGroupModel = getFromGroupNoteType(noteFromView, oldDateNote);
					final int fromIndex = fromGroupModel.getNoteIndex(noteFromView);
					final NotesGroupModel toGroupModel = getFromGroupNoteType(noteFromView, newDateNote);
					fromGroupModel.removeNote(noteFromView);
					noteFromView.setDateNote(newDateNote);
					int toIndex = toGroupModel.getNotes().size();
					toGroupModel.insertNote(noteFromView, toIndex);
					updateNoteDate(noteFromView);

					dataForUpdate.add(new UpdateNotesDateChangedEvent.UpdateNotesDateChangedEventData(noteFromView, fromGroupModel, fromIndex, toGroupModel, toIndex, oldDateNote));
				}));

				globalEventBus.fireEventFromSource(new UpdateNotesDateChangedEvent(dataForUpdate), this);
				fireRefreshExpired();
			} else if (NoteChangeStatus.MULTIPLE_CHANGED.equals(noteChangeStatus)) {
				//Если этот флаг возвращает null, значит у заметок статус проставился IN_WORK, значит их не было на UI, и их нужно перетянуть
				final Boolean favorite = event.getFavorite();

				if (favorite != null) {
					final List<Note> notesForUpdate = new ArrayList<>();

					event.getNoteIds().forEach(id -> getNoteById(id).ifPresent(noteFromView -> {
						final boolean newFavorite = Boolean.TRUE.equals(favorite);

						if (newFavorite != noteFromView.isFavorite()) {
							noteFromView.setFavorite(newFavorite);
							notesForUpdate.add(noteFromView);
						}
					}));

					globalEventBus.fireEventFromSource(new UpdateNotesWithoutDateChangedEvent(notesForUpdate), this);
				} else {
					getNote().thenConsume(notes -> {
						final List<AddNotesEvent.AddNoteEventData> dataForUpdate = new ArrayList<>();

						notes.stream().filter(note -> event.getNoteIds().contains(note.getId())).forEach(note -> {
							final NotesGroupModel toGroupModel = getFromGroupNoteType(note, note.getDateNote());
							final int toIndex = toGroupModel.getIndexForNewNoteInGroup(note.getDateNote());
							toGroupModel.insertNote(note, toIndex);

							dataForUpdate.add(new AddNotesEvent.AddNoteEventData(note, toIndex));
						});

						globalEventBus.fireEventFromSource(new AddNotesEvent(dataForUpdate), this);
					});
				}
			} else if (NoteChangeStatus.MULTIPLE_DELETED.equals(noteChangeStatus)) {
				final List<Note> notesForDelete = new ArrayList<>();

				event.getNoteIds().forEach(id -> getNoteById(id).ifPresent(notesForDelete::add));
				notesForDelete.forEach(note -> {
					final NotesGroupModel fromGroupModel = getFromGroupNoteType(note, note.getDateNote());
					fromGroupModel.removeNote(note);
					expiredNotesForNotification.remove(note);
				});

				globalEventBus.fireEventFromSource(new RemoveNotesEvent(notesForDelete), this);
				fireRefreshExpired();
			}
		});
	}

	private NotesGroupModel getFromGroupNoteType(Note noteFromView, Date groupDate) {
		return noteFromView.isCreated() ? getSynchNotesGroupModel() : getNoteGroupOrCreateIfNotExist(groupDate);
	}

	public EventBus getGlobalEventBus() {
		return globalEventBus;
	}

	/**
	 * Очистить модель и добавить список заметок с сортировкой по группам
	 * @param notes - список заметок
	 */
	@Override
	public void setNotes(List<Note> notes) {
		getNotesGroupModelCollection().forEach(NotesGroupModel::clear);
		Optional.ofNullable(notes).ifPresent(notesList -> notesList.forEach(this::addNote));
	}

	private void addNote(Note note) {
		getNoteGroupOrCreateIfNotExist(note.getDateNote()).addNote(note);
	}

	@Override
	public NotesGroupModel getNoteGroupOrCreateIfNotExist(Date date) {
		Date roundDate = DateUtils.roundToDay(new Date(date.getTime()));
		if (DateUtils.isToday(roundDate)) {
			roundDate = DateUtils.getToday();
		}
		if (DateUtils.compareDate(roundDate, DateUtils.getToday()) < 0) {
			roundDate = DateUtils.getYesterday();
		}
		if (!groupsMap.containsKey(roundDate)) {
			groupsMap.put(roundDate, groupModelFactory.createNotesGroupModel(roundDate, false));
		}
		return groupsMap.get(roundDate);
	}

	/**
	 * Создание заметки с указанием параметров сортировки
	 * @param note - заметка
	 * @return Promise - промис
	 */
	@Override
	public Promise<Void> createNote(final Note note, NoteCreationContext context) {
		return AGMDispatcher.getInstance().execute(new CreateNoteQuicklyAction(note, context))
				.thenConsume(createNoteResult -> {
					note.setId(createNoteResult.getId());
					note.setStatus(NoteCreationGroup.SYNC.equals(context.getGroupType()) ? NoteStatus.CREATED : NoteStatus.IN_WORK);
					note.getFiles().forEach(file -> file.setIdOwner(note.getId()));
				});
	}

	/**
	 * Создание заметки из встречи
	 * @param note - заметка
	 * @param eventId - Id встречи из которой создается заметка
	 * @param staffId - staffId сотрудника встреча которого перетягивается
	 * @return Promise - промис
	 */
	public Promise<Note> createNoteFromEvent(final Note note, Integer eventId, Integer staffId) {
		CreateNoteFromEventAction createNoteFromEventAction = new CreateNoteFromEventAction(note, eventId, staffId, FileType.NOTE);
		return AGMDispatcher.getInstance().execute(createNoteFromEventAction)
				.then(new Callback<CreateNoteFromEventResult, Note>() {

					@Override
					public Promise<Note> onFulfilled(CreateNoteFromEventResult result) {
						note.setId(result.getId());
						note.setStatus(NoteStatus.IN_WORK);
						note.setFiles(result.getFiles());
						note.getFiles().forEach(file -> file.setIdOwner(note.getId()));
						return Promises.fulfilled(note);
					}
				});
	}

	/**
	 * Обновление заметки
	 * @param note - заметка
	 * @param name - новое наименование
	 * @param description - новое описание
	 * @param date - новая дата
	 * @return Promise - промис
	 */
	@Override
	public Promise<Note> updateNote(final Note note, final String name, final String description, final Date date) {
		return AGMDispatcher.getInstance().execute(new UpdateNoteAction(note.getId(), name, description, note.getDateNote(), date))
				.then(new Callback<ReloadStatisticResult, Note>() {

			@Override
			public Promise<Note> onFulfilled(ReloadStatisticResult value) {
				note.resetAutomatic();
				if (name != null) {
					note.setName(name);
				}
				if (description != null) {
					note.setDescription(description);
				}
				if (!note.isSameDate(date)) {
					note.setDateNote(date != null ? new Date(date.getTime()) : null);
				}
				updateNoteDate(note);
				return Promises.fulfilled(note);
			}
		});
	}

	/**
	 * Перемещение заметки
	 * @param note - заметка
	 * @param date - новая дата
	 * @param noteTo - относительно какой заметки (идентификатор)
	 * @param isDown - вниз/вверх от относительной заметки
	 * @return Promise - промис
	 */
	@Override
	public Promise<Void> moveNote(final Note note, final Date date, Integer noteTo, boolean isDown) {
		return AGMDispatcher.getInstance().execute(new MoveNoteAction(note.getId(), noteTo, isDown))
				.thenConsume(reloadStatisticResult -> {
					if (!note.isSameDate(date)) {
						note.resetAutomatic();
						note.setDateNote(date != null ? DateUtils.mergeTime(date, note.getDateNote()) : null);
						updateNoteDate(note);
					}
				});
	}

	/**
	 * перемещение всех напоминаний с датой groupDate на dateTo
	 *
	 * @param groupDate Дата группы напоминаний, которые переносятся
	 * @param dateTo дата на которую переносятся напоминания
	 * */
	@Override
	public Promise<Void> moveOnDate(Date groupDate, List<Note> notesInGroup, Date dateTo) {
		if (DateUtils.compareDate(groupDate, dateTo) != 0) {
			List<Integer> notesIds = !CommonUtils.isNullOrEmptyList(notesInGroup) ? notesInGroup.stream().map(Note::getId).collect(Collectors.toList()) : null;
			return AGMDispatcher.getInstance().execute(new MoveOnDateAction(groupDate, dateTo, notesIds)).thenConsume(reloadStatisticResult -> {
				getNoteGroupOrCreateIfNotExist(groupDate).getNotes().forEach(note -> {
							Date noteDate = new Date(dateTo.getTime());
							final Time noteTime = DateUtils.getTimeByDate(note.getDateNote());
							DateUtils.setMH(noteDate, noteTime);
							note.setDateNote(noteDate);
							updateNoteDate(note);
							NotesGroupModel noteGroup = getNoteGroupOrCreateIfNotExist(dateTo);
							noteGroup.insertNote(note, noteGroup.getNotes().size());
						});
				getNoteGroupOrCreateIfNotExist(groupDate).clear();
				fireRefreshExpired();
			});
		}
		return Promises.fulfilled();
	}

	/**
	 * Смена статуса на "выполнено"
	 */
	@Override
	public Promise<Void> completeNote(final Integer noteId) {
		return AGMDispatcher.getInstance().execute(new UpdateNoteStatusAction(noteId, NoteStatus.COMPLETED)).thenConsume(Noop::noop);
	}

	@Override
	public AddNotesEvent insertNoteAfterCreate(Note note, Object source) {
		final NotesGroupModel toGroupModel = getFromGroupNoteType(note, note.getDateNote());
		final int toIndex = toGroupModel.getIndexForNewNoteInGroup(note.getDateNote());
		toGroupModel.insertNote(note, toIndex);
		AddNotesEvent addNotesEvent = new AddNotesEvent(note, toIndex);
		globalEventBus.fireEventFromSource(addNotesEvent, source);
		fireRefreshExpired();
		return addNotesEvent;
	}

	@Override
	public void distributeCreatedNote(Note note) {
		note.setStatus(NoteStatus.IN_WORK);
		getSynchNotesGroupModel().removeNote(note);
		final NotesGroupModel toGroupModel = getNoteGroupOrCreateIfNotExist(note.getDateNote());
		final int toIndex = toGroupModel.getNotes().size();
		toGroupModel.insertNote(note, toIndex);
	}

	@Override
	public UpdateNotesDateChangedEvent moveNoteAfterDateChanged(Note note, Date oldDate, Object eventSource, boolean isOnlyTimeChanged, Integer orderNumber) {
		final NotesGroupModel fromGroupModel = getFromGroupNoteType(note, oldDate);
		final int fromIndex = fromGroupModel.getNoteIndex(note);
		final NotesGroupModel toGroupModel = getFromGroupNoteType(note, note.getDateNote());
		int toIndex = fromIndex;
		if (!isOnlyTimeChanged) {
			fromGroupModel.removeNote(note);
			if (orderNumber != null) {
				toIndex =  orderNumber - 1;
			} else {
				toIndex = note.isCreated() ? getSynchNotesGroupModel().getIndexForNewNoteInGroup(note.getDateNote()) : toGroupModel.getNotes().size();
			}
			toGroupModel.insertNote(note, toIndex);
			updateNoteDate(note);
		}
		UpdateNotesDateChangedEvent.UpdateNotesDateChangedEventData data =
				new UpdateNotesDateChangedEvent.UpdateNotesDateChangedEventData(note, fromGroupModel, fromIndex, toGroupModel, toIndex, oldDate);
		UpdateNotesDateChangedEvent updateNotesDateChangedEvent = new UpdateNotesDateChangedEvent(data);
		globalEventBus.fireEventFromSource(updateNotesDateChangedEvent, eventSource);
		fireRefreshExpired();
		return updateNotesDateChangedEvent;
	}

	private void updateNoteDate(Note note) {
		expiredNotesForNotification.removeIf(note1 -> note.getId().equals(note1.getId()) && !DateUtils.isExpired(note1.getDateNote()));
	}

	@Override
	public MoveNoteAfterSortEvent moveNoteAfterSort(NotesSortContext context, Object eventSource) {
		final Note sortedNote = context.getSortedNote();
		final NotesGroupModel fromGroupModel = context.getFromGroupModel();
		final NotesGroupModel toGroupModel = context.getToGroupModel();
		final int toIndex = context.getToIndex();

		if (fromGroupModel != null) {
			fromGroupModel.removeNote(sortedNote);
		}
		toGroupModel.insertNote(sortedNote, toIndex);
		updateNoteDate(sortedNote);
		MoveNoteAfterSortEvent moveNoteAfterSortEvent = new MoveNoteAfterSortEvent(context);
		globalEventBus.fireEventFromSource(moveNoteAfterSortEvent, eventSource);
		fireRefreshExpired();
		return moveNoteAfterSortEvent;
	}

	@Override
	public RemoveNotesEvent removeNote(Note note, Object eventSource) {
		final NotesGroupModel fromGroupModel = getFromGroupNoteType(note, note.getDateNote());
		fromGroupModel.removeNote(note);
		expiredNotesForNotification.remove(note);
		RemoveNotesEvent removeNoteEvent = new RemoveNotesEvent(note);
		globalEventBus.fireEventFromSource(removeNoteEvent, eventSource);
		fireRefreshExpired();
		return removeNoteEvent;
	}

	private Optional<Note> getNoteById(Integer noteId) {
		return getNotes().stream().filter(note -> note.getId().equals(noteId)).findFirst();
	}

	private void fireRefreshExpired() {
		globalEventBus.fireEvent(new RefreshNotesExpiredEvent());
	}

	private void fireRefreshExpiredAfterOnline() {
		globalEventBus.fireEvent(new RefreshNotesExpiredEvent(true));
	}

	@Override
	public List<Note> getNotes() {
		final List<Note> notes = new LinkedList<>(synchNotesGroupModel.getNotes());
		for (NotesGroupModel notesGroupModel : getNotesGroupModelCollection()) {
			notes.addAll(notesGroupModel.getNotes());
		}
		return notes;
	}

	@Override
	public Collection<NotesGroupModel> getNotesGroupModelCollection() {
		return groupsMap.values();
	}

	@Override
	public long getSizeByDate(Date date) {
		return getNoteGroupOrCreateIfNotExist(date).getSizeByDate(date) + getSynchNotesGroupModel().getSizeByDate(date);
	}

	@Override
	public boolean isEmpty() {
		return getNotes().isEmpty();
	}

	public NotesGroupModel getSynchNotesGroupModel() {
		return synchNotesGroupModel;
	}

	@Override
	public Promise<Note> getNote(Integer noteId) {
		return AGMDispatcher.getInstance().execute(new GetNotesAction(NoteStatus.IN_WORK, noteId)).thenMap(getNotesResult -> getNotesResult.getNotes().get(0));
	}

	@Override
	public Promise<List<Note>> getNote() {
		return AGMDispatcher.getInstance().execute(new GetNotesAction(NoteStatus.IN_WORK, null)).thenMap(GetNotesResult::getNotes);
	}

	private List<Note> getExpiredNotes() {
		Date currentDate = new Date();
		return getNotes().stream().filter(note -> note.getDateNote().compareTo(currentDate) < 0).collect(Collectors.toList());
	}

	@Override
	public List<Note> getExpiredNotesForNotification() {
		final Date now = new Date();
		List<Note> newExpiredNotes = getExpiredNotes().stream()
				.filter(note -> !expiredNotesForNotification.contains(note) && isNoteJustExpired(note.getDateNote(), now))
				.collect(Collectors.toList());
		expiredNotesForNotification.addAll(newExpiredNotes);
		return newExpiredNotes;
	}

	private boolean isNoteJustExpired(Date dateNote, Date date) {
		return dateNote.compareTo(DateUtils.addMinutes(new Date(date.getTime()), -1)) > 0 && dateNote.compareTo(date) < 0;
	}

	@Override
	public int getExpiredNotesSize() {
		return getExpiredNotes().size();
	}

	public Promise<Void> setFavorite(Note note, boolean setFavorite) {
		return AGMDispatcher.getInstance().execute(new SetNoteFavoriteAction(note.getId(), setFavorite))
				.thenConsume(emptyResult -> note.setFavorite(setFavorite));
	}
}
