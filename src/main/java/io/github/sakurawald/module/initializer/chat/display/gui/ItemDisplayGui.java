package io.github.sakurawald.module.initializer.chat.display.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ItemDisplayGui extends DisplayGuiBuilder {

    private final Text title;
    private final ItemStack itemStack;

    public ItemDisplayGui(Text title, ItemStack itemStack) {
        this.title = title;
        this.itemStack = itemStack;
    }

    @Override
    public SimpleGui build(ServerPlayerEntity player) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_3X3, player, false);
        gui.setLockPlayerInventory(true);
        gui.setTitle(this.title);

        /* construct base */
        for (int i = 0; i < 9; i++) {
            gui.setSlot(i, new GuiElementBuilder().setItem(Items.PINK_STAINED_GLASS_PANE));
        }
        /* construct item */
        gui.setSlot(4, itemStack);
        return gui;
    }
}