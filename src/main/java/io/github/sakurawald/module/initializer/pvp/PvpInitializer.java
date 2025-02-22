package io.github.sakurawald.module.initializer.pvp;

import com.mojang.brigadier.context.CommandContext;
import io.github.sakurawald.core.command.annotation.CommandNode;
import io.github.sakurawald.core.command.annotation.CommandSource;
import io.github.sakurawald.core.config.handler.abst.ConfigHandler;
import io.github.sakurawald.core.config.handler.impl.ObjectConfigHandler;
import io.github.sakurawald.module.initializer.pvp.config.model.PvPModel;
import io.github.sakurawald.module.initializer.ModuleInitializer;
import io.github.sakurawald.core.auxiliary.minecraft.CommandHelper;
import io.github.sakurawald.core.auxiliary.minecraft.MessageHelper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;


public class PvpInitializer extends ModuleInitializer {

    public static final ConfigHandler<PvPModel> pvpHandler = new ObjectConfigHandler<>("pvp.json", PvPModel.class);

    @Override
    public void onInitialize() {
        pvpHandler.loadFromDisk();
    }

    @Override
    public void onReload() {
        pvpHandler.loadFromDisk();
    }

    @CommandNode("pvp on")
    private int $on(@CommandSource ServerPlayerEntity player) {
        Set<String> whitelist = pvpHandler.model().whitelist;
        String name = player.getGameProfile().getName();
        if (!whitelist.contains(name)) {
            whitelist.add(name);
            pvpHandler.saveToDisk();

            MessageHelper.sendMessage(player, "pvp.on");

            return CommandHelper.Return.SUCCESS;
        }

        MessageHelper.sendMessage(player, "pvp.on.already");
        return CommandHelper.Return.FAIL;
    }

    @CommandNode("pvp off")
    private int $off(@CommandSource ServerPlayerEntity player) {
            Set<String> whitelist = pvpHandler.model().whitelist;
            String name = player.getGameProfile().getName();
            if (whitelist.contains(name)) {
                whitelist.remove(name);
                pvpHandler.saveToDisk();

                MessageHelper.sendMessage(player, "pvp.off");
                return CommandHelper.Return.SUCCESS;
            }

            MessageHelper.sendMessage(player, "pvp.off.already");
            return CommandHelper.Return.FAIL;
    }

    @CommandNode("pvp status")
    private int $status(@CommandSource ServerPlayerEntity player) {
            Set<String> whitelist = pvpHandler.model().whitelist;
            player.sendMessage(MessageHelper.ofComponent(player, "pvp.status")
                    .append(whitelist.contains(player.getGameProfile().getName()) ? MessageHelper.ofComponent(player, "on") : MessageHelper.ofComponent(player, "off")));
            return CommandHelper.Return.SUCCESS;
    }

    @CommandNode("pvp list")
    private int $list(@CommandSource CommandContext<ServerCommandSource> ctx) {
        Set<String> whitelist = pvpHandler.model().whitelist;
        MessageHelper.sendMessage(ctx.getSource(), "pvp.list", whitelist);
        return CommandHelper.Return.SUCCESS;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean contains(String name) {
        return pvpHandler.model().whitelist.contains(name);
    }

}
