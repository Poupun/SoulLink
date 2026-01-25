package com.jellycreative.soullink;

import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.network.SoulLinkNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Soul-Link - A multiplayer mod that links players together.
 * 
 * NeoForge 1.20.4 Port
 */
@Mod(SoulLink.MOD_ID)
public class SoulLink {
    public static final String MOD_ID = "soullink";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SoulLink(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(SoulLinkNetwork::register);
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SoulLinkConfig.SPEC, "soullink-common.toml");
        
        LOGGER.info("Soul-Link initialized! All players are now linked.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Soul-Link common setup complete");
    }
}
