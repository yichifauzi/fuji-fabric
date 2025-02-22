package io.github.sakurawald.module.mixin.command_rewrite;

import io.github.sakurawald.core.config.Configs;
import io.github.sakurawald.core.structure.RegexRewriteEntry;
import io.github.sakurawald.core.auxiliary.LogUtil;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 1000 - 500)
public class ServerPlayNetworkHandlerMixin {

    @ModifyVariable(method = "executeCommand", at = @At(value = "HEAD"), ordinal = 0, argsOnly = true)
    public String interceptPacketsOfIssuedCommand(@NotNull String string) {
        for (RegexRewriteEntry entry : Configs.configHandler.model().modules.command_rewrite.regex) {
            if (entry.regex == null || entry.replacement == null) {
                LogUtil.warn("There is an invalid `null` entry in `command_rewrite.regex`, you should remove it.");
                continue;
            }

            if (string.matches(entry.regex)) {
                return string.replaceAll(entry.regex, entry.replacement);
            }
        }

        return string;
    }
}
