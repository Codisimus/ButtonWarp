package com.codisimus.plugins.buttonwarp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import org.bukkit.block.Block;

/**
 * Holds ButtonWarp data and is used to load/save data
 * 
 * @author Codisimus
 */
public class SaveSystem {
    public static LinkedList<Warp> warps = new LinkedList<Warp>();

    /**
     * Loads Serializable data
     * 
     */
    public static void load() {
        String line = "";
        
        try {
            try {
                ObjectInputStream objectIn = null;

                objectIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
                    "plugins/ButtonWarp/warps.dat")));

                warps = (LinkedList<Warp>)objectIn.readObject();
                
                objectIn.close();
                
                return;
            }
            catch (Exception e) {
                System.out.println("[ButtonWarp] Loading outdated file.");
            }
            
            //Open save file in BufferedReader
            new File("plugins/ButtonWarp").mkdir();
            new File("plugins/ButtonWarp/ButtonWarp.save").createNewFile();
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
                    warp.setButtons(data[12]);
                
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
                save();
            }
        }
        catch (Exception loadFailed) {
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
        try {
            ObjectOutputStream objectOut = new ObjectOutputStream(new BufferedOutputStream(
                new FileOutputStream("plugins/ButtonWarp/warps.dat")));

            objectOut.writeObject(warps);
            
            objectOut.close();
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
