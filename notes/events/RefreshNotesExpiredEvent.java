package app.m8.web.client.view.calendarmodule.notes.events;

import com.google.gwt.event.shared.GwtEvent;


public class RefreshNotesExpiredEvent extends GwtEvent<RefreshNotesExpiredHandler> {

	public static final Type<RefreshNotesExpiredHandler> TYPE = new Type<>();

	private boolean reload;

	public RefreshNotesExpiredEvent() {
	}

	public RefreshNotesExpiredEvent(boolean reload) {
		this.reload = reload;
	}

	@Override
	public Type<RefreshNotesExpiredHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(RefreshNotesExpiredHandler handler) {
		handler.onExpiredChanged(this);
	}

	public boolean isReload() {
		return reload;
	}
}
