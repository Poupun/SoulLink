package com.jellycreative.soullink;

import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.handler.SoulLinkEventHandler;
import com.jellycreative.soullink.network.SoulLinkNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Soul-Link Mod Main Class
 * 
 * This mod links the health, hunger, and knockback of all players together.
 * When one player takes damage, all players take damage.
 * When one player heals, all players heal.
 * When one player loses hunger, all players lose hunger.
 * When one player regains hunger, all players regain hunger.
 */
@Mod(SoulLink.MOD_ID)
public class SoulLink {
    public static final String MOD_ID = "soullink";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SoulLink() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register the setup method for modloading
        modEventBus.addListener(this::commonSetup);
        
        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SoulLinkConfig.SPEC);
        
        // Register our event handler
        MinecraftForge.EVENT_BUS.register(new SoulLinkEventHandler());
        
        LOGGER.info("Soul-Link mod initialized! All players are now linked together.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Register network packets
            SoulLinkNetwork.register();
            LOGGER.info("Soul-Link network registered.");
        });
    }
}
