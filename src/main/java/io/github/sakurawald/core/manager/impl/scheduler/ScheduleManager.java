package io.github.sakurawald.core.manager.impl.scheduler;


import io.github.sakurawald.core.config.Configs;
import io.github.sakurawald.core.config.job.SaveConfigHandlerJob;
import io.github.sakurawald.core.manager.Managers;
import io.github.sakurawald.core.manager.abst.BaseManager;
import io.github.sakurawald.core.auxiliary.LogUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.*;

public class ScheduleManager extends BaseManager {

    public static final String CRON_EVERY_MINUTE = "0 * * ? * * *";

    private Scheduler scheduler;

    {
        /* set logger level for quartz */
        Level level = Level.getLevel(Configs.configHandler.model().core.quartz.logger_level);
        Configurator.setAllLevels("org.quartz", level);

        // note: for some early initialize, here will cause NPE
        resetScheduler();
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> Managers.getScheduleManager().startScheduler());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            Managers.getScheduleManager().triggerJobs(SaveConfigHandlerJob.class.getName());
            Managers.getScheduleManager().shutdownScheduler();
        });
    }

    public void scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        this.scheduler.scheduleJob(jobDetail,trigger);
    }

    public void rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) throws SchedulerException {
        this.scheduler.rescheduleJob(triggerKey,newTrigger);
    }

    public void deleteJobs(Class<?> clazz) {
        List<JobKey> jobKeys = new ArrayList<>(getJobKeys(clazz.getName()));
        this.deleteJobs(jobKeys);
    }

    private void deleteJobs(List<JobKey> jobKeys) {
        try {
            LogUtil.debug("Delete job keys: {}", jobKeys);
            this.scheduler.deleteJobs(jobKeys);
        } catch (SchedulerException e) {
            LogUtil.error("Failed to delete jobs: " + e);
        }
    }

    private Set<JobKey> getJobKeys(@NotNull String jobGroup) {
        GroupMatcher<JobKey> groupMatcher = GroupMatcher.groupEquals(jobGroup);
        try {
            return scheduler.getJobKeys(groupMatcher);
        } catch (SchedulerException e) {
            LogUtil.error("Exception in ScheduleUtil.getJobKeys", e);
        }
        return Collections.emptySet();
    }

    public void triggerJobs(@NotNull String jobGroup) {
        getJobKeys(jobGroup).forEach(jobKey -> {
            try {
                scheduler.triggerJob(jobKey);
            } catch (SchedulerException e) {
                LogUtil.error("Exception in ScheduleUtil.triggerJobs", e);
            }
        });
    }

    private void resetScheduler() {
        /* new scheduler */
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private void startScheduler() {
        resetScheduler();

        try {
            scheduler.start();
        } catch (SchedulerException e) {
            LogUtil.error("Exception in ScheduleUtil.startScheduler", e);
        }
    }

    private void shutdownScheduler() {
        try {
            scheduler.shutdown();

            // note: reset scheduler right now to fix client-side NPE
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                resetScheduler();
            }

        } catch (SchedulerException e) {
            LogUtil.error("Exception in ScheduleUtil.shutdownScheduler", e);
        }
    }
}
