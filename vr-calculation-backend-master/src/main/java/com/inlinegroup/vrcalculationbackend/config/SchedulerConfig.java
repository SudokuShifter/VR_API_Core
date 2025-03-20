package com.inlinegroup.vrcalculationbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    private final VRCalcConfig config;

    public SchedulerConfig(VRCalcConfig config) {
        this.config = config;
    }

    @Bean
    public String getCronValue() {
        return config.getVrTaskSchedulerCron();
    }

    @Bean
    public String getCFCronValue() {
        return config.getVrCfSchedulerCron();
    }
}
