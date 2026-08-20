package com.foodloop.notification.domain;

/** Every notification this service creates stays QUEUED — see pom.xml's Javadoc on why SENT/FAILED aren't reachable yet. */
public enum NotificationStatus {
    QUEUED,
    SENT,
    FAILED
}
