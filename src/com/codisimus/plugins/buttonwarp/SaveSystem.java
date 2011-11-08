package com.codisimus.plugins.buttonwarp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import org.bukkit.block.Block;

/**
 * Holds ButtonWarp data and is used to load/save data
 * 
 * @author Codisimus
 */
public class SaveSystem {
    public static LinkedList<Warp> warps = new LinkedList<Warp>();
    public static boolean save = true;

    /**
     * Loads Warps from file
     * Saving is turned off if an error occurs
     */
    public static void load() {
        String line = "";

        try {
            File[] files = new File("plugins/ButtonWarp").listFiles();
            Properties p = new Properties();
            
            for (File file: files) {
                String name = file.getName();
                if (name.endsWith(".dat")) {
                    p.load(new FileInputStream(file));
                    
                    Warp warp = new Warp(name.substring(0, name.length() - 4), ButtonWarp.format(p.getProperty("Message")),
                            Double.parseDouble(p.getProperty("Amount")), p.getProperty("Source"));
                    
                    String[] location = p.getProperty("Location").split("'");
                    warp.world = location[0];
                    warp.x = Double.parseDouble(location[1]);
                    warp.y = Double.parseDouble(location[2]);
                    warp.z = Double.parseDouble(location[3]);
                    warp.pitch = Float.parseFloat(location[4]);
                    warp.yaw = Float.parseFloat(location[5]);
                    
                    String[] resetTime = p.getProperty("ResetTime").split("'");
                    warp.days = Integer.parseInt(resetTime[0]);
                    warp.hours = Integer.parseInt(resetTime[1]);
                    warp.minutes = Integer.parseInt(resetTime[2]);
                    warp.seconds = Integer.parseInt(resetTime[3]);
                    
                    String resetType = p.getProperty("ResetType");
                    if (resetType.equals("player"))
                        warp.global = false;
                    else if (resetType.equals("global"))
                        warp.global = true;
                    
                    String access = p.getProperty("Access");
                    if (!access.equals("public"))
                        warp.access.addAll(Arrays.asList(access.split(", ")));
                    
                    warp.setButtons(p.getProperty("ButtonsData"));
                
                    warps.add(warp);
                }
            }
            
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
            
            BufferedReader bReader = new BufferedReader(new FileReader("plugins/ButtonWarp/warps.save"));
            line = bReader.readLine(); //Skip version

            //Convert each line into data until all lines are read
            while ((line = bReader.readLine()) != null) {
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

                warps.add(warp);
            }

            bReader.close();
            save();
        }
        catch (Exception loadFailed) {
            save = false;
            System.err.println("[ButtonWarp] Load failed, saving turned off to prevent loss of data");
            System.err.println("[ButtonWarp] Errored line: "+line);
            loadFailed.printStackTrace();
        }
    }

    /**
     * Loads Warps from outdated save file
     *
     */
    public static void loadOld() {
        String line = "";

        try {
            //Open save file in BufferedReader
            File file = new File("plugins/ButtonWarp/ButtonWarp.save");
            if (file.exists())
                System.out.println("[ButtonWarp] Updating old save file");
            else
                return;
            BufferedReader bReader = new BufferedReader(new FileReader("plugins/ButtonWarp/ButtonWarp.save"));

            //Convert each line into data until all lines are read
            while ((line = bReader.readLine()) != null) {
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

                warps.add(warp);
            }

            bReader.close();
            save();
        }
        catch (Exception loadFailed) {
            save = false;
            System.err.println("[ButtonWarp] Load failed, saving turned off to prevent loss of data");
            System.err.println("[ButtonWarp] Errored line: "+line);
            loadFailed.printStackTrace();
        }
    }

    /**
     * Writes Serializable object to save file
     * Old file is overwritten
     */
    public static void save() {
        //Cancel if saving is turned off
        if (!save) {
            System.out.println("[ButtonWarp] Warning! Data is not being saved.");
            return;
        }
        
        try {
            Properties p = new Properties();
            for (Warp warp: warps) {
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
                
                p.store(new FileOutputStream("plugins/ButtonWarp/"+warp.name+".dat"), null);
            }
        }
        catch (Exception saveFailed) {
            System.err.println("[ButtonWarp] Save Failed!");
            saveFailed.printStackTrace();
        }
    }
    
    /**
     * Returns the Warp with the given name
     * 
     * @param name The name of the Warp you wish to find
     * @return The Warp with the given name or null if not found
     */
    public static Warp findWarp(String name) {
        //Iterate through all Warps to find the one with the given Name
        for(Warp warp : warps)
            if (warp.name.equals(name))
                return warp;
        
        //Return null because the Warp does not exist
        return null;
    }

    /**
     * Returns the Warp that contains the given Block
     * 
     * @param button The Block that is part of the Warp
     * @return The Warp that contains the given Block or null if not found
     */
    public static Warp findWarp(Block block) {
        //Iterate through all Warps to find the one with the given Block
        for(Warp warp : warps)
            for (Button button: warp.buttons)
                if (button.isBlock(block))
                    return warp;
        
        //Return null because the Warp does not exist
        return null;
    }
}
