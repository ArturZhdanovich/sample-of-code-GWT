package app.m8.web.client.view.calendarmodule.notes.group;

import app.components.client.base.CalendarEventType;
import app.components.client.base.CalendarType;
import app.components.client.base.DateField;
import app.components.client.base.calendar.vertical.VerticalCalendarPositioner;
import app.components.client.popup.popupmanager.LayerLevel;
import app.components.client.util.DateFormat;
import app.components.client.util.DateUtils;

import com.google.gwt.event.dom.client.FocusEvent;


public class NotesGroupDateField extends DateField {
	
	public NotesGroupDateField() {
		super(DateFormat.FD14_DD_MMM);
		setDateFieldSize(null);
		setDateFieldType(null);
		setEventType(CalendarEventType.REMIND_DATE);
		setShowExpiredDateInCalendar(true);
		getPopupCalendar().setLayerLevel(LayerLevel.LEVEL_2);
		setCalendarType(CalendarType.VERTICAL);
		setCustomPositioner(new VerticalCalendarPositioner(PopupAlign.RIGHT));
		setFirstEnabledDate(DateUtils.getToday());
		setWithTime(true);
		setBlankValue(consts.delay());
	}

	@Override
	protected DateTextBox createDateTextBox() {
		return new NotesGroupDateTextBox();
	}

	protected class NotesGroupDateTextBox extends DateTextBox {

		@Override
		public void onFocus(FocusEvent focusEvent) {
		}

		@Override
		public void setText(String text) {
			super.setText(consts.delay());
		}
	}

}