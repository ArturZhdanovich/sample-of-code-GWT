package app.m8.web.client.view.calendarmodule.notes.animation;

import app.components.client.ClientConstants;
import app.components.client.animation.AnimationUtils;
import app.components.client.animation.velocity.BeginAnimationCallback;
import app.components.client.animation.velocity.CompleteAnimationCallback;
import app.m8.web.client.view.calendarmodule.notes.note.NotePresenter;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;

import java.util.List;

public class NotesAnimationUtils {

	private static final int REMOVE_ANIMATION_TIME = 50;
	private static final String BLINK_ANIMATION = "note-blink-animation";

	public static native void runMoveAnimation(JavaScriptObject insertedElements, JavaScriptObject clonedElements, JavaScriptObject elementHeights,
											   BeginAnimationCallback beginCallback, CompleteAnimationCallback completeCallback) /*-{
		$wnd.animateCalendarNotesMove(
				insertedElements,
				clonedElements,
				elementHeights,
				function(elements) {
					beginCallback.@app.components.client.animation.velocity.BeginAnimationCallback::onBegin(Lcom/google/gwt/core/client/JsArray;)(elements);
				},
				function(elements) {
					completeCallback.@app.components.client.animation.velocity.CompleteAnimationCallback::onComplete(Lcom/google/gwt/core/client/JsArray;)(elements);
				});
	}-*/;

	/**
	 * Запустить анимацию моргания нескольких элементов
	 * @param elements - список элементов
	 * @param completeCallback - коллбэк по завершению
	 */
	public static void runBlinkAnimation(List<NotePresenter> elements, CompleteAnimationCallback completeCallback) {
		elements.forEach(notePresenter -> notePresenter.getView().getContentWrapper().addStyleName(BLINK_ANIMATION));
		new Timer() {
			@Override
			public void run() {
				elements.forEach(notePresenter -> notePresenter.getView().getContentWrapper().removeStyleName(BLINK_ANIMATION));
				completeCallback.onComplete(null);
			}
		}.schedule(ClientConstants.ANIMATION_STEP_DURATION);
	}
	
	public static void runRemoveAnimation(JsArray<Element> animatedElements, CompleteAnimationCallback completeCallback) {
		AnimationUtils.animateElementDeletion(animatedElements, REMOVE_ANIMATION_TIME, 0, completeCallback);
	}
}
