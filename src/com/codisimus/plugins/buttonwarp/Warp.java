package com.codisimus.plugins.buttonwarp;

import java.util.Calendar;
import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
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
        
        Location sendTo = null;
        if (world != null) {
            sendTo = new Location(ButtonWarp.server.getWorld(world), x, y, z);
            sendTo.setPitch(pitch);
            sendTo.setYaw(yaw);
        }
        
        //The order of money transactions and teleporting is determined by the value of the amount
        if (amount > 0) {
            //Teleport the Player first
            if (sendTo != null)
                player.teleport(sendTo);
            
            //Cancel the Reward if not enough time has passed
            if (!isTimedOut(player, button))
                return false;
            
            //Cancel the Reward if the Player does not have permission
            if (ButtonWarp.hasPermission(player, "getreward"))
                Register.reward(player, source, amount);
        }
        else {
            //Cancel teleporting if not enough time has passed
            if (!isTimedOut(player, button))
                return false;
            
            if (amount < 0)
                //Do not charge the Player if they have the freewarp permission
                if (!ButtonWarp.hasPermission(player, "freewarp"))
                    //Cancel teleporting if the Player cannot afford the Warp
                    if (!Register.charge(player.getName(), source, Math.abs(amount)))
                        return false;
            
            //Teleport the Player last
            if (sendTo != null)
                player.teleport(sendTo);
        }
        
        //Send the message to the Player if there is one
        if (!msg.isEmpty())
            player.sendMessage(msg);
        
        return true;
    }
    
    /**
     * Returns true if the Player is in one Groups in the access list
     * Access is public if the access list is empty
     * 
     * @param player The Player who is being checked for access rights
     * @return True if the Player has access rights
     */
    public boolean hasAccess(Player player) {
        //Return true if the list is empty
        if (access.isEmpty())
            return true;
        
        //Return true if the Player is in any of the Groups in the list
        for (String group: access)
            if (ButtonWarp.permissions.getUser(player).inGroup(group))
                return true;
        
        //Return false because the Player does not have access rights
        player.sendMessage("You do not have permission to use that.");
        return false;
    }
    
    /**
     * Returns true if the Player is smuggling items
     * 
     * @param player The Player who is being checked for smuggling
     * @return true if the Player is smuggling
     */
    public boolean isSmuggling(Player player, Button button) {
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
                player.sendMessage("You cannot take armour with you.");
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
    public boolean isTimedOut(Player player, Button button) {
        //Get the user to be looked up for last time of use
        String user = player.getName();
        if (global)
            user = "global";
        
        //Find out how much time remains
        int[] time = button.getTime(user);
        String timeRemaining = getTimeRemaining(time);
        
        //Check if the time remaining is never
        if (timeRemaining.equals("-1")) {
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
     * Returns -1 if the Button never resets
     * 
     * @param time The given time
     * @return the remaining time until the Button resets
     */
    public String getTimeRemaining(int[] time) {
        //Return 0 if a time was not given
        if (time == null)
            return "0";
        
        //Return -1 if the reset time is set to never
        if (days < 0 || hours < 0 || minutes < 0 || seconds < 0)
            return "-1";
        
        //Calculate the time that the Warp will reset
        int resetDay = time[1] + days;
        int resetHour = time[2] + hours;
        int resetMinute = time[3] + minutes;
        int resetSecond = time[4] + seconds;
        
        //Update time values into the correct format
        while (resetSecond >= 60) {
            resetMinute++;
            resetSecond = resetSecond - 60;
        }
        while (resetMinute >= 60) {
            resetHour++;
            resetMinute = resetMinute - 60;
        }
        while (resetHour >= 60) {
            resetDay++;
            resetHour = resetHour - 60;
        }
        
        Calendar calendar = Calendar.getInstance();
        
        //Return 0 if the current time is later than the reset time
        int day = calendar.get(Calendar.DAY_OF_YEAR);
        if (day > resetDay)
            return "0";
        
        if (day < resetDay)
            //Display remaining days
            return (resetDay - day)+" days";
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour > resetHour)
            return "0";
        
        if (hour < resetHour)
            //Display remaining hours
            return (resetHour - hour)+" hours";
        
        int minute = calendar.get(Calendar.MINUTE);
        if (minute > resetMinute)
            return "0";
        
        if (minute < resetMinute)
            //Display remaining minutes
            return (resetMinute - minute)+" minutes";
        
        int second = calendar.get(Calendar.SECOND);
        if (second >= resetSecond)
            return "0";
        else
            //Display remaining seconds
            return (resetSecond - second)+" seconds";
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
        //Iterate through chests to find the Button of the given Block
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
    public void setButtons(String data) {
        //Cancel if no data is given
        if (data.isEmpty())
            return;
        
        int index;
        
        //Load data for each Button
        for (String string: data.split("; ")) {
            try {
                index = string.indexOf('{');

                //Load the Block Location data of the Chest
                String[] blockData = string.substring(0, index).split("'");
                
                //Construct a a new Button with the Location data
                Button button = new Button(blockData[0], Integer.parseInt(blockData[1]),
                        Integer.parseInt(blockData[2]), Integer.parseInt(blockData[3]));
                
                button.takeItems = Boolean.parseBoolean(blockData[4]);
                button.max = Integer.parseInt(blockData[5]);

                //Load the HashMap of Users of the Chest
                for (String user: string.substring(index + 1, string.length() - 1).split(", "))
                    //Don't load if the data is corrupt or empty
                    if ((index = user.indexOf('@')) != -1) {
                        int[] time = new int[5];
                        String[] timeData = user.substring(index + 1).split("'");
                        
                        String userData = user.substring(0, index);
                        index = userData.indexOf("'");
                        String userName = userData.substring(0, index);
                        
                        time[0] = Integer.parseInt(userData.substring(index + 1));
                        for (int j = 1; j < 5; j++)
                            time[j] = Integer.parseInt(timeData[j - 1]);

                        button.users.put(userName, time);
                    }
                
                buttons.add(button);
            }
            catch (Exception invalidChest) {
                System.out.println("[ButtonWarp] Error occured while loading, "+'"'+string+'"'+" is not a valid Button");
                SaveSystem.save = false;
                System.out.println("[ButtonWarp] Saving turned off to prevent loss of data");
                invalidChest.printStackTrace();
            }
        }
    }
    
    /**
     * Loads data from the outdated save file
     * 
     * @param string The data of the Buttons
     */
    public void setButtonsOld(String string) {
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
}
