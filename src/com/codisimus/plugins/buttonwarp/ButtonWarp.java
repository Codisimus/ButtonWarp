package com.codisimus.plugins.buttonwarp;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
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
    static Permission permission;
    static PluginManager pm;
    static Server server;
    private Properties p;
    static Plugin plugin;
    static int defaultDays;
    static int defaultHours;
    static int defaultMinutes;
    static int defaultSeconds;
    static boolean defaultTakeItems;
    static int defaultMax;
    private static HashMap<String, Warp> warps = new HashMap<String, Warp>();
    private static String dataFolder;

    @Override
    public void onDisable () {
    }

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
        pm = server.getPluginManager();
        plugin = this;
        
        File dir = this.getDataFolder();
        if (!dir.isDirectory())
            dir.mkdir();
        
        dataFolder = dir.getPath();
        
        dir = new File(dataFolder+"/Warps");
        if (!dir.isDirectory())
            dir.mkdir();
        
        //Load Config settings
        try {
            //Copy the file from the jar if it is missing
            File file = new File(dataFolder+"/config.properties");
            if (!file.exists())
                this.saveResource("config.properties", true);
            
            //Load config file
            p = new Properties();
            FileInputStream fis = new FileInputStream(file);
            p.load(fis);
            
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
            
            fis.close();
        }
        catch (Exception missingProp) {
            System.err.println("Failed to load ButtonWarp "+this.getDescription().getVersion());
            missingProp.printStackTrace();
        }
        
        //Find Permissions
        RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null)
            permission = permissionProvider.getProvider();
        
        //Find Economy
        RegisteredServiceProvider<Economy> economyProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)
            Econ.economy = economyProvider.getProvider();
        
        //Load Warps Data
        loadData();
        
        //Register Events
        pm.registerEvents(new ButtonWarpListener(), this);
        
        //Register the command found in the plugin.yml
        ButtonWarpCommand.command = (String)this.getDescription().getCommands().keySet().toArray()[0];
        getCommand(ButtonWarpCommand.command).setExecutor(new ButtonWarpCommand());
        
        System.out.println("ButtonWarp "+this.getDescription().getVersion()+" is enabled!");
    }

    /**
     * Loads the given key and prints an error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    private String loadValue(String key) {
        //Print an error if the key is not found
        if (!p.containsKey(key)) {
            System.err.println("[ButtonWarp] Missing value for "+key+" in config file");
            System.err.println("[ButtonWarp] Please regenerate config file");
        }
        
        return p.getProperty(key);
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
     * Adds various Unicode characters and colors to a string
     * 
     * @param string The string being formated
     * @return The formatted String
     */
    static String format(String string) {
        return string.replaceAll("&", "§").replaceAll("<ae>", "æ").replaceAll("<AE>", "Æ")
                .replaceAll("<o/>", "ø").replaceAll("<O/>", "Ø")
                .replaceAll("<a>", "å").replaceAll("<A>", "Å");
    }
    
    /**
     * Changes Unicode characters back
     * 
     * @param string The string being unformated
     * @return The unformatted String
     */
    private static String unformat(String string) {
        return string.replaceAll("§", "&").replaceAll("æ", "<ae>").replaceAll("Æ", "<AE>")
                .replaceAll("ø", "<o/>").replaceAll("Ø", "<O/>")
                .replaceAll("å", "<a>").replaceAll("Å", "<A>");
    }
    
    /**
     * Loads Warps from the save file
     * Saving is turned off if an error occurs
     */
    public static void loadData() {
        File[] files = plugin.getDataFolder().listFiles();

        //Organize files
        if (files != null)
            for (File file: files) {
                String name = file.getName();
                if (name.endsWith(".dat")) {
                    File dest = new File(dataFolder+"/Warps/"+name.substring(0, name.length() - 4)+".properties");
                    file.renameTo(dest);
                }
            }
        
        files = new File(dataFolder+"/Warps/").listFiles();
        
        //Load each .dat file
        for (File file: files) {
            String name = file.getName();
            if (name.endsWith(".properties")) {
                try {
                    //Load the Properties file for reading
                    Properties p = new Properties();
                    FileInputStream fis = new FileInputStream(file);
                    p.load(fis);

                    //Construct a new Warp using the file name and values of message, amount, and source
                    Warp warp = new Warp(name.substring(0, name.length() - 11), ButtonWarp.format(p.getProperty("Message")),
                            Double.parseDouble(p.getProperty("Amount")), p.getProperty("Source"));

                    //Set the Location data
                    String[] location = p.getProperty("Location").split("'");
                    warp.world = location[0];
                    warp.x = Double.parseDouble(location[1]);
                    warp.y = Double.parseDouble(location[2]);
                    warp.z = Double.parseDouble(location[3]);
                    warp.pitch = Float.parseFloat(location[4]);
                    warp.yaw = Float.parseFloat(location[5]);

                    //Set the reset time
                    String[] resetTime = p.getProperty("ResetTime").split("'");
                    warp.days = Integer.parseInt(resetTime[0]);
                    warp.hours = Integer.parseInt(resetTime[1]);
                    warp.minutes = Integer.parseInt(resetTime[2]);
                    warp.seconds = Integer.parseInt(resetTime[3]);

                    //Set the reset type
                    String resetType = p.getProperty("ResetType");
                    if (resetType.equals("player"))
                        warp.global = false;
                    else if (resetType.equals("global"))
                        warp.global = true;

                    //Convert the value of access to an Array of Strings
                    String access = p.getProperty("Access");
                    if (!access.equals("public"))
                        warp.access.addAll(Arrays.asList(access.split(", ")));

                    //Load the data of all the Buttons
                    warp.setButtons(p.getProperty("ButtonsData"));

                    warps.put(warp.name, warp);
                    fis.close();
                }
                catch (Exception loadFailed) {
                    System.err.println("[ButtonWarp] Failed to load "+name);
                    loadFailed.printStackTrace();
                }
            }
        }

        //End loading if at least one Warp was loaded
        if (!warps.isEmpty())
            return;

        //Open save file in BufferedReader
        File file = new File("plugins/ButtonWarp/warps.save");
        if (file.exists())
            System.out.println("[ButtonWarp] Updating old save file");
        else {
            loadOld();
            return;
        }
            
        try {
            BufferedReader bReader = new BufferedReader(new FileReader(file));
            String line = bReader.readLine(); //Skip version

            //Convert each line into data until all lines are read
            while ((line = bReader.readLine()) != null)
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
                        if (!warpData[8].contains("},  "))
                            warpData[8] = warpData[8].replaceAll("}, ", "},  ");

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

                            for (String user: users)
                                if ((index = user.indexOf('=')) != -1)
                                    button.users.put(user.substring(0, index), timeArray);

                            warp.buttons.add(button);
                        }
                    }

                    addWarp(warp);
                }
                catch (Exception loadFailed) {
                    System.err.println("[ButtonWarp] Load failed, Errored line:");
                    System.err.println("[ButtonWarp] "+line);
                    loadFailed.printStackTrace();
                }

            bReader.close();
        }
        catch (Exception ex) {
            System.err.println("[ButtonWarp] Unexpected error");
            ex.printStackTrace();
        }
    }

    /**
     * Loads Warps from outdated save file
     *
     */
    private static void loadOld() {
        try {
            //Open save file in BufferedReader
            File file = new File("plugins/ButtonWarp/ButtonWarp.save");
            if (file.exists())
                System.out.println("[ButtonWarp] Updating old save file");
            else
                return;
            BufferedReader bReader = new BufferedReader(new FileReader("plugins/ButtonWarp/ButtonWarp.save"));
            
            String line;

            //Convert each line into data until all lines are read
            while ((line = bReader.readLine()) != null)
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

                    if (data[11].equals("user"))
                        warp.global = false;
                    else if (data[11].equals("global"))
                        warp.global = true;

                    if (data.length > 12)
                        warp.setButtonsOld(data[12]);

                    //Update outdated save files
                    if (data[4].endsWith("~NETHER"))
                        data[4].replace("~NETHER", "");

                    //Load the location data if the World is loaded
                    warp.world = data[4];
                    warp.x = Double.parseDouble(data[5]);
                    warp.y = Double.parseDouble(data[6]);
                    warp.z = Double.parseDouble(data[7]);
                    warp.pitch = Float.parseFloat(data[8]);
                    warp.yaw = Float.parseFloat(data[9]);

                    addWarp(warp);
                }
                catch (Exception loadFailed) {
                    System.err.println("[ButtonWarp] Load failed, Errored line:");
                    System.err.println("[ButtonWarp] "+line);
                    loadFailed.printStackTrace();
                }

            bReader.close();
        }
        catch (Exception ex) {
            System.err.println("[ButtonWarp] Unexpected error");
            ex.printStackTrace();
        }
    }
    
    /**
     * Invokes save() method for each Warp
     *
     */
    public static void saveAll() {
        for (Warp warp: warps.values())
            saveWarp(warp);
    }

    /**
     * Writes the given Warp to its save file
     * If the file already exists, it is overwritten
     * 
     * @param warp The given Warp
     */
    static void saveWarp(Warp warp) {
        try {
            Properties p = new Properties();
            
            p.setProperty("Message", ButtonWarp.unformat(warp.msg));
            p.setProperty("Amount", String.valueOf(warp.amount));
            p.setProperty("Source", warp.source);
            p.setProperty("Location", warp.world+"'"+warp.x+"'"+warp.y+"'"+warp.z+"'"+warp.pitch+"'"+warp.yaw);
            p.setProperty("ResetTime", warp.days+"'"+warp.hours+"'"+warp.minutes+"'"+warp.seconds);

            if (warp.global)
                p.setProperty("ResetType", "global");
            else
                p.setProperty("ResetType", "player");

            if (warp.access.isEmpty())
                p.setProperty("Access", "public");
            else {
                String access = warp.access.toString();
                p.setProperty("Access", access.substring(1, access.length() - 1));
            }

            String value = "";
            for (Button button: warp.buttons)
                value = value.concat("; "+button.toString());
            if (!value.isEmpty())
                value = value.substring(2);
            p.setProperty("ButtonsData", value);

            //Write the Warp Properties to file
            FileOutputStream fos = new FileOutputStream(dataFolder+"/Warps/"+warp.name+".properties");
            p.store(fos, null);
            fos.close();
        }
        catch (Exception saveFailed) {
            System.err.println("[ButtonWarp] Save Failed!");
            saveFailed.printStackTrace();
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
     * 
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
        
        System.out.println("[ButtonWarp] reloaded");
        if (player != null)
            player.sendMessage("ButtonWarp reloaded");
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
     * @param button The Block that is part of the Warp
     * @return The Warp that contains the given Block or null if not found
     */
    public static Warp findWarp(Block block) {
        //Iterate through all Warps to find the one with the given Block
        for (Warp warp: warps.values())
            if (warp.findButton(block) != null)
                return warp;
        
        //Return null because the Warp does not exist
        return null;
    }
}