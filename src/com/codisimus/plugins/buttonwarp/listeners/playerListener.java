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
public class playerListener extends PlayerListener {

    @Override
    public void onPlayerInteract (PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        
        //Return if the Block is not a switch
        switch (block.getTypeId()) {
            case 69: //Material == Switch
                //Return unless the Switch was clicked
                if (action.equals(Action.LEFT_CLICK_BLOCK) || action.equals(Action.RIGHT_CLICK_BLOCK))
                    break;
                else
                    return;

            case 70: //Material == Stone Plate
                //Return unless the Pressure Plate was Stepped on
                if (action.equals(Action.PHYSICAL))
                    break;
                else
                    return;

            case 72: //Material == Wood Plate
                //Return unless the Pressure Plate was Stepped on
                if (action.equals(Action.PHYSICAL))
                    break;
                else
                    return;

            case 77: //Material == Stone Button
                //Return unless the Stone Button was clicked
                if (action.equals(Action.LEFT_CLICK_BLOCK) || action.equals(Action.RIGHT_CLICK_BLOCK))
                    break;
                else
                    return;

            default: return;
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
        if (warp.activate(player, block))
            SaveSystem.save();
        else
            event.setCancelled(true);
    }
}