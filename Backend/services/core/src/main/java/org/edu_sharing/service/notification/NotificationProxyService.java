package org.edu_sharing.service.notification;

import org.edu_sharing.rest.notification.data.StatusDTO;
import org.edu_sharing.rest.notification.event.NotificationEventDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface NotificationProxyService {

    Page<NotificationEventDTO> getNotifications(String receiverId, List<StatusDTO> status, Pageable pageable);

    NotificationEventDTO setNotificationStatusByNotificationId(String id, StatusDTO status);

    void setNotificationStatusByReceiverId(String receiverId, List<StatusDTO> oldStatusList, StatusDTO newStatus);

    void deleteNotification(String id);
}
