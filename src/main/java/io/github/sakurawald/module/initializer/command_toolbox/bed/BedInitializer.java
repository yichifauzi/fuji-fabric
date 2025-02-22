package io.github.sakurawald.module.initializer.command_toolbox.bed;

import io.github.sakurawald.core.command.annotation.CommandNode;
import io.github.sakurawald.core.command.annotation.CommandSource;
import io.github.sakurawald.module.initializer.ModuleInitializer;
import io.github.sakurawald.core.auxiliary.minecraft.CommandHelper;
import io.github.sakurawald.core.auxiliary.minecraft.MessageHelper;
import io.github.sakurawald.core.auxiliary.minecraft.ServerHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public class BedInitializer extends ModuleInitializer {

    @CommandNode("bed")
    private int $bed(@CommandSource ServerPlayerEntity player) {
        BlockPos respawnPosition = player.getSpawnPointPosition();
        RegistryKey<World> respawnDimension = player.getSpawnPointDimension();

        ServerWorld serverLevel = ServerHelper.getDefaultServer().getWorld(respawnDimension);
        if (respawnPosition == null || serverLevel == null) {
            MessageHelper.sendMessage(player, "bed.not_found");
            return CommandHelper.Return.FAIL;
        }

        player.teleport(serverLevel, respawnPosition.getX(), respawnPosition.getY(), respawnPosition.getZ(), player.getYaw(), player.getPitch());
        MessageHelper.sendMessage(player, "bed.success");
        return CommandHelper.Return.SUCCESS;
    }
}
