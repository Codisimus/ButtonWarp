
package com.codisimus.plugins.buttonwarp;

import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 *
 * @author Codisimus
 */
public class ButtonWarpWorldListener extends WorldListener{

    @Override
    public void onWorldLoad (WorldLoadEvent event) {
        SaveSystem.loadLocations(event.getWorld());
    }
}

