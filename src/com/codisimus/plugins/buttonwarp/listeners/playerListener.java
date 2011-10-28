package com.codisimus.plugins.buttonwarp.listeners;

import com.codisimus.plugins.buttonwarp.ButtonWarp;
import com.codisimus.plugins.buttonwarp.SaveSystem;
import com.codisimus.plugins.buttonwarp.Warp;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

/**
 * Listens for Players activating Warps
 *
 * @author Codisimus
 */
public class playerListener extends PlayerListener{

    @Override
    public void onPlayerInteract (PlayerInteractEvent event) {
        //Return if the Action was arm flailing
        Action action = event.getAction();
        if (action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_AIR))
            return;
        
        //Return if the Block is not a switch
        Block block = event.getClickedBlock();
        if (!ButtonWarp.isSwitch(block.getTypeId()))
            return;
        
        //Return if a Pressure Plate was clicked
        switch (block.getTypeId()) {
            case 70:
                if (action.equals(Action.LEFT_CLICK_BLOCK) || action.equals(Action.RIGHT_CLICK_BLOCK))
                    return;
                break;
            case 72:
                if (action.equals(Action.LEFT_CLICK_BLOCK) || action.equals(Action.RIGHT_CLICK_BLOCK))
                    return;
                break;
            default: break;
        }
        
        //Return if the Block is not part of an existing Warp
        Warp warp = SaveSystem.findWarp(block);
        if (warp == null)
            return;
        
        //Return if the Player does not have permission to use Warps
        Player player = event.getPlayer();
        if(!ButtonWarp.hasPermission(player, "use")) {
            player.sendMessage("You do not have permission to use that.");
            event.setCancelled(true);
            return;
        }
        
        //Cancel the event if the Warp was not successfully activated
        if (!warp.activate(player, block))
            event.setCancelled(true);
        else
            SaveSystem.save();
    }
}