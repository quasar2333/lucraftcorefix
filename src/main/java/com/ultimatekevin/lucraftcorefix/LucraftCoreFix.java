package com.ultimatekevin.lucraftcorefix;

import com.ultimatekevin.lucraftcorefix.command.CommandLCReload;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = LucraftCoreFix.MODID, name = LucraftCoreFix.NAME, version = LucraftCoreFix.VERSION, dependencies = "required-after:lucraftcore", acceptableRemoteVersions = "*")
public class LucraftCoreFix {

    public static final String MODID = "lucraftcorefix";
    public static final String NAME = "LucraftCore Fix";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("LucraftCore Fix is loaded and has patched network packets via Mixin CoreMod.");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandLCReload());
    }
}