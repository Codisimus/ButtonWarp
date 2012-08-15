package com.codisimus.plugins.buttonwarp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
            player.sendMessage("You do not have permission to use that.");
            return;
        }
        */
        Vehicle vehicle = event.getVehicle();
        Vector vector = vehicle.getVelocity();
        
        Location location = vehicle.getLocation();
        location.setX(warp.x);
        location.setY(warp.y);
        location.setZ(warp.z);
        vehicle.teleport(location);
        
        vehicle.setVelocity(vector);
    }
}
