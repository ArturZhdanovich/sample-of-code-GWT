package app.m8.web.client.view.calendarmodule.notes.shared;

import app.components.client.util.DateUtils;
import app.m8.web.shared.CommonUtils;
import app.m8.web.shared.core.note.Note;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class NotesGroupModel {

	private final Date groupDate;
	private final boolean synchGroup;
	private final List<Note> notes = new LinkedList<>();

	@Inject
	public NotesGroupModel(@Assisted Date groupDate, @Assisted boolean synchGroup) {
		this.groupDate = groupDate;
		this.synchGroup = synchGroup;
	}

	void addNote(Note note) {
		notes.add(note);
	}

	void removeNote(Note note) {
		notes.remove(note);
	}

	void insertNote(Note note, int toIndex) {
		notes.add(toIndex, note);
	}

	public void clear() {
		notes.clear();
	}

	public boolean isSynchGroup() {
		return synchGroup;
	}

	public List<Note> getNotes() {
		return notes;
	}

	public int getNoteIndex(Note note) {
		return notes.indexOf(note);
	}

	/**
	 * Получить индекс заметки после перемещения в другую группу
	 *
	 * @param noteDate дата заметки для которой ищется индекс вставки
	 * @return индекс для вставки заметки
	 */
	public int getIndexForNewNoteInGroup(Date noteDate) {
		return (int) getNotes().stream()
				.filter(note -> this.insertNoteFilterResolver(note, noteDate))
				.count();
	}

	private boolean insertNoteFilterResolver(Note filterNote, Date noteDate) {
		int compareDateResult = filterNote.getDateNote().compareTo(noteDate);
		if (isSynchGroup()) {
			return compareDateResult <= 0;
		}
		return compareDateResult < 0;
	}

	/**
	 * Получить общее кол-во напоминаний по дате
	 *
	 * @param date - дата
	 * @return int - целочисленное значение, определяющее общее кол-во напоминаний относительно переданной даты в текущей группе
	 */
	long getSizeByDate(Date date) {
		return getNotes()
				.stream()
				.filter(note -> DateUtils.compareDate(date, note.getDateNote()) == 0)
				.count();
	}

	public boolean isEmpty() {
		return notes.isEmpty();
	}

	public Date getGroupDate() {
		return groupDate;
	}

	public Note findNoteById(Integer sortedNoteId) {
		return CommonUtils.findById(notes, sortedNoteId);
	}

}
