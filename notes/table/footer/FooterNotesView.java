package app.m8.web.client.view.calendarmodule.notes.table.footer;

import app.components.client.CompositeView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class FooterNotesView extends CompositeView {

	private static final NotesGroupViewUiBinder uiBinder = GWT.create(NotesGroupViewUiBinder.class);

	interface NotesGroupViewUiBinder extends UiBinder<Widget, FooterNotesView> {
	}

	@UiField
	Label syncBtn;
    @UiField
    Label allNotesBtn;

    public FooterNotesView() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	public Label getSyncBtn() {
		return syncBtn;
	}

	public void setSyncBtnVisible(boolean synchGroup) {
		syncBtn.setVisible(synchGroup);
	}

	public HandlerRegistration addSyncBtnClickHandler(ClickHandler handler) {
		return syncBtn.addClickHandler(handler);
	}

	public void setAllNotesBtnVisible(boolean visible) {
		allNotesBtn.setVisible(visible);
	}

	public HandlerRegistration addAllNotesBtnClickHandler(ClickHandler handler) {
		return allNotesBtn.addClickHandler(handler);
	}
}