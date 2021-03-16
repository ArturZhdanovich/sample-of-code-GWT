package app.m8.web.client.view.calendarmodule.notes.note;

import app.components.client.CompositeView;
import app.components.client.DomUtils;
import app.components.client.base.ExpandingArea;
import app.components.client.base.event.ValidationHandler;
import app.components.client.contenteditable.CursorPosition;
import app.components.client.contenteditable.SimpleLinksTextArea;
import app.components.client.dotpopup.DotActionView;
import app.components.client.popup.popupmanager.BasePopupPanel;
import app.components.client.upload.DropFilesPanel;
import app.components.client.upload.RootHighlightDropFilesPanel;
import app.components.client.util.DateUtils;
import app.m8.web.client.attachment.AttachmentPanelView;
import app.m8.web.client.view.calendarmodule.calendar.util.CalendarConstants;
import app.m8.web.client.view.calendarmodule.notes.note.components.NoteDateField;
import app.m8.web.client.view.calendarmodule.notes.note.components.NoteTimeField;
import app.m8.web.shared.GlobalConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.Date;
import java.util.Optional;


public class NoteView extends CompositeView {

	public static final String SELECTED_STYLE = "reminders-item--selected";
	public static final String FOCUSED_STYLE = "reminders-item--edit-mode";
	public static final String OPENED_STYLE = "reminders-item--opened";
	private static final String REMOVE_HOVERED_STYLE = "reminders-item--remove-highlight";

	private static final NoteViewUiBinder uiBinder = GWT.create(NoteViewUiBinder.class);

	interface NoteViewUiBinder extends UiBinder<Widget, NoteView> {

	}

	@UiField
	DropFilesPanel dropContainer;
	@UiField
	ExpandingArea name;
	@UiField
	NoteDateField dateField;
	@UiField
	NoteTimeField timeField;
	@UiField
	SimpleLinksTextArea description;
	@UiField
	AttachmentPanelView attachmentView;
	@UiField
	Label completeBtn;
	@UiField
	FlowPanel contentWrapper;
	@UiField
	Label expandIcon;
	@UiField
	Label restoreNodelabel;
	@UiField
	Label favoriteIcon;
	@UiField
	DotActionView dotActionView;
	@UiField
	Label copyIcon;
	@UiField
	Label dragIcon;

	public NoteView() {
		initWidget(uiBinder.createAndBindUi(this));
		dropContainer.addStyleName(CalendarConstants.NOTE_CONTAINER_JS_STYLE);
		name.setMaxLength(GlobalConstants.TEXT_LIMIT_SYMBOLS_400);
		dateField.setWithValidation(true);
		description.setMultiLine(true);
		description.setPlaceholder(GlobalConstants.THREE_DOTS_STRING);
		description.setMaxLength(GlobalConstants.TEXT_LIMIT_SYMBOLS_2000);
		RootHighlightDropFilesPanel.attachDragHandlers(getElement());
	}

	void setFavorite(boolean isFavorite) {
		favoriteIcon.setStyleName(GlobalConstants.FAVORITE, isFavorite);
	}

	public Element getDragElement() {
		return getElement();
	}

	boolean isDescriptionEmpty() {
		return description.getText().isEmpty();
	}

	void setNoteName(String noteName) {
		name.setText(noteName);
	}

	void refreshDescriptionEditModeContent(boolean isEditState, Optional<CursorPosition> cursorPosition) {
		description.refreshEditModeContent(isEditState, cursorPosition);
	}

	void updateDescriptionEditModeContent(boolean isEditState, String noteDescription) {
		description.updateEditModeContent(isEditState, noteDescription);
	}

	void setDescriptionVisible(boolean visible) {
		description.setVisible(visible);
	}

	public boolean isDescriptionVisible() {
		return description.isVisible();
	}

	void refreshDateValue(Date dateNote) {
		dateField.setValue(dateNote, false);
		timeField.setValue(DateUtils.isMidnight(dateNote) ? null : dateNote, false);
	}

	private void setItemState(String stateStyle, boolean set) {
		contentWrapper.setStyleName(stateStyle, set);
	}

	void setNoteNormalState() {
		setNameFocus(false);
		setDescriptionFocus(false);
		setSelectedStyle(false);
	}

	ExpandingArea getName() {
		return name;
	}

	SimpleLinksTextArea getDescription() {
		return description;
	}

	void setNoteSelectedState() {
		setSelectedStyle(true);
	}

	void updateDatePopupPosition() {
		if (getDatePopupCalendar().isShowing()) {
			dateField.updateCalendarPosition();
		}
	}

	void setDateValue(Date defaultTime) {
		dateField.setValue(defaultTime);
	}

	void setTimeValue(Date defaultTime) {
		timeField.setValue(defaultTime);
	}

	void setFocusedStyle(boolean focused) {
		setItemState(FOCUSED_STYLE, focused);
	}

	void setOpenedStyle(boolean opened) {
		setItemState(OPENED_STYLE, opened);
	}

	void setRemoveHoveredStyle(boolean hovered) {
		setItemState(REMOVE_HOVERED_STYLE, hovered);
	}

	public void setStubStyle(boolean set) {
		setItemState("reminders-item--stub", set);
	}

	private void setSelectedStyle(boolean selected) {
		setItemState(SELECTED_STYLE, selected);
	}

	boolean isOpened() {
		return isItemState(OPENED_STYLE);
	}

	boolean isSelected() {
		return isItemState(SELECTED_STYLE);
	}

	private boolean isItemState(String stateStyle) {
		return contentWrapper.getStyleName().contains(stateStyle);
	}

	public FlowPanel getContentWrapper() {
		return contentWrapper;
	}

	void refreshDateExpired(Date date) {
		final boolean expired = date != null && DateUtils.isExpiredWithTime(date);
		dateField.setExpired(expired);
		if (timeField.getValue() != null) {
			timeField.setExpired(expired);
		}
	}

	public void setNameFocus(boolean focus) {
		Scheduler.get().scheduleDeferred(() -> name.setFocus(focus));
	}

	boolean isNameFocus() {
		return name.isFocused();
	}

	boolean isDescriptionFocus() {
		return description.isFocused();
	}

	void setDescriptionFocus(boolean focus) {
		if (focus) {
			Scheduler.get().scheduleDeferred(() -> description.grabFocus());
		} else {
			description.releaseFocus();
		}
	}

	void insertContentEditableLineBreak() {
		description.insertContentEditableLineBreak();
	}

	void setNameBoxIncorrectFieldStyle(boolean isIncorrect) {
		name.setStyleName(GlobalConstants.INCORRECT_FIELD_TYPE_2, isIncorrect);
	}

	void refreshNamePlaceholder() {
		final boolean isEmpty = name.isEmptyValue();
		setNameBoxIncorrectFieldStyle(isEmpty);
	}

	boolean isNameBoxEmpty() {
		return name.isEmptyValue();
	}

	DropFilesPanel getDropContainer() {
		return dropContainer;
	}

	AttachmentPanelView getAttachmentView() {
		return attachmentView;
	}

	HandlerRegistration addClickHandler(ClickHandler handler) {
		return addDomHandler(handler, ClickEvent.getType());
	}

	HandlerRegistration addDateValueChangeHandler(ValueChangeHandler<Date> handler) {
		return dateField.addValueChangeHandler(handler);
	}

	HandlerRegistration addTimeValueChangeHandler(ValueChangeHandler<Date> handler) {
		return timeField.addValueChangeHandler(handler);
	}

	HandlerRegistration addNameBoxKeyDownHandler(EventListener listener) {
		return DomUtils.addEventListenerTo(name.getElement(), BrowserEvents.KEYDOWN, listener, true);
	}

	HandlerRegistration addDescriptionBoxKeyDownHandler(EventListener listener) {
		return DomUtils.addEventListenerTo(description.getElement(), BrowserEvents.KEYDOWN, listener, true);
	}

	HandlerRegistration addDescriptionInputHandler(EventListener listener) {
		return DomUtils.addEventListenerTo(description.getElement(), BrowserEvents.INPUT, listener, true);
	}

	HandlerRegistration addNameBoxValidationHandler(ValidationHandler handler) {
		return name.addValidationHandler(handler);
	}

	HandlerRegistration addNameBoxFocusHandler(FocusHandler handler) {
		return name.addFocusHandler(handler);
	}

	HandlerRegistration addNameBoxBlurHandler(BlurHandler handler) {
		return name.addBlurHandler(handler);
	}

	HandlerRegistration addCompleteBtnClickHandler(ClickHandler handler) {
		return completeBtn.addDomHandler(handler, ClickEvent.getType());
	}

	HandlerRegistration addCompleteBtnMouseOverHandler(MouseOverHandler handler) {
		return completeBtn.addDomHandler(handler, MouseOverEvent.getType());
	}

	HandlerRegistration addCompleteBtnMouseOutHandler(MouseOutHandler handler) {
		return completeBtn.addDomHandler(handler, MouseOutEvent.getType());
	}

	HandlerRegistration addRestoreNodelabelClickHandler(ClickHandler handler) {
		return restoreNodelabel.addClickHandler(handler);
	}

	HandlerRegistration addCopyIconClickHandler(ClickHandler handler) {
		return copyIcon.addClickHandler(handler);
	}

	void refreshCopyIconVisibility() {
		copyIcon.setVisible(isDescriptionVisible() && !isDescriptionEmpty());
	}

	HandlerRegistration addFavoriteIconClickHandler(ClickHandler handler) {
		return favoriteIcon.addClickHandler(handler);
	}

	Label getRestoreNodelabel() {
		return restoreNodelabel;
	}

	BasePopupPanel getDatePopupCalendar() {
		return dateField.getPopupCalendar();
	}

	BasePopupPanel getTimePopupCalendar() {
		return timeField.getPopupCalendar();
	}

	String getTrimmedNameText() {
		return name.getText().trim();
	}

	String getTrimmedDescriptionText() {
		return description.getText().trim();
	}

	boolean isSameDescriptionText(String text) {
		return description.isSameText(text);
	}

	Date getViewDate() {
		return dateField.getValue();
	}

	Date getViewTime() {
		return timeField.getValue();
	}

	NoteDateField getDateField() {
		return dateField;
	}

	Label getCopyIcon() {
		return copyIcon;
	}

	Label getFavoriteIcon() {
		return favoriteIcon;
	}

	NoteTimeField getTimeField() {
		return timeField;
	}

	Label getExpandIcon() {
		return expandIcon;
	}

	DotActionView getDotActionView() {
		return dotActionView;
	}
}