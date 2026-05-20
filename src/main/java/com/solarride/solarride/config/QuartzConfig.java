package com.solarride.solarride.config;

import com.solarride.solarride.scheduler.SlaMonitorJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz is auto-configured by spring-boot-starter-quartz with JDBC job store
 * and Spring DI support. Properties are in application.yml under spring.quartz.
 * Clustered JDBC store ensures scheduled timers survive application restarts.
 */
@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail slaMonitorJobDetail() {
        return JobBuilder.newJob(SlaMonitorJob.class)
                .withIdentity("slaMonitorJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger slaMonitorTrigger(JobDetail slaMonitorJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(slaMonitorJobDetail)
                .withIdentity("slaMonitorTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * * * ?"))
                .build();
    }
}
