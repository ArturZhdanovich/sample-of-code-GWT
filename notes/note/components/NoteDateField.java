package app.m8.web.client.view.calendarmodule.notes.note.components;

import app.components.client.base.CalendarEventType;
import app.components.client.base.DateField;
import app.components.client.base.calendar.vertical.ScrollStrategy;
import app.components.client.base.calendar.vertical.VerticalCalendarPositioner;
import app.components.client.popup.popupmanager.ChatContactsLayerLevels;
import app.components.client.util.DateFormat;
import app.components.client.util.DateUtils;

import com.google.gwt.event.dom.client.ClickEvent;


public class NoteDateField extends DateField {
	
	public NoteDateField() {
		super(DateFormat.FD14_DD_MMM, "reminders-item__date");
		setDateFieldSize(null);
		setDateFieldType(null);
		setEventType(CalendarEventType.REMIND_DATE);
		setScrollStrategy(ScrollStrategy.SELECTED_DATE_IN_FIRST_ROW);
		setShowExpiredDateInCalendar(true);
		setCustomPositioner(new VerticalCalendarPositioner(PopupAlign.CENTER));
		setFirstEnabledDate(DateUtils.getToday());
		getPopupCalendar().setModal(false);
		getPopupCalendar().setLayerLevel(ChatContactsLayerLevels.CONTACTS_POPUP_LEVEL);
	}
	
	@Override
	public void onActivateDeactivate(boolean active) {
		super.onActivateDeactivate(active);
		setValue(getValue(), false);
	}

	@Override
	protected void onDateClick(ClickEvent event) {
		if (!isPopupCalendarShowing()) {
			super.onDateClick(event);
		}
	}

	/**
	 * устанавливает стиль дефтфилда к которому будут применятся стили для валиадции
	 */
	public void setWithValidation(boolean set) {
		setStyleName("reminders-item__date--has-validation", set);
	}
}