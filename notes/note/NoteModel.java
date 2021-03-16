package app.m8.web.client.view.calendarmodule.notes.note;

import app.components.client.dotpopup.DotAction;
import app.components.client.dotpopup.DotActionModel;
import app.components.client.mvp.model.ModelWithEvents;
import app.components.client.mvp.promise.Promise;
import app.m8.web.client.attachment.DefaultAttachmentModel;
import app.m8.web.client.attachment.file.FileService;
import app.m8.web.client.util.AppData;
import app.m8.web.client.view.calendarmodule.calendarpopup.CalendarPopupEventBus;
import app.m8.web.shared.core.help.GuiType;
import app.m8.web.shared.core.note.Note;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import java.util.Collections;
import java.util.Date;


public class NoteModel implements ModelWithEvents {

	private Note note;
	private final CalendarPopupEventBus calendarPopupEventBus;
	private final DefaultAttachmentModel<Note> attachmentModel;
	private final FileService fileService;
	private final DotActionModel dotActionModel;

	private NoteUIState state = NoteUIState.NORMAL;

	@AssistedInject
	public NoteModel(@Assisted Note note, CalendarPopupEventBus calendarPopupEventBus, EventBus globalEventBus, FileService fileService) {
		this.note = note;
		this.calendarPopupEventBus = calendarPopupEventBus;
		this.fileService = fileService;
		this.dotActionModel = createDotActionModel();
		this.attachmentModel = new DefaultAttachmentModel<>(globalEventBus, note, true, Collections.singletonList(note), true);
	}

	private DotActionModel createDotActionModel() {
		return new DotActionModel(getEventBus()) {

			@Override
			public Promise<Void> promiseInitModel() {
				clear();
				if (!AppData.isCurrentUserGeneralizedOutStaffStatus()) {
					if (!AppData.isGuiHidden(GuiType.STORAGE)) {
						addAction(DotAction.SEND_TO_IDEAS);
					}
					if (!AppData.isGuiHidden(GuiType.TASKS)) {
						addAction(DotAction.PLUS_WINDOW_TASK);
					}
				}
				return super.promiseInitModel();
			}
		};
	}

	public boolean isDotActionButtonVisible() {
		return !AppData.isCurrentUserGeneralizedOutStaffStatus() && (!AppData.isGuiHidden(GuiType.STORAGE) || !AppData.isGuiHidden(GuiType.TASKS));
	}

	@Override
	public CalendarPopupEventBus getEventBus() {
		return calendarPopupEventBus;
	}

	void resetAutomaticState() {
		note.resetAutomatic();
	}

	DefaultAttachmentModel<Note> getAttachmentModel() {
		return attachmentModel;
	}

	public boolean isEditState() {
		return state.equals(NoteUIState.EDIT);
	}

	public boolean isSelectedState() {
		return state.equals(NoteUIState.SELECTED);
	}

	public boolean isNormalState() {
		return state.equals(NoteUIState.NORMAL);
	}

	boolean setState(NoteUIState state) {
		boolean isStateChanged = !state.equals(this.state);
		if (isStateChanged) {
			this.state = state;
		}
		return isStateChanged;
	}

	String getNoteName() {
		return note.getName();
	}

	String getNoteDescription() {
		return note.getDescription();
	}

	Date getDateNote() {
		return note.getDateNote();
	}

	public Note getNote() {
		return note;
	}

	public void updateNote(Note note) {
		this.note = note;
	}

	public FileService getFileService() {
		return fileService;
	}

	DotActionModel getDotActionModel() {
		return dotActionModel;
	}
}