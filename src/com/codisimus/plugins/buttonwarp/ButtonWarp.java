
package com.codisimus.plugins.buttonwarp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.PermissionManager;

/**
 *
 * @author Codisimus
 */
public class ButtonWarp extends JavaPlugin {
    protected static PermissionManager permissions;
    protected static PluginManager pm;
    protected static Server server;
    private Properties p;

    @Override
    public void onDisable () {
    }

    @Override
    public void onEnable () {
        server = getServer();
        pm = server.getPluginManager();
        checkFiles();
        loadConfig();
        SaveSystem.loadFromFile();
        registerEvents();
        System.out.println("ButtonWarp "+this.getDescription().getVersion()+" is enabled!");
    }
    
    /**
     * Makes sure all needed files exist
     * 
     */
    private void checkFiles() {
        File file = new File("plugins/ButtonWarp/config.properties");
        if (!file.exists())
            moveFile("config.properties");
    }
    
    /**
     * Moves file from ButtonWarp.jar to appropriate folder
     * Destination folder is created if it doesn't exist
     * 
     * @param fileName The name of the file to be moved
     */
    private void moveFile(String fileName) {
        try {
            JarFile jar = new JarFile("plugins/ButtonWarp.jar");
            ZipEntry entry = jar.getEntry(fileName);
            String destination = "plugins/ButtonWarp/";
            File file = new File(destination.substring(0, destination.length()-1));
            if (!file.exists())
                file.mkdir();
            File efile = new File(destination, fileName);
            InputStream in = new BufferedInputStream(jar.getInputStream(entry));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
            byte[] buffer = new byte[2048];
            while (true) {
                int nBytes = in.read(buffer);
                if (nBytes <= 0)
                    break;
                out.write(buffer, 0, nBytes);
            }
            out.flush();
            out.close();
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Loads settings from the config.properties file
     * 
     */
    private void loadConfig() {
        p = new Properties();
        try {
            p.load(new FileInputStream("plugins/ButtonWarp/config.properties"));
        }
        catch (Exception e) {
        }
        Warp.economy = loadValue("Economy");
        PluginListener.useBP = Boolean.parseBoolean(loadValue("UseBukkitPermissions"));
        Warp.takeItems = Boolean.parseBoolean(loadValue("TakeItemsToNewWorld"));
    }

    /**
     * Loads the given key and prints error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    private String loadValue(String key) {
        if (!p.containsKey(key)) {
            System.err.println("[ButtonWarp] Missing value for "+key+" in config file");
            System.err.println("[ButtonWarp] Please regenerate config file");
        }
        return p.getProperty(key);
    }
    
    /**
     * Registers events for the ButtonWarp Plugin
     *
     */
    private void registerEvents() {
        ButtonWarpPlayerListener playerListener = new ButtonWarpPlayerListener();
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, new PluginListener(), Priority.Monitor, this);
        pm.registerEvent(Type.WORLD_LOAD, new ButtonWarpWorldListener(), Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
    }

    /**
     * Returns boolean value of whether the given player has the specific permission
     * 
     * @param player The Player who is being checked for permission
     * @param type The String of the permission, ex. admin
     * @return true if the given player has the specific permission
     */
    public static boolean hasPermission(Player player, String type) {
        if (permissions != null)
            return permissions.has(player, "buttonwarp."+type);
        return player.hasPermission("buttonwarp."+type);
    }
}
