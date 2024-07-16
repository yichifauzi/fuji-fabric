package io.github.sakurawald.module.mixin.color.sign;

import io.github.sakurawald.util.MessageUtil;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

@Mixin(SignBlockEntity.class)
@Slf4j
public class SignBlockEntityMixin {

    @ModifyVariable(method = "setText", at = @At("HEAD"), argsOnly = true)
    SignText method(SignText signText) {
        Text[] messages = signText.getMessages(false);
        Text[] newMessages = new Text[messages.length];
        for (int i = 0; i < messages.length; i++) {
            String string = PlainTextComponentSerializer.plainText().serialize(messages[i].asComponent());
            Text formated = MessageUtil.ofText(string);
            newMessages[i] = formated;
        }

        return new SignText(newMessages, newMessages, signText.getColor(), signText.isGlowing());
    }
}