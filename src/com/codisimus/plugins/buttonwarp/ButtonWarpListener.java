package com.codisimus.plugins.buttonwarp;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listens for interactions with Warps
 *
 * @author Codisimus
 */
public class ButtonWarpListener implements Listener {
    static int delay;

    /**
     * Activates Warps when Players click a linked button
     *
     * @param event The PlayerInteractEvent that occurred
     */
    @EventHandler (ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        //Return if the Event was arm flailing
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Action action = event.getAction();

        //Return if the Block is not a switch
        switch (block.getType()) {
        case LEVER:
            //Return unless the Lever was clicked
            switch (action) {
            case LEFT_CLICK_BLOCK: break;
            case RIGHT_CLICK_BLOCK: break;
            default: return;
            }
            break;

        case STONE_PLATE:
            //Return unless the Pressure Plate was Stepped on
            if (action.equals(Action.PHYSICAL)) {
                break;
            } else {
                return;
            }

        case WOOD_PLATE:
            //Return unless the Pressure Plate was Stepped on
            if (action.equals(Action.PHYSICAL)) {
                break;
            } else {
                return;
            }

        case STONE_BUTTON:
            //Return unless the Stone Button was clicked
            switch (action) {
            case LEFT_CLICK_BLOCK: break;
            case RIGHT_CLICK_BLOCK: break;
            default: return;
            }
            break;

        default: return;
        }

        final Player player = event.getPlayer();
        if (ButtonWarpDelayListener.warpers.contains(player)) {
            return;
        }
        
        //Return if the Block is not part of an existing Warp
        final Warp warp = ButtonWarp.findWarp(block);
        if (warp == null) {
            return;
        }

        //Return if the Player does not have permission to use Warps
        if (!ButtonWarp.hasPermission(player, "use")) {
            player.sendMessage(ButtonWarpMessages.cannotUseWarps);
            event.setCancelled(true);
            return;
        }

        //Cancel the event if the Warp was not successfully activated
        final Button button = warp.findButton(block);
        if (!warp.canActivate(player, button)) {
            event.setCancelled(true);
        }
        
        //Delay Teleporting
        ButtonWarp.server.getScheduler().scheduleSyncDelayedTask(ButtonWarp.plugin, new Runnable() {
            @Override
            public void run() {
                if (ButtonWarpDelayListener.warpers.contains(player)) {
                    warp.activate(player, button);
                    ButtonWarpDelayListener.warpers.remove(player);
                }
            }
        }, 20L * delay);

        if (delay > 0) {
            ButtonWarpDelayListener.warpers.add(player);
            if (!ButtonWarpMessages.delay.isEmpty()) {
                player.sendMessage(ButtonWarpMessages.delay);
            }
        }
    }

    /**
     * Only allows admins to break Blocks that are linked to Warps
     *
     * @param event The BlockBreakEvent that occurred
     */
    @EventHandler (ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
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
        Warp warp = ButtonWarp.findWarp(block);
        if (warp == null) {
            return;
        }

        //Cancel the event if it was the Block was not broken by a Player
        Player player = event.getPlayer();
        if (player == null) {
            event.setCancelled(true);
            return;
        }

        //Cancel the event if the Player does not have the admin node
        if (!ButtonWarp.hasPermission(player, "admin")) {
            player.sendMessage(ButtonWarpMessages.permission);
            event.setCancelled(true);
        }
    }
}
