package com.meridian.reportreliability.model;

public record ReportSummary(
        int totalRuns,
        int successfulRuns,
        int neverSucceededRuns,
        int retryRecommendedRuns,
        int unresolvedRuns,
        int reviewRequiredRuns) {
}
