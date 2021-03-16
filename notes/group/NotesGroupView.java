package app.m8.web.client.view.calendarmodule.notes.group;

import app.components.client.CompositeView;
import app.components.client.util.DateFormat;
import app.components.client.util.DateUtils;
import app.m8.web.client.message.AppConstants;
import app.m8.web.client.message.MessageFactory;
import app.m8.web.client.view.calendarmodule.notes.note.NoteView;
import app.m8.web.client.view.calendarmodule.notes.note.area.NoteAreaView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.Date;


public class NotesGroupView extends CompositeView {

	private static final NotesGroupViewUiBinder uiBinder = GWT.create(NotesGroupViewUiBinder.class);

	private static final String SYNC = "reminders-group--sync";
	private static final String TODAY = "reminders-group--today";
	private static final String PAST = "reminders-group--past";

	interface NotesGroupViewUiBinder extends UiBinder<Widget, NotesGroupView> {

	}

	@UiField
	FlowPanel notesContainer;
	@UiField
	Label header;
	@UiField(provided = true)
	NotesGroupDateField dateField = new NotesGroupDateField();
	@UiField
	Label printBtn;
	@UiField
	Label plusBtn;
	@UiField
	FlowPanel noteAreaContainer;

	public NotesGroupView() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	void refreshHeader(Date date, boolean synchGroup) {
		final boolean isEmptyDateOrSyncGroup = date != null && !synchGroup;
		final boolean today = DateUtils.isToday(date);
		final boolean pastDate = DateUtils.compareDate(date, DateUtils.getDate()) < 0;
		dateField.setValue(date, false);
		setStyleName(SYNC, synchGroup);
		setStyleName(TODAY, today);
		setStyleName(PAST, pastDate);
		if (isEmptyDateOrSyncGroup) {
			final AppConstants appConstants = MessageFactory.getAppConstants();
			if (pastDate) {
				header.setText(appConstants.overdue());
			} else {
				if (today) {
					header.setText(appConstants.todayCap());
				} else if (DateUtils.isTomorrow(date)) {
					header.setText(appConstants.tomorrowCap());
				} else {
					header.setText(DateUtils.formatDate(DateFormat.FD17_FORMAT_DD_MMMM_SHORT_WEEK_DAY, date));
				}
			}
		}
	}

	void addNoteArea(NoteAreaView noteArea) {
		noteAreaContainer.add(noteArea);
	}

	void addNoteView(NoteView noteView) {
		notesContainer.add(noteView);
	}

	public void insertNoteView(Widget widget, int beforeIndex) {
		notesContainer.insert(widget, beforeIndex);
	}

	void clear() {
		notesContainer.clear();
	}

	public FlowPanel getNoteAreaContainer() {
		return noteAreaContainer;
	}

	public FlowPanel getNotesContainer() {
		return notesContainer;
	}

	HandlerRegistration addDateValueChangeHandler(ValueChangeHandler<Date> handler) {
		return dateField.addValueChangeHandler(handler);
	}

	HandlerRegistration addPrintBtnClickHandler(ClickHandler handler) {
		return printBtn.addClickHandler(handler);
	}

	HandlerRegistration addPlusBtnClickHandler(ClickHandler handler) {
		return plusBtn.addClickHandler(handler);
	}

}