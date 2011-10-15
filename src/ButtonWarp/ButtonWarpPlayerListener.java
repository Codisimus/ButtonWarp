
package ButtonWarp;

import java.util.LinkedList;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

/**
 *
 * @author Codisimus
 */
public class ButtonWarpPlayerListener extends PlayerListener{

    @Override
    public void onPlayerCommandPreprocess (PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();
        String[] split = msg.split(" ");

        if (split[0].equals("/buttonwarp") || split[0].equals("/bw")) {
            event.setCancelled(true);
            try {
                //Check for Command
                if (split[1].startsWith("help"))
                    throw new Exception();
                else if (split[1].equals("amount") || split[1].equals("source")) {
                    //Check for Permission
                    if (!ButtonWarp.hasPermission(player, "amount")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    //Find the Warp that will be modified
                    Warp warp = SaveSystem.findWarp(split[2]);
                    if (warp == null) {
                        event.getPlayer().sendMessage("Warp "+split[2]+" does not exsist.");
                        return;
                    }
                    //Check for Command
                    if (split[1].equals("amount")) {
                        warp.amount = Integer.parseInt(split[3]);
                        player.sendMessage("Amount for Warp "+split[2]+" has been set to "+split[3]+"!");
                    }
                    else if (split[1].equals("source")) {
                        if (split[3].equals("bank"))
                            warp.source = "bank:"+split[4];
                        else
                            warp.source = split[3];
                        player.sendMessage("Money source for Warp "+split[2]+" has been set to "+warp.source+"!");
                    }
                }
                else if (split[1].equals("rl")) {
                    //Check for Permission
                    if (!ButtonWarp.hasPermission(player, "rl")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    SaveSystem.loadFromFile();
                    ButtonWarp.pm = ButtonWarp.server.getPluginManager();
                    System.out.println("ButtonWarp reloaded");
                    player.sendMessage("ButtonWarp reloaded");
                    return;
                }
                else {
                    //Check for Permission
                    if (!ButtonWarp.hasPermission(player, "make")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    //Check for Command
                    if (split[1].equals("link") || split[1].equals("unlink")) {
                        //Find the Warp related to the target block
                        Block block = player.getTargetBlock(null, 100);
                        if (!isSwitch(block.getTypeId())) {
                            player.sendMessage("You must target a button/pressure plate.");
                            return;
                        }
                        Warp warp = SaveSystem.findWarp(block);
                        //Check for Command
                        if (split[1].equals("link")) {
                            if (warp != null) {
                                player.sendMessage("Button is already linked to Warp "+warp.name+".");
                                return;
                            }
                            warp = SaveSystem.findWarp(split[2]);
                            if (warp == null) {
                                event.getPlayer().sendMessage("Warp "+split[2]+" does not exsist.");
                                return;
                            }
                            warp.addButton(block);
                            player.sendMessage("Button has been linked to Warp "+split[2]+"!");
                        }
                        else if (split[1].equals("unlink")) {
                            if (warp == null) {
                                event.getPlayer().sendMessage("Target button is not linked to a Warp");
                                return;
                            }
                            warp.removeButton(block);
                            player.sendMessage("Button sucessfully unlinked!");
                        }
                    }
                    else if (split[1].equals("list")) {
                        LinkedList<Warp> warps = SaveSystem.getWarps();
                        String warpList = "";
                        player.sendMessage("Current Warps:");
                        for (Warp warp : warps)
                            warpList = warpList.concat(warp.name+": Amount="+warp.amount+" A, ");
                        player.sendMessage(warpList);
                        return;
                    }
                    else if (split[1].equals("make")) {
                        if (SaveSystem.findWarp(split[2]) != null) {
                            player.sendMessage("A Warp named "+split[2]+" already exists.");
                            return;
                        }
                        Warp warp;
                        if (split.length > 3 && split[3].startsWith("nowarp"))
                            warp = new Warp(split[2], null);
                        else
                            warp = new Warp(split[2], player);
                        player.sendMessage("Warp "+split[2]+" Made!");
                        SaveSystem.addWarp(warp);
                    }
                    else {
                        //Find the Warp that will be modified
                        Warp warp = SaveSystem.findWarp(split[2]);
                        if (warp == null ) {
                            Block block = player.getTargetBlock(null, 100);
                            warp = SaveSystem.findWarp(block);
                            if (warp == null) {
                                player.sendMessage("Warp "+split[2]+" does not exsist.");
                                return;
                            }
                        }
                        //Check for command
                        if (split[1].equals("move")) {
                            warp.sendTo = player.getLocation();
                            player.sendMessage("Warp "+split[2]+" moved to current location");
                        }
                        else if (split[1].equals("access")) {
                            warp.access = split[3];
                            player.sendMessage("Access for Warp "+split[2]+" has been set to "+split[3]+"!");
                        }
                        else if (split[1].equals("msg")) {
                            warp.msg = msg.replace(split[0]+" "+split[1]+" "+split[2]+" ", "");
                            player.sendMessage("Message for Warp "+split[2]+" has been set to '"+warp.msg+"'");
                        }
                        else if (split[1].equals("time")) {
                            warp.resetTime = split[3];
                            player.sendMessage("Reset time for Warp "+split[2]+" has been set to "+split[3]+"!");
                        }
                        else if (split[1].equals("type")) {
                            warp.resetType = split[3];
                            player.sendMessage("Reset type for Warp "+split[2]+" has been set to "+split[3]+"!");
                        }
                        else if (split[1].equals("delete")) {
                            SaveSystem.removeWarp(warp);
                            player.sendMessage("Warp "+split[2]+" was deleted!");
                        }
                        else if (split[1].equals("locate")) {
                            String location = warp.sendTo.getBlock().getLocation().toString();
                            player.sendMessage("Warp "+split[2]+" sends you to "+location+"!");
                            return;
                        }
                    }
                }
                SaveSystem.save();
            }
            catch (Exception helpPage) {
                player.sendMessage("§e     ButtonWarp Help Page:");
                player.sendMessage("§2/bw make [Name]§b Makes Warp at current location");
                player.sendMessage("§2/bw make [Name] nowarp§b Makes a Warp that doesn't teleport");
                player.sendMessage("§2/bw move [Name]§b Moves Warp to current location");
                player.sendMessage("§2/bw link [Name]§b Links target button/pressure plate with Warp");
                player.sendMessage("§2/bw unlink [Name]§b Unlinks target block with Warp");
                player.sendMessage("§2/bw delete [Name]§b Deletes Warp and unlinks blocks");
                player.sendMessage("§2/bw amount [Name] [Amount]§b Sets amount for Warp");
                player.sendMessage("§2/bw access [Name] [Group]§b Only Players in Group can Warp");
                player.sendMessage("§2/bw access [Name] [Group1,Group2,...]§b Allow multiple Groups");
                player.sendMessage("§2/bw access [Name] public §b Anyone can Warp");
                player.sendMessage("§2/bw source [Name] server§b Generates/Destroys money");
                player.sendMessage("§2/bw source [Name] [Player]§b Gives/Takes money from Player");
                player.sendMessage("§2/bw source [Name] bank [Bank]§b Gives/Takes money from Bank");
                player.sendMessage("§2/bw msg [Name] [Msg]§b Sets message recieved after using Warp");
                player.sendMessage("§2/bw time [Name] [0'0'0'0]§b Sets cooldown time for using Warp");
                player.sendMessage("§2/bw type [Name] [Type]§b Sets cooldown type for using Warp");
                player.sendMessage("§2/bw list§b Lists all Warps");
                player.sendMessage("§2/bw locate [Name]§b Gives location of Warp");
                player.sendMessage("§2/bw rl§b Reloads ButtonWarp");
            }
        }
    }

    @Override
    public void onPlayerInteract (PlayerInteractEvent event) {
        Action action = event.getAction();
        
        //Return if Action was arm flailing
        if (action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_AIR))
            return;
        
        Block block = event.getClickedBlock();
        
        //Return if Block is not a switch
        if (!isSwitch(block.getTypeId()))
            return;
        
        int id = block.getTypeId();
        
        //Return if a Pressure Plate was clicked
        if ((id == 70 || id == 72) && (action.equals(Action.LEFT_CLICK_BLOCK) || action.equals(Action.RIGHT_CLICK_BLOCK)))
            return;
        
        Warp warp = SaveSystem.findWarp(block);
        
        //Return if Block is not part of an existing Warp
        if (warp == null)
            return;
        
        Player player = event.getPlayer();
        
        //Return if Player does not have permission to use Warps
        if(!ButtonWarp.hasPermission(player, "use")) {
            player.sendMessage("You do not have permission to use that.");
            event.setCancelled(true);
            return;
        }
        
        if (!warp.activate(player, block))
            event.setCancelled(true);
    }
    
    /**
     * Checks if the given Material ID is a Button or Pressure Plate
     * 
     * @param id The Material ID to be checked
     * @return true if the Material ID is a Button or Pressure Plate
     */
    protected static boolean isSwitch(int id) {
        switch (id) {
            case 70: return true; //Material == Stone Plate
            case 72: return true; //Material == Wood Plate
            case 77: return true; //Material == Stone Button
            default: return false;
        }
    }
}

