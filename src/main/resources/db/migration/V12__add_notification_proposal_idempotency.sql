CREATE UNIQUE INDEX uk_notification_history_proposal_created_match
    ON notification_history(user_id, type, related_match_id)
    WHERE type = 'MATCH_PROPOSAL_CREATED'
      AND related_match_id IS NOT NULL;
