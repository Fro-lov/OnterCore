package com.onter.ontercore.features.admin.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReloadCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("onter")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ReloadCommand::reloadConfig)));
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        LOGGER.info("Reloading OnterCore configuration...");
        context.getSource().sendSuccess(() -> Component.literal("OnterCore configuration reloaded!"), true);
        return 1;
    }
}
