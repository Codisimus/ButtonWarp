package com.codisimus.plugins.buttonwarp;

import java.io.*;
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
    private static HashMap<String, Warp> warps = new HashMap<String, Warp>();
    private static String dataFolder;
    private Properties p;

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

        //Load Config settings
        FileInputStream fis = null;
        try {
            //Copy the file from the jar if it is missing
            File file = new File(dataFolder+"/config.properties");
            if (!file.exists()) {
                this.saveResource("config.properties", true);
            }

            //Load config file
            p = new Properties();
            fis = new FileInputStream(file);
            p.load(fis);

            ButtonWarpListener.delay = Integer.parseInt(loadValue("WarpDelay"));

            defaultTakeItems = Boolean.parseBoolean(loadValue("DefaultCanTakeItems"));
            defaultMax = Integer.parseInt(loadValue("DefaultMaxWarpsPerReset"));

            String[] defaultResetTime = loadValue("DefaultResetTime").split("'");
            defaultDays = Integer.parseInt(defaultResetTime[0]);
            defaultHours = Integer.parseInt(defaultResetTime[1]);
            defaultMinutes = Integer.parseInt(defaultResetTime[2]);
            defaultSeconds = Integer.parseInt(defaultResetTime[3]);

            ButtonWarpCommand.multiplier = Integer.parseInt(loadValue("CommandWarpMultiplier"));

            Warp.log = Boolean.parseBoolean(loadValue("LogWarps"));
            Warp.log = Boolean.parseBoolean(loadValue("BroadcastWarps"));

            ButtonWarpMessages.broadcast = loadValue("WarpUsedBroadcast");
            ButtonWarpMessages.permission = loadValue("PermissionMessage");
            ButtonWarpMessages.insufficentFunds = loadValue("InsufficientFundsMessage");
            ButtonWarpMessages.sourceInsufficentFunds = loadValue("SourceInsufficientFundsMessage");
            ButtonWarpMessages.delay = loadValue("WarpDelayMessage");
            ButtonWarpMessages.alreadyWarping = loadValue("AlreadyWarpingMessage");
            ButtonWarpMessages.cancel = loadValue("WarpCancelMessage");
            ButtonWarpMessages.cannotUseWarps = loadValue("CannotUseWarpsMessage");
            ButtonWarpMessages.noAccess = loadValue("NoAccessMessage");
            ButtonWarpMessages.cannotTakeItems = loadValue("CannotTakeItemsMessage");
            ButtonWarpMessages.cannotTakeArmor = loadValue("CannotTakeArmorMessage");
            ButtonWarpMessages.worldMissing = loadValue("WorldMissingMessage");
            ButtonWarpMessages.cannotHaveAnotherReward = loadValue("CannotHaveAnotherRewardMessage");
            ButtonWarpMessages.cannotUseAgain = loadValue("CannotUseAgainMessage");
            ButtonWarpMessages.timeRemainingReward = loadValue("TimeRemainingRewardMessage");
            ButtonWarpMessages.timeRemainingUse = loadValue("TimeRemainingUseMessage");

            ButtonWarpMessages.formatAll();
        } catch (Exception missingProp) {
            logger.severe("Failed to load ButtonWarp "+this.getDescription().getVersion());
            missingProp.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }

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
     * Loads the given key and prints an error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    private String loadValue(String key) {
        if (p.containsKey(key)) {
            return p.getProperty(key);
        } else {
            logger.severe("Missing value for " + key);
            logger.severe("Please regenerate the config.properties file");
            return null;
        }
    }

    /**
     * Returns boolean value of whether the given player has the specific permission
     *
     * @param player The Player who is being checked for permission
     * @param node The String of the permission, ex. admin
     * @return true if the given player has the specific permission
     */
    public static boolean hasPermission(Player player, String node) {
        return permission.has(player, "buttonwarp."+node);
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
                    warp.setButtons(p.getProperty("ButtonsData"));

                    warps.put(warp.name, warp);
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

        //End loading if at least one Warp was loaded
        if (!warps.isEmpty()) {
            return;
        }

        //Open save file in BufferedReader
        File file = new File("plugins/ButtonWarp/warps.save");
        if (file.exists()) {
            logger.info("Updating old save file");
        } else {
            loadOld();
            return;
        }

        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(new FileReader(file));
            String line = bReader.readLine(); //Skip version

            //Convert each line into data until all lines are read
            while ((line = bReader.readLine()) != null) {
                try {
                    String[] warpData = line.split(";");

                    Warp warp = new Warp(warpData[0], warpData[1], Double.parseDouble(warpData[2]), warpData[3]);

                    //Load the location data of the Warp if it exists
                    String[] locationData = warpData[4].substring(1, warpData[4].length() - 1).split("'");
                    if (locationData.length != 0) {
                        warp.world = locationData[0];
                        warp.x = Double.parseDouble(locationData[1]);
                        warp.y = Double.parseDouble(locationData[2]);
                        warp.z = Double.parseDouble(locationData[3]);
                        warp.pitch = Float.parseFloat(locationData[4]);
                        warp.yaw = Float.parseFloat(locationData[5]);
                    }

                    //Load the time data of the Warp
                    String[] time = (warpData[5].substring(1, warpData[5].length() - 1)).split("'");
                    warp.days = Integer.parseInt(time[0]);
                    warp.hours = Integer.parseInt(time[1]);
                    warp.minutes = Integer.parseInt(time[2]);
                    warp.seconds = Integer.parseInt(time[3]);

                    warp.global = Boolean.parseBoolean(warpData[6]);

                    if (warpData[7].length() != 2) {
                        //Load the access groups of the Warp
                        String[] groups = (warpData[7].substring(1, warpData[7].length() - 1)).split(", ");
                        warp.access.addAll(Arrays.asList(groups));
                    }

                    if (warpData[8].length() != 2) {
                        if (!warpData[8].contains("},  ")) {
                            warpData[8] = warpData[8].replace("}, ", "},  ");
                        }

                        //Load the Buttons of the Warp
                        int index, x, y, z;
                        String[] buttons = (warpData[8].substring(1, warpData[8].length() - 1)).split(",  ");
                        for (String string: buttons) {
                            String[] buttonData = string.split("\\{", 2);

                            //Load the Block Location data of the Button
                            String[] blockData = buttonData[0].split("'");
                            x = Integer.parseInt(blockData[1]);
                            y = Integer.parseInt(blockData[2]);
                            z = Integer.parseInt(blockData[3]);
                            Button button = new Button(blockData[0], x, y, z);

                            //Load the HashMap of Users of the Button
                            String[] users = buttonData[1].substring(0, buttonData[1].length() - 1).split(", ");

                            int[] timeArray = new int[5];
                            timeArray[0] = 1;
                            timeArray[1] = 0;
                            timeArray[2] = 0;
                            timeArray[3] = 0;
                            timeArray[4] = 0;

                            for (String user: users) {
                                if ((index = user.indexOf('=')) != -1) {
                                    button.users.put(user.substring(0, index), timeArray);
                                }
                            }

                            warp.buttons.add(button);
                        }
                    }

                    addWarp(warp);
                } catch (Exception loadFailed) {
                    logger.severe("Load failed, Errored line:");
                    logger.severe(line);
                    loadFailed.printStackTrace();
                }
            }
        } catch (Exception ex) {
            logger.severe("Unexpected error");
            ex.printStackTrace();
        } finally {
            try {
                bReader.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Loads Warps from outdated save file
     */
    private static void loadOld() {
        BufferedReader bReader = null;
        try {
            //Open save file in BufferedReader
            File file = new File("plugins/ButtonWarp/ButtonWarp.save");
            if (file.exists()) {
                logger.info("Updating old save file");
            } else {
                return;
            }
            bReader = new BufferedReader(new FileReader("plugins/ButtonWarp/ButtonWarp.save"));

            String line;

            //Convert each line into data until all lines are read
            while ((line = bReader.readLine()) != null) {
                try {
                    String[] data = line.split(";");

                    Warp warp = new Warp(data[0], null);

                    warp.msg = data[1];
                    warp.amount = Double.parseDouble(data[2]);
                    warp.source = data[3];

                    if (!data[10].equals("none")) {
                        String[] time = data[10].split("'");
                        warp.days = Integer.parseInt(time[0]);
                        warp.hours = Integer.parseInt(time[1]);
                        warp.minutes = Integer.parseInt(time[2]);
                        warp.seconds = Integer.parseInt(time[3]);
                    }

                    if (data[11].equals("user")) {
                        warp.global = false;
                    } else if (data[11].equals("global")) {
                        warp.global = true;
                    }

                    if (data.length > 12) {
                        warp.setButtonsOld(data[12]);
                    }

                    //Update outdated save files
                    if (data[4].endsWith("~NETHER")) {
                        data[4].replace("~NETHER", "");
                    }

                    //Load the location data if the World is loaded
                    warp.world = data[4];
                    warp.x = Double.parseDouble(data[5]);
                    warp.y = Double.parseDouble(data[6]);
                    warp.z = Double.parseDouble(data[7]);
                    warp.pitch = Float.parseFloat(data[8]);
                    warp.yaw = Float.parseFloat(data[9]);

                    addWarp(warp);
                } catch (Exception loadFailed) {
                    logger.severe("Load failed, Errored line:");
                    logger.severe(line);
                    loadFailed.printStackTrace();
                }
            }
        } catch (Exception ex) {
            logger.severe("Unexpected error");
            ex.printStackTrace();
        } finally {
            try {
                bReader.close();
            } catch (Exception e) {
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
                value = value.concat("; "+button.toString());
            }
            if (!value.isEmpty()) {
                value = value.substring(2);
            }
            p.setProperty("ButtonsData", value);

            //Write the Warp Properties to file
            fos = new FileOutputStream(dataFolder+"/Warps/"+warp.name+".properties");
            p.store(fos, null);
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
