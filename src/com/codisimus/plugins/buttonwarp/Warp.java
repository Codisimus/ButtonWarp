
package com.codisimus.plugins.buttonwarp;

import com.codisimus.plugins.buttonwarp.register.payment.Method;
import com.codisimus.plugins.buttonwarp.register.payment.Method.MethodAccount;
import com.codisimus.plugins.buttonwarp.register.payment.Method.MethodBankAccount;
import java.util.Calendar;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * A Warp is a Location that a Player is sent to when pressing a linked Button
 * Money transactions may also occur
 * 
 * @author Codisimus
 */
public class Warp {
    protected static String economy;
    protected static Method econ;
    protected static boolean takeItems;
    protected String name;
    protected String access = "public";
    protected int amount = 0;
    protected String source = "server";
    protected Location sendTo = null;
    protected String resetTime = "none";
    protected String resetType = "user";
    protected String restrictedUsers = "";
    protected String msg = "";
    private boolean success = true;

    /**
     * Creates a Warp from the save file.
     * 
     * @param name The name of the Warp
     * @param buttons All Blocks that are linked with the Warp
     * @param amount The amount (given or taken) when activating the Warp
     * @param source The name of the player/bank who gives/takes money for the Warp
     */
    public Warp (String name, String msg, int amount, String source, String time, String type, String users) {
        this.name = name;
        this.msg = msg;
        this.amount = amount;
        this.source = source;
        resetTime = time;
        resetType = type;
        restrictedUsers = users;
    }

    /**
     * Constructs a new Warp
     * 
     * @param name The name of the Warp which cannot already exist
     * @param creator The Player who is creating the Warp or null if no location is wanted
     * @return The newly created Warp
     */
    public Warp (String name, Player creator) {
        this.name = name;
        if (creator != null)
            sendTo = creator.getLocation();
    }

    /**
     * Activates a Warp, teleporting the Player and making money transactions
     * 
     * @param player The Player who is activating the Warp
     * @param block The Block which was pressed
     * @return True if the activation was successful
     */
    public Boolean activate(final Player player, final Block block) {
        //Start a new Thread
        Thread tele = new Thread() {
            @Override
            public void run() {
                if (!hasAccess(player)) {
                    player.sendMessage("You do not have permission to use that.");
                    success = false;
                    return;
                }
                if (!takeItems)
                    //check if new world
                    if (!player.getWorld().equals(sendTo.getWorld())) {
                        //check for items in inventory
                        ItemStack[] items = player.getInventory().getContents();
                        for (ItemStack item : items)
                            if (item != null) {
                                player.sendMessage("You can't take items to another World.");
                                success = false;
                                return;
                            }
                        //check for armour
                        items = player.getInventory().getArmorContents();
                        for (ItemStack item : items)
                            if (item.getTypeId() != 0) {
                                player.sendMessage("You can't take armour to another World.");
                                success = false;
                                return;
                            }
                    }
                //check for money transactions
                if (amount > 0) {
                    player.teleport(sendTo);
                    //cancel reward if not enough time has passed
                    if (!isTimedOut(player, block)) {
                        success = false;
                        return;
                    }
                    //cancel reward if Player doesn't have permission
                    if (ButtonWarp.hasPermission(player, "getreward"))
                        receive(player);
                }
                else if (amount < 0) {
                    //cancel teleport if not enough time has passed
                    if (!isTimedOut(player, block)) {
                        success = false;
                        return;
                    }
                    //cancel payment if player has freewarp permission
                    if (!ButtonWarp.hasPermission(player, "freewarp"))
                        //cancel teleport if Player doesn't enough money
                        if (!pay(player)) {
                            success = false;
                            return;
                        }
                    player.teleport(sendTo);
                }
                else {
                    //cancel teleport if not enough time has passed
                    if (!isTimedOut(player, block)) {
                        success = false;
                        return;
                    }
                    player.teleport(sendTo);
                }
                //send msg to player if it is not blank
                if (!msg.isEmpty())
                    player.sendMessage(msg);
            }
        };
        tele.start();
        return success;
    }
    
    /**
     * Returns whether the player has access to the Warp
     * 
     * @param player The Player who is activating the Warp
     * @return true if the play has access rights
     */
    private boolean hasAccess(Player player) {
        if (!access.equalsIgnoreCase("public")) {
            String[] split = access.split(",");
            for (String group: split)
                if (ButtonWarp.permissions.getUser(player).inGroup(group))
                    return true;
            return false;
        }
        return true;
    }

    /**
     * Determines whether enough time has passed since last time the Player activated the Warp
     * 
     * @param player The Player who is activating the Warp
     * @param button The Block which was pressed
     */
    private boolean isTimedOut(Player player, Block button) {
        String blockXYZ = button.getWorld().getName()+","+button.getX();
        blockXYZ = blockXYZ.concat(","+button.getY()+","+button.getZ()+",");
        String pressTime = "NoTiNsTrInG";
        String[] split = restrictedUsers.split("~");
        //Loop till correct Block value is found
        for (int i=0; i<split.length; i++) {
            String[] users = split[i].split(",");
            String buttonXYZ = users[0]+","+users[1]+","+users[2]+","+users[3]+",";
            if (buttonXYZ.equals(blockXYZ)) {
                //Loop till correct player name is found
                for (int j=1; j<users.length; j++)
                    if (users[j].contains(player.getName()) || users[j].contains("all")) {
                        //return false if the resetTime is set to never
                        if (resetTime.equals("never")) {
                            if (amount > 0)
                                player.sendMessage("You already received your reward");
                            else
                                player.sendMessage("You cannot use that again");
                            return false;
                        }
                        pressTime = users[j+1];
                        String[] time = pressTime.split("'");
                        //return false if too little time has passed
                        if(!enoughTimePassed(time)) {
                            if (amount > 0)
                                player.sendMessage("You must wait to receive another reward");
                            else
                                player.sendMessage("You must wait to use that again");
                            return false;
                        }
                    }
            }
        }
        if (restrictedUsers.contains(pressTime))
            restrictedUsers = restrictedUsers.replace(pressTime, getCurrentTime());
        else {
            String user = "all";
            if(!resetType.equalsIgnoreCase("global"))
                user = player.getName();
            restrictedUsers = restrictedUsers.replace(blockXYZ, blockXYZ+user+","+getCurrentTime()+",");
        }
        return true;
    }

    /**
     * Determines if given time + resetTime is later than current time
     * 
     * @param time The given time
     * @return false if given time + resetTime is later than current time
     */
    private boolean enoughTimePassed(String[] time) {
        //return true if there is no reset time
        if (resetTime.equals("none"))
            return true;
        int pressDay = Integer.parseInt(time[0]);
        int pressHour = Integer.parseInt(time[1]);
        int pressMinute = Integer.parseInt(time[2]);
        int pressSecond = Integer.parseInt(time[3]);
        time = getCurrentTime().split("'");
        int nowDay = Integer.parseInt(time[0]);
        int nowHour = Integer.parseInt(time[1]);
        int nowMinute = Integer.parseInt(time[2]);
        int nowSecond = Integer.parseInt(time[3]);
        time = resetTime.split("'");
        int resetDay = Integer.parseInt(time[0]);
        int resetHour = Integer.parseInt(time[1]);
        int resetMinute = Integer.parseInt(time[2]);
        int resetSecond = Integer.parseInt(time[3]);
        pressSecond = resetSecond + pressSecond;
        if (pressSecond >= 60) {
            pressMinute++;
            pressSecond = pressSecond - 60;
        }
        pressMinute = resetMinute + pressMinute;
        if (pressMinute >= 60) {
            pressHour++;
            pressMinute = pressMinute - 60;
        }
        pressHour = resetHour + pressHour;
        if (pressHour >= 24) {
            pressDay++;
            pressHour = pressHour - 24;
        }
        pressDay = resetDay + pressDay;
        if (pressSecond >= 60) {
            pressMinute++;
            pressSecond = pressSecond - 60;
        }
        if (pressDay < nowDay)
            return true;
        else if(pressDay == nowDay)
            if (pressHour < nowHour)
                return true;
            else if(pressHour == nowHour)
                if (pressMinute < nowMinute)
                    return true;
                else if(pressMinute == nowMinute)
                    if (pressSecond <= nowSecond)
                        return true;
        return false;
    }

    /**
     * Returns the current time in the format DAY'HOUR'MINUTE'SECOND
     * 
     * @return The current time in the format DAY'HOUR'MINUTE'SECOND
     */
    private static String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        String time = (calendar.get(Calendar.DAY_OF_YEAR)+"'");
        time = (time+calendar.get(Calendar.HOUR_OF_DAY)+"'");
        time = (time+calendar.get(Calendar.MINUTE)+"'");
        time = (time+calendar.get(Calendar.SECOND));
        return time;
    }

    /**
     * Completes a transaction of a Player paying money
     * Payed money will be delivered to source
     * 
     * @param player The player who is paying
     * @return true if the player had enough money
     */
    private boolean pay(Player player) {
        MethodAccount account = econ.getAccount(player.getName());
        if (!account.hasEnough(Math.abs(amount))) {
            player.sendMessage("Insufficient funds!");
            return false;
        }
        account.subtract(Math.abs(amount));
        if (!source.equalsIgnoreCase("server"))
            if (source.startsWith("bank:"))
                econ.getBankAccount(source.substring(5), null).subtract(amount);
            else
                econ.getAccount(source).subtract(amount);
        return true;
    }

    /**
     * Completes a transaction of a Player receiving money
     * Received money will come from source
     * 
     * @param player The player who is receiving money
     * @return true if the source had enough money
     */
    private void receive(Player player) {
        if (!source.equalsIgnoreCase("server")) {
            MethodBankAccount bankAccount;
            MethodAccount account;
            if (source.startsWith("bank:")) {
                bankAccount = econ.getBankAccount(source.substring(5), null);
                if (!bankAccount.hasEnough(amount)) {
                    player.sendMessage("Bank has insufficient funds");
                    return;
                }
                bankAccount.subtract(amount);
            }
            else {
                account = econ.getAccount(source);
                if (!account.hasEnough(amount)) {
                    player.sendMessage("Player has insufficient funds");
                    return;
                }
                account.subtract(amount);
            }
        }
        econ.getAccount(player.getName()).add(amount);
    }

    /**
     * Resets the restricted list for all buttons of this Warp
     * 
     */
    protected void reset() {
        String[] split = restrictedUsers.split("~");
        restrictedUsers = "";
        for (int i=0; i<split.length; i++) {
            String[] users = split[i].split(",");
            String buttonXYZ = users[0]+","+users[1]+","+users[2]+","+users[3]+",~";
            restrictedUsers = restrictedUsers.concat(buttonXYZ);
        }
    }

    /**
     * Adds a button to the Warp
     * 
     * @param button The Block being added
     */
    protected void addButton(Block button) {
        String buttonXYZ = (button.getWorld().getName()+","+button.getX()+","+button.getY()+","+button.getZ()+",~");
        restrictedUsers = restrictedUsers.concat(buttonXYZ);
    }

    /**
     * Removes a button from the Warp
     * 
     * @param button The Block that will be removed
     */
    protected void removeButton(Block button) {
        String chestXYZ = (button.getWorld().getName()+","+button.getX()+","+button.getY()+","+button.getZ()+",");
        String[] split = restrictedUsers.split("~");
        for (int i = 0 ; i < split.length; ++i)
            if (split[i].startsWith(chestXYZ))
                restrictedUsers = restrictedUsers.replaceAll(split[i]+"~", "");
    }
}
