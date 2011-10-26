package com.codisimus.plugins.buttonwarp.listeners;

import com.codisimus.plugins.buttonwarp.Button;
import com.codisimus.plugins.buttonwarp.ButtonWarp;
import com.codisimus.plugins.buttonwarp.Register;
import com.codisimus.plugins.buttonwarp.SaveSystem;
import com.codisimus.plugins.buttonwarp.Warp;
import java.util.Arrays;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executes Player Commands
 * 
 * @author Codisimus
 */
public class commandListener implements CommandExecutor {
    //public static final HashSet TRANSPARENT = Sets.newHashSet(27, 28, 37, 38, 39, 40, 50, 65, 66, 69, 70, 72, 75, 76, 78);
    
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
        if (!(sender instanceof Player))
            return true;
        
        Player player = (Player)sender;

        //Display help page if the Player did not add any arguments
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        //Set the ID of the command
        int commandID = 0;
        if (args[0].equals("make"))
            commandID = 1;
        else if (args[0].equals("move"))
            commandID = 2;
        else if (args[0].equals("link"))
            commandID = 3;
        else if (args[0].equals("unlink"))
            commandID = 4;
        else if (args[0].equals("delete"))
            commandID = 5;
        else if (args[0].equals("amount"))
            commandID = 6;
        else if (args[0].equals("access"))
            commandID = 7;
        else if (args[0].equals("source"))
            commandID = 8;
        else if (args[0].equals("msg"))
            commandID = 9;
        else if (args[0].equals("time"))
            commandID = 10;
        else if (args[0].equals("type"))
            commandID = 11;
        else if (args[0].equals("list"))
            commandID = 12;
        else if (args[0].equals("info"))
            commandID = 13;
        else if (args[0].equals("rl"))
            commandID = 14;
        
        //Execute the command
        switch (commandID) {
            case 1: //command == make
                switch (args.length) {
                    case 2: make(player, args[1], false); return true;
                        
                    case 3:
                        if (args[2].equals("nowarp")) {
                            make(player, args[1], true);
                            return true;
                        }
                        break;
                        
                    default: break;
                }
                
                sendHelp(player);
                return true;
                
            case 2: //command == move
                switch (args.length) {
                    case 2: move(player, args[1], false); return true;
                        
                    case 3:
                        if (args[2].equals("nowarp")) {
                            move(player, args[1], true);
                            return true;
                        }
                        break;
                        
                    default: break;
                }
                
                sendHelp(player);
                return true;
                
            case 3: //command == link
                if (args.length == 2)
                    link(player, args[1]);
                else
                    sendHelp(player);
                
                return true;
                
            case 4: //command == unlink
                if (args.length == 1)
                    unlink(player);
                else
                    sendHelp(player);
                
                return true;
                
            case 5: //command == delete
                if (args.length == 2)
                    delete(player, args[1]);
                else
                    sendHelp(player);
                
                return true;
                
            case 6: //command == amount
                switch (args.length) {
                    case 2:
                        try {
                            amount(player, null, Double.parseDouble(args[1]));
                            return true;
                        }
                        catch (Exception notDouble) {
                            break;
                        }
                        
                    case 3:
                        try {
                            amount(player, args[1], Double.parseDouble(args[2]));
                            return true;
                        }
                        catch (Exception notDouble) {
                            break;
                        }
                        
                    default: break;
                }
                
                sendHelp(player);
                return true;
                
            case 7: //command == access
                if (args.length == 2)
                    access(player, null, args[1]);
                else if (args.length == 3)
                    access(player, args[1], args[2]);
                else
                    sendHelp(player);
                
                return true;
                
            case 8: //command == source
                switch (args.length) {
                    case 2:
                        source(player, null, false, args[1]);
                        return true;
                        
                    case 3:
                        if (args[1].equals("bank"))
                            source(player, null, true, args[2]);
                        else
                            source(player, args[1], false, args[2]);
                        
                        return true;
                        
                    case 4:
                        if (args[2].equals("bank"))
                            source(player, args[1], true, args[3]);
                        else
                            break;
                        
                        return true;
                        
                    default: break;
                }
                
                sendHelp(player);
                return true;
                
            case 9: //command == msg
                if (args.length < 3) {
                    sendHelp(player);
                    return true;
                }
                
                String msg = "";
                for (int i=2; i < args.length; i++)
                    msg.concat(args[i].concat(" "));
                
                msg(player, args[1], msg);
                return true;
                
            case 10: //command == time
                switch (args.length) {
                    case 5:
                        try {
                            time(player, null, Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                                    Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                            return true;
                        }
                        catch (Exception notInt) {
                            sendHelp(player);
                            break;
                        }
                        
                    case 6:
                        try {
                            time(player, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
                                    Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                            return true;
                        }
                        catch (Exception notInt) {
                            sendHelp(player);
                            break;
                        }
                        
                    default: break;
                }
                
                sendHelp(player);
                return true;
                
            case 11: //command == type
                boolean global;
                switch (args.length) {
                    case 2:
                        if (args[1].equals("global"))
                            global = true;
                        else if (args[1].equals("player"))
                            global = false;
                        else
                            break;
                        
                        type(player, null, global);
                        return true;
                        
                    case 3:
                        if (args[2].equals("global"))
                            global = true;
                        else if (args[2].equals("player"))
                            global = false;
                        else
                            break;
                        
                        type(player, args[1], global);
                        return true;
                        
                    default: break;
                }
                
                sendHelp(player);
                return true;
                
            case 12: //command == list
                if (args.length == 1)
                    list(player);
                else
                    sendHelp(player);
                
                return true;
                
            case 13: //command == info
                if (args.length == 2)
                    info(player, args[1]);
                else
                    sendHelp(player);
                
                return true;
                
            case 14: //command == rl
                if (args.length == 1)
                    rl(player);
                else
                    sendHelp(player);
                
                return true;
                
            default: sendHelp(player); return true;
        }
    }
    
    public static void make(Player player, String name, boolean noWarp) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        //Cancel if the Warp already exists
        if (SaveSystem.findWarp(name) != null) {
            player.sendMessage("A Warp named "+name+" already exists.");
            return;
        }
        
        if (noWarp) {
            SaveSystem.warps.add(new Warp(name, null));
            player.sendMessage("Warp "+name+" Made!");
        }
        else {
            SaveSystem.warps.add(new Warp(name, player));
            player.sendMessage("Warp "+name+" Made at current location!");
        }
        
        SaveSystem.save();
    }
    
    public static void move(Player player, String name, boolean noWarp) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        //Cancel if the Warp does not exist
        Warp warp = SaveSystem.findWarp(name);
        if (warp != null ) {
            player.sendMessage("Warp "+name+" does not exsist.");
            return;
        }

        if (noWarp) {
            warp.world = "";
            player.sendMessage("Warp "+name+" moved to nowhere");
        }
        else {
            Location location = player.getLocation();
            warp.x = location.getX();
            warp.y = location.getY();
            warp.z = location.getZ();
            warp.pitch = location.getPitch();
            warp.yaw = location.getYaw();
            player.sendMessage("Warp "+name+" moved to current location");
        }
        
        SaveSystem.save();
    }
    
    public static void link(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        //Cancel if the Player is not targeting a correct Block
        Block block = player.getTargetBlock(null, 10);
        if (!ButtonWarp.isSwitch(block.getTypeId())) {
            player.sendMessage("You must target a Button/Pressure Plate.");
            return;
        }
        
        //Cancel if the Block is already linked to a Warp
        Warp warp = SaveSystem.findWarp(block);
        if (warp != null) {
            player.sendMessage("Button is already linked to Warp "+warp.name+".");
            return;
        }
        
        //Cancel if the Warp with the given name does not exist
        warp = SaveSystem.findWarp(name);
        if (warp == null) {
            player.sendMessage("Warp "+name+" does not exsist.");
            return;
        }
        
        warp.buttons.add(new Button(block));
        player.sendMessage("Button has been linked to Warp "+name+"!");
        SaveSystem.save();
    }
    
    public static void unlink(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        //Cancel if the Player is not targeting a correct Block
        Block block = player.getTargetBlock(null, 10);
        if (!ButtonWarp.isSwitch(block.getTypeId())) {
            player.sendMessage("You must target a Button/Pressure Plate.");
            return;
        }
        
        //Cancel if the Block is not linked to a Warp
        Warp warp = SaveSystem.findWarp(block);
        if (warp == null) {
            player.sendMessage("Target Block is not linked to a Warp");
            return;
        }
        
        warp.buttons.remove(warp.findButton(block));
        player.sendMessage("Button has been unlinked from Warp "+warp.name+"!");
        SaveSystem.save();
    }
    
    public static void delete(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        Warp warp = null;
        
        if (name == null) {
            //Find the Warp that will be modified using the target Block
            warp = SaveSystem.findWarp(player.getTargetBlock(null, 10));
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Target Block is not linked to a Warp");
                return;
            }
        }
        else {
            //Find the Warp that will be modified using the given name
            warp = SaveSystem.findWarp(name);
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Warp "+name+" does not exsist.");
                return;
            }
        }
        
        SaveSystem.warps.remove(warp);
        player.sendMessage("Warp "+warp.name+" was deleted!");
        SaveSystem.save();
    }
    
    public static void amount(Player player, String name, double amount) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "amount")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        Warp warp = null;
        
        if (name == null) {
            //Find the Warp that will be modified using the target Block
            warp = SaveSystem.findWarp(player.getTargetBlock(null, 10));
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Target Block is not linked to a Warp");
                return;
            }
        }
        else {
            //Find the Warp that will be modified using the given name
            warp = SaveSystem.findWarp(name);
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Warp "+name+" does not exsist.");
                return;
            }
        }

        warp.amount = amount;
        player.sendMessage("Amount for Warp "+warp.name+" has been set to "+amount+"!");
    }
    
    public static void access(Player player, String name, String access) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        Warp warp = null;
        
        if (name == null) {
            //Find the Warp that will be modified using the target Block
            warp = SaveSystem.findWarp(player.getTargetBlock(null, 10));
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Target Block is not linked to a Warp");
                return;
            }
        }
        else {
            //Find the Warp that will be modified using the given name
            warp = SaveSystem.findWarp(name);
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Warp "+name+" does not exsist.");
                return;
            }
        }
        
        warp.access.clear();
        if (!access.equals("public"))
            warp.access.addAll(Arrays.asList(access.split(",")));
        player.sendMessage("Access for Warp "+warp.name+" has been set to "+access+"!");
    }
    
    public static void source(Player player, String name, boolean bank, String source) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "amount")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        Warp warp = null;
        
        if (name == null) {
            //Find the Warp that will be modified using the target Block
            warp = SaveSystem.findWarp(player.getTargetBlock(null, 10));
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Target Block is not linked to a Warp");
                return;
            }
        }
        else {
            //Find the Warp that will be modified using the given name
            warp = SaveSystem.findWarp(name);
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Warp "+name+" does not exsist.");
                return;
            }
        }

        if (bank)
            source = "bank:".concat(source);
        
        warp.source = source;
        player.sendMessage("Money source for Warp "+warp.name+" has been set to "+source+"!");
    }
    
    public static void msg(Player player, String name, String msg) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }
        
        //Find the Warp that will be modified using the given name
        Warp warp = SaveSystem.findWarp(name);

        //Cancel if the Warp does not exist
        if (warp == null ) {
            player.sendMessage("Warp "+name+" does not exsist.");
            return;
        }

        msg = msg.replaceAll("&", "§").replaceAll("<ae>", "æ").replaceAll("<AE>", "Æ")
                .replaceAll("<o/>", "ø").replaceAll("<O/>", "Ø").replaceAll("<a>", "å")
                .replaceAll("<A>", "Å");
        
        warp.msg = msg;
        player.sendMessage("Message for Warp "+warp.name+" has been set to '"+msg+"'");
    }
    
    public static void time(Player player, String name, int days, int hours, int minutes, int seconds) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        Warp warp = null;
        
        if (name == null) {
            //Find the Warp that will be modified using the target Block
            warp = SaveSystem.findWarp(player.getTargetBlock(null, 10));
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Target Block is not linked to a Warp");
                return;
            }
        }
        else {
            //Find the Warp that will be modified using the given name
            warp = SaveSystem.findWarp(name);
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Warp "+name+" does not exsist.");
                return;
            }
        }
        
        warp.days = days;
        warp.hours = hours;
        warp.minutes = minutes;
        warp.seconds = seconds;
        player.sendMessage("Reset time for Warp "+warp.name+" has been set to "+days+" days, "
                +hours+" hours, "+minutes+" minutes, and "+seconds+" seconds.");
    }
    
    public static void type(Player player, String name, boolean global) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        Warp warp = null;
        
        if (name == null) {
            //Find the Warp that will be modified using the target Block
            warp = SaveSystem.findWarp(player.getTargetBlock(null, 10));
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Target Block is not linked to a Warp");
                return;
            }
        }
        else {
            //Find the Warp that will be modified using the given name
            warp = SaveSystem.findWarp(name);
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Warp "+name+" does not exsist.");
                return;
            }
        }
        
        warp.global = global;
        String type = "Player";
        if (global)
            type = "global";
        player.sendMessage("Reset type for Warp "+name+" has been set to "+type+"!");
    }
    
    public static void list(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        String warpList = "Current Warps:  ";
        for (Warp warp: SaveSystem.warps)
            warpList = warpList.concat(warp.name+": Amount="+Register.format(warp.amount)+", ");
        player.sendMessage(warpList.substring(0, warpList.length() - 2));
        return;
    }
    
    public static void info(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "make")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        Warp warp = null;
        
        if (name == null) {
            //Find the Warp that will be modified using the target Block
            warp = SaveSystem.findWarp(player.getTargetBlock(null, 10));
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Target Block is not linked to a Warp");
                return;
            }
        }
        else {
            //Find the Warp that will be modified using the given name
            warp = SaveSystem.findWarp(name);
            
            //Cancel if the Warp does not exist
            if (warp == null ) {
                player.sendMessage("Warp "+name+" does not exsist.");
                return;
            }
        }
        
        String type = "Player";
        if (warp.global)
            type = "global";
        player.sendMessage("Name: "+name+", Amount: "+Register.format(warp.amount)+", Money Source: "+warp.source);
        player.sendMessage("Warp Location: "+warp.world+", "+warp.x+", "+warp.y+", "+warp.z+", Access: "+warp.access);
        player.sendMessage("Reset Time: "+warp.days+" days, "+warp.hours+" hours, "+warp.minutes+" minutes, and "
                +warp.seconds+" seconds. Reset Type: "+type);
    }
    
    public static void rl(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!ButtonWarp.hasPermission(player, "rl")) {
            player.sendMessage("You do not have permission to do that.");
            return;
        }

        SaveSystem.load();
        ButtonWarp.pm = ButtonWarp.server.getPluginManager();
        System.out.println("[ButtonWarp] reloaded");
        player.sendMessage("ButtonWarp reloaded");
        return;
    }
    
    /**
     * Displays the ButtonWarp Help Page to the given Player
     *
     * @param player The Player needing help
     */
    public static void sendHelp(Player player) {
        player.sendMessage("§e     ButtonWarp Help Page:");
        player.sendMessage("§2/bw make [Name]§b Makes Warp at current location");
        player.sendMessage("§2/bw make [Name] nowarp§b Makes a Warp that doesn't teleport");
        player.sendMessage("§2/bw move [Name] (nowarp)§b Moves Warp to current location");
        player.sendMessage("§2/bw link [Name]§b Links target button/pressure plate with Warp");
        player.sendMessage("§2/bw unlink §b Unlinks target Block with Warp");
        player.sendMessage("§2/bw delete (Name)§b Deletes Warp and unlinks blocks");
        player.sendMessage("§2/bw amount (Name) [Amount]§b Sets amount for Warp");
        player.sendMessage("§2/bw access (Name) [Group]§b Only Players in Group can Warp");
        player.sendMessage("§2/bw access (Name) [Group1,Group2,...]§b Allow multiple Groups");
        player.sendMessage("§2/bw access (Name) public §b Anyone can Warp");
        player.sendMessage("§2/bw source (Name) server§b Generates/Destroys money");
        player.sendMessage("§2/bw source (Name) [Player]§b Gives/Takes money from Player");
        player.sendMessage("§2/bw source (Name) bank [Bank]§b Gives/Takes money from Bank");
        player.sendMessage("§2/bw msg (Name) [Msg]§b Sets message recieved after using Warp");
        player.sendMessage("§2/bw time (Name) [Days] [Hrs] [Mins] [Secs]§b Sets cooldown time");
        player.sendMessage("§2/bw type (Name) ['global' or 'player']§b Sets cooldown type");
        player.sendMessage("§2/bw list§b Lists all Warps");
        player.sendMessage("§2/bw info (Name)§b Gives information about the Warp");
        player.sendMessage("§2/bw rl§b Reloads ButtonWarp");
    }
}
