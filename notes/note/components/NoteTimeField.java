package app.m8.web.client.view.calendarmodule.notes.note.components;

import app.components.client.base.CalendarType;
import app.components.client.popup.TimeListBoxHorizontalPosition;
import app.components.client.popup.TimeListBoxPositioner;
import app.components.client.popup.popupmanager.ChatContactsLayerLevels;
import app.components.client.util.DateFormat;
import app.m8.web.client.view.date.TimeField;
import app.m8.web.shared.core.calendar.Time;

import com.google.gwt.event.dom.client.ClickEvent;


public class NoteTimeField extends TimeField {

	public NoteTimeField() {
		super(DateFormat.FT3, "reminders-item__date");
		addStyleName("reminders-item__time");
		setClearButtonEnable(true);
		setCanClearWhenCalendarVisible(true);
		setCloseCalendarAfterClean(true);
		setDateFieldIcon(StyleTypes.Icon.RIGHT);
		getPopupCalendar().addStyleName("note-time-popup");
		getPopupCalendar().setModal(false);
		setCalendarType(CalendarType.TIME_ONE_COLUMN);
		setListBoxVisibleRowCounts(23);
		getPopupCalendar().setLayerLevel(ChatContactsLayerLevels.CONTACTS_POPUP_LEVEL);
		setDefaultScrollTime(new Time(13, 30));
		setCustomPositioner(new TimeListBoxPositioner(TimeListBoxHorizontalPosition.RIGHT));
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
