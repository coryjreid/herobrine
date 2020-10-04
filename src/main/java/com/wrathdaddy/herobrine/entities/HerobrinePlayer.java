package com.wrathdaddy.herobrine.entities;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;

/**
 * The Herobrine player entity which becomes a Herobrine instance in game.
 */
public class HerobrinePlayer extends EntityPlayerMP {

    private static final String TEAM_NAME = "Bots";
    private static final String TEAM_COLOR = "dark_aqua";

    private HerobrinePlayer(
            final MinecraftServer minecraftServer,
            final WorldServer worldServer,
            final GameProfile gameProfile,
            final boolean setLocation,
            final int dimension,
            final BlockPos spawnLocation) {

        super(minecraftServer, worldServer, gameProfile, new PlayerInteractionManager(worldServer));

        interactionManager.setGameType(GameType.CREATIVE);

        if (setLocation) {
            setLocationAndAngles(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), 0.0f, 0.0f);
        }

        final Scoreboard scoreboard = minecraftServer.getWorld(dimension).getScoreboard();
        if(!scoreboard.getTeamNames().contains(TEAM_NAME)){
            scoreboard.createTeam(TEAM_NAME);
            final ScorePlayerTeam scoreplayerteam = scoreboard.getTeam(TEAM_NAME);
            final TextFormatting textformatting = TextFormatting.getValueByName(TEAM_COLOR);
            scoreplayerteam.setColor(textformatting);
            scoreplayerteam.setPrefix(textformatting.toString());
            scoreplayerteam.setSuffix(TextFormatting.RESET.toString());
        }
        if (getTeam() == null) {
            scoreboard.addPlayerToTeam(getName(), TEAM_NAME);
        }
    }

    public static HerobrinePlayer create(
            final String playerName,
            final MinecraftServer minecraftServer,
            final WorldServer worldServer,
            final int dimension,
            final BlockPos spawnLocation) {

        GameProfile gameProfile = minecraftServer.getPlayerProfileCache().getGameProfileForUsername(playerName);
        boolean setLocation = false;
        if (gameProfile == null) {
            gameProfile = new GameProfile(null, playerName);
            setLocation = true;
        }

        return new HerobrinePlayer(minecraftServer, worldServer, gameProfile, setLocation, dimension, spawnLocation);
    }

    @Override
    public void addSelfToInternalCraftingInventory() {

    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean canBeHitWithPotion() {
        return false;
    }

    @Override
    public boolean canBeAttackedWithItem() {
        return false;
    }
}
