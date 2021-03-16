package app.m8.web.client.view.calendarmodule.notes.events;

import com.google.gwt.event.shared.EventHandler;

public interface UpdateNotesDateChangedHandler extends EventHandler {
	void onUpdateNoteWithoutDateChanged(UpdateNotesDateChangedEvent event);
}
