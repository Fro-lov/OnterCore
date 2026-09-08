package com.onter.ontercore.features.admin.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GiveKeyCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveKeyCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("onter")
                .then(Commands.literal("givekey")
                        .requires(source -> source.hasPermission(2))
                        .executes(GiveKeyCommand::giveKey)));
    }

    private static int giveKey(CommandContext<CommandSourceStack> context) {
        
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Эту команду может выполнить только игрок!"));
            return 0;
        }
        LOGGER.info("Giving key to player: {}", player.getName().getString());
        context.getSource().sendSuccess(() -> Component.literal("Key given to " + player.getName().getString()), true);
        return 1;
    }
}
