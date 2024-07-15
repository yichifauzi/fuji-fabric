package io.github.sakurawald.module.mixin.chat;

import io.github.sakurawald.module.initializer.resource_world.interfaces.SimpleRegistryMixinInterface;
import io.github.sakurawald.module.mixin.resource_world.registry.SimpleRegistryMixin;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.registry.*;
import net.minecraft.text.Decoration;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Map;

@Mixin(value = RegistryLoader.class, priority = 900)
@Slf4j
public class RegistryLoaderMixin {

    /**
     * This amazing mixin is written by Patbox.
     * And the source is inside: <a href="https://github.dev/Patbox/StyledChat/blob/1.21/src/main/java/eu/pb4/styledchat/mixin/MessageArgumentTypeMixin.java">
     * Thanks to his great work.
     * <p>
     * I modified the code to override the vanilla MessageType.CHAT format.
     *
     * @see net.minecraft.registry.SimpleRegistry#add(net.minecraft.registry.RegistryKey, java.lang.Object, net.minecraft.registry.entry.RegistryEntryInfo)
     */
    @Inject(method = "load(Lnet/minecraft/registry/RegistryLoader$RegistryLoadable;Lnet/minecraft/registry/DynamicRegistryManager;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;"
            , at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", ordinal = 0, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void styledChat$injectMessageTypes(@Coerce Object registryLoadable, DynamicRegistryManager dynamicRegistryManager, List<RegistryLoader.Entry<?>> entries, CallbackInfoReturnable<DynamicRegistryManager.Immutable> cir, Map map,
                                                      List<RegistryLoader.Loader<?>> loaders, RegistryOps.RegistryInfoGetter registryInfoGetter) {
        Decoration firstDecoration = new Decoration("%s", List.of(Decoration.Parameter.CONTENT), Style.EMPTY);
        Decoration secondDecoration = Decoration.ofChat("chat.type.text.narrate");

        for (RegistryLoader.Loader<?> entry : loaders) {
            MutableRegistry<?> registry = entry.comp_2246();
            RegistryKey<? extends Registry<?>> registryKey = registry.getKey();

            if (registryKey.equals(RegistryKeys.MESSAGE_TYPE)) {
                Registry<MessageType> registryForMessageType = (Registry<MessageType>) registry;

                // The code is tricky, we need to register it later to override the vanilla registerKey.
                ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                    SimpleRegistryMixinInterface<MessageType> ex = (SimpleRegistryMixinInterface<MessageType>) registry;
                    ex.fuji$setFrozen(false);
                    Registry.register(registryForMessageType, MessageType.CHAT, new MessageType(firstDecoration, secondDecoration));
                    ex.fuji$setFrozen(true);
                });

            }
        }
    }
}