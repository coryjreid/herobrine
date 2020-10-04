package com.wrathdaddy.herobrine.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * This class contains various helper functions related to Entities.
 *
 * @author King Lemming
 */
public class EntityHelper {

    private EntityHelper() {

    }

    public static void transferEntityToWorld(final Entity entity, final WorldServer oldWorld, final WorldServer newWorld) {

        oldWorld.profiler.startSection("placing");

        if (entity.isEntityAlive()) {
            newWorld.spawnEntity(entity);
            newWorld.updateEntityWithOptionalForce(entity, false);
        }
        oldWorld.profiler.endSection();
        entity.setWorld(newWorld);
    }

    public static void transferPlayerToDimension(final EntityPlayerMP player, final int dimension, final PlayerList manager) {

        final int oldDim = player.dimension;
        final WorldServer worldserver = manager.getServerInstance().getWorld(player.dimension);
        player.dimension = dimension;
        final WorldServer worldserver1 = manager.getServerInstance().getWorld(player.dimension);
        player.connection.sendPacket(new SPacketRespawn(player.dimension, player.world.getDifficulty(), player.world.getWorldInfo().getTerrainType(), player.interactionManager.getGameType()));
        worldserver.removeEntityDangerously(player);
        if (player.isBeingRidden()) {
            player.removePassengers();
        }
        if (player.isRiding()) {
            player.dismountRidingEntity();
        }
        player.isDead = false;
        transferEntityToWorld(player, worldserver, worldserver1);
        manager.preparePlayer(player, worldserver);
        player.interactionManager.setWorld(worldserver1);
        manager.updateTimeAndWeatherForPlayer(player, worldserver1);
        manager.syncPlayerInventory(player);

        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, oldDim, dimension);
    }
}
