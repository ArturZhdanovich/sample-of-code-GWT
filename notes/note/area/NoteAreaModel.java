package app.m8.web.client.view.calendarmodule.notes.note.area;

import app.components.client.mvp.model.ModelWithEvents;
import app.components.client.mvp.promise.Promise;
import app.components.client.mvp.promise.Promises;
import app.m8.web.client.attachment.DefaultAttachmentModel;
import app.m8.web.client.view.calendarmodule.calendarpopup.CalendarPopupEventBus;
import app.m8.web.client.view.calendarmodule.notes.events.AddNotesEvent;
import app.m8.web.client.view.calendarmodule.notes.table.NotesTableModel;
import app.m8.web.shared.core.HasId;
import app.m8.web.shared.core.file.AGMFile;
import app.m8.web.shared.core.file.FileType;
import app.m8.web.shared.core.file.HasFiles;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.note.NoteCreationContext;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class NoteAreaModel implements ModelWithEvents {

	private final EventBus eventBus;

	private NotesTableModel notesTableModel;
	private DefaultAttachmentModel<NoteAreaFilesProxy> attachmentModel;

	@Inject
	public NoteAreaModel(EventBus eventBus) {
		this.eventBus = eventBus;
		createAttachmentModel();
	}

	@Override
	public CalendarPopupEventBus getEventBus() {
		return notesTableModel.getEventBus();
	}

	void createAttachmentModel() {
		final NoteAreaFilesProxy noteFilesProxy = new NoteAreaFilesProxy(new ArrayList<>());
		this.attachmentModel = new DefaultAttachmentModel<NoteAreaFilesProxy>(eventBus, noteFilesProxy, true,
				Collections.emptyList(), true) {

			@Override
			public boolean isLinkFile() {
				return false;
			}
		};
	}

	void setNotesTableModel(NotesTableModel notesTableModel) {
		this.notesTableModel = notesTableModel;
	}

	DefaultAttachmentModel<NoteAreaFilesProxy> getAttachmentModel() {
		return attachmentModel;
	}

	List<AGMFile> getFiles() {
		return attachmentModel.getFiles();
	}

	Promise<Void> createNote(Note note, NoteCreationContext sortingContext) {
		return notesTableModel.createNote(note, sortingContext);
	}

	Promise<AddNotesEvent> insertNoteAfterCreate(Note note) {
		return Promises.fulfilled(notesTableModel.insertNoteAfterCreate(note));
	}

	private static class NoteAreaFilesProxy implements HasFiles<AGMFile>, HasId {

		private final List<AGMFile> files;

		private NoteAreaFilesProxy(List<AGMFile> files) {
			this.files = files;
		}

		@Override
		public Integer getId() {
			return null;
		}

		@Override
		public List<AGMFile> getFiles() {
			return files;
		}

		@Override
		public FileType getFileType() {
			return FileType.NOTE;
		}
	}
}
