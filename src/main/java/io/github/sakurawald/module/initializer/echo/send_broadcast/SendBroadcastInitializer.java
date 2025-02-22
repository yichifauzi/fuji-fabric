package io.github.sakurawald.module.initializer.echo.send_broadcast;

import io.github.sakurawald.core.command.argument.wrapper.impl.GreedyString;
import io.github.sakurawald.core.command.annotation.CommandNode;
import io.github.sakurawald.core.command.annotation.CommandRequirement;
import io.github.sakurawald.module.initializer.ModuleInitializer;
import io.github.sakurawald.core.auxiliary.minecraft.CommandHelper;
import io.github.sakurawald.core.auxiliary.minecraft.MessageHelper;
import io.github.sakurawald.core.auxiliary.minecraft.ServerHelper;
import net.minecraft.server.network.ServerPlayerEntity;

public class SendBroadcastInitializer extends ModuleInitializer {

    @CommandNode("send-broadcast")
    @CommandRequirement(level =  4)
    int sendBroadcast(GreedyString rest){
        String message = rest.getValue();

        for (ServerPlayerEntity player : ServerHelper.getDefaultServer().getPlayerManager().getPlayerList()) {
            player.sendMessage(MessageHelper.ofText(player, false, message));
        }
        return CommandHelper.Return.SUCCESS;
    }

}
