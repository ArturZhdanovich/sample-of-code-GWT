package app.m8.web.client.view.calendarmodule.notes.selection;

import app.m8.web.client.view.calendarmodule.notes.note.NotePresenter;

import com.google.inject.Singleton;

@Singleton
public class SingleNoteSelector implements NoteSelector {

	/**Выбранная заметка*/
	private NotePresenter selectedElement;
	
	@Override
	public void select(NotePresenter notePresenter) {
		if (notePresenter != null) {
			if (!notePresenter.equals(selectedElement)) {
				unSelect(selectedElement);
				selectedElement = notePresenter;
				setSelection(selectedElement, true);
			} else if (!notePresenter.isSelected()) {
				setSelection(selectedElement, true);
			}
		} else {
			clearSelection();
		}
	}
	
	@Override
	public void unSelect(NotePresenter notePresenter) {
		if (notePresenter != null && notePresenter.equals(selectedElement)) {
			setSelection(selectedElement, false);
			selectedElement = null;
		}
	}
	
	@Override
	public void clearSelection() {
		if (selectedElement != null) {
			unSelect(selectedElement);
		}
	}
	
	private void setSelection(NotePresenter element, boolean isSelected) {
		element.setSelected(isSelected);
	}
}
