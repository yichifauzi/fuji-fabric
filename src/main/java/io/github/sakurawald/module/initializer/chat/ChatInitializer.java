package io.github.sakurawald.module.initializer.chat;

import com.google.common.collect.EvictingQueue;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import io.github.sakurawald.Fuji;
import io.github.sakurawald.config.Configs;
import io.github.sakurawald.config.handler.ConfigHandler;
import io.github.sakurawald.config.handler.ObjectConfigHandler;
import io.github.sakurawald.config.model.ChatModel;
import io.github.sakurawald.module.initializer.ModuleInitializer;
import io.github.sakurawald.module.initializer.chat.display.DisplayHelper;
import io.github.sakurawald.module.common.job.MentionPlayersJob;
import io.github.sakurawald.util.CommandUtil;
import io.github.sakurawald.util.DateUtil;
import io.github.sakurawald.util.PermissionUtil;
import io.github.sakurawald.util.MessageUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

@Slf4j
public class ChatInitializer extends ModuleInitializer {
    public static final ConfigHandler<ChatModel> chatHandler = new ObjectConfigHandler<>("chat.json", ChatModel.class);

    private final MiniMessage miniMessage = MiniMessage.builder().build();

    private Map<Pattern, String> patterns;

    @Getter
    private Queue<Component> chatHistory;

    @Override
    public void onInitialize() {
        chatHandler.loadFromDisk();
        chatHistory = EvictingQueue.create(Configs.configHandler.model().modules.chat.history.cache_size);

        compilePatterns();

        registerItemPlaceholder();
        registerInvPlaceholder();
        registerEnderPlaceholder();
        registerPosPlaceholder();
        registerDatePlaceholder();
        registerPrefixPlaceholder();
        registerSuffixPlaceholder();
    }

    @Override
    public void onReload() {
        chatHandler.loadFromDisk();

        EvictingQueue<Component> newQueue = EvictingQueue.create(Configs.configHandler.model().modules.chat.history.cache_size);
        newQueue.addAll(chatHistory);
        chatHistory.clear();
        chatHistory = newQueue;

        compilePatterns();
    }

    private void compilePatterns() {
        patterns = new HashMap<>();

        for (RegexEntry regexEntry : Configs.configHandler.model().modules.chat.rewrite.regex) {
            patterns.put(Pattern.compile(regexEntry.regex), regexEntry.replacement);
        }

    }


    private void registerDatePlaceholder() {
        Placeholders.register(
                Identifier.of(Fuji.MOD_ID, "date"),
                (ctx, arg) -> PlaceholderResult.value(Text.literal(DateUtil.getCurrentDate())));
    }

    private void registerEnderPlaceholder() {
        Placeholders.register(
                Identifier.of(Fuji.MOD_ID, "ender"),
                (ctx, arg) -> {
                    if (ctx.player() == null) PlaceholderResult.invalid();

                    ServerPlayerEntity player = ctx.player();
                    String displayUUID = DisplayHelper.createEnderChestDisplay(player);
                    Component replacement =
                            MessageUtil.ofComponent(player, "display.ender_chest.text")
                                    .hoverEvent(MessageUtil.ofComponent(player, "display.click.prompt"))
                                    .clickEvent(buildDisplayClickEvent(displayUUID));
                    return PlaceholderResult.value(MessageUtil.toText(replacement));
                });
    }

    private void registerInvPlaceholder() {
        Placeholders.register(
                Identifier.of(Fuji.MOD_ID, "inv"),
                (ctx, arg) -> {
                    if (ctx.player() == null) PlaceholderResult.invalid();

                    ServerPlayerEntity player = ctx.player();
                    String displayUUID = DisplayHelper.createInventoryDisplay(player);
                    Component replacement =
                            MessageUtil.ofComponent(player, "display.inventory.text")
                                    .hoverEvent(MessageUtil.ofComponent(player, "display.click.prompt"))
                                    .clickEvent(buildDisplayClickEvent(displayUUID));

                    return PlaceholderResult.value(MessageUtil.toText(replacement));
                });
    }

    public void registerItemPlaceholder() {
        Placeholders.register(
                Identifier.of(Fuji.MOD_ID, "item"),
                (ctx, arg) -> {
                    if (ctx.player() == null) PlaceholderResult.invalid();

                    ServerPlayerEntity player = ctx.player();
                    String displayUUID = DisplayHelper.createItemDisplay(player);
                    Component replacement =
                            Component.text("[")
                                    .append(Component.translatable(player.getMainHandStack().getTranslationKey()))
                                    .append(Component.text("]"))
                                    .hoverEvent(MessageUtil.ofComponent(player, "display.click.prompt"))
                                    .clickEvent(buildDisplayClickEvent(displayUUID));
                    return PlaceholderResult.value(MessageUtil.toText(replacement));
                });
    }

    public void registerPosPlaceholder() {
        Placeholders.register(
                Identifier.of(Fuji.MOD_ID, "pos"),
                (ctx, arg) -> {
                    if (ctx.player() == null) PlaceholderResult.invalid();

                    ServerPlayerEntity player = ctx.player();

                    int x = player.getBlockX();
                    int y = player.getBlockY();
                    int z = player.getBlockZ();
                    String dim_name = player.getWorld().getRegistryKey().getValue().toString();
                    String dim_display_name = MessageUtil.getString(player, dim_name);

                    String clickCommand = MessageUtil.getString(player, "chat.xaero_waypoint_add.command", x, y, z, dim_name.replaceAll(":", "\\$"));

                    String hoverString = MessageUtil.getString(player, "chat.current_pos");
                    switch (dim_name) {
                        case "minecraft:overworld":
                            hoverString += "\n" + MessageUtil.getString(player, "minecraft:the_nether")
                                    + ": %d %s %d".formatted(x / 8, y, z / 8);
                            break;
                        case "minecraft:the_nether":
                            hoverString += "\n" + MessageUtil.getString(player, "minecraft:overworld")
                                    + ": %d %s %d".formatted(x * 8, y, z * 8);
                            break;
                    }

                    Component component = MessageUtil.ofComponent(player, true, "placeholder.pos", x, y, z, dim_display_name)
                            .clickEvent(ClickEvent.runCommand(clickCommand))
                            .hoverEvent(Component.text(hoverString + "\n").append(MessageUtil.ofComponent(player, "chat.xaero_waypoint_add")));

                    return PlaceholderResult.value(MessageUtil.toText(component));
                });

    }

    private void registerPrefixPlaceholder() {
        Placeholders.register(
                Identifier.of(Fuji.MOD_ID, "player_prefix"),
                (ctx, arg) -> {
                    if (ctx.player() == null) PlaceholderResult.invalid();

                    ServerPlayerEntity player = ctx.player();
                    String prefix = PermissionUtil.getPrefix(player);
                    return PlaceholderResult.value(MessageUtil.ofText(prefix));
                });
    }

    private void registerSuffixPlaceholder() {
        Placeholders.register(
                Identifier.of(Fuji.MOD_ID, "player_suffix"),
                (ctx, arg) -> {
                    if (ctx.player() == null) PlaceholderResult.invalid();

                    ServerPlayerEntity player = ctx.player();
                    String prefix = PermissionUtil.getSuffix(player);
                    return PlaceholderResult.value(MessageUtil.ofText(prefix));
                });
    }


    @Override
    public void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                literal("chat")
                        .then(literal("format")
                                .then(literal("reset")
                                        .executes(this::$reset)
                                )
                                .then(literal("set")
                                        .then(argument("format", StringArgumentType.greedyString())
                                                .executes(this::$format)
                                        )
                                )
                        )
        );
    }

    private int $format(CommandContext<ServerCommandSource> ctx) {
        return CommandUtil.playerOnlyCommand(ctx, player -> {
            /* save the format*/
            String format = StringArgumentType.getString(ctx, "format");
            String name = player.getGameProfile().getName();
            chatHandler.model().format.player2format.put(name, format);
            chatHandler.saveToDisk();

            /* feedback */
            format = MessageUtil.getString(player, "chat.format.set").replace("%s", format);
            Component component = miniMessage.deserialize(format).asComponent()
                    .replaceText(builder -> {
                        builder.match("%message%").replacement(MessageUtil.ofComponent(player, "chat.format.show"));
                    });
            player.sendMessage(component);
            return Command.SINGLE_SUCCESS;
        });
    }

    private int $reset(CommandContext<ServerCommandSource> ctx) {
        return CommandUtil.playerOnlyCommand(ctx, player -> {
            String name = player.getGameProfile().getName();
            chatHandler.model().format.player2format.remove(name);
            chatHandler.saveToDisk();
            MessageUtil.sendMessage(player, "chat.format.reset");
            return Command.SINGLE_SUCCESS;
        });
    }


    @NotNull
    private ClickEvent buildDisplayClickEvent(String displayUUID) {
        return ClickEvent.callback(audience -> {
            if (audience instanceof ServerCommandSource css && css.getPlayer() != null) {
                DisplayHelper.viewDisplay(css.getPlayer(), displayUUID);
            }
        }, ClickCallback.Options.builder().lifetime(Duration.of(Configs.configHandler.model().modules.chat.display.expiration_duration_s, ChronoUnit.SECONDS))
                .uses(Integer.MAX_VALUE).build());
    }

    private String resolveMentionTag(String string) {
        /* resolve player tag */
        ArrayList<ServerPlayerEntity> mentionedPlayers = new ArrayList<>();

        String[] playerNames = Fuji.SERVER.getPlayerNames();
        // fix: mention the longest name first
        Arrays.sort(playerNames, Comparator.comparingInt(String::length).reversed());

        for (String playerName : playerNames) {
            // here we must continue so that mentionPlayers will not be added
            if (!string.contains(playerName)) continue;
            string = string.replace(playerName, "<aqua>%s</aqua>".formatted(playerName));
            mentionedPlayers.add(Fuji.SERVER.getPlayerManager().getPlayer(playerName));
        }

        /* run mention player task */
        if (!mentionedPlayers.isEmpty()) {
            MentionPlayersJob.scheduleJob(mentionedPlayers);
        }

        return string;
    }

    public String resolvePatterns(String string) {
        for (Map.Entry<Pattern, String> entry : patterns.entrySet()) {
            string = entry.getKey().matcher(string).replaceAll(entry.getValue());
        }
        return string;
    }

    public Text parseText(ServerPlayerEntity player, String message) {
        /* parse message */
        message = resolvePatterns(message);
        message = resolveMentionTag(message);
        message = chatHandler.model().format.player2format.getOrDefault(player.getGameProfile().getName(), message)
                .replace("%message%", message);

        /* parse format */
        String format = Configs.configHandler.model().modules.chat.format;

        /* combine */
        String string = format.replace("%message%", message);
        return MessageUtil.ofText(player, false, string);
    }

}