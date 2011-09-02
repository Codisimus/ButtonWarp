
package ButtonWarp;

import java.util.LinkedList;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.server.PluginEnableEvent;

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
                if (split[1].equals("make")) {
                    if (!ButtonWarp.hasPermission(player, "make")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
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
                else if (split[1].equals("move")) {
                    if (!ButtonWarp.hasPermission(player, "admin.move")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    Warp warp = SaveSystem.findWarp(split[2]);
                    if (warp == null) {
                        event.getPlayer().sendMessage("Warp "+split[2]+" does not exsist.");
                        return;
                    }
                    warp.sendTo = player.getLocation();
                    player.sendMessage("Warp "+split[2]+" moved to current location");
                    SaveSystem.save();
                }
                else if (split[1].equals("link")) {
                    if (!ButtonWarp.hasPermission(player, "make")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    Block block = player.getTargetBlock(null, 100);
                    Material mat = block.getType();
                    if (!mat.equals(Material.STONE_BUTTON)) {
                        player.sendMessage("You must link the Warp to a button.");
                        return;
                    }
                    Warp warp = SaveSystem.findWarp(block);
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
                    SaveSystem.save();
                }
                else if (split[1].equals("amount")) {
                    if (!ButtonWarp.hasPermission(player, "amount")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    Warp warp = SaveSystem.findWarp(split[2]);
                    if (warp == null) {
                        event.getPlayer().sendMessage("Warp "+split[2]+" does not exsist.");
                        return;
                    }
                    warp.amount = Integer.parseInt(split[3]);
                    player.sendMessage("Amount for Warp "+split[2]+" has been set to "+split[3]+"!");
                    SaveSystem.save();
                }
                else if (split[1].equals("msg")) {
                    if (!ButtonWarp.hasPermission(player, "msg")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    Warp warp = SaveSystem.findWarp(split[2]);
                    if (warp == null) {
                        event.getPlayer().sendMessage("Warp "+split[2]+" does not exsist.");
                        return;
                    }
                    warp.msg = msg.replace(split[0]+" "+split[1]+" "+split[2]+" ", "");
                    player.sendMessage("Message for Warp "+split[2]+" has been set to "+warp.msg+"!");
                    SaveSystem.save();
                }
                else if (split[1].equals("time")) {
                    if (!ButtonWarp.hasPermission(player, "time")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    Warp warp = SaveSystem.findWarp(split[2]);
                    if (warp == null) {
                        event.getPlayer().sendMessage("Warp "+split[2]+" does not exsist.");
                        return;
                    }
                    warp.resetTime = split[3];
                    player.sendMessage("Reset time for Warp "+split[2]+" has been set to "+split[3]+"!");
                    SaveSystem.save();
                }
                else if (split[1].equals("type")) {
                    if (!ButtonWarp.hasPermission(player, "type")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    Warp warp = SaveSystem.findWarp(split[2]);
                    if (warp == null) {
                        event.getPlayer().sendMessage("Warp "+split[2]+" does not exsist.");
                        return;
                    }
                    warp.resetType = split[3];
                    player.sendMessage("Reset type for Warp "+split[2]+" has been set to "+split[3]+"!");
                    SaveSystem.save();
                }
                else if (split[1].equals("unlink")) {
                    if (!ButtonWarp.hasPermission(player, "make")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    Block block = player.getTargetBlock(null, 100);
                    Material mat = block.getType();
                    if (!mat.equals(Material.STONE_BUTTON)) {
                        player.sendMessage("You must target the button you wish to unlink");
                        return;
                    }
                    Warp warp = SaveSystem.findWarp(block);
                    if (warp == null) {
                        event.getPlayer().sendMessage("Target button is not linked to a Warp");
                        return;
                    }
                    warp.removeButton(block);
                    player.sendMessage("Button sucessfully unlinked!");
                    SaveSystem.save();
                }
                else if (split[1].equals("delete")) {
                    if (!ButtonWarp.hasPermission(player, "make")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    Warp warp = SaveSystem.findWarp(split[2]);
                    if (warp == null) {
                        Block block = player.getTargetBlock(null, 100);
                        warp = SaveSystem.findWarp(block);
                        if (warp == null) {
                            player.sendMessage("Warp "+split[2]+" does not exsist.");
                            return;
                        }
                    }
                    SaveSystem.removeWarp(warp);
                    player.sendMessage("Warp "+split[2]+" was deleted!");
                    SaveSystem.save();
                }
                else if (split[1].equals("source")) {
                    if (!ButtonWarp.hasPermission(player, "source")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    Warp warp = SaveSystem.findWarp(split[2]);
                    if (warp == null) {
                        event.getPlayer().sendMessage("Warp "+split[2]+" does not exsist.");
                        return;
                    }
                    if (split[3].equals("bank"))
                        warp.source = "bank:"+split[4];
                    else
                        warp.source = split[3];
                    player.sendMessage("Money source for Warp "+split[2]+" has been set to "+warp.source+"!");
                    SaveSystem.save();
                }
                else if (split[1].equals("list")) {
                    if (!ButtonWarp.hasPermission(player, "admin.list")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    LinkedList<Warp> warps = SaveSystem.getWarps();
                    String warpList = "";
                    player.sendMessage("Current Warps:");
                    for (Warp warp : warps) {
                        warpList = warpList.concat(warp.name+":"+warp.amount+", ");
                    }
                    player.sendMessage(warpList);
                }
                else if (split[1].equals("locate")) {
                    if (!ButtonWarp.hasPermission(player, "admin.locate")) {
                        player.sendMessage("You do not have permission to do that.");
                        return;
                    }
                    Warp warp = SaveSystem.findWarp(split[2]);
                    if (warp == null ) {
                        Block block = player.getTargetBlock(null, 100);
                        warp = SaveSystem.findWarp(block);
                        if (warp == null) {
                            player.sendMessage("Warp "+split[2]+" does not exsist.");
                            return;
                        }
                    }
                    String location = warp.sendTo.getBlock().getLocation().toString();
                    player.sendMessage("Warp "+split[2]+" sends you to "+location+"!");
                }
                else if (split[1].equals("rl")) {
                    SaveSystem.loadFromFile();
                    ButtonWarp.pm = ButtonWarp.server.getPluginManager();
                    System.out.println("ButtonWarp reloaded");
                    player.sendMessage("ButtonWarp reloaded");
                }
                else if (split[1].startsWith("help"))
                    throw new Exception();
            }
            catch (Exception helpPage) {
                player.sendMessage("§e     ButtonWarp Help Page:");
                player.sendMessage("§2/bw make [Name]§b Makes Warp at current location");
                player.sendMessage("§2/bw make [Name] nowarp§b Makes a Warp that doesn't teleport");
                player.sendMessage("§2/bw move [Name]§b Moves Warp to current location");
                player.sendMessage("§2/bw link [Name]§b Links target button with Warp");
                player.sendMessage("§2/bw unlink [Name]§b Unlinks target button with Warp");
                player.sendMessage("§2/bw delete [Name]§b Deletes Warp and unlinks buttons");
                player.sendMessage("§2/bw amount [Name] [Amount]§b Sets amount for Warp");
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
        else
            return;
    }

    @Override
    public void onPlayerInteract (PlayerInteractEvent event) {
        //Checks if Action was clicking a Block
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Block block = event.getClickedBlock();
            Player player = event.getPlayer();
            if (block.getType().equals(Material.STONE_BUTTON)) {
                Warp warp = SaveSystem.findWarp(block);
                if (warp == null)
                    return;
                else if(!ButtonWarp.hasPermission(player, "use")) {
                    player.sendMessage("You do not have permission to use that.");
                    event.setCancelled(true);
                }
                else
                    if (!warp.activate(player, block))
                        event.setCancelled(true);
            }
        }
    }
}

