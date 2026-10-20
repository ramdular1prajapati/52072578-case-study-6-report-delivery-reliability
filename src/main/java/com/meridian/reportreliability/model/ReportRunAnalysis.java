package com.meridian.reportreliability.model;

import java.util.List;

public record ReportRunAnalysis(
        String runId,
        String tenantId,
        String tenantName,
        String reportName,
        String scheduledAt,
        RunOutcome outcome,
        int attemptCount,
        int latestAttemptNumber,
        String latestStatus,
        Integer successfulAttemptNumber,
        boolean retryRecommended,
        String reason,
        List<ReportJob> attempts) {
}
