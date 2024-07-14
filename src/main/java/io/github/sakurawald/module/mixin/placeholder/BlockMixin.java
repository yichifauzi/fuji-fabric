package io.github.sakurawald.module.mixin.placeholder;

import io.github.sakurawald.module.initializer.placeholder.MainStats;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "afterBreak", at = @At("HEAD"))
    private void $afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack, CallbackInfo ci) {
        MainStats.uuid2stats.get(player.getUuid().toString()).mined += 1;
    }

    @Inject(method = "onPlaced", at = @At("HEAD"))
    public void $onPlaced(World world, BlockPos pos, BlockState state, LivingEntity entity, ItemStack itemStack, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayerEntity player)) return;
        MainStats.uuid2stats.get(player.getUuid().toString()).placed += 1;
    }
}