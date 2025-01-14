package io.github.sakurawald.module.initializer;


import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Tips:
 * 1. Don't catch and handle the command exception, just use @SneakThrow and CommandSyntaxException.
 * 2. Use CommandHelper.Return to provide useful return value.
 * 3. If you use source.sendFeedback() method, then it will be controlled by game rule `sendCommandFeedback`
 * 4. If possible, don't register new ArgumentType, just use the existed ArgumentType. (Mojang provides many useful argument types which implements ArgumentType)
 */
public class ModuleInitializer {

    /** The template-method */
    public final void doInitialize() {
        this.onInitialize();
    }

    public void onInitialize() {
        // no-op
    }

    public void onReload() {
        // no-op
    }

}
