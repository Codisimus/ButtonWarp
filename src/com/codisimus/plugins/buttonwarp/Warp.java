package com.codisimus.plugins.buttonwarp;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Properties;
import org.apache.commons.lang.time.DateUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A Warp is a Location that a Player is sent to when pressing a linked Button
 *
 * @author Codisimus
 */
public class Warp implements Comparable {
    static boolean log;
    static boolean broadcast;
    static boolean sound;
    private static ButtonWarpCommandSender cs = new ButtonWarpCommandSender();

    public String name; //A unique name for the Warp
    public String msg = ""; //Message sent to Player when using the Warp

    public double amount = 0; //Amount of money rewarded (negative for charging money)
    public String source = "server"; //Player name || 'Bank:'+Bank Name || 'server'

    public LinkedList<String> commands = new LinkedList<String>();

    /* Location */
    public String world;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public boolean ignoreYaw = false;
    public boolean ignorePitch = false;

    /* Reset time (will never reset if any are negative) */
    public int days = ButtonWarp.defaultDays;
    public int hours = ButtonWarp.defaultHours;
    public int minutes = ButtonWarp.defaultMinutes;
    public int seconds = ButtonWarp.defaultSeconds;

    public boolean global = false; //Reset Type
    public boolean restricted = ButtonWarp.defaultRestricted;
    public LinkedList<Button> buttons = new LinkedList<Button>(); //List of Blocks that activate the Warp

    Properties activationTimes = new Properties(); //ButtonLocation'PlayerName=Activations'Time

    /**
     * Constructs a new Warp with the given name at the given Player's location
     *
     * @param name The unique name of the Warp
     * @param player The Player who is creating the Warp, may be null
     */
    public Warp (String name, Player player) {
        this.name = name;

        //Set the Location data if the Player is provided
        if (player != null) {
            world = player.getWorld().getName();
            Location location = player.getLocation();
            x = location.getX();
            y = location.getY();
            z = location.getZ();
            yaw = location.getYaw();
            pitch = location.getPitch();
        }
    }

    /**
     * Constructs a new Warp with the given name, message, amount, and source
     *
     * @param name The unique name of the Warp
     * @param msg The message that the Warp will display
     * @param amount The price/reward of the Warp
     * @param source The source of the money transactions for the Warp
     */
    public Warp (String name, String msg, double amount, String source) {
        this.name = name;
        this.msg = msg;
        this.amount = amount;
        this.source = source;
    }

    /**
     * Returns true if the Player is able to activate the Warp
     * This method will charge the Player for activating the Warp
     *
     * @param player The Player who is activating the Warp
     * @param button The Block which was pressed
     * @return True if the Player is able to activate the Warp
     */
    public Boolean canActivate(Player player, Button button) {
        //False if the Player does not have access rights
        if (!hasAccess(player)) {
            return false;
        }

        //False if the Player is attempting to smuggle items
        if (isSmuggling(player, button)) {
            return false;
        }

        //False if the destination is on an unloaded World
        if (world != null && ButtonWarp.server.getWorld(world) == null) {
            player.sendMessage(ButtonWarpMessages.worldMissing.replace("<world>", world));
            return false;
        }

        //True if the Warp rewards money
        if (amount > 0) {
            return true; //Will check if timed out later
        }

        /* Is Timed Out? */
        String user = global ? "global" : player.getName();
        String key = button.getKey(user);
        String value = activationTimes.getProperty(key);
        int uses = 0;
        long time = 0;

        if (value != null) { //Not first use
            String[] split = value.split("'");
            uses = Integer.parseInt(split[0]);
            time = Long.parseLong(split[1]);
            String timeRemaining = getTimeRemaining(time);

            if (timeRemaining == null) { //Never resets
                //Return true if the User has not maxed out their uses
                if (uses >= button.max) {
                    player.sendMessage(amount > 1
                                       ? ButtonWarpMessages.cannotHaveAnotherReward
                                       : ButtonWarpMessages.cannotUseAgain);
                    return false;
                }
                uses++;
            } else if (!timeRemaining.equals("0")) { //Not timed out
                if (uses >= button.max) { //Player maxed out their uses
                    player.sendMessage((amount > 1
                                        ? ButtonWarpMessages.timeRemainingReward
                                        : ButtonWarpMessages.timeRemainingUse)
                                        .replace("<time>", timeRemaining));
                    return false;
                }
                uses++;
            } else { //Is timed out
                value = null; //We want to set a new time
            }
        }
        /* End Is Timed Out */

        //False if the Warp is not free and the Player cannot afford it
        if (amount < 0 && !ButtonWarp.hasPermission(player, "freewarp")) {
            if (!Econ.charge(player, source, Math.abs(amount))) {
                return false;
            }
        }

        //True, set the new activation time
        if (value == null) {
            setTime(button, user);
        } else {
            setTime(key, uses, time);
        }
        return true;
    }

    /**
     * Activates a Warp, teleporting the Player and triggering money transactions
     *
     * @param player The Player who is activating the Warp
     * @param button The Block which was pressed
     */
    public void activate(final Player player, Button button) {
        if (world != null) {
            teleport(player);
        }
        String playerName = player.getName();

        if (amount > 0) { //Rewards money
            if (isTimedOutForReward(player, button)) {
                if (ButtonWarp.hasPermission(player, "getreward")) {
                    Econ.reward(player, source, amount);
                }
            }
        }

        //Execute each Warp command
        for (String cmd : commands) {
            ButtonWarp.server.dispatchCommand(cs, cmd.replace("<player>", playerName));
        }

        //Send the message to the Player if there is one
        if (!msg.isEmpty()) {
            player.sendMessage(msg);
        }

        //Print the Warp usage to the console if logging Warps is on
        if (broadcast) {
            ButtonWarp.server.broadcastMessage(ButtonWarpMessages.broadcast
                                                .replace("<player>", playerName)
                                                .replace("<name>", name));
        } else if (log) {
            ButtonWarp.logger.info(playerName + " used Warp " + name);
        }
    }

    /**
     * Returns true if the Player is allowed to use the Warp
     * If the Warp is restricted then the Player is checked for buttonwarp.warp.<WarpName>
     *
     * @param player The Player who is being checked for access rights
     * @return True if the Player has access rights
     */
    private boolean hasAccess(Player player) {
        //Return true if the list is empty
        if (restricted) {
            if (!ButtonWarp.hasPermission(player, "warp." + name)) {
                player.sendMessage(ButtonWarpMessages.noAccess);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the Player is smuggling items
     *
     * @param player The Player who is being checked for smuggling
     * @return true if the Player is smuggling
     */
    private boolean isSmuggling(Player player, Button button) {
        //Return false if smuggling is allowed
        if (button.takeItems) {
            return false;
        }

        //Check item inventory for any items
        for (ItemStack item: player.getInventory().getContents()) {
            if (item != null) {
                player.sendMessage(ButtonWarpMessages.cannotTakeItems);
                return true;
            }
        }

        //Check armour contents for any items
        for (ItemStack item: player.getInventory().getArmorContents()) {
            if (item.getTypeId() != 0) {
                player.sendMessage(ButtonWarpMessages.cannotTakeArmor);
                return true;
            }
        }

        //Return false because the Player is not smuggling
        return false;
    }

    /**
     * Return true if enough time has passed since the Player last used the Button
     *
     * @param player The Player who is activating the Warp
     * @param button The Button which was pressed
     */
    private boolean isTimedOutForReward(Player player, Button button) {
        String user = global ? "global" : player.getName();
        String key = button.getKey(user);
        String value = activationTimes.getProperty(key);

        if (value == null) {
            return true;
        }

        String[] split = value.split("'");
        int uses = Integer.parseInt(split[0]);
        long time = Long.parseLong(split[1]);
        String timeRemaining = getTimeRemaining(time);

        if (timeRemaining == null) { //Never Resets
            //Return true if the User has not maxed out their uses
            if (uses >= button.max) {
                player.sendMessage(amount > 1
                                   ? ButtonWarpMessages.cannotHaveAnotherReward
                                   : ButtonWarpMessages.cannotUseAgain);
                return false;
            }
            setTime(key, uses + 1, time);
        } else if (!timeRemaining.equals("0")) { //Not timed out
            if (uses >= button.max) { //Player maxed out their uses
                player.sendMessage((amount > 1
                                    ? ButtonWarpMessages.timeRemainingReward
                                    : ButtonWarpMessages.timeRemainingUse)
                                    .replace("<time>", timeRemaining));
                return false;
            }
            setTime(key, uses + 1, time);
        } else { //Is timed out
            setTime(button, user);
        }

        return true;
    }

    /**
     * Returns the remaining time until the Button resets
     * Returns null if the Button never resets
     *
     * @param time The given time
     * @return the remaining time until the Button resets
     */
    private String getTimeRemaining(long time) {
        //Return null if the reset time is set to never
        if (days < 0 || hours < 0 || minutes < 0 || seconds < 0) {
            return null;
        }

        //Calculate the time that the Warp will reset
        time += days * DateUtils.MILLIS_PER_DAY
                + hours * DateUtils.MILLIS_PER_HOUR
                + minutes * DateUtils.MILLIS_PER_MINUTE
                + seconds * DateUtils.MILLIS_PER_SECOND;

        long timeRemaining = time - getCurrentMillis();

        if (timeRemaining > DateUtils.MILLIS_PER_DAY) {
            return (int) timeRemaining / DateUtils.MILLIS_PER_DAY + " day(s)";
        } else if (timeRemaining > DateUtils.MILLIS_PER_HOUR) {
            return (int) timeRemaining / DateUtils.MILLIS_PER_HOUR + " hour(s)";
        } else if (timeRemaining > DateUtils.MILLIS_PER_MINUTE) {
            return (int) timeRemaining / DateUtils.MILLIS_PER_MINUTE + " minute(s)";
        } else if (timeRemaining > DateUtils.MILLIS_PER_SECOND) {
            return (int) timeRemaining / DateUtils.MILLIS_PER_SECOND + " second(s)";
        } else {
            return "0";
        }
    }

    /**
     * Updates the Player's time value in the Map with the current time
     * The time is saved as ACTIVATION'TIME
     *
     * @param button The Button to set the time for
     * @param player The name of the Player whose time is to be updated
     */
    public void setTime(Button button, String player) {
        String key = button.getKey(player);
        setTime(key, 1, getCurrentMillis());
        save();
    }

    /**
     * Updates the Player's time value in the Map
     * The time is saved as ACTIVATION'TIME
     *
     * @param key The String of the Button Location and Player name
     * @param uses How many time the Player used the Button
     * @param time The last time the Button was reset
     */
    public void setTime(String key, int uses, long time) {
        activationTimes.setProperty(key, uses + "'" + time);
        save();
    }

    /**
     * Retrieves the time for the given Player
     *
     * @param button The Button to set the time for
     * @param player The name of the Player whose time is requested
     * @return The time as a String
     */
    public String getTime(Button button, String player) {
        String key = button.getKey(player);
        return activationTimes.getProperty(key);
    }

    /**
     * Teleports the given Player
     *
     * @param player The Player to be teleported
     * @param sendTo The destination of the Player
     */
    public void teleport(Player player) {
        World targetWorld = ButtonWarp.server.getWorld(world);
        if (targetWorld == null) {
            player.sendMessage(ButtonWarpMessages.worldMissing.replace("<world>", world));
            return;
        }
        Location sendTo = new Location(targetWorld, x, y, z);
        sendTo.setYaw(ignoreYaw ? player.getLocation().getYaw() : yaw);
        sendTo.setPitch(ignorePitch ? player.getLocation().getPitch() : pitch);

        //Ensure that the Chunk is loaded
        Chunk chunk = sendTo.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }

        player.teleport(sendTo);
        if (sound) {
            player.playSound(sendTo, Sound.ENDERMAN_TELEPORT, 0.8F, 0.075F);
        }
    }

    /**
     * Resets the user times for all Buttons of this Warp
     * if a Block is given then only reset that Button
     *
     * @param block The given Block
     */
    public void reset(Block block) {
        if (block == null) {
            //Reset all Buttons
            activationTimes.clear();
        } else {
            //Find the Button of the given Block and reset it
            String button = findButton(block).getLocationString() + "'";
            for (String key: activationTimes.stringPropertyNames()) {
                if (key.startsWith(button)) {
                    activationTimes.remove(key);
                }
            }
        }
        save();
    }

    /**
     * Returns the Button that is associated with the given Block
     *
     * @param block The given Block
     * @return the Button that is associated with the given Block
     */
    public Button findButton(Block block) {
        //Iterate through buttons to find the Button of the given Block
        for (Button button: buttons) {
            if (button.isBlock(block)) {
                return button;
            }
        }

        //Return null because the Button does not exist
        return null;
    }

    /**
     * Loads data from the save file
     *
     * @param data The data of the Buttons
     */
    void setButtons(String data) {
        //Cancel if no data is given
        if (data.isEmpty()) {
            return;
        }

        //Load data for each Button
        for (String string: data.split("; ")) {
            try {
                //Load the Block Location data of the Button
                String[] buttonData = string.split("'");

                //Construct a a new Button with the Location data
                Button button = new Button(buttonData[0], Integer.parseInt(buttonData[1]),
                        Integer.parseInt(buttonData[2]), Integer.parseInt(buttonData[3]));

                button.takeItems = Boolean.parseBoolean(buttonData[4]);
                button.max = Integer.parseInt(buttonData[5]);

                buttons.add(button);
            } catch (Exception invalidButton) {
                ButtonWarp.logger.info('"'+string+'"'+" is not a valid Button for Warp "+name);
                invalidButton.printStackTrace();
            }
        }
    }

    /**
     * Loads data from the save file
     *
     * @param data The data of the Buttons
     */
    void setButtonsOld(String data) {
        //Cancel if no data is given
        if (data.isEmpty()) {
            return;
        }

        int index;

        //Load data for each Button
        for (String string: data.split("; ")) {
            try {
                index = string.indexOf('{');

                //Load the Block Location data of the Button
                String[] blockData = string.substring(0, index).split("'");

                //Construct a a new Button with the Location data
                Button button = new Button(blockData[0], Integer.parseInt(blockData[1]),
                        Integer.parseInt(blockData[2]), Integer.parseInt(blockData[3]));

                button.takeItems = Boolean.parseBoolean(blockData[4]);
                button.max = Integer.parseInt(blockData[5]);

                //Load the HashMap of Users of the Button
                for (String user: string.substring(index + 1, string.length() - 1).split(", ")) {
                    //Don't load if the data is corrupt or empty
                    if ((index = user.indexOf('@')) != -1) {
                        int[] time = new int[6];
                        String[] timeData = user.substring(index + 1).split("'");

                        String userData = user.substring(0, index);
                        index = userData.indexOf("'");
                        String userName = userData.substring(0, index);

                        time[0] = Integer.parseInt(userData.substring(index + 1));

                        if (timeData.length == 4) {
                            time[1] = 2011;
                            for (int j = 1; j < 5; j++) {
                                time[j+1] = Integer.parseInt(timeData[j - 1]);
                            }
                        } else {
                            for (int j = 1; j < 6; j++) {
                                time[j] = Integer.parseInt(timeData[j - 1]);
                            }
                        }

                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.YEAR, time[1]);
                        cal.set(Calendar.DAY_OF_YEAR, time[2]);
                        cal.set(Calendar.HOUR, time[3]);
                        cal.set(Calendar.MINUTE, time[4]);
                        cal.set(Calendar.SECOND, time[5]);

                        String key = button.getLocationString() + "'" + userName;
                        String value = time[0] + "'" + cal.getTimeInMillis();
                        activationTimes.setProperty(key, value);
                    }
                }

                buttons.add(button);
            } catch (Exception invalidButton) {
                ButtonWarp.logger.info('"'+string+'"'+" is not a valid Button for Warp "+name);
                invalidButton.printStackTrace();
            }
        }
    }

    public static long getCurrentMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Writes Warp data to file
     *
     */
    public void save() {
        ButtonWarp.saveWarp(this);
    }

    @Override
    public int compareTo(Object object) {
        if (object instanceof Warp) {
            Warp warp = (Warp) object;
            return name.compareTo(warp.name);
        }
        return 0;
    }
}
