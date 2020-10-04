package com.wrathdaddy.herobrine;

import com.wrathdaddy.herobrine.commands.HerobrineCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.Logger;

/**
 * A mod which spawns a "fake" player in the world for keeping your farms loaded as if a player is nearby.
 */
@Mod.EventBusSubscriber
@Mod(
        modid = Herobrine.MODID,
        name = Herobrine.NAME,
        version = Herobrine.VERSION,
        serverSideOnly = true,
        acceptableRemoteVersions = "*")
public class Herobrine {
    public static final String MODID = "herobrine";
    public static final String NAME = "Herobrine";
    public static final String VERSION = "1.0";

    private static Logger logger;
    private final MinecraftServer mMinecraftServer = FMLCommonHandler.instance().getMinecraftServerInstance();

    @EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(final FMLInitializationEvent event) {
        PermissionAPI.registerNode("herobrine", DefaultPermissionLevel.ALL, "Ability to use Herobrine");
    }

    @EventHandler
    public void serverStarting(final FMLServerStartingEvent event) {
        event.registerServerCommand(new HerobrineCommand());
    }
}
