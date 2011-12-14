package com.codisimus.plugins.buttonwarp.listeners;

import com.codisimus.plugins.buttonwarp.ButtonWarp;
import com.codisimus.plugins.buttonwarp.SaveSystem;
import com.codisimus.plugins.buttonwarp.Warp;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;

/**
 * Listens for griefing events
 *
 * @author Codisimus
 */
public class BlockEventListener extends BlockListener {

    /**
     * Only allows admins to break Blocks that are linked to Warps
     *
     * @param event The BlockBreakEvent that occurred
     */
    @Override
    public void onBlockBreak (BlockBreakEvent event) {
        //Return if the Block is not a switch
        Block block = event.getBlock();
        switch (block.getType()) {
            case LEVER: break;
            case STONE_PLATE: break;
            case WOOD_PLATE: break;
            case STONE_BUTTON: break;
            default: return;
        }
        
        //Return if the Block is not linked to a Warp
        Warp warp = SaveSystem.findWarp(block);
        if (warp == null)
            return;
        
        //Cancel the event if it was the Block was not broken by a Player
        Player player = event.getPlayer();
        if (player == null) {
            event.setCancelled(true);
            return;
        }
        
        //Cancel the event if the Player does not have the admin node
        if (!ButtonWarp.hasPermission(player, "admin")) {
            player.sendMessage("Only Admins can destroy ButtonWarp Blocks");
            event.setCancelled(true);
        }
    }
}
