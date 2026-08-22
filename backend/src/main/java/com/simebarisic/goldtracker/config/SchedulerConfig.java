package com.simebarisic.goldtracker.config;

import com.simebarisic.goldtracker.service.ScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.TimeZone;

/**
 * Ekvivalent scheduler.js: cron raspored se cita iz .env (CRON_SCHEDULE, TIMEZONE),
 * isti 5-poljni unix cron format na koji je Sime navikao iz Node verzije
 * (npr. "0 7 * * *" = svaki dan u 7:00). Spring ocekuje 6 polja (s sekundama),
 * pa se ispred jednostavno doda "0 " za sekunde.
 */
@Configuration
public class SchedulerConfig implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);

    private final ScraperService scraperService;
    private final String cronSchedule;
    private final String timezone;

    public SchedulerConfig(
            ScraperService scraperService,
            @Value("${app.scheduler.cron:0 7 * * *}") String cronSchedule,
            @Value("${app.scheduler.timezone:Europe/Zagreb}") String timezone) {
        this.scraperService = scraperService;
        this.cronSchedule = cronSchedule;
        this.timezone = timezone;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        String springCron = toSpringCron(cronSchedule);
        log.info("Scheduler aktivan: \"{}\" ({})", cronSchedule, timezone);

        CronTrigger trigger = new CronTrigger(springCron, TimeZone.getTimeZone(timezone));
        registrar.addTriggerTask(this::runScheduledCheck, trigger);
    }

    private void runScheduledCheck() {
        log.info("Pokrecem jutarnju provjeru...");
        try {
            var result = scraperService.runCheck();
            log.info("Provjera zavrsena: {} stranica, {} proizvoda, {} promjena.",
                    result.getPagesChecked(), result.getProductsFound(), result.getChanges().size());
        } catch (Exception e) {
            log.error("Jutarnja provjera neuspjela: {}", e.getMessage());
        }
    }

    // pretpostavlja standardni 5-poljni unix cron (min hour day month weekday);
    // ako je vec 6-poljni (ima sekunde), ostavlja ga netaknutim
    private String toSpringCron(String unixCron) {
        String trimmed = unixCron.trim();
        long fieldCount = trimmed.chars().filter(c -> c == ' ').count() + 1;
        if (fieldCount >= 6) return trimmed;
        return "0 " + trimmed;
    }
}
