package app.m8.web.client.view.calendarmodule.notes.events;

import com.google.gwt.event.shared.EventHandler;

public interface RefreshNotesExpiredHandler extends EventHandler {

	void onExpiredChanged(RefreshNotesExpiredEvent event);
}
