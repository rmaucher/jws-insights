/* Copyright (C) Red Hat 2023 */
package com.redhat.jws.insights;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.redhat.insights.InsightsScheduler;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;

public class TomcatInsightsScheduler implements InsightsScheduler {

    private boolean active = true;
    private final ScheduledExecutorService utilityExecutor;
    private final InsightsConfiguration configuration;

    private ScheduledFuture<?> connectFuture;
    private ScheduledFuture<?> jarUpdateFuture;

    public TomcatInsightsScheduler(InsightsLogger logger,
            InsightsConfiguration configuration, ScheduledExecutorService utilityExecutor) {
        this.utilityExecutor = utilityExecutor;
        this.configuration = configuration;
    }

    @Override
    public boolean isShutdown() {
        return !active;
    }

    @Override
    public ScheduledFuture<?> scheduleConnect(Runnable command) {
        if (!active) {
            throw new IllegalStateException("Not active");
        }
        connectFuture = utilityExecutor.scheduleAtFixedRate(command,
                0, configuration.getConnectPeriod().getSeconds(), TimeUnit.SECONDS);
        return connectFuture;
    }

    @Override
    public ScheduledFuture<?> scheduleJarUpdate(Runnable command) {
        if (!active) {
            throw new IllegalStateException("Not active");
        }
        jarUpdateFuture = utilityExecutor.scheduleAtFixedRate(command,
                configuration.getUpdatePeriod().getSeconds(),
                configuration.getUpdatePeriod().getSeconds(), TimeUnit.SECONDS);
        return jarUpdateFuture;
    }

    @Override
    public void shutdown() {
        active = false;
        if (connectFuture != null) {
            connectFuture.cancel(true);
        }
        if (jarUpdateFuture != null) {
            jarUpdateFuture.cancel(true);
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        return List.of();
    }

}
