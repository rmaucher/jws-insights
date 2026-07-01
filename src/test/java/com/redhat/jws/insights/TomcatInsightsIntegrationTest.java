/* Copyright (C) Red Hat 2023 */
package com.redhat.jws.insights;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import org.apache.catalina.Globals;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.json.JSONParser;
import org.junit.jupiter.api.Test;

public class TomcatInsightsIntegrationTest {

    private static final Log log = LogFactory.getLog(TomcatInsightsIntegrationTest.class);

    @Test
    public void testReport() throws Exception {

        long timeNow = System.currentTimeMillis();

        System.setProperty(Globals.CATALINA_HOME_PROP, "target/test-classes");
        Tomcat tomcat = new Tomcat();
        InsightsLifecycleListener listener = new InsightsLifecycleListener();
        listener.setArchiveUploadDir("target");
        listener.setOptingOut(false);
        listener.setUpdatePeriod("PT1S");
        listener.setMachineIdFilePath("target");
        listener.setUseHttpClient(false);
        tomcat.getServer().addLifecycleListener(listener);
        Connector connector = new Connector("HTTP/1.1");
        connector.setPort(0);
        tomcat.getService().addConnector(connector);
        tomcat.setConnector(connector);
        tomcat.addContext("/test", null);
        tomcat.addContext("/examples", "test-webapp");
        tomcat.start();

        Thread.sleep(2000);

        tomcat.stop();

        // Verify JSON file
        File dir = new File("target");
        File[] files = dir.listFiles();
        File insightsReport = null;
        for (File file : files) {
            if (file.getName().endsWith("_connect.json") && file.lastModified() > timeNow) {
                insightsReport = file;
            }
        }
        assertTrue(insightsReport != null);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                FileInputStream fis = new FileInputStream(insightsReport)) {
            fis.transferTo(out);
            String report = new String(out.toByteArray(), "UTF-8");
            log.info("Insights report: " + report);
            String result = (new JSONParser(report)).parse().toString();
            // Verify presence of basic report
            assertTrue(result.indexOf("basic") > 0);
            assertTrue(result.indexOf("idHash") > 0);
        }
    }

}
