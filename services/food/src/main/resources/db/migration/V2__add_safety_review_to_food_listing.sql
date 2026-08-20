-- The Safety Agent's pre-publish hold (spec §22, Phase 9). This is
-- deliberately separate from the FLAGGED status (already in V1's
-- chk_food_status and FoodStatus's transition map): FLAGGED is for a
-- live listing flagged after publish (e.g. from a future Trust report),
-- while this is a hold on a still-DRAFT listing that blocks publish
-- outright — a boolean gate, not a state-machine transition, because a
-- DRAFT listing under review is still a DRAFT, not some new status.
ALTER TABLE food.food_listing ADD COLUMN requires_safety_review BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE food.food_listing ADD COLUMN safety_review_reason TEXT;
