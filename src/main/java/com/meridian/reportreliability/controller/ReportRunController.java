package com.meridian.reportreliability.controller;

import com.meridian.reportreliability.model.ReportRunAnalysis;
import com.meridian.reportreliability.model.ReportSummary;
import com.meridian.reportreliability.service.ReportRunAnalyzer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReportRunController {
    private final ReportRunAnalyzer analyzer;

    public ReportRunController(ReportRunAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @GetMapping("/summary")
    public ReportSummary getSummary() {
        return analyzer.getSummary();
    }

    @GetMapping("/report-runs")
    public List<ReportRunAnalysis> getRuns() {
        return analyzer.getRuns();
    }

    @GetMapping("/report-runs/never-succeeded")
    public List<ReportRunAnalysis> getNeverSucceededRuns() {
        return analyzer.getNeverSucceededRuns();
    }

    @GetMapping("/report-runs/{runId}")
    public ReportRunAnalysis getRun(@PathVariable String runId) {
        ReportRunAnalysis run = analyzer.findByRunId(runId);
        if (run == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report run not found: " + runId);
        }
        return run;
    }
}
