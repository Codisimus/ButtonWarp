
package ButtonWarp;

import org.bukkit.event.server.ServerListener;
import com.codisimus.buttonwarp.register.payment.Methods;
import org.bukkit.event.server.PluginEnableEvent;
import ru.tehkode.permissions.bukkit.PermissionsEx;

/**
 * Checks for plugins whenever one is enabled
 * 
 */
public class PluginListener extends ServerListener {
    public PluginListener() { }
    protected static boolean useOP;

    @Override
    public void onPluginEnable(PluginEnableEvent event) {
        linkPermissions();
        linkEconomy();
    }
    
    /**
     * Find and link a Permission plugin
     * 
     */
    private void linkPermissions() {
        //Return if we have already have a permissions plugin
        if (ButtonWarp.permissions != null)
            return;
        
        //Return if PermissionsEx is not enabled
        if (!ButtonWarp.pm.isPluginEnabled("PermissionsEx"))
            return;
        
        //Return if OP permissions will be used
        if (useOP)
            return;
        
        ButtonWarp.permissions = PermissionsEx.getPermissionManager();
        System.out.println("[ButtonWarp] Successfully linked with PermissionsEx!");
    }
    
    /**
     * Find and link an Economy plugin
     * 
     */
    private void linkEconomy() {
        //Return if we already have an Economy plugin
        if (Methods.hasMethod())
            return;
        
        //Return if no Economy is wanted
        if (Warp.economy.equalsIgnoreCase("none"))
            return;
        
        //Set preferred plugin if there is one
        if (!Warp.economy.equalsIgnoreCase("auto"))
            Methods.setPreferred(Warp.economy);
        
        Methods.setMethod(ButtonWarp.pm);
        
        //Reset Methods if the preferred Economy was not found
        if (!Methods.getMethod().getName().equalsIgnoreCase(Warp.economy) && !Warp.economy.equalsIgnoreCase("auto")) {
            Methods.reset();
            return;
        }
        
        Warp.econ = Methods.getMethod();
        System.out.println("[ButtonWarp] Successfully linked with "+Warp.econ.getName()+" "+Warp.econ.getVersion()+"!");
    }
}