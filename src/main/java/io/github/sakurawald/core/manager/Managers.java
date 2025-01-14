package io.github.sakurawald.core.manager;

import io.github.sakurawald.core.manager.impl.module.ModuleManager;
import io.github.sakurawald.core.manager.impl.attachment.AttachmentManager;
import io.github.sakurawald.core.manager.impl.backup.BackupRescueManager;
import io.github.sakurawald.core.manager.impl.backup.StandardBackupManager;
import io.github.sakurawald.core.manager.impl.bossbar.BossBarManager;
import io.github.sakurawald.core.manager.impl.backup.BaseBackupManager;
import io.github.sakurawald.core.manager.impl.scheduler.ScheduleManager;
import lombok.Getter;

public class Managers {

    @Getter
    private static final ModuleManager moduleManager = new ModuleManager();

    @Getter
    private static final BossBarManager bossBarManager = new BossBarManager();

    @Getter
    private static final ScheduleManager scheduleManager = new ScheduleManager();

    @Getter
    private static final BaseBackupManager standardBackupManager = new StandardBackupManager();

    @Getter
    private static final BaseBackupManager rescueBackupManager = new BackupRescueManager();

    @Getter
    private static final AttachmentManager attachmentManager = new AttachmentManager();
}
