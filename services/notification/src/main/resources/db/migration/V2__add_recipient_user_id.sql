-- Pickup Agent (spec §20) notifies a volunteer directly — a user, not an
-- org — which this table couldn't represent before now. recipient_org_id
-- becomes optional and recipient_user_id is added as the alternative;
-- exactly one must be set per row.
ALTER TABLE notification.notification
    ALTER COLUMN recipient_org_id DROP NOT NULL;

ALTER TABLE notification.notification
    ADD COLUMN recipient_user_id UUID;

ALTER TABLE notification.notification
    ADD CONSTRAINT chk_notification_recipient_exactly_one CHECK (
        (recipient_org_id IS NOT NULL) <> (recipient_user_id IS NOT NULL)
    );

CREATE INDEX idx_notification_recipient_user ON notification.notification (recipient_user_id);
