package io.github.sakurawald.module.initializer.works;

import io.github.sakurawald.core.command.annotation.CommandNode;
import io.github.sakurawald.core.command.annotation.CommandSource;
import io.github.sakurawald.core.config.handler.abst.ConfigHandler;
import io.github.sakurawald.core.config.handler.impl.ObjectConfigHandler;
import io.github.sakurawald.module.initializer.works.config.model.WorksModel;
import io.github.sakurawald.core.manager.impl.scheduler.ScheduleManager;
import io.github.sakurawald.module.initializer.ModuleInitializer;
import io.github.sakurawald.module.initializer.works.gui.WorksGui;
import io.github.sakurawald.module.initializer.works.job.WorksScheduleJob;
import io.github.sakurawald.core.auxiliary.minecraft.CommandHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.quartz.JobDataMap;

@SuppressWarnings("SameReturnValue")
public class WorksInitializer extends ModuleInitializer {

    public static final ConfigHandler<WorksModel> worksHandler = new ObjectConfigHandler<>("works.json", WorksModel.class);

    @Override
    public void onInitialize() {
        worksHandler.loadFromDisk();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            new WorksScheduleJob(new JobDataMap() {
                {
                    this.put(MinecraftServer.class.getName(), server);
                }
            }, () -> ScheduleManager.CRON_EVERY_MINUTE).schedule();
        });
    }

    @Override
    public void onReload() {
        worksHandler.loadFromDisk();
    }

    @CommandNode("works")
    private int $works(@CommandSource ServerPlayerEntity player) {
        new WorksGui(player, worksHandler.model().works, 0).open();
        return CommandHelper.Return.SUCCESS;
    }

}

