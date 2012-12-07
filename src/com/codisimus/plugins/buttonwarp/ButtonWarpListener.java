package com.codisimus.plugins.buttonwarp;

import java.util.HashSet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
    private static HashSet<String> antiSpam = new HashSet<String>();

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
        final Player player = event.getPlayer();

        //Return if the Block is not a switch
        Material type = block.getType();
        switch (type) {
        case LEVER: //Fall through
        case STONE_BUTTON: //Fall through
        case WOOD_BUTTON:
            switch (action) {
            case LEFT_CLICK_BLOCK: break;
            case RIGHT_CLICK_BLOCK: break;
            default: return;
            }
            break;

        case TRIPWIRE:
            //Find Tripwire Hook
            Block temp = block;
            //Check North
            while (temp.getType() == Material.TRIPWIRE) {
                temp = temp.getRelative(BlockFace.NORTH);
            }
            if (temp.getType() == Material.TRIPWIRE_HOOK
                    && ButtonWarp.findWarp(temp) != null) {
                block = temp;
                break;
            }
            //Check East
            while (temp.getType() == Material.TRIPWIRE) {
                temp = temp.getRelative(BlockFace.EAST);
            }
            if (temp.getType() == Material.TRIPWIRE_HOOK
                    && ButtonWarp.findWarp(temp) != null) {
                block = temp;
                break;
            }
            //Check South
            while (temp.getType() == Material.TRIPWIRE) {
                temp = temp.getRelative(BlockFace.SOUTH);
            }
            if (temp.getType() == Material.TRIPWIRE_HOOK
                    && ButtonWarp.findWarp(temp) != null) {
                block = temp;
                break;
            }
            //Check West
            while (temp.getType() == Material.TRIPWIRE) {
                temp = temp.getRelative(BlockFace.WEST);
            }
            if (temp.getType() == Material.TRIPWIRE_HOOK
                    && ButtonWarp.findWarp(temp) != null) {
                block = temp;
                break;
            }
            //Fall through
        case STONE_PLATE: //Fall through
        case WOOD_PLATE:
            if (action.equals(Action.PHYSICAL)) {
                break;
            } else {
                return;
            }

        default: return;
        }

        if (ButtonWarpDelayListener.warpers.containsKey(player)) {
            return;
        }

        //Return if the Block is not part of an existing Warp
        final Warp warp = ButtonWarp.findWarp(block);
        if (warp == null) {
            return;
        }

        switch (type) {
        case STONE_PLATE: //Fall through
        case WOOD_PLATE:
            Block playerBlock = player.getLocation().getBlock();
            if (!block.equals(playerBlock)
                    || antiSpam.contains(player.getName()
                    + '@' + playerBlock.getLocation().toString())) {
                event.setCancelled(true);
                return;
            } else {
                break;
            }
        default: break;
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
            final String key = player.getName() + '@'
                    + player.getLocation().getBlock().getLocation().toString();
            antiSpam.add(key);
            ButtonWarp.server.getScheduler().scheduleSyncDelayedTask(ButtonWarp.plugin, new Runnable() {
                @Override
                public void run() {
                    antiSpam.remove(key);
                }
            }, 100L);
            return;
        }

        if (warp.world == null) {
            warp.activate(player, button);
            return;
        }

        //Delay Teleporting
        int id = ButtonWarp.server.getScheduler().scheduleSyncDelayedTask(ButtonWarp.plugin, new Runnable() {
            @Override
            public void run() {
                warp.activate(player, button);
                if (delay > 0) {
                    ButtonWarpDelayListener.warpers.remove(player);
                }
            }
        }, 20L * delay);

        if (delay > 0) {
            ButtonWarpDelayListener.warpers.put(player, id);
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
        Block block = event.getBlock();
        if (!ButtonWarpCommand.LINKABLE.contains(block.getType())) {
            return;
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
