package io.github.sakurawald.core.auxiliary.minecraft;

import com.mojang.authlib.GameProfile;
import lombok.Setter;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;

import java.util.List;
import java.util.Optional;

public class ServerHelper {

    @Setter
    private static MinecraftServer server;

    public static MinecraftServer getDefaultServer() {
        return server;
    }

    public static List<ServerPlayerEntity> getPlayers() {
        return getDefaultServer().getPlayerManager().getPlayerList();
    }

    public static Optional<GameProfile> getGameProfileByName(String playerName) {
        UserCache userCache = getDefaultServer().getUserCache();
        if (userCache == null) return Optional.empty();

        return userCache.findByName(playerName);
    }

    public static PlayerManager getPlayerManager() {
        return getDefaultServer().getPlayerManager();
    }

    public static boolean isPlayerOnline(String name) {
        return getDefaultServer().getPlayerManager().getPlayerList().stream().anyMatch(p -> p.getGameProfile().getName().equals(name));
    }

    public static void sendPacketToAll(Packet<?> packet) {
        getPlayerManager().sendToAll(packet);
    }

    public static void sendPacketToAllExcept(Packet<?> packet, ServerPlayerEntity player) {
        for (ServerPlayerEntity serverPlayerEntity : getPlayerManager().getPlayerList()) {
            if (serverPlayerEntity == player) continue;
            serverPlayerEntity.networkHandler.sendPacket(packet);
        }
    }

    public static void sendPacket(Packet<?> packet, ServerPlayerEntity player) {
        player.networkHandler.sendPacket(packet);
    }
}
