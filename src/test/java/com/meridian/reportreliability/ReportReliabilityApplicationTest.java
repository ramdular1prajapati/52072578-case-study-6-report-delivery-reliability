package com.meridian.reportreliability;

import com.meridian.reportreliability.model.RunOutcome;
import com.meridian.reportreliability.service.ReportRunAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReportReliabilityApplicationTest {

    @Autowired
    private ReportRunAnalyzer analyzer;

    @Test
    void providedDatasetFindsBothRunsThatNeverSucceeded() {
        assertThat(analyzer.getSummary().totalRuns()).isEqualTo(12);
        assertThat(analyzer.getSummary().neverSucceededRuns()).isEqualTo(2);
        assertThat(analyzer.findByRunId("R-005").outcome()).isEqualTo(RunOutcome.UNRESOLVED);
        assertThat(analyzer.findByRunId("R-010").outcome()).isEqualTo(RunOutcome.RETRY_RECOMMENDED);
    }
}
