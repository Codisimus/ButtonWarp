package com.codisimus.plugins.buttonwarp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class ButtonWarp extends JavaPlugin {
    static Server server;
    static Logger logger;
    static Permission permission;
    static PluginManager pm;
    static Plugin plugin;
    static int defaultDays;
    static int defaultHours;
    static int defaultMinutes;
    static int defaultSeconds;
    static boolean defaultTakeItems;
    static int defaultMax;
    static String dataFolder;
    private static HashMap<String, Warp> warps = new HashMap<String, Warp>();

    /**
     * Loads this Plugin by doing the following:
     * Loads the settings from the Config file
     * Finds the Permission and Economy Plugins to use
     * Loads the saved ButtonWarp data
     * Registers the Events to listen for
     */
    @Override
    public void onEnable () {
        server = getServer();
        logger = getLogger();
        pm = server.getPluginManager();
        plugin = this;

        File dir = this.getDataFolder();
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dataFolder = dir.getPath();

        dir = new File(dataFolder+"/Warps");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        ButtonWarpConfig.load();

        //Find Permissions
        RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }

        //Find Economy
        RegisteredServiceProvider<Economy> economyProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            Econ.economy = economyProvider.getProvider();
        }

        //Load Warps Data
        loadData();

        //Register Events
        pm.registerEvents(new ButtonWarpListener(), this);
        pm.registerEvents(new ButtonWarpVehicleListener(), this);
        if (ButtonWarpListener.delay > 0) {
            pm.registerEvents(new ButtonWarpDelayListener(), this);
        }

        //Register the command found in the plugin.yml
        ButtonWarpCommand.command = (String)this.getDescription().getCommands().keySet().toArray()[0];
        getCommand(ButtonWarpCommand.command).setExecutor(new ButtonWarpCommand());

        Properties version = new Properties();
        try {
            version.load(this.getResource("version.properties"));
        } catch (Exception ex) {
            logger.warning("version.properties file not found within jar");
        }
        logger.info("ButtonWarp "+this.getDescription().getVersion()+" (Build "+version.getProperty("Build")+") is enabled!");
    }

    /**
     * Returns boolean value of whether the given player has the specific permission
     *
     * @param player The Player who is being checked for permission
     * @param node The String of the permission, ex. admin
     * @return true if the given player has the specific permission
     */
    public static boolean hasPermission(Player player, String node) {
        return permission.has(player, "buttonwarp." + node);
    }

    /**
     * Loads Warps from the save file
     * Saving is turned off if an error occurs
     */
    public static void loadData() {
        File[] files = plugin.getDataFolder().listFiles();

        //Organize files
        if (files != null) {
            for (File file: files) {
                String name = file.getName();
                if (name.endsWith(".dat")) {
                    File dest = new File(dataFolder+"/Warps/"+name.substring(0, name.length() - 4)+".properties");
                    file.renameTo(dest);
                }
            }
        }

        files = new File(dataFolder+"/Warps/").listFiles();

        FileInputStream fis = null;
        for (File file: files) {
            String name = file.getName();
            if (name.endsWith(".properties")) {
                try {
                    //Load the Properties file for reading
                    Properties p = new Properties();
                    fis = new FileInputStream(file);
                    p.load(fis);

                    //Construct a new Warp using the file name and values of message, amount, and source
                    Warp warp = new Warp(name.substring(0, name.length() - 11), ButtonWarpMessages.format(p.getProperty("Message")),
                            Double.parseDouble(p.getProperty("Amount")), p.getProperty("Source"));

                    if (p.containsKey("Location")) {
                        //Set the Location data
                        String[] location = p.getProperty("Location").split("'");
                        warp.world = location[0];
                        warp.x = Double.parseDouble(location[1]);
                        warp.y = Double.parseDouble(location[2]);
                        warp.z = Double.parseDouble(location[3]);
                        warp.pitch = Float.parseFloat(location[4]);
                        warp.yaw = Float.parseFloat(location[5]);
                        if (p.containsKey("IgnorePitch")) {
                            warp.ignorePitch = Boolean.parseBoolean(p.getProperty("IgnorePitch"));
                        }
                        if (p.containsKey("IgnoreYaw")) {
                            warp.ignoreYaw = Boolean.parseBoolean(p.getProperty("IgnoreYaw"));
                        }
                    }

                    if (p.containsKey("Commands")) {
                        String command = p.getProperty("Commands");
                        if (!command.equals("none")) {
                            warp.commands.addAll(Arrays.asList(command.split(", ")));
                        }
                    }

                    //Set the reset time
                    String[] resetTime = p.getProperty("ResetTime").split("'");
                    warp.days = Integer.parseInt(resetTime[0]);
                    warp.hours = Integer.parseInt(resetTime[1]);
                    warp.minutes = Integer.parseInt(resetTime[2]);
                    warp.seconds = Integer.parseInt(resetTime[3]);

                    //Set the reset type
                    warp.global = p.getProperty("ResetType").equals("global");

                    //Convert the value of access to an Array of Strings
                    String access = p.getProperty("Access");
                    if (!access.equals("public")) {
                        warp.access.addAll(Arrays.asList(access.split(", ")));
                    }

                    //Load the data of all the Buttons
                    if (p.containsKey("Buttons")) {
                        warp.setButtons(p.getProperty("Buttons"));
                    } else {
                        warp.setButtonsOld(p.getProperty("ButtonsData"));
                    }

                    warps.put(warp.name, warp);

                    file = new File(dataFolder + "/Warps/"
                                    + warp.name + ".warptimes");
                    if (file.exists()) {
                        fis = new FileInputStream(file);
                        warp.activationTimes.load(fis);
                    } else {
                        warp.save();
                    }
                } catch (Exception loadFailed) {
                    logger.severe("Failed to load " + name);
                    loadFailed.printStackTrace();
                } finally {
                    try {
                        fis.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    /**
     * Invokes save() method for each Warp
     */
    public static void saveAll() {
        for (Warp warp: warps.values()) {
            saveWarp(warp);
        }
    }

    /**
     * Writes the given Warp to its save file
     * If the file already exists, it is overwritten
     *
     * @param warp The given Warp
     */
    static void saveWarp(Warp warp) {
        FileOutputStream fos = null;
        try {
            Properties p = new Properties();

            if (warp.commands.isEmpty()) {
                p.setProperty("Commands", "none");
            } else {
                String command = warp.commands.toString();
                p.setProperty("Commands", command.substring(1, command.length() - 1));
            }

            p.setProperty("Message", ButtonWarpMessages.unformat(warp.msg));
            p.setProperty("Amount", String.valueOf(warp.amount));
            p.setProperty("Source", warp.source);
            if (warp.world != null) {
                p.setProperty("Location", warp.world+"'"+warp.x+"'"+warp.y+"'"+warp.z+"'"+warp.pitch+"'"+warp.yaw);
                p.setProperty("IgnorePitch", String.valueOf(warp.ignorePitch));
                p.setProperty("IgnoreYaw", String.valueOf(warp.ignoreYaw));
            }
            p.setProperty("ResetTime", warp.days+"'"+warp.hours+"'"+warp.minutes+"'"+warp.seconds);

            p.setProperty("ResetType", warp.global ? "global" : "player");

            if (warp.access.isEmpty()) {
                p.setProperty("Access", "public");
            } else {
                String access = warp.access.toString();
                p.setProperty("Access", access.substring(1, access.length() - 1));
            }

            String value = "";
            for (Button button: warp.buttons) {
                value += "; " + button.toString();
            }
            if (!value.isEmpty()) {
                value = value.substring(2);
            }
            p.setProperty("Buttons", value);

            //Write the Warp Properties to file
            fos = new FileOutputStream(dataFolder+"/Warps/"+warp.name+".properties");
            p.store(fos, null);
            fos.close();

            //Write the Warp activation times to file
            fos = new FileOutputStream(dataFolder + "/Warps/"
                                        + warp.name + ".warptimes");
            warp.activationTimes.store(fos, null);
        } catch (Exception saveFailed) {
            logger.severe("Save Failed!");
            saveFailed.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Returns the Collection of all Warps
     *
     * @return The Collection of all Warps
     */
    public static Collection<Warp> getWarps() {
        return warps.values();
    }

    /**
     * Adds the given Warp to the collection of Warps
     *
     * @param warp The given Warp
     */
    public static void addWarp(Warp warp) {
        warps.put(warp.name, warp);
        warp.save();
    }

    /**
     * Removes the given Warp from the collection of Warps
     *
     * @param warp The given Warp
     */
    public static void removeWarp(Warp warp) {
        warps.remove(warp.name);
        File trash = new File(dataFolder+"/Warps/"+warp.name+".properties");
        trash.delete();
        trash = new File(dataFolder+"/Warps/"+warp.name+".warptimes");
        trash.delete();
    }

    /**
     * Reloads ButtonWarp data
     */
    public static void rl() {
        rl(null);
    }

    /**
     * Reloads ButtonWarp data
     *
     * @param player The Player reloading the data
     */
    public static void rl(Player player) {
        warps.clear();
        loadData();

        logger.info("reloaded");
        if (player != null) {
            player.sendMessage("§5ButtonWarp reloaded");
        }
    }

    /**
     * Returns the Warp with the given name
     *
     * @param name The name of the Warp you wish to find
     * @return The Warp with the given name or null if not found
     */
    public static Warp findWarp(String name) {
        return warps.get(name);
    }

    /**
     * Returns the Warp that contains the given Block
     *
     * @param block The Block that is part of the Warp
     * @return The Warp that contains the given Block or null if not found
     */
    public static Warp findWarp(Block block) {
        //Iterate through all Warps to find the one with the given Block
        for (Warp warp: warps.values()) {
            if (warp.findButton(block) != null) {
                return warp;
            }
        }

        //Return null because the Warp does not exist
        return null;
    }
}
