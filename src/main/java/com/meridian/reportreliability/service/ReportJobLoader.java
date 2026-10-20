package com.meridian.reportreliability.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.reportreliability.model.ReportJob;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class ReportJobLoader {
    private final List<ReportJob> jobs;

    public ReportJobLoader(ObjectMapper objectMapper) {
        try {
            var resource = new ClassPathResource("data/report_jobs.json");
            Map<String, List<ReportJob>> data = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<>() {
                    });
            this.jobs = List.copyOf(data.getOrDefault("jobs", List.of()));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load data/report_jobs.json", exception);
        }
    }

    public List<ReportJob> getJobs() {
        return jobs;
    }
}
