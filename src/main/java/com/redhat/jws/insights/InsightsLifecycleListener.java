/* Copyright (C) Red Hat 2023 */
package com.redhat.jws.insights;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;

import com.redhat.insights.InsightsReportController;
import com.redhat.insights.core.httpclient.InsightsJdkHttpClient;
import com.redhat.insights.http.InsightsFileWritingClient;
import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.http.InsightsMultiClient;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.reports.InsightsReport;
import com.redhat.insights.reports.InsightsSubreport;
import com.redhat.insights.tls.PEMSupport;

public class InsightsLifecycleListener implements LifecycleListener {

    private InsightsReportController insightsReportController;
    private InsightsReport insightsReport;
    private InsightsLogger logger = new TomcatLogger();
    private TomcatInsightsConfiguration configuration = new TomcatInsightsConfiguration();
    private boolean useHttpClient = true;
    private boolean useFileClient = true;

    @Override
    public void lifecycleEvent(LifecycleEvent event) {

        if (Lifecycle.AFTER_START_EVENT.equals(event.getType())) {
            if (!(event.getLifecycle() instanceof Server)) {
                throw new IllegalArgumentException("Not associated with a server");
            }
            Server server = (Server) event.getLifecycle();

            // Init Insights
            PEMSupport pemSupport = new PEMSupport(logger, configuration);
            Supplier<SSLContext> sslContextSupplier = () -> {
               try {
                  return pemSupport.createTLSContext();
               } catch (Throwable e) {
                  throw new IllegalStateException("Error setting TLS", e);
               }
            };

            Map<String, InsightsSubreport> subReports = new LinkedHashMap<>(3);
            subReports.put("jars", new ClasspathJarInfoSubreport(logger));
            subReports.put("jws", new JWSSubreport(server, logger));
            // The "tomcat" report is the json from the status manager servlet
            subReports.put("tomcat", new TomcatSubreport());
            insightsReport = TomcatReport.of(logger, configuration, subReports);

            TomcatInsightsScheduler insightsScheduler =
                  new TomcatInsightsScheduler(logger, configuration, server.getUtilityExecutor());

            try {
                ArrayList<InsightsHttpClient> clients = new ArrayList<>();
                if (useHttpClient) {
                    clients.add(new InsightsJdkHttpClient(logger, configuration, sslContextSupplier));
                }
                if (useFileClient) {
                    clients.add(new InsightsFileWritingClient(logger, configuration));
                }
                insightsReportController = InsightsReportController.of(logger, configuration, insightsReport,
                        () -> new InsightsMultiClient(logger, clients.toArray(new InsightsHttpClient[0])),
                        insightsScheduler, new LinkedBlockingQueue<>());
                insightsReportController.generate();
            } catch (Throwable e) {
                throw new IllegalStateException("Insights init failure", e);
            }

        } else if (Lifecycle.STOP_EVENT.equals(event.getType())) {
            if (insightsReportController != null) {
                insightsReportController.shutdown();
            }
        } else if (Lifecycle.PERIODIC_EVENT.equals(event.getType())) {
        }

    }

    public String getIdentificationName() {
        return configuration.getIdentificationName();
    }

    public void setIdentificationName(String identificationName) {
        configuration.setIdentificationName(identificationName);
    }

    public String getMachineIdFilePath() {
        return configuration.getMachineIdFilePath();
    }

    public void setMachineIdFilePath(String machineIdFilePath) {
        configuration.setMachineIdFilePath(machineIdFilePath);
    }

    public String getArchiveUploadDir() {
        return configuration.getArchiveUploadDir();
    }

    public void setArchiveUploadDir(String archiveUploadDir) {
        configuration.setArchiveUploadDir(archiveUploadDir);
    }

    public String getCertFilePath() {
        return configuration.getCertFilePath();
    }

    public void setCertFilePath(String certFilePath) {
        configuration.setCertFilePath(certFilePath);
    }

    public String getCertHelperBinary() {
        return configuration.getCertHelperBinary();
    }

    public void setCertHelperBinary(String certHelperBinary) {
        configuration.setCertHelperBinary(certHelperBinary);
    }

    public String getConnectPeriod() {
        return configuration.getConnectPeriod().toString();
    }

    public void setConnectPeriod(String connectPeriod) {
        configuration.setConnectPeriodValue(connectPeriod);
    }

    public String getHttpClientRetryBackoffFactor() {
        return configuration.getHttpClientRetryBackoffFactorValue();
    }

    public void setHttpClientRetryBackoffFactor(String httpClientRetryBackoffFactor) {
        configuration.setHttpClientRetryBackoffFactorValue(httpClientRetryBackoffFactor);
    }

    public long getHttpClientRetryInitialDelay() {
        return configuration.getHttpClientRetryInitialDelay();
    }

    public void setHttpClientRetryInitialDelay(long httpClientRetryInitialDelay) {
        configuration.setHttpClientRetryInitialDelay(httpClientRetryInitialDelay);
    }

    public int getHttpClientRetryMaxAttempts() {
        return configuration.getHttpClientRetryMaxAttempts();
    }

    public void setHttpClientRetryMaxAttempts(int httpClientRetryMaxAttempts) {
        configuration.setHttpClientRetryMaxAttempts(httpClientRetryMaxAttempts);
    }

    public String getHttpClientTimeoutValue() {
        return configuration.getHttpClientTimeout().toString();
    }

    public void setHttpClientTimeout(String httpClientTimeout) {
        configuration.setHttpClientTimeoutValue(httpClientTimeout);
    }

    public String getKeyFilePath() {
        return configuration.getKeyFilePath();
    }

    public void setKeyFilePath(String keyFilePath) {
        configuration.setKeyFilePath(keyFilePath);
    }

    public String getMaybeAuthToken() {
        return configuration.getMaybeAuthToken().orElse(null);
    }

    public void setMaybeAuthTokenValue(String maybeAuthToken) {
        configuration.setMaybeAuthTokenValue(maybeAuthToken);
    }

    public String getProxyHost() {
        return configuration.getProxyHost();
    }

    public void setProxyHost(String proxyHost) {
        configuration.setProxyHost(proxyHost);
    }

    public int getProxyPort() {
        return configuration.getProxyPort();
    }

    public void setProxyPort(int proxyPort) {
        configuration.setProxyPort(proxyPort);
    }

    public String getUpdatePeriod() {
        return configuration.getUpdatePeriod().toString();
    }

    public void setUpdatePeriod(String updatePeriod) {
        configuration.setUpdatePeriod(updatePeriod);
    }

    public String getUploadBaseURL() {
        return configuration.getUploadBaseURL();
    }

    public void setUploadBaseURL(String uploadBaseURL) {
        configuration.setUploadBaseURL(uploadBaseURL);
    }

    public String getUploadUri() {
        return configuration.getUploadUri();
    }

    public void setUploadUri(String uploadUri) {
        configuration.setUploadUri(uploadUri);
    }

    public boolean isOptingOut() {
        return configuration.isOptingOut();
    }

    public void setOptingOut(boolean optingOut) {
        configuration.setOptingOut(optingOut);
    }

    public boolean getUseHttpClient() {
        return this.useHttpClient;
    }

    public void setUseHttpClient(boolean useHttpClient) {
        this.useHttpClient = useHttpClient;
    }

    public boolean getUseFileClient() {
        return this.useFileClient;
    }

    public void setUseFileClient(boolean useFileClient) {
        this.useFileClient = useFileClient;
    }

}
