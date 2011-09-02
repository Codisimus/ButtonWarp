
package ButtonWarp;

import org.bukkit.event.server.ServerListener;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijikokun.register.payment.Methods;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

/**
 * Checks for plugins whenever one is enabled
 * 
 */
public class PluginListener extends ServerListener {
    public PluginListener() { }
    private Methods methods = new Methods();
    protected static boolean useOP;

    @Override
    public void onPluginEnable(PluginEnableEvent event) {
        if (ButtonWarp.permissions == null && !useOP) {
            Plugin permissions = ButtonWarp.pm.getPlugin("Permissions");
            if (permissions != null) {
                ButtonWarp.permissions = ((Permissions)permissions).getHandler();
                System.out.println("[ButtonWarp] Successfully linked with Permissions!");
            }
        }
        if (Warp.economy == null)
            System.err.println("[ButtonWarp] Config file outdated, Please regenerate");
        else if (!Warp.economy.equalsIgnoreCase("none") && !methods.hasMethod()) {
            try {
                methods.setMethod(ButtonWarp.pm.getPlugin(Warp.economy));
                if (methods.hasMethod()) {
                    Warp.econ = methods.getMethod();
                    System.out.println("[ButtonWarp] Successfully linked with "+
                            Warp.econ.getName()+" "+Warp.econ.getVersion()+"!");
                }
            }
            catch (Exception e) {
            }
        }
    }
}