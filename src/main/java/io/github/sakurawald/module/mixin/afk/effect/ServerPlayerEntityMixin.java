package io.github.sakurawald.module.mixin.afk.effect;

import com.mojang.authlib.GameProfile;
import io.github.sakurawald.core.config.Configs;
import io.github.sakurawald.module.initializer.afk.AfkInitializer;
import io.github.sakurawald.module.initializer.afk.accessor.AfkStateAccessor;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    @Unique
    ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

    public ServerPlayerEntityMixin(World world, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(world, blockPos, f, gameProfile);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void invulnerableEffect(DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        AfkStateAccessor afkStateAccessor = (AfkStateAccessor) player;
        if (afkStateAccessor.fuji$isAfk()) {
            cir.setReturnValue(false);
        }
    }

    // note: function move() in 'afk.effect module' will that one in 'afk module', since the latter Mixin will override the original one.
    @Override
    public void move(MovementType movementType, Vec3d vec3d) {

        AfkStateAccessor afkStateAccessor = (AfkStateAccessor) player;
        if (!Configs.configHandler.model().modules.afk.effect.moveable && afkStateAccessor.fuji$isAfk()) {
            double originalX = player.getX();
            double originalY = player.getY();
            double originalZ = player.getZ();

            // if a player moved itself
            if (movementType == MovementType.PLAYER) {
                if (AfkInitializer.isPlayerActuallyMovedItself(movementType,vec3d)) {
                    afkStateAccessor.fuji$setAfk(false);
                    super.move(movementType, vec3d);
                }

                // ignore the move() for Vec3d.ZERO
                return;
            }

            // reset position to where the player enters afk state.
            player.requestTeleport(originalX, originalY, originalZ);
            return;
        }

        // call super
        super.move(movementType, vec3d);
    }

}
