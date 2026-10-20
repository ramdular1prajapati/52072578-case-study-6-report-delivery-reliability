package com.meridian.reportreliability.service;

import com.meridian.reportreliability.model.ReportJob;
import com.meridian.reportreliability.model.RunOutcome;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportRunAnalyzerTest {

    @Test
    void completedRetryMeansRunSucceeded() {
        var analyzer = new ReportRunAnalyzer(List.of(
                job("R-1", 1, "failed"),
                job("R-1", 2, "completed")));

        var run = analyzer.getRuns().getFirst();
        assertThat(run.outcome()).isEqualTo(RunOutcome.SUCCEEDED);
        assertThat(run.successfulAttemptNumber()).isEqualTo(2);
        assertThat(run.retryRecommended()).isFalse();
    }

    @Test
    void knownFailureWithinBudgetIsMarkedForRetry() {
        var analyzer = new ReportRunAnalyzer(List.of(job("R-2", 1, "error")));

        var run = analyzer.getRuns().getFirst();
        assertThat(run.outcome()).isEqualTo(RunOutcome.RETRY_RECOMMENDED);
        assertThat(run.retryRecommended()).isTrue();
    }

    @Test
    void exhaustedFailureAndUnknownStatusAreNotRetried() {
        var analyzer = new ReportRunAnalyzer(List.of(
                job("R-3", 1, "timed_out"),
                job("R-3", 2, "failed"),
                job("R-3", 3, "failed"),
                job("R-4", 1, "queued")));

        assertThat(analyzer.findByRunId("R-3").outcome()).isEqualTo(RunOutcome.UNRESOLVED);
        assertThat(analyzer.findByRunId("R-4").outcome()).isEqualTo(RunOutcome.REVIEW_REQUIRED);
        assertThat(analyzer.getSummary().neverSucceededRuns()).isEqualTo(2);
    }

    private ReportJob job(String runId, int attempt, String status) {
        return new ReportJob(
                "J-" + runId + "-" + attempt,
                runId,
                "T-1",
                "Tenant One",
                "Monthly Statement",
                LocalDateTime.parse("2026-09-01T02:00:00"),
                status,
                attempt,
                10,
                null);
    }
}
