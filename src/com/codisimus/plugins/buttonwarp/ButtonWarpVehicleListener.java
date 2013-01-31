package com.codisimus.plugins.buttonwarp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

/**
 * Listens for interactions with Warps
 *
 * @author Codisimus
 */
public class ButtonWarpVehicleListener implements Listener {

    /**
     * Activates Warps when Players click a linked button
     *
     * @param event The PlayerInteractEvent that occurred
     */
    @EventHandler (ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        Block block = event.getTo().getBlock();
        if (block.getType() != Material.DETECTOR_RAIL) {
            return;
        }

        //Return if the Block is not part of an existing Warp
        Warp warp = ButtonWarp.findWarp(block);
        if (warp == null) {
            return;
        }
        /*
        Entity entity = event.getVehicle().getPassenger();
        if (!(entity instanceof Player)) {
            return;
        }

        //Return if the Player does not have permission to use Warps
        Player player = (Player) entity;
        if (!ButtonWarp.hasPermission(player, "use")) {
            player.sendMessage(ButtonWarpMessages.permission);
            return;
        }
        */
        final Entity entity = event.getVehicle().getPassenger();
        final Vehicle vehicle = event.getVehicle();

        //Eject the Player
        entity.leaveVehicle();

        Location location = vehicle.getLocation();
        location.setX(warp.x);
        location.setY(warp.y + 1);
        location.setZ(warp.z);
        if (!location.getChunk().isLoaded()) {
            location.getChunk().load();
        }

        vehicle.teleport(location);

        Location loc = entity.getLocation();
        location .setYaw(loc.getYaw());
        location.setPitch(loc.getPitch());
        entity.teleport(location);

        ButtonWarp.server.getScheduler().runTaskLater(ButtonWarp.plugin, new Runnable() {
                    @Override
                    public void run() {
                        vehicle.setPassenger(entity);
                    }
                }, 10L);
    }
}
