package com.meridian.reportreliability.service;

import com.meridian.reportreliability.model.ReportJob;
import com.meridian.reportreliability.model.ReportRunAnalysis;
import com.meridian.reportreliability.model.ReportSummary;
import com.meridian.reportreliability.model.RunOutcome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ReportRunAnalyzer {
    private static final int MAX_ATTEMPTS = 3;
    private static final Set<String> FAILURE_STATUSES = Set.of("failed", "timed_out", "error");
    private static final String SUCCESS_STATUS = "completed";

    private final List<ReportRunAnalysis> runs;

    @Autowired
    public ReportRunAnalyzer(ReportJobLoader loader) {
        this(loader.getJobs());
    }

    public ReportRunAnalyzer(List<ReportJob> jobs) {
        Map<String, List<ReportJob>> grouped = jobs.stream()
                .collect(Collectors.groupingBy(
                        ReportJob::run_id,
                        TreeMap::new,
                        Collectors.toList()));

        this.runs = grouped.values().stream()
                .map(this::analyzeRun)
                .toList();
    }

    public List<ReportRunAnalysis> getRuns() {
        return runs;
    }

    public List<ReportRunAnalysis> getNeverSucceededRuns() {
        return runs.stream()
                .filter(run -> run.outcome() != RunOutcome.SUCCEEDED)
                .toList();
    }

    public ReportSummary getSummary() {
        int successful = count(RunOutcome.SUCCEEDED);
        int retryRecommended = count(RunOutcome.RETRY_RECOMMENDED);
        int unresolved = count(RunOutcome.UNRESOLVED);
        int reviewRequired = count(RunOutcome.REVIEW_REQUIRED);
        return new ReportSummary(
                runs.size(),
                successful,
                runs.size() - successful,
                retryRecommended,
                unresolved,
                reviewRequired);
    }

    public ReportRunAnalysis findByRunId(String runId) {
        return runs.stream()
                .filter(run -> run.runId().equals(runId))
                .findFirst()
                .orElse(null);
    }

    private int count(RunOutcome outcome) {
        return (int) runs.stream().filter(run -> run.outcome() == outcome).count();
    }

    private ReportRunAnalysis analyzeRun(List<ReportJob> runAttempts) {
        List<ReportJob> attempts = runAttempts.stream()
                .sorted(Comparator.comparingInt(ReportJob::attempt_number))
                .toList();
        ReportJob first = attempts.getFirst();
        ReportJob latest = attempts.getLast();
        Integer successfulAttempt = attempts.stream()
                .filter(job -> SUCCESS_STATUS.equalsIgnoreCase(job.status()))
                .map(ReportJob::attempt_number)
                .min(Integer::compareTo)
                .orElse(null);

        RunOutcome outcome;
        String reason;
        boolean retryRecommended = false;

        if (successfulAttempt != null) {
            outcome = RunOutcome.SUCCEEDED;
            reason = "Succeeded on attempt " + successfulAttempt + ".";
        } else if (FAILURE_STATUSES.contains(latest.status().toLowerCase())) {
            retryRecommended = latest.attempt_number() < MAX_ATTEMPTS;
            outcome = retryRecommended ? RunOutcome.RETRY_RECOMMENDED : RunOutcome.UNRESOLVED;
            reason = retryRecommended
                    ? "No attempt succeeded; latest attempt ended with '" + latest.status()
                    + "'. Retry is within the " + MAX_ATTEMPTS + "-attempt limit."
                    : "No attempt succeeded; latest attempt ended with '" + latest.status()
                    + "' after the " + MAX_ATTEMPTS + "-attempt limit.";
        } else {
            outcome = RunOutcome.REVIEW_REQUIRED;
            reason = "No attempt succeeded and latest status '" + latest.status()
                    + "' is not classified; manual review is required.";
        }

        return new ReportRunAnalysis(
                first.run_id(),
                first.tenant_id(),
                first.tenant_name(),
                first.report_name(),
                first.scheduled_at().toString(),
                outcome,
                attempts.size(),
                latest.attempt_number(),
                latest.status(),
                successfulAttempt,
                retryRecommended,
                reason,
                attempts);
    }
}
