/* Copyright (C) Red Hat 2023 */
package com.redhat.jws.insights;

import java.util.Map;

import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.reports.AbstractTopLevelReportBase;
import com.redhat.insights.reports.InsightsSubreport;

/**
 * The main report for Tomcat/JWS. The superclass is the one generating the
 * "basic" report as long as generateReport is called.
 */
public class TomcatReport extends AbstractTopLevelReportBase {

    private TomcatReport(
            InsightsLogger logger,
            InsightsConfiguration config,
            Map<String, InsightsSubreport> subReports) {
        super(logger, config, subReports);
    }

    public static TomcatReport of(InsightsLogger logger, InsightsConfiguration configuration, Map<String, InsightsSubreport> subReports) {
        return new TomcatReport(logger, configuration, subReports);
    }

    @Override
    protected long getProcessPID() {
        String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        int atIndex = name.indexOf('@');
        if (atIndex > 0) {
            try {
                return Long.parseLong(name.substring(0, atIndex));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        // Fallback: use ProcessHandle if available (Java 9+)
        return ProcessHandle.current().pid();
    }

    @Override
    protected Package[] getPackages() {
        return Package.getPackages();
    }

}