package com.codisimus.plugins.buttonwarp;

import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Checks if warping Players leave their current Block.
 */
public class ButtonWarpDelayListener implements Listener {
    static HashMap<Player, BukkitTask> warpers = new HashMap<Player, BukkitTask>();

    @EventHandler (ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (warpers.containsKey(player) && !event.getTo().getBlock().equals(event.getFrom().getBlock())) {
            warpers.get(player).cancel();
            warpers.remove(player);
            if (!ButtonWarpMessages.cancel.isEmpty()) {
                player.sendMessage(ButtonWarpMessages.cancel);
            }
        }
    }
}
