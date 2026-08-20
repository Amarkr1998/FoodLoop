-- NGO Coordination Agent (services/ngo, Phase: deferred agents) sets this
-- when a proposal fulfills an open NGO bulk request. Nullable and additive:
-- every existing proposal and every non-NGO-initiated proposal leaves it
-- null, unaffected.
ALTER TABLE matching.match_proposal ADD COLUMN ngo_request_id UUID NULL;
