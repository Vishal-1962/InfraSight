package com.infrasight.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "infrasight")
public class InfraSightProperties {

    /** Days to keep check results. Zero or less disables cleanup. */
    private int retentionDays = 30;

    /** Allow monitored URLs that resolve to loopback, private or link-local addresses. */
    private boolean allowPrivateTargets = false;

    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    public boolean isAllowPrivateTargets() { return allowPrivateTargets; }
    public void setAllowPrivateTargets(boolean allowPrivateTargets) { this.allowPrivateTargets = allowPrivateTargets; }
}
