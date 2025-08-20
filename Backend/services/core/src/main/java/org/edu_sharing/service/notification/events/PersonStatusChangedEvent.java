package org.edu_sharing.service.notification.events;

public record PersonStatusChangedEvent(
        String receiver,
        String firstname,
        String lastName,
        String oldStatus,
        String newStatus
) {
}
