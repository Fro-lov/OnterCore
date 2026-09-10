package com.onter.ontercore.features.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.onter.ontercore.config.OnterConfig;
import com.onter.ontercore.core.database.DatabaseManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.util.UUID;

public class PlayerInventorySyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerInventorySyncService.class);
    private static int tickCounter = 0;

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!OnterConfig.INSTANCE.inventorySyncEnabled.get() || !OnterConfig.INSTANCE.inventorySyncOnLogout.get()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerSnapshot(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!OnterConfig.INSTANCE.inventorySyncEnabled.get() || !DatabaseManager.isConnected()) {
            return;
        }

        int intervalSeconds = OnterConfig.INSTANCE.inventorySyncIntervalSeconds.get();
        if (intervalSeconds <= 0) return;

        tickCounter++;
        if (tickCounter >= intervalSeconds * 20) {
            tickCounter = 0;
            MinecraftServer server = event.getServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    syncPlayerSnapshot(player);
                }
            }
        }
    }

    public static void syncPlayerSnapshot(ServerPlayer player) {
        if (!DatabaseManager.isConnected()) return;

        UUID uuid = player.getUUID();
        String username = player.getGameProfile().getName();
        String server = OnterConfig.INSTANCE.serverName.get();
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int foodLevel = player.getFoodData().getFoodLevel();
        float saturation = player.getFoodData().getSaturationLevel();
        int expLevel = player.experienceLevel;
        float expProgress = player.experienceProgress;
        String dimension = player.level().dimension().location().toString();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Serialize Inventory and Ender Chest on server thread
        JsonObject fullData = new JsonObject();
        fullData.add("inventory", serializeInventory(player.getInventory()));
        fullData.add("ender_chest", serializeEnderChest(player.getEnderChestInventory()));
        String inventoryJson = fullData.toString();

        // Save to MySQL asynchronously
        DatabaseManager.executeAsync(connection -> {
            String sql = """
                INSERT INTO onter_player_inventories
                    (player_uuid, username, server_name, health, max_health, food_level, saturation,
                     experience_level, experience_progress, dimension, x, y, z, inventory_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    username = VALUES(username),
                    server_name = VALUES(server_name),
                    health = VALUES(health),
                    max_health = VALUES(max_health),
                    food_level = VALUES(food_level),
                    saturation = VALUES(saturation),
                    experience_level = VALUES(experience_level),
                    experience_progress = VALUES(experience_progress),
                    dimension = VALUES(dimension),
                    x = VALUES(x),
                    y = VALUES(y),
                    z = VALUES(z),
                    inventory_json = VALUES(inventory_json),
                    updated_at = CURRENT_TIMESTAMP;
            """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, username);
                ps.setString(3, server);
                ps.setFloat(4, health);
                ps.setFloat(5, maxHealth);
                ps.setInt(6, foodLevel);
                ps.setFloat(7, saturation);
                ps.setInt(8, expLevel);
                ps.setFloat(9, expProgress);
                ps.setString(10, dimension);
                ps.setDouble(11, x);
                ps.setDouble(12, y);
                ps.setDouble(13, z);
                ps.setString(14, inventoryJson);
                ps.executeUpdate();
            }
        });
    }

    private static JsonArray serializeInventory(Inventory inv) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                JsonObject itemObj = serializeItemStack(stack, i);
                array.add(itemObj);
            }
        }
        return array;
    }

    private static JsonArray serializeEnderChest(PlayerEnderChestContainer enderChest) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            ItemStack stack = enderChest.getItem(i);
            if (!stack.isEmpty()) {
                JsonObject itemObj = serializeItemStack(stack, i);
                array.add(itemObj);
            }
        }
        return array;
    }

    private static JsonObject serializeItemStack(ItemStack stack, int slot) {
        JsonObject obj = new JsonObject();
        obj.addProperty("slot", slot);
        obj.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        obj.addProperty("count", stack.getCount());
        obj.addProperty("display_name", stack.getHoverName().getString());
        obj.addProperty("rarity", stack.getRarity().name());

        if (stack.isDamageableItem()) {
            obj.addProperty("damage", stack.getDamageValue());
            obj.addProperty("max_damage", stack.getMaxDamage());
        }

        // Enchantments
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (!enchants.isEmpty()) {
            JsonObject enchantsObj = new JsonObject();
            enchants.entrySet().forEach(entry -> {
                String enchantName = entry.getKey().unwrapKey()
                        .map(k -> k.location().toString())
                        .orElse(entry.getKey().toString());
                enchantsObj.addProperty(enchantName, entry.getIntValue());
            });
            obj.add("enchantments", enchantsObj);
        }

        return obj;
    }
}
