package io.github.sakurawald.core.command.argument.adapter.impl;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.sakurawald.core.auxiliary.minecraft.ServerHelper;
import io.github.sakurawald.core.command.argument.adapter.abst.BaseArgumentTypeAdapter;
import io.github.sakurawald.core.command.argument.wrapper.impl.OfflinePlayerName;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.UserCache;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class OfflinePlayerArgumentTypeAdapter extends BaseArgumentTypeAdapter {

    @Override
    public boolean match(Type type) {
        return OfflinePlayerName.class.equals(type);
    }

    @Override
    public ArgumentType<?> makeArgumentType() {
        return StringArgumentType.string();
    }

    private static @NotNull List<String> getPlayerNameListFromUserCache() {
        UserCache userCache = ServerHelper.getDefaultServer().getUserCache();
        if (userCache == null) return List.of();

        List<String> playerNames = new ArrayList<>();
        userCache.byName.values().forEach(o -> playerNames.add(o.getProfile().getName()));
        return playerNames;
    }

    @Override
    public RequiredArgumentBuilder<ServerCommandSource, ?> makeRequiredArgumentBuilder(Parameter parameter) {
        return super.makeRequiredArgumentBuilder(parameter).suggests((context, builder) -> {
            getPlayerNameListFromUserCache().forEach(builder::suggest);
            return builder.buildFuture();
        });
    }

    @Override
    public Object makeArgumentObject(CommandContext<ServerCommandSource> context, Parameter parameter) {
        return new OfflinePlayerName(StringArgumentType.getString(context, parameter.getName()));
    }
}
