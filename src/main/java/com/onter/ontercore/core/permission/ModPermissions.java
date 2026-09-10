package com.onter.ontercore.core.permission;

import com.onter.ontercore.ontercore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

public class ModPermissions {

    public static final PermissionNode<Boolean> COMMAND_BASE = new PermissionNode<>(
            ontercore.MODID,
            "command.base",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMMAND_INFO = new PermissionNode<>(
            ontercore.MODID,
            "command.info",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMMAND_HISTORY = new PermissionNode<>(
            ontercore.MODID,
            "command.history",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMMAND_STATUS = new PermissionNode<>(
            ontercore.MODID,
            "command.status",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMMAND_MAINTENANCE = new PermissionNode<>(
            ontercore.MODID,
            "command.maintenance",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMMAND_BROADCAST = new PermissionNode<>(
            ontercore.MODID,
            "command.broadcast",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMMAND_RELOAD = new PermissionNode<>(
            ontercore.MODID,
            "command.reload",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> COMMAND_SYNC = new PermissionNode<>(
            ontercore.MODID,
            "command.sync",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> player != null && player.hasPermissions(2)
    );

    public static final PermissionNode<Boolean> BYPASS_MAINTENANCE = new PermissionNode<>(
            ontercore.MODID,
            "bypass.maintenance",
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> player != null && player.hasPermissions(2)
    );

    @SubscribeEvent
    public void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(
                COMMAND_BASE,
                COMMAND_INFO,
                COMMAND_HISTORY,
                COMMAND_STATUS,
                COMMAND_MAINTENANCE,
                COMMAND_BROADCAST,
                COMMAND_RELOAD,
                COMMAND_SYNC,
                BYPASS_MAINTENANCE
        );
    }

    public static boolean check(CommandSourceStack source, PermissionNode<Boolean> node, int fallbackOpLevel) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionAPI.getPermission(player, node);
        }
        return source.hasPermission(fallbackOpLevel);
    }

    public static boolean canBypassMaintenance(ServerPlayer player, int fallbackOpLevel) {
        if (PermissionAPI.getPermission(player, BYPASS_MAINTENANCE)) {
            return true;
        }
        return player.hasPermissions(fallbackOpLevel);
    }
}
