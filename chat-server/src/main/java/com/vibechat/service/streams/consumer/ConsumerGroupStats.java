package com.vibechat.service.streams.consumer;

/**
 * Consumer Group 통계 DTO
 */
public class ConsumerGroupStats {
    private final boolean containerRunning;
    private final boolean consumersHealthy;

    public ConsumerGroupStats(boolean containerRunning, boolean consumersHealthy) {
        this.containerRunning = containerRunning;
        this.consumersHealthy = consumersHealthy;
    }

    public boolean isContainerRunning() {
        return containerRunning;
    }

    public boolean areConsumersHealthy() {
        return consumersHealthy;
    }

    public boolean isOverallHealthy() {
        return containerRunning && consumersHealthy;
    }

    @Override
    public String toString() {
        return String.format("ConsumerGroupStats{container=%s, consumers=%s, overall=%s}",
            containerRunning ? "RUNNING" : "STOPPED",
            consumersHealthy ? "HEALTHY" : "UNHEALTHY",
            isOverallHealthy() ? "OK" : "FAIL"
        );
    }
}