package io.github.sakurawald.module.mixin.chat.history;

import io.github.sakurawald.core.manager.Managers;
import io.github.sakurawald.module.initializer.chat.history.ChatHistoryInitializer;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 1000 + 125)
public abstract class ServerGamePacketListenerImplMixin {

    @Unique
    private static final ChatHistoryInitializer module = Managers.getModuleManager().getInitializer(ChatHistoryInitializer.class);

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "handleDecoratedMessage", at = @At(value = "HEAD"))
    public void listenChatMessageSentEvent(SignedMessage signedMessage, CallbackInfo ci) {
        module.getChatHistory().add(signedMessage.getContent());
    }
}
