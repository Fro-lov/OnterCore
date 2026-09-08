package com.onter.ontercore;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.onter.ontercore.commands.OnterCommand;
import com.onter.ontercore.config.OnterConfig;
import com.onter.ontercore.core.database.DatabaseManager;
import com.onter.ontercore.core.state.ServerStateManager;
import com.onter.ontercore.features.admin.commands.GiveKeyCommand;
import com.onter.ontercore.features.admin.commands.ReloadCommand;
import com.onter.ontercore.features.analytics.PlayerAnalyticsListener;
import com.onter.ontercore.features.broadcaster.AnnouncementTicker;
import com.onter.ontercore.features.telegram.TelegramNotifier;
import com.onter.ontercore.init.ModDataComponents;
import com.onter.ontercore.init.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ontercore.MODID)
public class ontercore {
    public static final String MODID = "ontercore";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.examplemod"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
            }).build());

    public ontercore(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register core event handlers
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new com.onter.ontercore.core.permission.ModPermissions());
        NeoForge.EVENT_BUS.register(new PlayerAnalyticsListener());
        NeoForge.EVENT_BUS.register(new AnnouncementTicker());
        NeoForge.EVENT_BUS.register(new ServerStateManager());

        modEventBus.addListener(this::addCreative);

        // Register Server Configuration (generates config/ontercore-server.toml)
        modContainer.registerConfig(ModConfig.Type.SERVER, OnterConfig.SPEC);

        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        LOGGER.info("[OnterCore] Core Mod loaded successfully!");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[OnterCore] Common setup completed.");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[OnterCore] Server is starting, initializing database and state...");
        DatabaseManager.init();
        ServerStateManager.init();
        TelegramNotifier.notifyServerLifecycle("STARTED");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("[OnterCore] Server is stopping, closing database connections...");
        TelegramNotifier.notifyServerLifecycle("STOPPING");
        DatabaseManager.shutdown();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Register OnterCore command suite
        OnterCommand.register(dispatcher);
        
        // Register admin commands
        ReloadCommand.register(dispatcher);
        GiveKeyCommand.register(dispatcher);
    }
}

