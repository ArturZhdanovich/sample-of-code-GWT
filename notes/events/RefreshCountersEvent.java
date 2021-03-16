package app.m8.web.client.view.calendarmodule.notes.events;

import com.google.gwt.event.shared.GwtEvent;

public class RefreshCountersEvent extends GwtEvent<RefreshCountersHandler> {
	
	public static final Type<RefreshCountersHandler> TYPE = new Type<>();
	
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<RefreshCountersHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(RefreshCountersHandler handler) {
		handler.onRefreshCounters(this);
	}
}
