package io.github.sakurawald.module.initializer.command_toolbox.top;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.sakurawald.module.common.structure.Position;
import io.github.sakurawald.module.initializer.ModuleInitializer;
import io.github.sakurawald.util.CommandUtil;
import io.github.sakurawald.util.MessageUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

public class TopInitializer extends ModuleInitializer {
    @Override
    public void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("top").executes(TopInitializer::$top));
    }

    private static int $top(CommandContext<ServerCommandSource> ctx) {
        return CommandUtil.playerOnlyCommand(ctx,player -> {
            World world = player.getWorld();
            BlockPos topPosition = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, player.getBlockPos());

            Position position = Position.of(player).withY(topPosition.getY());
            position.teleport(player);

            MessageUtil.sendMessage(player,  "top");
            return Command.SINGLE_SUCCESS;
        });
    }

}