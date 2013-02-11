package com.codisimus.plugins.buttonwarp;

import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Executes Player Commands
 *
 * @author Codisimus
 */
public class ButtonWarpCommand implements CommandExecutor {
    static int multiplier;
    static String command;
    private static enum Action {
        HELP, MAKE, MOVE, LINK, UNLINK, DELETE, COST, REWARD, ACCESS, SOURCE,
        CMD, MSG, TIME, GLOBAL, MAX, ALLOW, DENY, LIST, INFO, RESET, RL
    }
    private static enum Help { CREATE, SETUP, BUTTON }
    static final EnumSet<Material> LINKABLE = EnumSet.of(
            Material.LEVER, Material.STONE_PLATE, Material.WOOD_PLATE,
            Material.STONE_BUTTON, Material.WOOD_BUTTON, Material.TRIPWIRE,
            Material.TRIPWIRE_HOOK, Material.DETECTOR_RAIL);

    /**
     * Listens for ButtonWarp commands to execute them
     *
     * @param sender The CommandSender who may not be a Player
     * @param command The command that was executed
     * @param alias The alias that the sender used
     * @param args The arguments for the command
     * @return true always
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //Cancel if the command is not from a Player
        if (!(sender instanceof Player)) {
            if (args.length > 0 && args[0].equals("rl")) {
                ButtonWarp.rl();
            }
            return true;
        }

        final Player player = (Player) sender;

        //Display the help page if the Player did not add any arguments
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        Action action;

        try {
            action = Action.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException notEnum) {
            if (args.length != 1) {
                sendHelp(player);
                return true;
            }

            //Cancel if the Player does not have permission to use the command
            if (!ButtonWarp.hasPermission(player, "commandwarp")) {
                player.sendMessage(ButtonWarpMessages.permission);
                return true;
            }

            final Warp warp = getWarp(player, args[0]);
            if (warp == null) {
                sendHelp(player);
                return true;
            }

            if (ButtonWarpDelayListener.warpers.containsKey(player)) {
                player.sendMessage("§4You are already in the process of warping!");
                return true;
            }

            if (warp.world != null && ButtonWarp.server.getWorld(warp.world) == null) {
                player.sendMessage(ButtonWarpMessages.worldMissing.replace("<world>", warp.world));
                return true;
            }

            if (warp.amount < 0 && !ButtonWarp.hasPermission(player, "freewarp")
                    && !Econ.charge(player, warp.source, Math.abs(warp.amount) * multiplier)) {
                return true;
            }

            //Delay Teleporting
            BukkitTask teleTask = ButtonWarp.server.getScheduler().runTaskLater(ButtonWarp.plugin, new Runnable() {
                    @Override
                    public void run() {
                        warp.teleport(player);
                        if (ButtonWarpListener.delay > 0) {
                            ButtonWarpDelayListener.warpers.remove(player);
                        }
                    }
                }, 20L * ButtonWarpListener.delay);

            if (ButtonWarpListener.delay > 0) {
                ButtonWarpDelayListener.warpers.put(player, teleTask);
                if (!ButtonWarpMessages.delay.isEmpty()) {
                    player.sendMessage(ButtonWarpMessages.delay);
                }
            }
            return true;
        }

        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, args[0]) && !args[0].equals("help")) {
            player.sendMessage(ButtonWarpMessages.permission);
            return true;
        }

        //Execute the correct command
        switch (action) {
        case MAKE:
            switch (args.length) {
            case 2:
                make(player, args[1], false);
                return true;

            case 3:
                if (args[2].equals("nowhere")) {
                    make(player, args[1], true);
                    return true;
                }
                break;

            default: break;
            }

            sendCreateHelp(player);
            return true;

        case MOVE:
            switch (args.length) {
            case 2:
                move(player, args[1], false);
                return true;

            case 3:
                if (args[2].equals("nowhere")) {
                    move(player, args[1], true);
                    return true;
                }
                break;

            default: break;
            }

            sendCreateHelp(player);
            return true;

        case LINK:
            if (args.length == 2) {
                link(player, args[1]);
            } else {
                sendCreateHelp(player);
            }
            return true;

        case UNLINK:
            if (args.length == 1) {
                unlink(player);
            } else {
                sendCreateHelp(player);
            }
            return true;

        case DELETE:
            switch (args.length) {
                case 1: delete(player, null); return true;
                case 2: delete(player, args[1]); return true;
                default: sendCreateHelp(player); return true;
            }

        case COST:
            switch (args.length) {
            case 2:
                try {
                    amount(player, null, -Math.abs(Double.parseDouble(args[1])));
                    return true;
                } catch (Exception notDouble) {
                    break;
                }

            case 3:
                try {
                    amount(player, args[1], -Math.abs(Double.parseDouble(args[2])));
                    return true;
                } catch (Exception notDouble) {
                    break;
                }

            default: break;
            }

            sendSetupHelp(player);
            return true;

        case REWARD:
            switch (args.length) {
            case 2:
                try {
                    amount(player, null, Math.abs(Double.parseDouble(args[1])));
                    return true;
                } catch (Exception notDouble) {
                    break;
                }

            case 3:
                try {
                    amount(player, args[1], Math.abs(Double.parseDouble(args[2])));
                    return true;
                } catch (Exception notDouble) {
                    break;
                }

            default: break;
            }

            sendSetupHelp(player);
            return true;

        case ACCESS:
            switch (args.length) {
                case 2: access(player, null, args[1]); return true;
                case 3: access(player, args[1], args[2]); return true;
                default: sendSetupHelp(player); return true;
            }

        case SOURCE:
            switch (args.length) {
            case 2:
                source(player, null, false, args[1]);
                return true;

            case 3:
                if (args[1].equals("bank")) {
                    source(player, null, true, args[2]);
                } else {
                    source(player, args[1], false, args[2]);
                }
                return true;

            case 4:
                if (args[2].equals("bank")) {
                    source(player, args[1], true, args[3]);
                } else {
                    break;
                }
                return true;

            default: break;
            }

            sendSetupHelp(player);
            return true;

        case CMD:
            if (args.length < 3) {
                sendSetupHelp(player);
                return true;
            }

            if (args[1].equals("add") || args[1].equals("remove")) {
                setCommand(player, null, args[1].equals("add"), concatArgs(args, 2));
            } else {
                setCommand(player, args[1], args[2].equals("add"), concatArgs(args, 3));
            }
            return true;

        case MSG:
            if (args.length < 3) {
                sendSetupHelp(player);
                return true;
            }

            String msg = "";
            for (int i=2; i < args.length; i++) {
                msg = msg.concat(args[i].concat(" "));
            }

            msg(player, args[1], msg);
            return true;

        case TIME:
            switch (args.length) {
            case 5:
                try {
                    time(player, null, Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                            Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                    return true;
                } catch (Exception notInt) {
                    sendSetupHelp(player);
                    break;
                }

            case 6:
                try {
                    time(player, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
                            Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                    return true;
                } catch (Exception notInt) {
                    sendSetupHelp(player);
                    break;
                }

            default: break;
            }

            sendSetupHelp(player);
            return true;

        case GLOBAL:
            switch (args.length) {
            case 2: //Name is not provided
                try {
                    global(player, null, Boolean.parseBoolean(args[1]));
                    return true;
                } catch (Exception notBool) {
                    break;
                }

            case 3: //Name is provided
                try {
                    global(player, args[1], Boolean.parseBoolean(args[2]));
                    return true;
                } catch (Exception notBool) {
                    break;
                }

            default: break;
            }

            sendSetupHelp(player);
            return true;

        case MAX:
            if (args.length == 2) {
                try {
                    max(player, Integer.parseInt(args[1]));
                    return true;
                } catch (Exception notInt) {
                }
            }

            sendButtonHelp(player);
            return true;

        case ALLOW:
            if (args.length == 2 && args[1].startsWith("item")) {
                allow(player);
            } else {
                sendButtonHelp(player);
            }
            return true;

        case DENY:
            if (args.length == 2 && args[1].startsWith("item")) {
                deny(player);
            } else {
                sendButtonHelp(player);
            }
            return true;

        case LIST:
            switch (args.length) {
            case 1: list(player, null); return true;
            case 2: list(player, args[1]); return true;
            default: sendHelp(player); return true;
            }

        case INFO:
            switch (args.length) {
            case 1: info(player, null); return true;
            case 2: info(player, args[1]); return true;
            default: sendHelp(player); return true;
            }

        case RESET:
            switch (args.length) {
            case 1: reset(player, null); return true;
            case 2: reset(player, args[1]); return true;
            default: break;
            }

            sendHelp(player);
            return true;

        case RL:
            if (args.length == 1) {
                ButtonWarp.rl(player);
            } else {
                sendHelp(player);
            }
            return true;

        case HELP:
            if (args.length == 2) {
                Help help;

                try {
                    help = Help.valueOf(args[1].toUpperCase());
                } catch (Exception notEnum) {
                    sendHelp(player);
                    return true;
                }

                switch (help) {
                case CREATE: sendCreateHelp(player); break;
                case SETUP: sendSetupHelp(player); break;
                case BUTTON: sendButtonHelp(player); break;
                }
            } else {
                sendHelp(player);
            }

            return true;

        default: sendHelp(player); return true;
        }
    }

    /**
     * Creates a new Warp of the given name at the given Player's Location
     *
     * @param player The Player creating the Warp
     * @param name The name of the Warp being created (must not already exist)
     * @param noWarp If true the Warp will be created with a null Location
     */
    private static void make(Player player, String name, boolean noWhere) {
        //Cancel if the Warp already exists
        if (ButtonWarp.findWarp(name) != null) {
            player.sendMessage("§4A Warp named §6" + name + "§4 already exists.");
            return;
        }

        if (noWhere) {
            //Create a Warp with a null Location
            ButtonWarp.addWarp(new Warp(name, null));
            player.sendMessage("§5Warp §6" + name + "§5 Made!");
        } else {
            //Create a Warp with the Player's Location
            ButtonWarp.addWarp(new Warp(name, player));
            player.sendMessage("§5Warp §6" + name + "§5 Made at current location!");
        }
    }

    /**
     * Moves the Location of the specified Warp
     *
     * @param player The Player moving the Warp
     * @param name The name of the Warp being moved
     * @param noWarp If true the Warp will be moved to a null Location
     */
    private static void move(Player player, String name, boolean noWhere) {
        //Cancel if the Warp with the given name does not exist
        Warp warp = ButtonWarp.findWarp(name);
        if (warp == null ) {
            player.sendMessage("§4Warp §6" + name + "§4 does not exsist.");
            return;
        }

        if (noWhere) {
            //Set the Warp to a null Location
            warp.world = null;
            player.sendMessage("§5Warp §6" + name + "§5 moved to nowhere");
        } else {
            //Set the Warp to the Player's Location
            warp.world = player.getWorld().getName();
            Location location = player.getLocation();
            warp.x = location.getX();
            warp.y = location.getY();
            warp.z = location.getZ();
            warp.pitch = location.getPitch();
            warp.yaw = location.getYaw();
            player.sendMessage("§5Warp §6" + name + "§5 moved to current location");
        }

        warp.save();
    }

    /**
     * Links the target Block to the specified Warp
     *
     * @param player The Player linking the Block they are targeting
     * @param name The name of the Warp the Block will be linked to
     */
    private static void link(Player player, String name) {
        //Cancel if the Player is not targeting a correct Block type
        Block block = player.getTargetBlock(null, 10);
        Material type = block.getType();
        if (!LINKABLE.contains(type)) {
            player.sendMessage("§4You are targeting a §6" + type.name()
                                + "§4, linkable items are "
                                + LINKABLE.toString());
            return;
        }

        //Cancel if the Block is already linked to a Warp
        Warp warp = ButtonWarp.findWarp(block);
        if (warp != null) {
            player.sendMessage("§4Button is already linked to Warp §6"
                                + warp.name);
            return;
        }

        //Cancel if the Warp with the given name does not exist
        warp = ButtonWarp.findWarp(name);
        if (warp == null) {
            player.sendMessage("§4Warp §6" + name + "§4 does not exsist.");
            return;
        }

        warp.buttons.add(new Button(block));
        player.sendMessage("§5Button has been linked to Warp §6" + name);
        warp.save();
    }

    /**
     * Unlinks the target Block from the specified Warp
     *
     * @param player The Player unlinking the Block they are targeting
     */
    private static void unlink(Player player) {
        //Cancel if the Player is not targeting a correct Block type
        Block block = player.getTargetBlock(null, 10);
        Material type = block.getType();
        if (!LINKABLE.contains(type)) {
            player.sendMessage("§4You are targeting a §6" + type.name()
                                + "§4, linkable items are "
                                + LINKABLE.toString());
            return;
        }

        //Cancel if the Block is not linked to a Warp
        Warp warp = ButtonWarp.findWarp(block);
        if (warp == null) {
            player.sendMessage("§4Target Block is not linked to a Warp");
            return;
        }

        warp.buttons.remove(warp.findButton(block));
        player.sendMessage("§5Button has been unlinked from Warp §6"
                            + warp.name);
        warp.save();
    }

    /**
     * Deletes the specified Warp
     * If a name is not provided, the Warp of the target Block is deleted
     *
     * @param player The Player deleting the Warp
     * @param name The name of the Warp to be deleted
     */
    private static void delete(Player player, String name) {
        //Cancel if the Warp was not found
        Warp warp = getWarp(player, name);
        if (warp == null) {
            return;
        }

        ButtonWarp.removeWarp(warp);
        player.sendMessage("§5Warp §6" + warp.name + "§5 was deleted!");
    }

    /**
     * Modifies the amount of the specified Warp
     * If a name is not provided, the Warp of the target Block is modified
     *
     * @param player The Player modifying the Warp
     * @param name The name of the Warp to be modified
     * @param amount The new amount value
     */
    private static void amount(Player player, String name, double amount) {
        //Cancel if the Warp was not found
        Warp warp = getWarp(player, name);
        if (warp == null) {
            return;
        }

        warp.amount = amount;
        player.sendMessage("§5Amount for Warp §6" + warp.name
                            + "§5 has been set to §6" + amount);
        warp.save();
    }

    /**
     * Modifies the access of the specified Warp
     * If a name is not provided, the Warp of the target Block is modified
     *
     * @param player The Player modifying the Warp
     * @param name The name of the Warp to be modified
     * @param access The new access value
     */
    private static void access(Player player, String name, String access) {
        //Cancel if the Warp was not found
        Warp warp = getWarp(player, name);
        if (warp == null) {
            return;
        }

        if (access.equals("public")) {
            warp.restricted = false;
        } else if (access.equals("restricted")) {
            warp.restricted = true;
        } else {
            player.sendMessage("§6" + access + "§4is not valid access type. Use §6public §4or §6restricted");
            return;
        }
        player.sendMessage("§5Access for Warp §6" + warp.name
                            + "§5 has been set to §6" + access);

        warp.save();
    }

    /**
     * Modifies the source of the specified Warp
     * If a name is not provided, the Warp of the target Block is modified
     *
     * @param player The Player modifying the Warp
     * @param name The name of the Warp to be modified
     * @param bank True if the new source is a bank
     * @param source The new source value
     */
    private static void source(Player player, String name, boolean bank, String source) {
        //Cancel if the Warp was not found
        Warp warp = getWarp(player, name);
        if (warp == null) {
            return;
        }

        if (bank) {
            source = "bank:".concat(source);
        }

        warp.source = source;
        player.sendMessage("§5Money source for Warp §6" + warp.name
                            + "§5 has been set to §6" + source);
        warp.save();
    }

    /**
     * Manages commands of the specified Warp
     * If a name is not provided, the Warp of the target Block is modified
     *
     * @param player The Player modifying the Warp
     * @param name The name of the Warp to be modified
     * @param add True if the command is to be added
     * @param cmd The command to be added/removed
     */
    public static void setCommand(Player player, String name, boolean add, String cmd) {
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }

        Warp warp = getWarp(player, name);
        if (warp == null) {
            return;
        }

        for (String string: warp.commands) {
            if (cmd.equals(string)) {
                /* The command was found */
                if (!add) {
                    warp.commands.remove(cmd);
                    player.sendMessage("§6" + cmd + "§5 removed as a command "
                                        + "for Warp §6" + warp.name);
                    warp.save();
                } else {
                    player.sendMessage("§6" + cmd + "§5 is already a command "
                                        + "for Warp §6" + warp.name);
                }
                return;
            }
        }

        /* The command was not found */
        if (add) {
            warp.commands.add(cmd);
            player.sendMessage("§6" + cmd + "§5 added as a command "
                                + "for Warp §6" + warp.name);
            warp.save();
        } else {
            player.sendMessage("§6" + cmd + "§5 was not found as a command "
                                + "for Warp §6" + warp.name);
        }
    }

    /**
     * Modifies the message of the specified Warp
     * If a name is not provided, the Warp of the target Block is modified
     *
     * @param player The Player modifying the Warp
     * @param name The name of the Warp to be modified
     * @param msg The new message
     */
    private static void msg(Player player, String name, String msg) {
        //Find the Warp that will be modified using the given name
        Warp warp = ButtonWarp.findWarp(name);

        //Cancel if the Warp does not exist
        if (warp == null ) {
            player.sendMessage("§4Warp §6" + name + "§4 does not exsist.");
            return;
        }

        warp.msg = ButtonWarpMessages.format(msg);

        player.sendMessage("§5Message for Warp §46" + warp.name
                            + "§5 has been set to §6" + warp.msg);
        warp.save();
    }

    /**
     * Modifies the reset time of the specified Warp
     * If a name is not provided, the Warp of the target Block is modified
     *
     * @param player The Player modifying the Warp
     * @param name The name of the Warp to be modified
     * @param days The amount of days
     * @param hours The amount of hours
     * @param minutes The amount of minutes
     * @param seconds The amount of seconds
     */
    private static void time(Player player, String name, int days, int hours, int minutes, int seconds) {
        //Cancel if the Warp was not found
        Warp warp = getWarp(player, name);
        if (warp == null) {
            return;
        }

        warp.days = days;
        warp.hours = hours;
        warp.minutes = minutes;
        warp.seconds = seconds;
        player.sendMessage("§5Reset time for Warp §6" + warp.name
                            + "§5 has been set to §5" + days + " days, " + hours
                            + " hours, " + minutes + " minutes, and " + seconds
                            + " seconds");

        warp.save();
    }

    /**
     * Modifies the reset type of the specified Warp
     * If a name is not provided, the Warp of the target Block is modified
     *
     * @param player The Player modifying the Warp
     * @param name The name of the Warp to be modified
     * @param global True if the new reset type is global
     */
    private static void global(Player player, String name, boolean global) {
        //Cancel if the Warp was not found
        Warp warp = getWarp(player, name);
        if (warp == null) {
            return;
        }

        warp.global = global;
        player.sendMessage("§5Warp §6" + name + "§5 has been set to §6"
                            + (global ? "global" : "individual") + "§5 reset!");

        warp.save();
    }

    /**
     * Modifies the maximum uses per reset of the target Button
     *
     * @param player The Player modifying the maximum amount
     * @param max The new maximum amount
     */
    private static void max(Player player, int max) {
        Block block = player.getTargetBlock(null, 10);

        //Find the Warp that will be modified using the target Block
        Warp warp = ButtonWarp.findWarp(block);

        //Cancel if the Warp does not exist
        if (warp == null ) {
            player.sendMessage("§4Target Block is not linked to a Warp");
            return;
        }

        Button button = warp.findButton(block);
        button.max = max;

        player.sendMessage("§5Players may use target Button §6"
                            + max + "§5 times per reset");
        warp.save();
    }

    /**
     * Allows use of the target Button if the Player's inventory is not empty
     *
     * @param player The Player modifying the Button
     */
    private static void allow(Player player) {
        Block block = player.getTargetBlock(null, 10);

        //Find the Warp that will be modified using the target Block
        Warp warp = ButtonWarp.findWarp(block);

        //Cancel if the Warp does not exist
        if (warp == null ) {
            player.sendMessage("§4Target Block is not linked to a Warp");
            return;
        }

        Button button = warp.findButton(block);
        button.takeItems = true;

        player.sendMessage("§5Players may take items when using this Button to Warp");
        warp.save();
    }

    /**
     * Denies use of the target Button if the Player's inventory is not empty
     *
     * @param player The Player modifying the Button
     */
    private static void deny(Player player) {
        Block block = player.getTargetBlock(null, 10);

        //Find the Warp that will be modified using the target Block
        Warp warp = ButtonWarp.findWarp(block);

        //Cancel if the Warp does not exist
        if (warp == null ) {
            player.sendMessage("§4Target Block is not linked to a Warp");
            return;
        }

        Button button = warp.findButton(block);
        button.takeItems = false;

        player.sendMessage("§5Players cannot take items when using this Button to Warp");
        warp.save();
    }

    /**
     * Displays a list of current Warps
     *
     * @param player The Player requesting the list
     */
    private static void list(Player player, String keyword) {
        String warpList = "Current Warps:  §6";

        //Display each Warp, including the amount if an Economy plugin is present
        if (Econ.economy != null) {
            for (Warp warp : ButtonWarp.getWarps()) {
                if (keyword != null || StringUtils.containsIgnoreCase(warp.name, keyword)) {
                    if (warp.amount != 0) {
                        warpList += warp.name + "§f(§2" + Econ.format(warp.amount) + "§f)§7, §6";
                    } else {
                        warpList += warp.name + "§7, §6";
                    }
                }
            }
        } else {
            for (Warp warp : ButtonWarp.getWarps()) {
                if (keyword != null || StringUtils.containsIgnoreCase(warp.name, keyword)) {
                    warpList += warp.name + "§7, §6";
                }
            }
        }

        player.sendMessage(warpList.substring(0, warpList.length() - 4));
    }

    /**
     * Displays the info of the specified Warp
     * If a name is not provided, the Warp of the target Block is used
     *
     * @param player The Player requesting the info
     * @param name The name of the Warp
     */
    private static void info(Player player, String name) {
        //Cancel if the Warp was not found
        Warp warp = getWarp(player, name);
        if (warp == null) {
            return;
        }

        String type = "Player";
        if (warp.global) {
            type = "global";
        }

        String line = "§2Name:§b "+warp.name;
        if (Econ.economy != null) {
            line += " §2Amount:§b " + Econ.format(warp.amount)
                    + " §2Money Source:§b " + warp.source;
        }

        player.sendMessage(line);
        player.sendMessage("§2Warp Location:§b " + warp.world + ", "
                            + (int) warp.x + ", " + (int) warp.y + ", "
                            + (int) warp.z + " §2Reset Type:§b " + type);
        player.sendMessage("§2Reset Time:§b " + warp.days + " days, "
                            + warp.hours + " hours, " + warp.minutes
                            + " minutes, and " + warp.seconds + " seconds.");
        player.sendMessage("§2Commands:§b " + warp.commands);
        player.sendMessage("§2Restricted:§b " + warp.restricted);
    }

    /**
     * Reset the use times of the specified Warp/Button
     * If a name is not provided, the target Button is reset
     *
     * @param player The Player reseting the Buttons
     * @param name The name of the Warp
     */
    private static void reset(Player player, String name) {
        //Reset the target Button if a name was not provided
        if (name == null) {
            //Find the Warp that will be reset using the given name
            Block block = player.getTargetBlock(null, 10);
            Warp warp = ButtonWarp.findWarp(block);

            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("§4Target Block is not linked to a Warp");
                return;
            }

            warp.reset(block);

            player.sendMessage("§5Target Button has been reset.");
            return;
        }

        //Reset all Buttons in every Warp if the name provided is 'all'
        if (name.equals("all")) {
            for (Warp warp: ButtonWarp.getWarps()) {
                warp.reset(null);
            }

            player.sendMessage("§5All Buttons in all Warps have been reset.");
            return;
        }

        //Find the Warp that will be reset using the given name
        Warp warp = ButtonWarp.findWarp(name);

        //Cancel if the Warp does not exist
        if (warp == null ) {
            player.sendMessage("§4Warp §6" + name + "§4 does not exsist.");
            return;
        }

        //Reset all Buttons linked to the Warp
        warp.reset(null);

        player.sendMessage("§5All Buttons in Warp §6"
                            + name + "§5 have been reset.");
        warp.save();
    }

    /**
     * Displays the ButtonWarp Help Page to the given Player
     *
     * @param player The Player needing help
     */
    private static void sendHelp(Player player) {
        player.sendMessage("§e     ButtonWarp Help Page:");
        if (ButtonWarp.hasPermission(player, "commandwarp")) {
            player.sendMessage("§2/"+command+" <Name>§b Teleports to the Given Warp");
        }
        if (ButtonWarp.hasPermission(player, "list")) {
            player.sendMessage("§2/"+command+" list [keyword]§b Lists all Warps");
        }
        if (ButtonWarp.hasPermission(player, "info")) {
            player.sendMessage("§2/"+command+" info [Name]§b Gives information about the Warp");
        }
        if (ButtonWarp.hasPermission(player, "reset")) {
            player.sendMessage("§2/"+command+" reset <Name|all>§b Resets Buttons linked to the Warp");
        }
        if (ButtonWarp.hasPermission(player, "rl")) {
            player.sendMessage("§2/"+command+" rl§b Reloads ButtonWarp Plugin");
        }
        player.sendMessage("§2/"+command+" help create§b Displays ButtonWarp Create Help Page");
        player.sendMessage("§2/"+command+" help setup§b Displays ButtonWarp Setup Help Page");
        player.sendMessage("§2/"+command+" help button§b Displays ButtonWarp Button Help Page");
    }

    /**
     * Displays the ButtonWarp Create Help Page to the given Player
     *
     * @param player The Player needing help
     */
    private static void sendCreateHelp(Player player) {
        player.sendMessage("§e     ButtonWarp Create Help Page:");
        if (ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("§2/"+command+" make <Name>§b Makes Warp at current location");
            player.sendMessage("§2/"+command+" make <Name> nowhere§b Makes a Warp that doesn't teleport");
        }
        if (ButtonWarp.hasPermission(player, "move")) {
            player.sendMessage("§2/"+command+" move <Name> (nowhere)§b Moves an existing Warp");
        }
        if (ButtonWarp.hasPermission(player, "link")) {
            player.sendMessage("§2/"+command+" link <Name>§b Links target Block with Warp");
        }
        if (ButtonWarp.hasPermission(player, "unlink")) {
            player.sendMessage("§2/"+command+" unlink §b Unlinks target Block with Warp");
        }
        if (ButtonWarp.hasPermission(player, "delete")) {
            player.sendMessage("§2/"+command+" delete [Name]§b Deletes Warp");
        }
    }

    /**
     * Displays the ButtonWarp Setup Help Page to the given Player
     *
     * @param player The Player needing help
     */
    private static void sendSetupHelp(Player player) {
        player.sendMessage("§e     ButtonWarp Create Help Page:");
        if (ButtonWarp.hasPermission(player, "msg")) {
            player.sendMessage("§2/"+command+" msg <Name> <Msg>§b Sets message received after using Warp");
        }
        if (ButtonWarp.hasPermission(player, "cost")) {
            player.sendMessage("§2/"+command+" cost [Name] <Amount>§b Sets the cost for using the Warp");
        }
        if (ButtonWarp.hasPermission(player, "reward")) {
            player.sendMessage("§2/"+command+" reward [Name] <Amount>§b Sets the reward for using the Warp");
        }
        if (ButtonWarp.hasPermission(player, "source")) {
            player.sendMessage("§2/"+command+" source [Name] server§b Generates/Destroys money");
            player.sendMessage("§2/"+command+" source [Name] <Player>§b Gives/Takes money from Player");
            player.sendMessage("§2/"+command+" source [Name] bank <Bank>§b Gives/Takes money from Bank");
        }
        if (ButtonWarp.hasPermission(player, "cmd")) {
            player.sendMessage("§2/"+command+" cmd [Name] <add|remove> <Command>§b Sets a command to be executed");
        }
        if (ButtonWarp.hasPermission(player, "time")) {
            player.sendMessage("§2/"+command+" time [Name] <Days> <Hrs> <Mins> <Secs>§b Sets cooldown time");
        }
        if (ButtonWarp.hasPermission(player, "global")) {
            player.sendMessage("§2/"+command+" global [Name] <true|false>§b Toggles global cooldown");
        }
        if (ButtonWarp.hasPermission(player, "access")) {
            player.sendMessage("§2/"+command+" access [Name] public §bAnyone can Warp");
            player.sendMessage("§2/"+command+" access [Name] restricted§b Need Permission node to warp");
        }
    }

    /**
     * Displays the rest of the ButtonWarp Help Page to the given Player
     *
     * @param player The Player needing help
     */
    private static void sendButtonHelp(Player player) {
        player.sendMessage("§e     ButtonWarp Button Modification Help Page:");
        if (ButtonWarp.hasPermission(player, "max")) {
            player.sendMessage("§2/"+command+" max <MaxNumber>§b Sets Max uses per reset");
        }
        if (ButtonWarp.hasPermission(player, "allow")) {
            player.sendMessage("§2/"+command+" allow items§b Players can Warp with items");
        }
        if (ButtonWarp.hasPermission(player, "deny")) {
            player.sendMessage("§2/"+command+" deny items§b Players cannot Warp with items");
        }
        if (ButtonWarp.hasPermission(player, "reset")) {
            player.sendMessage("§2/"+command+" reset§b Resets activation times for target Button");
        }
    }

    /**
     * Returns the Warp with the given name
     * If no name is provided the Warp is found using the target Block
     *
     * @param player The Player target the Block
     * @param name The name of the Warp to be found
     * @return The Warp or null if none was found
     */
    private static Warp getWarp(Player player, String name) {
        Warp warp;

        if (name == null) {
            //Find the Warp using the target Block
            warp = ButtonWarp.findWarp(player.getTargetBlock(null, 10));

            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("§4Target Block is not linked to a Warp");
                return null;
            }
        } else {
            //Find the Warp using the given name
            warp = ButtonWarp.findWarp(name);

            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("§4Warp §6" + name + "§4 does not exsist.");
                return null;
            }
        }

        return warp;
    }

    public static String concatArgs(String[] args, int first) {
        return concatArgs(args, first, args.length - 1);
    }

    public static String concatArgs(String[] args, int first, int last) {
        String string = "";
        for (int i = first; i <= last; i++) {
            string += " " + args[i];
        }
        return string.isEmpty() ? string : string.substring(1);
    }
}
