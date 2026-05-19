package com.example.asgd.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ActivationCampaignProperties {

    @Value("${activation.campaign.start}")
    private String start;

    @Value("${activation.campaign.end}")
    private String end;

    public LocalDateTime start() {
        return LocalDateTime.parse(start);
    }

    public LocalDateTime end() {
        return LocalDateTime.parse(end);
    }
}
