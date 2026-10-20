package com.meridian.reportreliability.model;

import java.time.LocalDateTime;

public record ReportJob(
        String job_id,
        String run_id,
        String tenant_id,
        String tenant_name,
        String report_name,
        LocalDateTime scheduled_at,
        String status,
        int attempt_number,
        int duration_seconds,
        Integer row_count) {
}
