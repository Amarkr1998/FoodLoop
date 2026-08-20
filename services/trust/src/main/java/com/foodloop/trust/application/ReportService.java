package com.foodloop.trust.application;

import com.foodloop.trust.domain.Report;
import com.foodloop.trust.domain.ReportReason;
import com.foodloop.trust.domain.ReportRepository;
import com.foodloop.trust.domain.UserBehaviorSignal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional
    public Report create(UUID tenantId, UUID reporterUserId, UUID targetUserId, ReportReason reason, String description) {
        return reportRepository.save(new Report(tenantId, reporterUserId, targetUserId, reason, description));
    }

    /** The Trust & Risk Agent's getReportHistory tool (spec §21). */
    @Transactional(readOnly = true)
    public List<Report> listForUser(UUID targetUserId) {
        return reportRepository.findByTargetUserIdOrderByCreatedAtDesc(targetUserId);
    }

    /** The Trust & Risk Agent's getUserBehaviorSignals tool (spec §21) — computed live, never a stored aggregate. */
    @Transactional(readOnly = true)
    public UserBehaviorSignal getSignals(UUID targetUserId) {
        return UserBehaviorSignal.from(listForUser(targetUserId));
    }
}
