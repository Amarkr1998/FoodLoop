package com.foodloop.tenant.api;

import com.foodloop.tenant.domain.OrgMember;
import java.time.Instant;
import java.util.UUID;

public record OrgMemberResponse(UUID userId, String role, Instant joinedAt) {

    public static OrgMemberResponse from(OrgMember member) {
        return new OrgMemberResponse(member.getUserId(), member.getRole().name(), member.getJoinedAt());
    }
}
