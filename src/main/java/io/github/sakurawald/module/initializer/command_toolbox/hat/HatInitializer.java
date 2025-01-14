package io.github.sakurawald.module.initializer.command_toolbox.hat;

import io.github.sakurawald.core.command.annotation.CommandNode;
import io.github.sakurawald.core.command.annotation.CommandSource;
import io.github.sakurawald.module.initializer.ModuleInitializer;
import io.github.sakurawald.core.auxiliary.minecraft.CommandHelper;
import io.github.sakurawald.core.auxiliary.minecraft.MessageHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;


public class HatInitializer extends ModuleInitializer {

    @CommandNode("hat")
    private int $hat(@CommandSource ServerPlayerEntity player) {
        ItemStack mainHandItem = player.getMainHandStack();
        ItemStack headSlotItem = player.getEquippedStack(EquipmentSlot.HEAD);

        player.equipStack(EquipmentSlot.HEAD, mainHandItem);
        player.setStackInHand(Hand.MAIN_HAND, headSlotItem);
        MessageHelper.sendMessage(player, "hat.success");
        return CommandHelper.Return.SUCCESS;
    }

}
