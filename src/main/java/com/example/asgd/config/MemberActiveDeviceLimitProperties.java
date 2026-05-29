package com.example.asgd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "member.active-device-limit")
public class MemberActiveDeviceLimitProperties {

    private boolean enabled = true;
    private int maxActiveDevices = 3;
    private Duration leaseTtl = Duration.ofMinutes(10);
    private String failureStrategy = "fail-open";

    public MemberActiveDeviceLimitProperties() {
    }

    public MemberActiveDeviceLimitProperties(
            boolean enabled,
            int maxActiveDevices,
            Duration leaseTtl,
            String failureStrategy
    ) {
        this.enabled = enabled;
        this.maxActiveDevices = maxActiveDevices;
        this.leaseTtl = leaseTtl;
        this.failureStrategy = failureStrategy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxActiveDevices() {
        return maxActiveDevices;
    }

    public void setMaxActiveDevices(int maxActiveDevices) {
        this.maxActiveDevices = maxActiveDevices;
    }

    public Duration getLeaseTtl() {
        return leaseTtl;
    }

    public void setLeaseTtl(Duration leaseTtl) {
        this.leaseTtl = leaseTtl;
    }

    public String getFailureStrategy() {
        return failureStrategy;
    }

    public void setFailureStrategy(String failureStrategy) {
        this.failureStrategy = failureStrategy;
    }

    public boolean failOpen() {
        return !"fail-closed".equalsIgnoreCase(failureStrategy);
    }
}
