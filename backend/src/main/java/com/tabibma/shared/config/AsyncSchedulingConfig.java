package com.tabibma.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Async} (notification sends, Story 4.5) and {@code @Scheduled} (the reminder
 * sweep, ReminderService) — first consumers of either in this codebase.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncSchedulingConfig {
}
