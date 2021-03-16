package app.m8.web.client.view.calendarmodule.notes.shared;

import app.components.client.DomUtils;
import app.components.client.notifications.NotificationManager;
import app.components.client.util.DateFormat;
import app.components.client.util.DateUtils;
import app.m8.web.client.message.AppConstants;
import app.m8.web.client.message.MessageFactory;
import app.m8.web.client.util.AppData;
import app.m8.web.client.view.calendarmodule.notes.events.RefreshNotesExpiredEvent;
import app.m8.web.client.view.notificationcenter.NotificationCenterPresenter;
import app.m8.web.client.view.notificationcenter.modal.NotificationsPopupModel;
import app.m8.web.shared.core.note.Note;
import app.m8.web.shared.core.notification.NotificationRecord;
import app.m8.web.shared.core.notification.NotificationType;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.EventListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Date;
import java.util.List;

@Singleton
public class NotesNotificationServiceImpl {

	private final NotificationManager notificationManager;
	private final NotificationCenterPresenter notificationCenterPresenter;
	private final NotesRegistryService notesRegistryService;
	private AppConstants appConstants = MessageFactory.getAppConstants();

	@Inject
	public NotesNotificationServiceImpl(EventBus eventBus,
										NotificationManager notificationManager,
										NotificationCenterPresenter notificationCenterPresenter,
										NotesRegistryService notesRegistryService) {
		this.notificationManager = notificationManager;
		this.notificationCenterPresenter = notificationCenterPresenter;
		this.notesRegistryService = notesRegistryService;
		eventBus.addHandler(RefreshNotesExpiredEvent.TYPE, event -> showNotifications());
	}

	private void showNotifications() {
		List<Note> expiredNotes = notesRegistryService.getExpiredNotesForNotification();
		expiredNotes.forEach(this::createNotification);
	}

	private void createNotification(Note note) {
		final Integer noteId = note.getId();
		final NotificationRecord notification = createNotificationRecord(noteId, note.getName(), note.getDateNote());

		if (!DomUtils.isDocumentFocused()) {
			EventListener onClickCallback = event -> {
				DomUtils.focusWindow();
				notificationCenterPresenter.hideAllNotificationsIfNeeded();
				notificationCenterPresenter.getModel().showNotePanel(noteId);
			};
			notificationManager.showNotificationPopupIfPermissionGranted(appConstants.noteName(), note.getName(), onClickCallback,
					NotificationManager.NOTIFICATION_TAG + noteId + note.getDateNote().getTime());
		}
		showInnerPopupNotification(notification);
	}

	private void showInnerPopupNotification(NotificationRecord notification) {
		NotificationsPopupModel popupModel = notificationCenterPresenter.getModel().getPopupModel();
		// проверяем новое ли это уведомление до добавления в модель попапа уведомлений
		NotificationRecord newRecord = !popupModel.getExistingRecord(notification).isPresent() ? notification : null;
		popupModel.addNotification(notification);
		notificationCenterPresenter.showPopupWithNotifications(newRecord);
	}

	private NotificationRecord createNotificationRecord(final int noteId, final String eventName, Date eventBeginTime) {
		NotificationRecord notification = new NotificationRecord();
		notification.setDateLog(eventBeginTime);
		notification.setType(NotificationType.REMINDER);
		notification.setCanMove(true);
		notification.setObjectId(noteId);
		notification.setFavorite(false);
		notification.setAuthorStaffId(AppData.getCurrentUserId());
		notification.setNameObjectFirst(eventName);
		notification.setNameObjectSecond(DateUtils.formatDate(DateFormat.FT1, eventBeginTime));
		return notification;
	}
}
