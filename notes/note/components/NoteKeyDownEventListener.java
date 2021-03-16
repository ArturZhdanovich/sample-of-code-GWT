package app.m8.web.client.view.calendarmodule.notes.note.components;

import app.components.client.util.UIUtils;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;

public class NoteKeyDownEventListener implements EventListener {

    private static final int DOUBLE_ENTER_DELAY = 300;

    private final NoteKeyDownEventListenerSupervisor noteKeyDownEventListenerSupervisor;
    private int enterKeyDownCounter;
    private Runnable singleEnterAction;
    private Timer singleEnterTimer = new Timer() {

        @Override
        public void run() {
            singleEnterAction.run();
            resetKeyDownCounter();
        }
    };

    public NoteKeyDownEventListener(NoteKeyDownEventListenerSupervisor noteKeyDownEventListenerSupervisor, Runnable singleEnterAction) {
        this.noteKeyDownEventListenerSupervisor = noteKeyDownEventListenerSupervisor;
        this.singleEnterAction = singleEnterAction;
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (UIUtils.isCmdEnterPressed(event)) {
            noteKeyDownEventListenerSupervisor.onCmdEnter(event);
        } else if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
            event.preventDefault();
            event.stopPropagation();
            if (enterKeyDownCounter == 0) {
                event.preventDefault();
                singleEnterTimer.schedule(DOUBLE_ENTER_DELAY);
            }
            enterKeyDownCounter++;
            if (enterKeyDownCounter == 2) {
                if (singleEnterTimer.isRunning()) {
                    singleEnterTimer.cancel();
                }
                noteKeyDownEventListenerSupervisor.onDoubleClick(event);
                resetKeyDownCounter();
            }
        }
    }

    private void resetKeyDownCounter() {
        enterKeyDownCounter = 0;
    }
}