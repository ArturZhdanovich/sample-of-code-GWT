package app.m8.web.client.view.calendarmodule.notes.storage;

import app.m8.web.client.util.AppData;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.storage.client.Storage;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class NotesDescriptionVisibilityStorageImpl implements NotesDescriptionVisibilityStorage {

	private static final String NOTES_DESCRIPTION_VISIBILITY_LOCAL_STORAGE_KEY = "NotesDescriptionVisibility";
	private static final String NOTE_ID = "note_id";
	private static final String DESCRIPTION_VISIBILITY = "visible";

	private Map<Integer, Boolean> descriptionVisibilityCache;

	@Override
	public boolean isDescriptionOpen(Integer noteId) {
		if (getDescriptionVisibilityCache().containsKey(noteId)) {
			return getDescriptionVisibilityCache().get(noteId);
		}
		return true;
	}

	@Override
	public void setDescriptionOpen(Integer noteId, boolean isOpen) {
		getDescriptionVisibilityCache().put(noteId, isOpen);
		writeNoteDescriptionVisibilityToLocalStorage(noteId, true, isOpen);
	}

	@Override
	public void deleteNote(Integer noteId) {
		getDescriptionVisibilityCache().remove(noteId);
		writeNoteDescriptionVisibilityToLocalStorage(noteId, false, false);
	}

	private Map<Integer, Boolean> getDescriptionVisibilityCache() {
		if (descriptionVisibilityCache == null) {
			descriptionVisibilityCache = getStorageValues();
		}
		return descriptionVisibilityCache;
	}

	private Map<Integer, Boolean> getStorageValues() {
		Map<Integer, Boolean> descriptionVisibility = new HashMap<>();
		final Storage storage = Storage.getLocalStorageIfSupported();
		if (storage != null) {
			String notesVisibilityStorageString = storage.getItem(getStorageKey());
			if (notesVisibilityStorageString == null || notesVisibilityStorageString.isEmpty()) {
				return descriptionVisibility;
			}
			JSONArray jsonArray = JSONParser.parseStrict(notesVisibilityStorageString).isArray();
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject noteData = jsonArray.get(i).isObject();
				if (noteData == null) {
					continue;
				}
				int noteId = (int) noteData.get(NOTE_ID).isNumber().doubleValue();
				boolean isVisible = noteData.get(DESCRIPTION_VISIBILITY).isBoolean().booleanValue();
				descriptionVisibility.put(noteId, isVisible);
			}
		}
		return descriptionVisibility;
	}

	private void writeNoteDescriptionVisibilityToLocalStorage(Integer noteId, boolean writeValue, boolean add) {
		final Storage storage = Storage.getLocalStorageIfSupported();
		if (storage != null) {
			String notesVisibilityStorageString = storage.getItem(getStorageKey());
			if (notesVisibilityStorageString == null || notesVisibilityStorageString.isEmpty()) {
				notesVisibilityStorageString = "[]";
			}
			JSONArray jsonArray = JSONParser.parseStrict(notesVisibilityStorageString).isArray();
			Map<Integer, JSONObject> noteDescriptionVisibilityData = new HashMap<>();
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject noteData = jsonArray.get(i).isObject();
				if (noteData == null) {
					continue;
				}
				int savedNoteId = (int) noteData.get(NOTE_ID).isNumber().doubleValue();
				noteDescriptionVisibilityData.put(savedNoteId, noteData);
			}
			noteDescriptionVisibilityData.remove(noteId);
			if (writeValue) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put(NOTE_ID, new JSONNumber(noteId));
				jsonObject.put(DESCRIPTION_VISIBILITY, JSONBoolean.getInstance(add));
				noteDescriptionVisibilityData.put(noteId, jsonObject);
			}

			JSONArray jsonArrayToSave = new JSONArray();
			int i = 0;
			for (JSONObject jsonObject : noteDescriptionVisibilityData.values()) {
				jsonArrayToSave.set(i, jsonObject);
				i++;
			}
			storage.setItem(getStorageKey(), jsonArrayToSave.toString());
		}
	}

	private String getStorageKey() {
		return NOTES_DESCRIPTION_VISIBILITY_LOCAL_STORAGE_KEY + AppData.getStaffId();
	}
}