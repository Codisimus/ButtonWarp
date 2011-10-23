
package com.codisimus.plugins.buttonwarp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Holds ButtonWarp data and is used to load/save data
 * 
 * @author Cody
 */
public class SaveSystem {
    private static LinkedList<Warp> warps = new LinkedList<Warp>();
    private static boolean save = true;

    /**
     * Reads save file to load ButtonWarp data
     * Saving is turned off if an error occurs
     */
    public static void loadFromFile() {
        String line = "";
        
        try {
            //Open save file in BufferedReader
            new File("plugins/ButtonWarp").mkdir();
            new File("plugins/ButtonWarp/ButtonWarp.save").createNewFile();
            BufferedReader bReader = new BufferedReader(new FileReader("plugins/ButtonWarp/ButtonWarp.save"));

            //Convert each line into data until all lines are read
            while ((line = bReader.readLine()) != null) {
                String[] data = line.split(";");
                
                String name = data[0];
                String msg = data[1];
                int amount = Integer.parseInt(data[2]);
                String source = data[3];
                String time = data[10];
                String type = data[11];
                
                String users = "";
                if (data.length > 12)
                    users = data[12];
                
                Warp warp = new Warp(name, msg, amount, source, time, type, users);
                
                //Update outdated save files
                if (data[4].endsWith("~NETHER"))
                    data[4].replace("~NETHER", "");
                
                //Load the location data if the World is loaded
                World world = ButtonWarp.server.getWorld(data[4]);
                if (world != null) {
                    double x = Double.parseDouble(data[5]);
                    double y = Double.parseDouble(data[6]);
                    double z = Double.parseDouble(data[7]);
                    float pitch = Float.parseFloat(data[8]);
                    float yaw = Float.parseFloat(data[9]);
                    
                    Location location = new Location(world, x, y, z);
                    
                    location.setPitch(pitch);
                    location.setYaw(yaw);
                    
                    warp.sendTo = location;
                }
                
                warps.add(warp);
            }
        }
        catch (Exception loadFailed) {
            save = false;
            System.err.println("[ButtonWarp] Load failed, saving turned off to prevent loss of data");
            System.err.println("[ButtonWarp] Errored line: "+line);
            loadFailed.printStackTrace();
        }
    }
    
    /**
     * Reads save file to load Locations for given World
     * Saving is turned off if an error occurs
     */
    protected static void loadLocations(World world) {
        String line = "";
        
        try {
            //Open save file in BufferedReader
            new File("plugins/ButtonWarp").mkdir();
            new File("plugins/ButtonWarp/ButtonWarp.save").createNewFile();
            BufferedReader bReader = new BufferedReader(new FileReader("plugins/ButtonWarp/ButtonWarp.save"));
            
            String worldName = world.getName();
            
            while ((line = bReader.readLine()) != null) {
                String[] data = line.split(";");
                
                //Load the Location data if it is in the given World
                if (data[4].equals(worldName)) {
                    //Retrieve the warp to save the Location
                    Warp warp = findWarp(data[0]);
                    
                    double x = Double.parseDouble(data[5]);
                    double y = Double.parseDouble(data[6]);
                    double z = Double.parseDouble(data[7]);
                    float pitch = Float.parseFloat(data[8]);
                    float yaw = Float.parseFloat(data[9]);
                    
                    Location location = new Location(world, x, y, z);
                    
                    location.setPitch(pitch);
                    location.setYaw(yaw);
                    
                    warp.sendTo = location;
                }
            }
        }
        catch (Exception loadFailed) {
            save = false;
            System.err.println("[ButtonWarp] Load failed, saving turned off to prevent loss of data");
            System.err.println("[ButtonWarp] Errored line: "+line);
            loadFailed.printStackTrace();
        }
    }

    /**
     * Writes data to save file
     * Old file is overwritten
     */
    protected static void save() {
        //Cancel if saving is turned off
        if (!save)
            return;
        
        try {
            //Open save file for writing data
            BufferedWriter bWriter = new BufferedWriter(new FileWriter("plugins/ButtonWarp/ButtonWarp.save"));
            
            //Iterate through each Warp to write the data to the file
            for(Warp warp : warps) {
                //Write data in format "name;message;amount;source;world;x;y;z;pitch;yaw;resetTime;resetType;restrictedUsers;
                bWriter.write(warp.name.concat(";"));
                bWriter.write(warp.msg.concat(";"));
                bWriter.write(Integer.toString(warp.amount).concat(";"));
                bWriter.write(warp.source.concat(";"));
                
                Location location = warp.sendTo;
                if (location == null)
                    bWriter.write(";;;;;;");
                else {
                    World world = location.getWorld();
                    bWriter.write(world.getName()+";");
                    bWriter.write(location.getX()+";");
                    bWriter.write(location.getY()+";");
                    bWriter.write(location.getZ()+";");
                    bWriter.write(location.getPitch()+";");
                    bWriter.write(location.getYaw()+";");
                }
                
                bWriter.write(warp.resetTime.concat(";"));
                bWriter.write(warp.resetType.concat(";"));
                bWriter.write(warp.restrictedUsers.concat(";"));
                
                //Write each Warp on its own line
                bWriter.newLine();
            }
            bWriter.close();
        }
        catch (Exception saveFailed) {
            System.err.println("[ButtonWarp] Save Failed!");
            saveFailed.printStackTrace();
        }
    }
    
    /**
     * Returns the LinkedList of saved Warps
     * 
     * @return the LinkedList of saved Warps
     */
    public static LinkedList<Warp> getWarps() {
        return warps;
    }

    /**
     * Returns the Warp with the given name
     * 
     * @param name The name of the Warp you wish to find
     * @return The Warp with the given name or null if not found
     */
    public static Warp findWarp(String name) {
        for(Warp warp : warps)
            if (warp.name.equals(name))
                return warp;
        return null;
    }

    /**
     * Returns the Warp that contains the given Block
     * 
     * @param button The Block that is part of the Warp
     * @return The Warp that contains the given Block or null if not found
     */
    public static Warp findWarp(Block button) {
        String block = button.getWorld().getName()+","+button.getX();
        block = block.concat(","+button.getY()+","+button.getZ()+",");
        for(Warp warp : warps)
            if (warp.restrictedUsers.contains(block))
                return warp;
        return null;
    }

    /**
     * Adds the Warp to the LinkedList of saved Warps
     * 
     * @param warp The Warp to be added
     */
    protected static void addWarp(Warp warp) {
        warps.add(warp);
    }

    /**
     * Removes the Warp from the LinkedList of saved Warps
     * 
     * @param warp The Warp to be removed
     */
    protected static void removeWarp(Warp warp){
        warps.remove(warp);
    }
}
