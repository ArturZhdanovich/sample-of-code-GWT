package app.m8.web.client.view.calendarmodule.notes.note.area;

import app.components.client.CompositeView;
import app.components.client.DomUtils;
import app.components.client.base.ExpandingArea;
import app.components.client.base.event.ValidationHandler;
import app.components.client.popup.popupmanager.BasePopupPanel;
import app.components.client.upload.DropFilesPanel;
import app.components.client.upload.RootHighlightDropFilesPanel;
import app.components.client.util.DateUtils;
import app.m8.web.client.attachment.AttachmentPanelView;
import app.m8.web.client.message.AppConstants;
import app.m8.web.client.message.MessageFactory;
import app.m8.web.client.view.calendarmodule.notes.note.NoteView;
import app.m8.web.client.view.calendarmodule.notes.note.components.NoteDateField;
import app.m8.web.client.view.calendarmodule.notes.note.components.NoteTimeField;
import app.m8.web.shared.GlobalConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
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
import com.google.inject.Inject;

import java.util.Date;


public class NoteAreaView extends CompositeView {

	private static final AppConstants consts = MessageFactory.getAppConstants();
	private static final NoteAreaViewUiBinder uiBinder = GWT.create(NoteAreaViewUiBinder.class);

	interface NoteAreaViewUiBinder extends UiBinder<Widget, NoteAreaView> {

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
	Label clearBtn;
	@UiField
	ExpandingArea description;
	@UiField(provided = true)
	AttachmentPanelView attachmentView = new AttachmentPanelView();
	@UiField
	FlowPanel attachmentContainer;
	@UiField
	FlowPanel contentWrapper;
	@UiField
	Label expandIcon;

	@Inject
	public NoteAreaView() {
		initWidget(uiBinder.createAndBindUi(this));
		name.setMaxLength(GlobalConstants.TEXT_LIMIT_SYMBOLS_400);
		description.setMaxLength(GlobalConstants.TEXT_LIMIT_SYMBOLS_2000);
		description.setPlaceholder(GlobalConstants.THREE_DOTS_STRING);
		dateField.setFirstEnabledDate(new Date());
		RootHighlightDropFilesPanel.attachDragHandlers(getElement());
		setNoteAreaNormalState();
	}

	public void setNoteAreaNormalState() {
		refreshNamePlaceholder(false);
		setFocusedStyle(false);
		name.setFocus(false);
		setDescriptionVisible(false);
	}

	void setDescriptionVisible(boolean descriptionVisible) {
		description.setVisible(descriptionVisible);
		refreshOpenedStile();
	}

	public void setNoteAreaFocusState() {
		setFocusedStyle(true);
		setNameFocus(true);
	}

	void refreshDateExpired(Date date) {
		final boolean isExpired = date != null && DateUtils.isExpiredWithTime(date);
		dateField.setExpired(isExpired);
		if (timeField.getValue() != null) {
			timeField.setExpired(isExpired);
		}
	}

	/**
	 * Установит фокус
	 */
	public void setNameFocus(boolean focus) {
		if (focus) {
			name.grabFocusDeffered();
		} else {
			name.setFocus(false);
		}
		updatePlaceholder(focus);
	}

	void updatePlaceholder(boolean focus) {
		if (focus) {
			name.removePlaceholder();
		} else {
			validatePlaceholder();
		}
	}

	void setDescriptionFocus(boolean focus) {
		description.setFocus(focus);
	}

	void refreshNameBox() {
		name.setText(GlobalConstants.EMPTY_STRING);
	}

	void refreshDescriptionBox() {
		description.setText(GlobalConstants.EMPTY_STRING);
	}

	void showDescription() {
		setDescriptionVisible(true);
	}

	void hideDescription() {
		setDescriptionVisible(false);
	}

	public boolean isDescriptionVisible() {
		return description.isVisible();
	}

	private void setOpenedStyle(boolean opened) {
		contentWrapper.setStyleName(NoteView.OPENED_STYLE, opened);
	}

	public void refreshOpenedStile() {
		setOpenedStyle(getDescription().isVisible());
	}

	public void setDateValue(Date defaultTime) {
		dateField.setValue(defaultTime);
	}

	public void setTimeValue(Date dateNote) {
		if (!DateUtils.isMidnight(dateNote)) {
			timeField.setValue(dateNote, false);
		} else {
			timeField.setValue(null, false);
		}
	}

	void validatePlaceholder() {
		refreshNamePlaceholder(getTrimmedNameText().isEmpty());
	}

	private void refreshNamePlaceholder(boolean invalid) {
		name.setPlaceholder(invalid ? consts.noteNameEmptyPlaceholder() : consts.add());
		name.setStyleName(GlobalConstants.INCORRECT_FIELD_TYPE_2, invalid);
	}

	private void setFocusedStyle(boolean focused) {
		contentWrapper.setStyleName(NoteView.FOCUSED_STYLE, focused);
	}

	boolean isFocusedStyle() {
		return contentWrapper.getStyleName().contains(NoteView.FOCUSED_STYLE);
	}

	ExpandingArea getName() {
		return name;
	}

	ExpandingArea getDescription() {
		return description;
	}

	NoteDateField getDateField() {
		return dateField;
	}

	NoteTimeField getTimeField() {
		return timeField;
	}

	public Label getClearBtn() {
		return clearBtn;
	}

	DropFilesPanel getDropContainer() {
		return dropContainer;
	}

	public String getTrimmedNameText() {
		return name.getText().trim();
	}

	public void setNameText(String nameText) {
		name.setText(nameText);
	}

	public String getTrimmedDescriptionText() {
		return description.getText().trim();
	}

	Date getViewDate() {
		return dateField.getValue();
	}

	Date getViewTime() {
		return timeField.getValue();
	}

	public Label getExpandIcon() {
		return expandIcon;
	}

	public AttachmentPanelView getAttachmentView() {
		return attachmentView;
	}

	AttachmentPanelView createAttachmentView() {
		final AttachmentPanelView attachmentPanelView = new AttachmentPanelView();
		if (attachmentView != null) {
			attachmentContainer.insert(attachmentPanelView, attachmentContainer.getWidgetIndex(attachmentView));
			attachmentView.removeFromParent();
		} else {
			attachmentContainer.add(attachmentPanelView);
		}
		attachmentView = attachmentPanelView;
		return attachmentView;
	}

	public void refreshIconsVisibility() {
		final boolean isIconsVisible = !getTrimmedNameText().isEmpty() || !getTrimmedDescriptionText().isEmpty();
		expandIcon.setStyleName(GlobalConstants.INVISIBLE_STYLE, !isIconsVisible);
		dateField.setStyleName(GlobalConstants.INVISIBLE_STYLE, !isIconsVisible);
		timeField.setStyleName(GlobalConstants.INVISIBLE_STYLE, !isIconsVisible);
		clearBtn.setStyleName(GlobalConstants.INVISIBLE_STYLE, !isIconsVisible);
	}

	BasePopupPanel getDatePopupCalendar() {
		return dateField.getPopupCalendar();
	}

	BasePopupPanel getTimePopupCalendar() {
		return timeField.getPopupCalendar();
	}

	HandlerRegistration addNameBoxFocusHandler(FocusHandler handler) {
		return name.addFocusHandler(handler);
	}

	public HandlerRegistration addNameBoxKeyDownHandler(EventListener listener) {
		return DomUtils.addEventListenerTo(name.getElement(), BrowserEvents.KEYDOWN, listener, true);
	}

	public HandlerRegistration addNameBoxChangeHandler(EventListener changeHandler) {
		return name.addInputHandler(changeHandler);
	}

	public HandlerRegistration addDescriptionBoxChangeHandler(EventListener changeHandler) {
		return description.addInputHandler(changeHandler);
	}

	public HandlerRegistration addValidationHandler(ValidationHandler handler) {
		return name.addValidationHandler(handler);
	}

	HandlerRegistration addTimeValueChangeHandler(ValueChangeHandler<Date> handler) {
		return timeField.addValueChangeHandler(handler);
	}

	HandlerRegistration addDescriptionBoxKeyDownHandler(EventListener listener) {
		return DomUtils.addEventListenerTo(description.getTextArea().getElement(), BrowserEvents.KEYDOWN, listener, true);
	}

	HandlerRegistration addDateValueChangeHandler(ValueChangeHandler<Date> handler) {
		return dateField.addValueChangeHandler(handler);
	}

	public HandlerRegistration addPrintMouseOverHandler(MouseOverHandler handler) {
		return addDomHandler(handler, MouseOverEvent.getType());
	}

	public HandlerRegistration addPrintMouseOutHandler(MouseOutHandler handler) {
		return addDomHandler(handler, MouseOutEvent.getType());
	}
}
