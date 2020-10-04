package com.wrathdaddy.herobrine.commands;

import com.wrathdaddy.herobrine.entities.EntityHelper;
import com.wrathdaddy.herobrine.entities.HerobrinePlayer;
import com.wrathdaddy.herobrine.network.HerobrineNetworkManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketEntityHeadLook;
import net.minecraft.network.play.server.SPacketEntityTeleport;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentBase;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * The command which is the sole point of interaction for the mod.
 */
public class HerobrineCommand extends CommandBase {

    private final List<String> mAliases = new ArrayList<>();
    private final HashMap<String, HerobrinePlayer> mSpawnedHerobrinePlayers = new HashMap<>();

    public HerobrineCommand() {
        registerAliases();
    }

    @Override
    public List<String> getAliases() {
        return mAliases;
    }

    @Override
    public String getName() {
        return "herobrine";
    }

    // TODO Make this configurable via mod config
//    @Override
//    public boolean checkPermission(final MinecraftServer server, final ICommandSender sender) {
//        return sender.getCommandSenderEntity() instanceof EntityPlayerMP
//                && PermissionAPI.hasPermission((EntityPlayer) sender.getCommandSenderEntity(), getName());
//    }
//
//    @Override
//    public int getRequiredPermissionLevel() {
//        return 0;
//    }

    @Override
    public String getUsage(final ICommandSender sender) {
        return "\n"
                + "/herobrine spawn <name>\n"
                + "/herobrine move <name>\n"
                + "/herobrine move <name> <x> <y> <z> <dimension id>\n"
                + "/herobrine kill <name>";
    }

    @Override
    public void execute(final MinecraftServer server, final ICommandSender sender, final String[] args)
            throws CommandException {

        if (args.length <= 1) {
            throw new CommandException("Missing required arguments");
        }

        final HerobrineCommandAction commandAction;
        try {
            commandAction = HerobrineCommandAction.valueOf(args[0].trim().toUpperCase());
        } catch (final IllegalArgumentException ignoredException) {
            throw new CommandException("Unknown Herobrine action - please see usage");
        }

        final String selectedHerobrineName = args[1].trim();
        final String selectedHerobrineId = selectedHerobrineName.toLowerCase();
        final Entity commandSender = CommandBase.getPlayer(server, sender, sender.getName());
        final BlockPos senderPosition = sender.getPosition();
        final int senderDimension = commandSender.dimension;

        switch (commandAction) {
            case SPAWN: {
                doSpawnAction(selectedHerobrineId, selectedHerobrineName, sender, server, senderDimension);
                break;
            }
            case MOVE: {
                doMoveAction(selectedHerobrineId, selectedHerobrineName, args, senderDimension, senderPosition, server, sender);
                break;
            }
            case DELETE:
            case KILL: {
                doKillAction(selectedHerobrineId, selectedHerobrineName, senderDimension, senderPosition, server, sender);
                break;
            }
            default: {
                throw new CommandException("That Herobrine action is not yet implemented");
            }
        }
    }

    private void doKillAction(
            final String selectedHerobrineId,
            final String selectedHerobrineName,
            final int senderDimension,
            final BlockPos senderPosition,
            final MinecraftServer server,
            final ICommandSender sender) throws CommandException {

        if (!mSpawnedHerobrinePlayers.containsKey(selectedHerobrineId)) {
            throw new CommandException("Herobrine \"" + selectedHerobrineName + "\"" + " does not exist");
        }

        final HerobrinePlayer selectedHerobrinePlayer = mSpawnedHerobrinePlayers.get(selectedHerobrineId);

        selectedHerobrinePlayer.connection.disconnect(new TextComponentString(""));
        server.getPlayerList().playerLoggedOut(selectedHerobrinePlayer);
        server.getWorld(senderDimension).removeEntity(selectedHerobrinePlayer);
        updatePlayerLocation(selectedHerobrinePlayer, senderPosition.getX(), senderPosition.getY(), senderPosition.getZ());
        sendMovePackets(selectedHerobrinePlayer, server);
        mSpawnedHerobrinePlayers.remove(selectedHerobrineId);

        sender.sendMessage(createMessage("Herobrine " + selectedHerobrineName + " despawned"));
    }

    private void doMoveAction(
            final String selectedHerobrineId,
            final String selectedHerobrineName,
            final String[] args,
            final int senderDimension,
            final BlockPos senderPosition,
            final MinecraftServer server,
            final ICommandSender sender) throws CommandException {

        final int minimumArguments = 2;
        final int maximumArguments = 6;
        if (!mSpawnedHerobrinePlayers.containsKey(selectedHerobrineId)) {
            throw new CommandException("Herobrine \"" + selectedHerobrineName + "\"" + " does not exist");
        }
        if (args.length > minimumArguments && args.length < maximumArguments) {
            throw new CommandException("Missing required arguments");
        }
        if (args.length > maximumArguments) {
            throw new CommandException("Too many arguments");
        }
        if (!DimensionManager.isDimensionRegistered(senderDimension)) {
            throw new CommandException("Dimension " + senderDimension + " does not exist");
        }

        final HerobrinePlayer selectedHerobrinePlayer = mSpawnedHerobrinePlayers.get(selectedHerobrineId);

        selectedHerobrinePlayer.getPassengers().forEach(Entity::dismountRidingEntity);
        selectedHerobrinePlayer.dismountRidingEntity();

        int chosenDimension = senderDimension;
        final double x;
        final double y;
        final double z;
        if (args.length == maximumArguments) {
            try {
                x = CommandBase.parseDouble(args[2]);
                y = CommandBase.parseDouble(args[3]);
                z = CommandBase.parseDouble(args[4]);
                chosenDimension = CommandBase.parseInt(args[5]);
            } catch (final NumberInvalidException ignoredException) {
                throw new CommandException("Invalid coordinates");
            }
        } else {
            x = senderPosition.getX();
            y = senderPosition.getY();
            z = senderPosition.getZ();
        }
        if (!DimensionManager.isDimensionRegistered(chosenDimension)) {
            throw new CommandException("Dimension " + chosenDimension + " does not exist");
        }

        updatePlayerLocation(selectedHerobrinePlayer, x, y, z);
        if (selectedHerobrinePlayer.dimension != chosenDimension) {
            EntityHelper.transferPlayerToDimension(
                    selectedHerobrinePlayer,
                    chosenDimension,
                    selectedHerobrinePlayer.mcServer.getPlayerList());
        }
        sendMovePackets(selectedHerobrinePlayer, server);

        sender.sendMessage(createMessage("Moved Herobrine "
                + selectedHerobrineName
                + " to "
                + getPositionString(selectedHerobrinePlayer)));
    }

    private void doSpawnAction(
            final String selectedHerobrineId,
            final String selectedHerobrineName,
            final ICommandSender sender,
            final MinecraftServer server,
            final int senderDimension) throws CommandException {

        if (mSpawnedHerobrinePlayers.containsKey(selectedHerobrineId)) {
            throw new CommandException("Herobrine \"" + selectedHerobrineName + "\"" + " is already spawned");
        }
        if (!sender.getEntityWorld().isBlockLoaded(sender.getPosition())) {
            throw new CommandException("commands.summon.outOfWorld", new Object[0]);
        }

        final boolean isNewSpawn = !Arrays.asList(server.getPlayerProfileCache().getUsernames()).contains(selectedHerobrineName);

        final HerobrinePlayer newHerobrinePlayer = HerobrinePlayer.create(
                selectedHerobrineName,
                server,
                server.getWorld(senderDimension),
                senderDimension,
                sender.getPosition());
        mSpawnedHerobrinePlayers.put(selectedHerobrineId, newHerobrinePlayer);

        final NetworkManager networkManager = new HerobrineNetworkManager(EnumPacketDirection.SERVERBOUND);
        server.getPlayerList().initializeConnectionToPlayer(
                networkManager,
                newHerobrinePlayer,
                new NetHandlerPlayServer(server, networkManager, newHerobrinePlayer));

        if (isNewSpawn) {
            updatePlayerLocation(newHerobrinePlayer,
                    sender.getPosition().getX(),
                    sender.getPosition().getY(),
                    sender.getPosition().getZ());

            EntityHelper.transferPlayerToDimension(
                    newHerobrinePlayer,
                    senderDimension,
                    newHerobrinePlayer.mcServer.getPlayerList());
        }

        sender.sendMessage(createMessage("Spawned Herobrine " + selectedHerobrineName + " at " + getPositionString(newHerobrinePlayer)));
    }

    private TextComponentBase createMessage(final String message) {
        return new TextComponentString(message);
    }

    private void registerAliases() {
        mAliases.add("hb");
    }

    private void updatePlayerLocation(
            final EntityPlayerMP player,
            final double newX,
            final double newY,
            final double newZ) {

        player.setPositionAndUpdate(newX, newY, newZ);
    }

    private void sendMovePackets(final HerobrinePlayer hb, final MinecraftServer server) {
        server.getPlayerList().sendPacketToAllPlayers(new SPacketEntityHeadLook(hb, (byte) 0.0f));
        server.getPlayerList().sendPacketToAllPlayers(new SPacketEntityTeleport(hb));
        server.getPlayerList().serverUpdateMovingPlayer(hb);
    }

    private String getPositionString(final HerobrinePlayer herobrinePlayer) {
        final BlockPos position = herobrinePlayer.getPosition();
        return position.getX()
                + " "
                + position.getY()
                + " "
                + position.getZ()
                + " in dimension "
                + herobrinePlayer.dimension;
    }

    private enum HerobrineCommandAction {
        SPAWN, MOVE, KILL, DELETE
    }
}
