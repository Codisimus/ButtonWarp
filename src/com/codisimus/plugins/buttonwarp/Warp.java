package com.codisimus.plugins.buttonwarp;

import java.io.Serializable;
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
public class Warp implements Serializable {
    public String name; //A unique name for the Warp
    public LinkedList<String> access = new LinkedList<String>(); //List of Groups that have access (public if empty)
    public double amount = 0; //Amount of money rewarded (negative for charging money)
    public String source = "server"; //Player name || 'Bank:'+Bank Name || 'server'
    public String world; //Location data to teleport to
    public double x;
    public double y;
    public double z;
    public float pitch;
    public float yaw;
    public int days = -1; //Reset time (will never reset if any are negative) 
    public int hours = -1;
    public int minutes = -1;
    public int seconds = -1;
    public boolean global = false; //Reset Type
    public LinkedList<Button> buttons = new LinkedList<Button>();
    public String msg = ""; //Message sent to Player when using the Warp
    public boolean success = true;

    /**
     * Constructs a new Warp
     * 
     * @param name The unique name of the Warp
     * @param player The Player who is creating the Warp or null if no location is wanted
     * @return The newly created Warp
     */
    public Warp (String name, Player player) {
        this.name = name;
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
        
        //Cancel if the Player is attempting to smuggle items to a new World
        if (isSmuggling(player))
            return false;
        
        Location sendTo = new Location(ButtonWarp.server.getWorld(world), x, y, z);
        sendTo.setPitch(pitch);
        sendTo.setYaw(yaw);
        
        //The order of money transactions and teleporting is determined by the value of the amount
        if (amount > 0) {
            //Teleport the Player first
            player.teleport(sendTo);
            
            //Cancel the Reward if not enough time has passed
            if (!isTimedOut(player, block))
                return false;
            
            //Cancel the Reward if the Player does not have permission
            if (ButtonWarp.hasPermission(player, "getreward"))
                Register.reward(player, source, amount);
        }
        else if (amount < 0) {
            //Cancel teleporting if not enough time has passed
            if (!isTimedOut(player, block))
                return false;
            
            //Do not charge the Player if they have the freewarp permission
            if (!ButtonWarp.hasPermission(player, "freewarp"))
                //Cancel teleporting if the Player cannot afford the Warp
                if (!Register.charge(player.getName(), source, Math.abs(amount)))
                    return false;
            
            //Teleport the Player last
            player.teleport(sendTo);
        }
        else {
            //Cancel teleporting if not enough time has passed
            if (!isTimedOut(player, block))
                return false;
            
            //Teleport the Player last
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
     * Returns true if the Player is smuggling items to a new World
     * 
     * @param player The Player who is being checked for smuggling
     * @return true if the Player is smuggling
     */
    public boolean isSmuggling(Player player) {
        //Return false if smuggling is allowed
        if (ButtonWarp.takeItems)
            return false;
        
        //Return false if the Player is not traveling to a new World
        if (player.getWorld().getName().equals(world))
            return false;
        
        //Check item inventory for any items
        for (ItemStack item: player.getInventory().getContents())
            if (item != null) {
                player.sendMessage("You cannot take items to another World.");
                return true;
            }
        
        //Check armour contents for any items
        for (ItemStack item: player.getInventory().getArmorContents())
            if (item.getTypeId() != 0) {
                player.sendMessage("You cannot take armour to another World.");
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
    public boolean isTimedOut(Player player, Block block) {
        //Retrieve the Button associated with the given Block
        Button button = findButton(block);
        
        String user = player.getName();
        if (global)
            user = "global";
        
        //Return false if too little time has passed
        if(!enoughTimePassed(button.getTime(user))) {
            //Determine which message to send
            if (days < 0 || hours < 0 || minutes < 0 || seconds < 0) {
                if (amount > 0)
                    player.sendMessage("You already received your reward");
                else
                    player.sendMessage("You cannot use that again");

                return false;
            }
            
            if (amount > 0)
                player.sendMessage("You must wait to receive another reward");
            else
                player.sendMessage("You must wait to use that again");
            
            return false;
        }
        
        //Set the new time for the User and return true
        button.setTime(user);
        return true;
    }

    /**
     * Returns true if given time + the reset time < than current time
     * 
     * @param time The given time
     * @return True if given time + the reset time < than current time
     */
    public boolean enoughTimePassed(int[] time) {
        //Return true if a time was not given
        if (time == null)
            return true;
        
        //Return false if the reset time is set to never
        if (days < 0 || hours < 0 || minutes < 0 || seconds < 0)
            return false;
        
        //Calculate the time that the Warp will reset
        int resetDay = time[0] + days;
        int resetHour = time[1] + hours;
        int resetMinute = time[2] + minutes;
        int resetSecond = time[3] + seconds;
        
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
        
        //Return true if the current time is later than the reset time
        int day = ButtonWarp.calendar.get(Calendar.DAY_OF_YEAR);
        if (day != resetDay)
            return day > resetDay;
        
        int hour = ButtonWarp.calendar.get(Calendar.HOUR_OF_DAY);
        if (hour != resetHour)
            return hour > resetHour;
        
        int minute = ButtonWarp.calendar.get(Calendar.MINUTE);
        if (minute != resetMinute)
            return minute > resetMinute;
        
        return ButtonWarp.calendar.get(Calendar.SECOND) >= resetSecond;
    }

    /**
     * Resets the user times for all Buttons of this Warp
     * if a Block is given then only reset that Button
     * 
     * @param block The given Block
     */
    public void reset(Block block) {
        if (block == null)
            for (Button button: buttons)
                button.users.clear();
        else
            for (Button button: buttons)
                if (button.isBlock(block))
                    button.users.clear();
    }
    
    /**
     * Returns the Button that is associated with the given Block
     * 
     * @param block The given Block
     * @return the Button that is associated with the given Block
     */
    public Button findButton(Block block) {
        for (Button button: buttons)
            if (button.isBlock(block))
                return button;
        
        //Return null because the Button does not exist
        return null;
    }
    
    /**
     * Loads data from the outdated save file
     * 
     * @param string The data of the Buttons
     */
    public void setButtons(String string) {
        String[] split = string.split("~");
        
        for (String temp: split) {
            String[] users = temp.split(",");
            Button button = new Button(users[0], Integer.parseInt(users[1]), Integer.parseInt(users[2]), Integer.parseInt(users[3]));
            
            for (int j=4; j<users.length; j=j+2) {
                String[] timeString = users[j+1].split("'");
                int[] time = new int[4];
                
                for (int i=0; i<4; i++)
                    time[i] = Integer.parseInt(timeString[i]);
                
                button.users.put(users[j], time);
            }
            
            buttons.add(button);
        }
    }
}
