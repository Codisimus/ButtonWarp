package com.codisimus.plugins.buttonwarp;

import java.util.Calendar;
import java.util.LinkedList;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A Warp is a Location that a Player is sent to when pressing a linked Button
 * 
 * @author Codisimus
 */
public class Warp {
    public String name; //A unique name for the Warp
    public String msg = ""; //Message sent to Player when using the Warp

    public double amount = 0; //Amount of money rewarded (negative for charging money)
    public String source = "server"; //Player name || 'Bank:'+Bank Name || 'server'

    public String world; //Location data to teleport to
    public double x;
    public double y;
    public double z;
    public float pitch;
    public float yaw;

    public int days = ButtonWarp.defaultDays; //Reset time (will never reset if any are negative) 
    public int hours = ButtonWarp.defaultHours;
    public int minutes = ButtonWarp.defaultMinutes;
    public int seconds = ButtonWarp.defaultSeconds;

    public boolean global = false; //Reset Type

    public LinkedList<String> access = new LinkedList<String>(); //List of Groups that have access (public if empty)
    public LinkedList<Button> buttons = new LinkedList<Button>(); //List of Blocks that activate the Warp
    
    static boolean broadcast;
    static boolean log;

    /**
     * Constructs a new Warp with the given name at the given Player's location
     * 
     * @param name The unique name of the Warp
     * @param player The Player who is creating the Warp or null if no location is wanted
     * @return The newly created Warp
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
            pitch = location.getPitch();
            yaw = location.getYaw();
        }
    }

    /**
     * Constructs a new Warp with the given name, message, amount, and source
     *
     * @param name The unique name of the Warp
     * @param msg The message that the Warp will display
     * @param amount The price/reward of the Warp
     * @param source The source of the money transactions for the Warp
     * @return The newly created Warp
     */
    public Warp (String name, String msg, double amount, String source) {
        this.name = name;
        this.msg = msg;
        this.amount = amount;
        this.source = source;
    }

    /**
     * Activates a Warp, teleporting the Player and triggering money transactions
     * 
     * @param player The Player who is activating the Warp
     * @param block The Block which was pressed
     * @return True if the activation was successful
     */
    public Boolean activate(Player player, Block block) {
        //Cancel if the Player does not have access to use the Warp
        if (!hasAccess(player))
            return false;
        
        //Retrieve the Button associated with the given Block
        Button button = findButton(block);
        
        //Cancel if the Player is attempting to smuggle items to a new World
        if (isSmuggling(player, button))
            return false;
        
        //The order of money transactions and teleporting is determined by the value of the amount
        if (amount > 0) {
            //Teleport the Player first if there is a Location
            if (world != null) {
                World targetWorld = ButtonWarp.server.getWorld(world);
                if (targetWorld == null) {
                    player.sendMessage("The World you are trying to Warp to is currently unavailable");
                    return true;
                }

                Location sendTo = new Location(targetWorld, x, y, z);
                sendTo.setPitch(pitch);
                sendTo.setYaw(yaw);

                teleport(player, sendTo);
            }
            
            //Cancel the Reward if not enough time has passed
            if (!isTimedOut(player, button))
                return false;
            
            //Cancel the Reward if the Player does not have permission
            if (ButtonWarp.hasPermission(player, "getreward"))
                Econ.reward(player, source, amount);
        }
        else {
            //Cancel teleporting if not enough time has passed
            if (!isTimedOut(player, button))
                return false;
            
            if (amount < 0)
                //Do not charge the Player if they have the freewarp permission
                if (!ButtonWarp.hasPermission(player, "freewarp"))
                    //Cancel teleporting if the Player cannot afford the Warp
                    if (!Econ.charge(player, source, Math.abs(amount)))
                        return false;

            //Teleport the Player last if there is a Location
            if (world != null) {
                World targetWorld = ButtonWarp.server.getWorld(world);
                if (targetWorld == null) {
                    player.sendMessage("The World you are trying to Warp to is currently unavailable");
                    return true;
                }

                Location sendTo = new Location(targetWorld, x, y, z);
                sendTo.setPitch(pitch);
                sendTo.setYaw(yaw);

                teleport(player, sendTo);
            }
        }
        
        //Send the message to the Player if there is one
        if (!msg.isEmpty())
            player.sendMessage(msg);
        
        //Print the Warp usage to the console if logging Warps is on
        if (broadcast)
            ButtonWarp.server.broadcastMessage("[ButtonWarp] "+player.getName()+" used Warp "+name);
        else if (log)
            System.out.println("[ButtonWarp] "+player.getName()+" used Warp "+name);
        
        return true;
    }
    
    /**
     * Returns true if the Player is in one Groups in the access list
     * Access is public if the access list is empty
     * 
     * @param player The Player who is being checked for access rights
     * @return True if the Player has access rights
     */
    private boolean hasAccess(Player player) {
        //Return true if the list is empty
        if (access.isEmpty())
            return true;
        
        //Return true if the Player is in any of the Groups in the list
        for (String group: access)
            if (ButtonWarp.permission.playerInGroup(player, group))
                return true;
        
        //Return false because the Player does not have access rights
        player.sendMessage("You do not have access rights to that.");
        return false;
    }
    
    /**
     * Returns true if the Player is smuggling items
     * 
     * @param player The Player who is being checked for smuggling
     * @return true if the Player is smuggling
     */
    private boolean isSmuggling(Player player, Button button) {
        //Return false if smuggling is allowed
        if (button.takeItems)
            return false;
        
        //Check item inventory for any items
        for (ItemStack item: player.getInventory().getContents())
            if (item != null) {
                player.sendMessage("You cannot take items with you.");
                return true;
            }
        
        //Check armour contents for any items
        for (ItemStack item: player.getInventory().getArmorContents())
            if (item.getTypeId() != 0) {
                player.sendMessage("You cannot take armor with you.");
                return true;
            }
        
        //Return false because the Player is not smuggling
        return false;
    }

    /**
     * Return true if enough time has passed since the Player last used the Button
     * 
     * @param player The Player who is activating the Warp
     * @param block The Block which was pressed
     */
    private boolean isTimedOut(Player player, Button button) {
        //Get the user to be looked up for last time of use
        String user = player.getName();
        if (global)
            user = "global";
        
        //Find out how much time remains
        int[] time = button.getTime(user);
        String timeRemaining = getTimeRemaining(time);
        
        //Check if the time remaining is never
        if (timeRemaining == null) {
            //Return true if the User has not maxed out their uses
            if (time[0] < button.max) {
                time[0]++;
                button.users.put(user, time);
                return true;
            }
            
            //Display message and return false
            if (amount > 1)
                player.sendMessage("You cannot receive another reward");
            else
                player.sendMessage("You cannot use that again");
            return false;
        }
        
        //Check if the time remaining is greater than 0
        if (!timeRemaining.equals("0")) {
            //Return true if the User has not maxed out their uses
            if (time[0] < button.max) {
                time[0] = time[0] + 1;
                button.users.put(user, time);
                return true;
            }
            
            //Display message and return false
            if (amount > 1)
                player.sendMessage("You cannot receive another reward for "+timeRemaining);
            else
                player.sendMessage("You cannot use that again for "+timeRemaining);
            return false;
        }
        
        //Set the new time for the User and return true
        button.setTime(user);
        return true;
    }

    /**
     * Returns the remaining time until the Button resets
     * Returns null if the PhatLootsChest never resets
     * 
     * @param time The given time
     * @return the remaining time until the Button resets
     */
    private String getTimeRemaining(int[] time) {
        //Return 0 if a time was not given
        if (time == null)
            return "0";
        
        //Return null if the reset time is set to never
        if (days < 0 || hours < 0 || minutes < 0 || seconds < 0)
            return null;
        
        //Calculate the time that the Warp will reset
        int resetYear = time[1];
        int resetDay = time[2] + days;
        int resetHour = time[3] + hours;
        int resetMinute = time[4] + minutes;
        int resetSecond = time[5] + seconds;
        
        //Update time values into the correct format
        while (resetSecond >= 60) {
            resetMinute++;
            resetSecond = resetSecond - 60;
        }
        while (resetMinute >= 60) {
            resetHour++;
            resetMinute = resetMinute - 60;
        }
        while (resetHour >= 24) {
            resetDay++;
            resetHour = resetHour - 24;
        }
        while (resetDay >= 366) {
            resetDay++;
            resetHour = resetHour - 365;
        }
        
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int day = calendar.get(Calendar.DAY_OF_YEAR);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        
        String timeMsg = "";
        
        //Return null if the current time is later than the reset time
        if (year > resetYear)
            return "0";
        
        if (year < resetYear) {
            timeMsg = timeMsg.concat((resetDay - day - 1)+" years, ");
            resetDay = resetDay + 365;
        }
        
        if (day > resetDay)
            return "0";
        
        if (day < resetDay) {
            timeMsg = timeMsg.concat((resetDay - day - 1)+" days, ");
            resetHour = resetHour + 24;
        }
        
        if (hour > resetHour)
            return "0";
        
        if (hour < resetHour) {
            timeMsg = timeMsg.concat((resetHour - hour - 1)+" hours, ");
            resetMinute = resetMinute + 60;
        }
        
        if (minute > resetMinute)
            return "0";
        
        if (minute < resetMinute) {
            timeMsg = timeMsg.concat((resetMinute - minute - 1)+" minutes, ");
            resetSecond = resetSecond + 60;
        }
        
        if (second >= resetSecond)
            return "0";
        
        return timeMsg.concat((resetSecond - second)+" seconds");
    }
    
    /**
     * Teleports the given Player after a delay
     * 
     * @param player The Player to be teleported
     * @param sendTo The destination of the Player
     */
    private static void teleport(final Player player, final Location sendTo) {
        //Cancel if the Location is null
        if (sendTo == null)
            return;
        
        //Delay Teleporting
    	ButtonWarp.server.getScheduler().scheduleSyncDelayedTask(ButtonWarp.plugin, new Runnable() {
    	    public void run() {
                Chunk chunk = sendTo.getChunk();
                if (!chunk.isLoaded())
                    chunk.load();

                //Teleport the Player
                player.teleport(sendTo);
    	    }
    	}, 0L);
    }

    /**
     * Resets the user times for all Buttons of this Warp
     * if a Block is given then only reset that Button
     * 
     * @param block The given Block
     */
    public void reset(Block block) {
        if (block == null)
            //Reset all Buttons
            for (Button button: buttons)
                button.users.clear();
        else
            //Find the Button of the given Block and reset it
            for (Button button: buttons)
                if (button.isBlock(block)) {
                    button.users.clear();
                    return;
                }
    }
    
    /**
     * Returns the Button that is associated with the given Block
     * 
     * @param block The given Block
     * @return the Button that is associated with the given Block
     */
    public Button findButton(Block block) {
        //Iterate through buttons to find the Button of the given Block
        for (Button button: buttons)
            if (button.isBlock(block))
                return button;
        
        //Return null because the Button does not exist
        return null;
    }
    
    /**
     * Loads data from the save file
     * 
     * @param string The data of the Buttons
     */
    void setButtons(String data) {
        //Cancel if no data is given
        if (data.isEmpty())
            return;
        
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
                for (String user: string.substring(index + 1, string.length() - 1).split(", "))
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
                            for (int j = 1; j < 5; j++)
                                time[j+1] = Integer.parseInt(timeData[j - 1]);
                        }
                        else
                            for (int j = 1; j < 6; j++)
                                time[j] = Integer.parseInt(timeData[j - 1]);

                        button.users.put(userName, time);
                    }
                
                buttons.add(button);
            }
            catch (Exception invalidButton) {
                System.out.println("[ButtonWarp] "+'"'+string+'"'+" is not a valid Button for Warp "+name);
                invalidButton.printStackTrace();
            }
        }
    }
    
    /**
     * Loads data from the outdated save file
     * 
     * @param string The data of the Buttons
     */
    void setButtonsOld(String string) {
        for (String temp: string.split("~")) {
            String[] users = temp.split(",");
            Button button = new Button(users[0], Integer.parseInt(users[1]), Integer.parseInt(users[2]), Integer.parseInt(users[3]));
            
            for (int j = 4; j < users.length; j += 2) {
                String[] timeString = users[j+1].split("'");
                int[] time = new int[5];
                
                time[0] = 1;
                for (int i = 1; i < 5; i++)
                    time[i] = Integer.parseInt(timeString[i - 1]);
                
                button.users.put(users[j], time);
            }
            
            buttons.add(button);
        }
    }
    
    /**
     * Writes Warp data to file
     * 
     */
    public void save() {
        ButtonWarp.saveWarp(this);
    }
}
