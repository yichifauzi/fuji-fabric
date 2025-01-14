package io.github.sakurawald.module.initializer.nametag;

import io.github.sakurawald.core.auxiliary.LogUtil;
import io.github.sakurawald.core.auxiliary.minecraft.MessageHelper;
import io.github.sakurawald.core.auxiliary.minecraft.ServerHelper;
import io.github.sakurawald.core.config.Configs;
import io.github.sakurawald.module.initializer.ModuleInitializer;
import io.github.sakurawald.module.initializer.nametag.job.UpdateNametagJob;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;


public class NametagInitializer extends ModuleInitializer {

    private Map<ServerPlayerEntity, DisplayEntity.TextDisplayEntity> player2nametag;

    @Override
    public void onInitialize() {
        player2nametag = new HashMap<>();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> new UpdateNametagJob().schedule());
    }

    @Override
    public void onReload() {
        player2nametag.forEach((key, value) -> value.stopRiding());
    }

    private DisplayEntity.TextDisplayEntity makeNametag(ServerPlayerEntity player) {
        LogUtil.debug("make nametag for player: {}", player.getGameProfile().getName());

        DisplayEntity.TextDisplayEntity nametag = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, player.getWorld()) {
            @Override
            public void tick() {
                super.tick();

                if (this.getVehicle() == null) {
                    ServerHelper.sendPacketToAll(new EntitiesDestroyS2CPacket(this.getId()));

                    LogUtil.debug("discard nametag entity {}: its vehicle is null", this);
                    this.remove(RemovalReason.DISCARDED);
                }
            }
        };
        // let the nametag riding in internal server-side.
        nametag.setInvulnerable(true);
        nametag.startRiding(player);

        sendExistingNametagsToTheNewJoinedPlayer(player);
        broadcastTheNewNametagToAllPlayers(player, nametag);
        return nametag;
    }

    private BlockPos computeNametagSpawnBlockPos(ServerPlayerEntity bindingPlayer) {
        // spawn the nametag over the player's head, so that the player won't see the nametag ride animation.
        return bindingPlayer.getBlockPos().add(0, 3, 0);
    }

    private void sendExistingNametagsToTheNewJoinedPlayer(ServerPlayerEntity player) {
        player2nametag.forEach((key, value) -> {
            BlockPos blockPos = computeNametagSpawnBlockPos(key);
            EntitySpawnS2CPacket entitySpawnS2CPacket = new EntitySpawnS2CPacket(value, 0, blockPos);
            player.networkHandler.sendPacket(entitySpawnS2CPacket);

            EntityPassengersSetS2CPacket entityPassengersSetS2CPacket = new EntityPassengersSetS2CPacket(key);
            player.networkHandler.sendPacket(entityPassengersSetS2CPacket);

            ServerHelper.sendPacketToAll(new EntityTrackerUpdateS2CPacket(value.getId(), value.getDataTracker().getChangedEntries()));
        });
    }

    private void broadcastTheNewNametagToAllPlayers(ServerPlayerEntity player, DisplayEntity.TextDisplayEntity textDisplayEntity) {
        // spawn entity packet
        BlockPos blockPos = computeNametagSpawnBlockPos(player);
        EntitySpawnS2CPacket entitySpawnS2CPacket = new EntitySpawnS2CPacket(textDisplayEntity, 0, blockPos);
        ServerHelper.sendPacketToAll(entitySpawnS2CPacket);

        // ride entity packet
        EntityPassengersSetS2CPacket entityPassengersSetS2CPacket = new EntityPassengersSetS2CPacket(player);
        ServerHelper.sendPacketToAll(entityPassengersSetS2CPacket);
    }

    private byte setTextDisplayFlags(int base, int flag, boolean value) {
        return (byte) (value ? base | flag : base & ~flag);
    }

    private void setDisplayFlag(DataTracker dataTracker, byte flag, boolean value) {
        Byte original = dataTracker.get(DisplayEntity.TextDisplayEntity.TEXT_DISPLAY_FLAGS);
        dataTracker.set(DisplayEntity.TextDisplayEntity.TEXT_DISPLAY_FLAGS, setTextDisplayFlags(original, flag, value));
    }

    private void updateNametag(DisplayEntity.TextDisplayEntity nametag, ServerPlayerEntity player) {
        /* update props of nametag entity */
        var config = Configs.configHandler.model().modules.nametag;

        nametag.setBillboardMode(DisplayEntity.BillboardMode.CENTER);

        if (nametag.getVehicle() != null && nametag.getVehicle().isSneaking()) {
            // the nametag.setInvisible(true) doesn't work, so here is the workaround.
            nametag.setText(Text.empty());
            nametag.setBackground(-1);
        } else {
            Text text = MessageHelper.ofText(player, false, config.style.text);
            nametag.setText(text);

            nametag.getDataTracker().set(DisplayEntity.TRANSLATION, new Vector3f(config.style.offset.x, config.style.offset.y, config.style.offset.z));

            nametag.setDisplayWidth(config.style.size.width);
            nametag.setDisplayHeight(config.style.size.height);

            nametag.setBackground(config.style.color.background);
            nametag.setTextOpacity(config.style.color.text_opacity);

            nametag.getDataTracker().set(DisplayEntity.SCALE, new Vector3f(config.style.scale.x, config.style.scale.y, config.style.scale.z));

            setDisplayFlag(nametag.getDataTracker(), DisplayEntity.TextDisplayEntity.SHADOW_FLAG, config.style.shadow.shadow);
            nametag.setShadowRadius(config.style.shadow.shadow_radius);
            nametag.setShadowStrength(config.style.shadow.shadow_strength);

            setDisplayFlag(nametag.getDataTracker(), DisplayEntity.TextDisplayEntity.SEE_THROUGH_FLAG, config.render.see_through_blocks);
            nametag.setViewRange(config.render.view_range);

            if (config.style.brightness.override_brightness) {
                nametag.setBrightness(new Brightness(config.style.brightness.block, config.style.brightness.sky));
            }
        }

        /* send update props packet */
        if (nametag.getDataTracker().isDirty()) {
            var dirty = nametag.getDataTracker().getDirtyEntries();
            if (dirty != null) {
                int entityId = nametag.getId();
                ServerHelper.sendPacketToAll(new EntityTrackerUpdateS2CPacket(entityId, dirty));
            }
        }
    }

    public void updateNametags() {
        // since the virtual entity is not added into the server, so we should tick() it ourselves.
        player2nametag.values().forEach(DisplayEntity::tick);

        // invalidate keys
        player2nametag.entrySet().removeIf(entry -> entry.getKey().isRemoved() || entry.getValue().isRemoved());

        // update
        ServerHelper.getPlayers().forEach(player -> {
            // make if not exists
            if (!player2nametag.containsKey(player)) {
                player2nametag.put(player, makeNametag(player));
            }

            DisplayEntity.TextDisplayEntity nametag = player2nametag.get(player);
            updateNametag(nametag, player);
        });
    }

}
