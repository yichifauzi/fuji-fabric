package io.github.sakurawald.module.initializer.command_meta.shell;

import com.mojang.brigadier.context.CommandContext;
import io.github.sakurawald.command.argument.adapter.wrapper.GreedyString;
import io.github.sakurawald.command.annotation.Command;
import io.github.sakurawald.command.annotation.CommandPermission;
import io.github.sakurawald.command.annotation.CommandSource;
import io.github.sakurawald.config.Configs;
import io.github.sakurawald.module.initializer.ModuleInitializer;
import io.github.sakurawald.util.LogUtil;
import io.github.sakurawald.util.minecraft.CommandHelper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

public class ShellInitializer extends ModuleInitializer {

    @Command("shell")
    @CommandPermission(level = 4)
    private int shell(@CommandSource CommandContext<ServerCommandSource> ctx, GreedyString rest) {
        if (!Configs.configHandler.model().modules.command_meta.shell.enable_warning.equals("CONFIRM")) {
            throw new RuntimeException("Refuse to execute shell command: please read the official wiki.");
        }

        String $rest = rest.getString();

        CompletableFuture.runAsync(() -> {
            try {
                LogUtil.info("shell exec: {}", $rest);

                Process process = Runtime.getRuntime().exec($rest, null, null);
                InputStream inputStream = process.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                reader.close();
                process.waitFor();

                // output
                LogUtil.info(output.toString());
                ctx.getSource().sendMessage(Text.literal(output.toString()));
            } catch (IOException | InterruptedException e) {
                LogUtil.cryLoudly("Failed to execute a shell command.", e);
            }
        });

        return CommandHelper.Return.SUCCESS;
    }
}
