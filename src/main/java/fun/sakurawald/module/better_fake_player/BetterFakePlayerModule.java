package fun.sakurawald.module.better_fake_player;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fun.sakurawald.ServerMain;
import fun.sakurawald.config.ConfigManager;
import fun.sakurawald.module.AbstractModule;
import fun.sakurawald.util.MessageUtil;
import fun.sakurawald.util.TimeUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static fun.sakurawald.util.MessageUtil.ofComponent;
import static fun.sakurawald.util.MessageUtil.sendBroadcast;

public class BetterFakePlayerModule extends AbstractModule {
    private static final ArrayList<String> CONSTANT_EMPTY_LIST = new ArrayList<>();
    private final HashMap<String, ArrayList<String>> player2fakePlayers = new HashMap<>();
    private final HashMap<String, Long> player2expiration = new HashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommand);
        ServerLifecycleEvents.SERVER_STARTED.register(this::registerScheduleTask);
    }

    @SuppressWarnings("unused")
    public void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(
                Commands.literal("player").then(
                        Commands.literal("who").executes(this::$who)
                ).then(
                        Commands.literal("renew").executes(this::$renew)
                )
        );
    }

    @SuppressWarnings("SameReturnValue")
    private int $renew(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return Command.SINGLE_SUCCESS;

        renewFakePlayers(player);
        return Command.SINGLE_SUCCESS;
    }


    private int $who(CommandContext<CommandSourceStack> context) {
        /* validate */
        validateFakePlayers();

        /* output */
        StringBuilder builder = new StringBuilder();
        for (String player : player2fakePlayers.keySet()) {
            ArrayList<String> fakePlayers = player2fakePlayers.get(player);
            if (fakePlayers.isEmpty()) continue;
            builder.append(player).append(": ");
            for (String fakePlayer : fakePlayers) {
                builder.append(fakePlayer).append(" ");
            }
            builder.append("\n");
        }
        CommandSourceStack source = context.getSource();
        source.sendMessage(ofComponent(source, "better_fake_player.who.header").append(Component.text(builder.toString())));
        return Command.SINGLE_SUCCESS;
    }

    public boolean hasFakePlayers(ServerPlayer player) {
        return player2fakePlayers.containsKey(player.getGameProfile().getName());
    }

    public void renewFakePlayers(ServerPlayer player) {
        String name = player.getGameProfile().getName();
        int duration = ConfigManager.configWrapper.instance().modules.better_fake_player.renew_duration_ms;
        long newTime = System.currentTimeMillis() + duration;
        player2expiration.put(name, newTime);
        MessageUtil.sendMessage(player, "better_fake_player.renew.success", TimeUtil.getFormattedDate(newTime));
    }

    private void validateFakePlayers() {
        Iterator<Map.Entry<String, ArrayList<String>>> it = player2fakePlayers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ArrayList<String>> entry = it.next();

            ArrayList<String> myFakePlayers = entry.getValue();
            // fix: NPE
            if (myFakePlayers == null || myFakePlayers.isEmpty()) {
                it.remove();
                continue;
            }
            myFakePlayers.removeIf(name -> {
                ServerPlayer fakePlayer = ServerMain.SERVER.getPlayerList().getPlayerByName(name);
                return fakePlayer == null || fakePlayer.isRemoved();
            });
        }
    }

    public boolean canSpawnFakePlayer(ServerPlayer player) {
        /* validate */
        validateFakePlayers();

        /* check */
        int limit = this.getCurrentAmountLimit();
        int current = this.player2fakePlayers.getOrDefault(player.getGameProfile().getName(), CONSTANT_EMPTY_LIST).size();
        return current < limit;
    }

    public void addFakePlayer(ServerPlayer player, String fakePlayer) {
        this.player2fakePlayers.computeIfAbsent(player.getGameProfile().getName(), k -> new ArrayList<>()).add(fakePlayer);
    }

    public boolean canManipulateFakePlayer(CommandContext<CommandSourceStack> ctx, String fakePlayer) {
        // IMPORTANT: disable /player ... shadow command for online-player
        if (ctx.getNodes().get(2).getNode().getName().equals("shadow")) return false;

        // bypass: console
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return true;

        // bypass: op
        if (ServerMain.SERVER.getPlayerList().isOp(player.getGameProfile())) return true;

        ArrayList<String> myFakePlayers = this.player2fakePlayers.getOrDefault(player.getGameProfile().getName(), CONSTANT_EMPTY_LIST);
        return myFakePlayers.contains(fakePlayer);
    }

    private int getCurrentAmountLimit() {
        ArrayList<List<Integer>> rules = ConfigManager.configWrapper.instance().modules.better_fake_player.limit_rule;
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        int currentDays = currentDate.getDayOfWeek().getValue();
        int currentMinutes = currentTime.getHour() * 60 + currentTime.getMinute();
        for (List<Integer> rule : rules) {
            if (currentDays >= rule.get(0) && currentMinutes >= rule.get(1)) return rule.get(2);
        }
        return -1;
    }

    @SuppressWarnings("unused")
    public void registerScheduleTask(MinecraftServer server) {
        ServerMain.getSCHEDULED_EXECUTOR_SERVICE().scheduleAtFixedRate(this::manageFakePlayers, 0, 1, TimeUnit.MINUTES);
    }

    public boolean isFakePlayer(ServerPlayer player) {
        return player.getClass() != ServerPlayer.class;
    }

    public boolean isMyFakePlayer(ServerPlayer player, ServerPlayer fakePlayer) {
        return player2fakePlayers.getOrDefault(player.getGameProfile().getName(), CONSTANT_EMPTY_LIST).contains(fakePlayer.getGameProfile().getName());
    }

    public GameProfile createOfflineGameProfile(String fakePlayerName) {
        UUID offlinePlayerUUID = UUIDUtil.createOfflinePlayerUUID(fakePlayerName);
        return new GameProfile(offlinePlayerUUID, fakePlayerName);
    }

    private void manageFakePlayers() {
        /* validate */
        validateFakePlayers();

        int limit = getCurrentAmountLimit();
        long currentTimeMS = System.currentTimeMillis();
        for (String playerName : player2fakePlayers.keySet()) {
            /* check for renew limits */
            long expiration = player2expiration.getOrDefault(playerName, 0L);
            ArrayList<String> fakePlayers = player2fakePlayers.getOrDefault(playerName, CONSTANT_EMPTY_LIST);
            if (expiration <= currentTimeMS) {
                /* auto-renew for online-playerName */
                ServerPlayer playerByName = ServerMain.SERVER.getPlayerList().getPlayerByName(playerName);
                if (playerByName != null) {
                    renewFakePlayers(playerByName);
                    continue;
                }

                for (String fakePlayerName : fakePlayers) {
                    ServerPlayer fakePlayer = ServerMain.SERVER.getPlayerList().getPlayerByName(fakePlayerName);
                    if (fakePlayer == null) return;
                    fakePlayer.kill();
                    sendBroadcast("better_fake_player.kick_for_expiration", fakePlayer.getGameProfile().getName(), playerName);
                }
                // remove entry
                player2expiration.remove(playerName);

                // we'll kick all fake players, so we don't need to check for amount limits
                continue;
            }

            /* check for amount limits */
            for (int i = fakePlayers.size() - 1; i >= limit; i--) {
                ServerPlayer fakePlayer = ServerMain.SERVER.getPlayerList().getPlayerByName(fakePlayers.get(i));
                if (fakePlayer == null) continue;
                fakePlayer.kill();

                sendBroadcast("better_fake_player.kick_for_amount", fakePlayer.getGameProfile().getName(), playerName);
            }
        }
    }

}
