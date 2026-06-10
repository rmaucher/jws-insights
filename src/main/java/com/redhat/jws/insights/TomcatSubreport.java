/* Copyright (C) Red Hat 2023 */
package com.redhat.jws.insights;

import org.apache.catalina.util.ServerInfo;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.redhat.insights.reports.InsightsSubreport;

/**
 * JMX-based subreport for Tomcat that collects telemetry data by directly
 * querying the MBeanServer and using StatusTransformer to generate JSON
 * output including VM state, connector state, and detailed component state.
 */
public class TomcatSubreport implements InsightsSubreport {

    private final TomcatSubreportSerializer serializer;

    public TomcatSubreport() {
        serializer = new TomcatSubreportSerializer();
    }

    @Override
    public void generateReport() {
        // The serializer will generate the report
    }

    @Override
    public JsonSerializer<InsightsSubreport> getSerializer() {
        return serializer;
    }

    @Override
    public String getVersion() {
        // Use the Tomcat version number
        return ServerInfo.getServerNumber();
    }

    public void close() {
        serializer.unregister();
    }

}