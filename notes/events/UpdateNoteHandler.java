package app.m8.web.client.view.calendarmodule.notes.events;

import com.google.gwt.event.shared.EventHandler;


public interface UpdateNoteHandler extends EventHandler {

	void onCreateOrUpdate(UpdateNoteEvent event);
}