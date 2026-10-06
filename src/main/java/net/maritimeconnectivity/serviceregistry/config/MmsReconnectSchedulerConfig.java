package net.maritimeconnectivity.serviceregistry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class MmsReconnectSchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler mmsTaskScheduler() {
        ThreadPoolTaskScheduler tps = new ThreadPoolTaskScheduler();
        tps.setPoolSize(1);
        tps.setThreadNamePrefix("mms-reconnect-");
        tps.initialize();
        return tps;
    }

}
