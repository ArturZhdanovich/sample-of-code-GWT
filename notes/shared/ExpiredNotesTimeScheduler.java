package app.m8.web.client.view.calendarmodule.notes.shared;

import app.components.client.util.DateUtils;
import app.m8.web.client.view.calendarmodule.calendar.util.CalendarConstants;
import app.m8.web.client.view.calendarmodule.notes.events.RefreshNotesExpiredEvent;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Date;

@Singleton
public class ExpiredNotesTimeScheduler {

	private Timer minuteTimer;
	private EventBus eventBus;

	@Inject
	public ExpiredNotesTimeScheduler(EventBus eventBus) {
		this.eventBus = eventBus;
		initMinuteTimer();
		startMinuteTimer();
	}
	
	private void initMinuteTimer() {
		minuteTimer = new Timer() {

			@Override
			public void run() {
				eventBus.fireEvent(new RefreshNotesExpiredEvent());
			}
		};
	}

	private void startMinuteTimer() {
		Date now = new Date();
		// кол-во в миллисекундах до следующей минуты
		int secondsOffset = (DateUtils.SECONDS_IN_MINUTE  - DateUtils.getSeconds(now)) * DateUtils.MILLISECONDS_IN_SECOND;

		// кол-во в миллисекундах до следующего времени кратного 15 минутам(с начала часа)
		int minutes = DateUtils.getMinutes(now);
		final int minPastQuarter = minutes % CalendarConstants.QUARTER_VALUE;
		// отнимаем 1 т.к. отсчет минут идет с 0
		int minutesToQuarter = CalendarConstants.QUARTER_VALUE - minPastQuarter - 1;
		int minutesOffset = minutesToQuarter * DateUtils.SECONDS_IN_MINUTE * DateUtils.MILLISECONDS_IN_SECOND;

		// кол-во миллисекунд, через которое необходимо запустить таймер
		// он будет запущен через secondsToWaitAfterQuarterStarts после начала следующей четверти часа
		// это позволит обновлять состояние просроченных напоминаний в соответствии с текущим возможным значением времени напоминания
		// эти 5 секунд добавлены для небольшой и по сути не слишком необходимой задержки
		// т.к. мы начинаем считать отсрочку с секунд, а не с милисекунд,
		// да и на случай других запланированных действий по расписанию в приложении, что бы они одновременно в началом часа не возникали
		int secondsToWaitAfterQuarterStarts = 5;
        int scheduleTime = minutesOffset + secondsOffset + secondsToWaitAfterQuarterStarts * DateUtils.MILLISECONDS_IN_SECOND;
        new Timer() {

			@Override
			public void run() {
				if (minuteTimer != null) {
					minuteTimer.run();
					minuteTimer.scheduleRepeating(CalendarConstants.QUARTER_VALUE * DateUtils.MILLISECONDS_IN_MINUTE);
				}
			}
		}.schedule(scheduleTime);
	}
}