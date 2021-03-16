package app.m8.web.client.view.calendarmodule.notes.table;

import app.components.client.CompositeView;
import app.m8.web.client.view.calendarmodule.notes.group.NotesGroupView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;


public class NotesTableView extends CompositeView {

	private static final NotesTableViewUiBinder uiBinder = GWT.create(NotesTableViewUiBinder.class);

	interface NotesTableViewUiBinder extends UiBinder<Widget, NotesTableView> {

	}

	@UiField
	FlowPanel notesTableContainer;
	@UiField
	FlowPanel notesHeaderContainer;

	@Inject
	public NotesTableView() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	void insertNotesGroup(NotesGroupView notesGroup, int beforeIndex) {
		notesTableContainer.insert(notesGroup, beforeIndex);
	}

	void addNotesGroup(NotesGroupView notesGroup) {
		notesTableContainer.add(notesGroup);
	}

	void clear() {
		notesTableContainer.clear();
	}

	FlowPanel getNotesTableContainer() {
		return notesTableContainer;
	}

	public FlowPanel getNotesHeaderPanel() {
		return notesHeaderContainer;
	}

}
