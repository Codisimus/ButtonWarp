package com.codisimus.plugins.buttonwarp.listeners;

import com.codisimus.plugins.buttonwarp.ButtonWarp;
import com.codisimus.plugins.buttonwarp.Register;
import org.bukkit.event.server.ServerListener;
import com.codisimus.plugins.buttonwarp.register.payment.Methods;
import org.bukkit.event.server.PluginEnableEvent;
import ru.tehkode.permissions.bukkit.PermissionsEx;

/**
 * Checks for Permission/Economy plugins whenever a Plugin is enabled
 * 
 * @author Codisimus
 */
public class pluginListener extends ServerListener {
    public static boolean useBP;

    /**
     * Executes methods to look for various types of plugins to link
     *
     * @param event The PluginEnableEvent that occurred
     */
    @Override
    public void onPluginEnable(PluginEnableEvent event) {
        linkPermissions();
        linkEconomy();
    }
    
    /**
     * Finds and links a Permission plugin
     * 
     */
    public void linkPermissions() {
        //Return if we have already have a permissions plugin
        if (ButtonWarp.permissions != null)
            return;
        
        //Return if PermissionsEx is not enabled
        if (!ButtonWarp.pm.isPluginEnabled("PermissionsEx"))
            return;
        
        //Return if OP permissions will be used
        if (useBP)
            return;
        
        ButtonWarp.permissions = PermissionsEx.getPermissionManager();
        System.out.println("[ButtonWarp] Successfully linked with PermissionsEx!");
    }
    
    /**
     * Finds and links an Economy plugin
     * 
     */
    public void linkEconomy() {
        //Return if we already have an Economy plugin
        if (Methods.hasMethod())
            return;
        
        //Return if no Economy is wanted
        if (Register.economy.equalsIgnoreCase("none"))
            return;
        
        //Set preferred plugin if there is one
        if (!Register.economy.equalsIgnoreCase("auto"))
            Methods.setPreferred(Register.economy);

        //Find an Economy Plugin (will first look for preferred Plugin)
        Methods.setMethod(ButtonWarp.pm);
        
        //Return if no Economy Plugin was found
        if (!Methods.hasMethod())
            return;
        
        //Reset Methods if the preferred Economy was not found
        if (!Methods.getMethod().getName().equalsIgnoreCase(Register.economy) && !Register.economy.equalsIgnoreCase("auto")) {
            Methods.reset();
            return;
        }
        
        Register.econ = Methods.getMethod();
        System.out.println("[ButtonWarp] Successfully linked with "+Register.econ.getName()+" "+Register.econ.getVersion()+"!");
    }
}