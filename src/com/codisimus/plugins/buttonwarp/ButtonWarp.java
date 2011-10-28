package com.codisimus.plugins.buttonwarp;

import com.codisimus.plugins.buttonwarp.listeners.blockListener;
import com.codisimus.plugins.buttonwarp.listeners.commandListener;
import com.codisimus.plugins.buttonwarp.listeners.playerListener;
import com.codisimus.plugins.buttonwarp.listeners.pluginListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
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
 * Loads Plugin and manages Permissions
 *
 * @author Codisimus
 */
public class ButtonWarp extends JavaPlugin {
    public static PermissionManager permissions;
    public static PluginManager pm;
    public static Server server;
    public Properties p;
    public static boolean takeItems;

    @Override
    public void onDisable () {
    }

    @Override
    public void onEnable () {
        server = getServer();
        pm = server.getPluginManager();
        checkFiles();
        loadConfig();
        SaveSystem.load();
        registerEvents();
        getCommand("buttonwarp").setExecutor(new commandListener());
        System.out.println("ButtonWarp "+this.getDescription().getVersion()+" is enabled!");
    }
    
    /**
     * Makes sure all needed files exist
     * 
     */
    public void checkFiles() {
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
    public void moveFile(String fileName) {
        try {
            //Retrieve file from this plugin's .jar
            JarFile jar = new JarFile("plugins/ButtonWarp.jar");
            ZipEntry entry = jar.getEntry(fileName);
            
            //Create the destination folder if it does not exist
            String destination = "plugins/ButtonWarp/";
            File file = new File(destination.substring(0, destination.length()-1));
            if (!file.exists())
                file.mkdir();
            
            //Copy the file
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
        catch (Exception moveFailed) {
            System.err.println("[ButtonWarp] File Move Failed!");
            moveFailed.printStackTrace();
        }
    }
    
    /**
     * Loads settings from the config.properties file
     * 
     */
    public void loadConfig() {
        p = new Properties();
        try {
            p.load(new FileInputStream("plugins/ButtonWarp/config.properties"));
        }
        catch (Exception e) {
        }
        Register.economy = loadValue("Economy");
        pluginListener.useBP = Boolean.parseBoolean(loadValue("UseBukkitPermissions"));
        ButtonWarp.takeItems = Boolean.parseBoolean(loadValue("TakeItemsToNewWorld"));
    }

    /**
     * Loads the given key and prints error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    public String loadValue(String key) {
        //Print error if key is not found
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
    public void registerEvents() {
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, new pluginListener(), Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_INTERACT, new playerListener(), Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_BREAK, new blockListener(), Priority.Normal, this);
    }

    /**
     * Returns boolean value of whether the given player has the specific permission
     * 
     * @param player The Player who is being checked for permission
     * @param type The String of the permission, ex. admin
     * @return true if the given player has the specific permission
     */
    public static boolean hasPermission(Player player, String type) {
        //Check if a Permission Plugin is present
        if (permissions != null)
            return permissions.has(player, "buttonwarp."+type);
        
        //Return Bukkit Permission value
        return player.hasPermission("buttonwarp."+type);
    }
    
    /**
     * Checks if the given Material ID is a Button, Switch, or Pressure Plate
     * 
     * @param id The Material ID to be checked
     * @return true if the Material ID is a Button, Switch, or Pressure Plate
     */
    public static boolean isSwitch(int id) {
        switch (id) {
            case 69: return true; //Material == Switch
            case 70: return true; //Material == Stone Plate
            case 72: return true; //Material == Wood Plate
            case 77: return true; //Material == Stone Button
            default: return false;
        }
    }
}
