package io.github.sakurawald.module.initializer.skin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.github.sakurawald.Fuji;
import io.github.sakurawald.module.initializer.skin.config.SkinIO;
import io.github.sakurawald.module.initializer.skin.config.SkinStorage;
import io.github.sakurawald.core.auxiliary.LogUtil;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// Thanks to: https://modrinth.com/mod/skinrestorer
public class SkinRestorer {

    private static final Gson gson = new Gson();
    @Getter
    private static final SkinStorage skinStorage = new SkinStorage(new SkinIO(Fuji.CONFIG_PATH.resolve("skin")));

    public static CompletableFuture<Pair<Collection<ServerPlayerEntity>, Collection<GameProfile>>> setSkinAsync(@NotNull MinecraftServer server, @NotNull Collection<GameProfile> targets, @NotNull Supplier<Property> skinSupplier) {
        return CompletableFuture.<Pair<Property, Collection<GameProfile>>>supplyAsync(() -> {
            Set<GameProfile> acceptedProfiles = new HashSet<>();
            Property skin = skinSupplier.get();

            LogUtil.debug("skinSupplier.get() -> skin = {}", skin);
            if (skin == null) {
                LogUtil.error("Cannot get the skin for {}", targets.stream().findFirst().orElseThrow());
                return Pair.of(null, Collections.emptySet());
            }

            for (GameProfile profile : targets) {
                SkinRestorer.getSkinStorage().setSkin(profile.getId(), skin);
                acceptedProfiles.add(profile);
            }

            return Pair.of(skin, acceptedProfiles);
        }).<Pair<Collection<ServerPlayerEntity>, Collection<GameProfile>>>thenApplyAsync(pair -> {
            Property skin = pair.left();
            if (skin == null)
                return Pair.of(Collections.emptySet(), Collections.emptySet());

            Collection<GameProfile> acceptedProfiles = pair.right();
            Set<ServerPlayerEntity> acceptedPlayers = new HashSet<>();
            JsonObject newSkinJson = gson.fromJson(new String(Base64.getDecoder().decode(skin.value()), StandardCharsets.UTF_8), JsonObject.class);
            newSkinJson.remove("timestamp");
            for (GameProfile profile : acceptedProfiles) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(profile.getId());

                if (player == null || arePropertiesEquals(newSkinJson, player.getGameProfile()))
                    continue;

                applyRestoredSkin(player, skin);
                for (PlayerEntity observer : player.getWorld().getPlayers()) {
                    ServerPlayerEntity observer1 = (ServerPlayerEntity) observer;
                    observer1.networkHandler.sendPacket(new PlayerRemoveS2CPacket(Collections.singletonList(player.getUuid())));
                    observer1.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player)); // refresh the player information
                    if (player != observer1 && observer1.canSee(player)) {
                        observer1.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(player.getId()));
                        observer1.networkHandler.sendPacket(new EntitySpawnS2CPacket(player, 0, player.getBlockPos()));
                        observer1.networkHandler.sendPacket(new EntityPositionS2CPacket(player));
                        observer1.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(player.getId(), player.getDataTracker().getChangedEntries()));
                    } else if (player == observer1) {
                        observer1.networkHandler.sendPacket(new PlayerRespawnS2CPacket(player.createCommonPlayerSpawnInfo(player.getServerWorld()), (byte) 2));
                        observer1.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(observer1.getInventory().selectedSlot));
                        observer1.sendAbilitiesUpdate();
                        observer1.playerScreenHandler.updateToClient();
                        for (StatusEffectInstance instance : observer1.getStatusEffects()) {
                            // original code: observer1.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(observer1.getId(), instance));
                            observer1.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(observer1.getId(), instance, false));
                        }
                        observer1.networkHandler.requestTeleport(observer1.getX(), observer1.getY(), observer1.getZ(), observer1.getYaw(), observer1.getPitch());
                        observer1.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(player.getId(), player.getDataTracker().getChangedEntries()));
                        observer1.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
                    }
                }
                acceptedPlayers.add(player);
            }
            return Pair.of(acceptedPlayers, acceptedProfiles);
        }, server).orTimeout(10, TimeUnit.SECONDS).exceptionally(e -> Pair.of(Collections.emptySet(), Collections.emptySet()));
    }

    private static void applyRestoredSkin(@NotNull ServerPlayerEntity playerEntity, Property skin) {
        playerEntity.getGameProfile().getProperties().removeAll("textures");
        playerEntity.getGameProfile().getProperties().put("textures", skin);
    }

    private static boolean arePropertiesEquals(@NotNull JsonObject x, @NotNull GameProfile y) {
        Property py = y.getProperties().get("textures").stream().findFirst().orElse(null);
        if (py == null)
            return false;

        try {
            JsonObject jy = gson.fromJson(new String(Base64.getDecoder().decode(py.value()), StandardCharsets.UTF_8), JsonObject.class);
            jy.remove("timestamp");
            return x.equals(jy);
        } catch (Exception ex) {
            LogUtil.info("Can not compare skin", ex);
            return false;
        }
    }

}
