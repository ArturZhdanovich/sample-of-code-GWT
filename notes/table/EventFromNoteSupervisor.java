package app.m8.web.client.view.calendarmodule.notes.table;

import app.components.client.mvp.promise.Promise;
import app.m8.web.shared.core.note.Note;

public interface EventFromNoteSupervisor {

	/**
	 * Удаляет напоминание перед помещение созданной из напоминания встречи в сетку
	 * */
	Promise<Void> awaitNoteRemove(Note note);

	/**
	 * Очистить выбор заметки
	 * */
	void clearSelection();
}