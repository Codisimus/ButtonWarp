package com.codisimus.plugins.buttonwarp;

import java.util.HashSet;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Checks if warping Players leave their current Block.
 */
public class ButtonWarpDelayListener implements Listener {
    static HashSet<Player> warpers = new HashSet<Player>();

    @EventHandler (ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (warpers.contains(player) && !event.getTo().getBlock().equals(event.getFrom().getBlock())) {
            warpers.remove(player);
            if (!ButtonWarpMessages.cancel.isEmpty()) {
                player.sendMessage(ButtonWarpMessages.cancel);
            }
        }
    }
}
