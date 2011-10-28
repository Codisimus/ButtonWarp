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
public class blockListener extends BlockListener{

    @Override
    public void onBlockBreak (BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (!ButtonWarp.isSwitch(block.getTypeId()))
            return;
        
        Warp warp = SaveSystem.findWarp(block);
        
        if (warp == null)
            return;
        
        Player player = event.getPlayer();
        
        if (player == null) {
            event.setCancelled(true);
            return;
        }
        
        if (!ButtonWarp.hasPermission(player, "admin")) {
            player.sendMessage("You do not have permission to do that");
            event.setCancelled(true);
        }
    }
}
