package com.codisimus.plugins.buttonwarp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.LinkedList;
import org.bukkit.block.Block;

/**
 * Holds ButtonWarp data and is used to load/save data
 * 
 * @author Codisimus
 */
public class SaveSystem {
    public static LinkedList<Warp> warps = new LinkedList<Warp>();
    public static boolean save = true;
    public static int currentVersion = 1;

    /**
     * Loads Warps from file
     * 
     */
    public static void load() {
        String line = "";

        try {
            //Open save file in BufferedReader
            new File("plugins/ButtonWarp").mkdir();
            new File("plugins/ButtonWarp/warps.save").createNewFile();
            BufferedReader bReader = new BufferedReader(new FileReader("plugins/ButtonWarp/warps.save"));

            //Check the Version of the save file
            line = bReader.readLine();
            int version = Integer.parseInt(line.substring(8));
            if (version != currentVersion) {
                loadOld();
                return;
            }

            //Convert each line into data until all lines are read
            while ((line = bReader.readLine()) != null) {
                String[] warpData = line.split(";");

                Warp warp = new Warp(warpData[0], warpData[1], Double.parseDouble(warpData[2]), warpData[3]);

                //Load the location data of the Warp if it exists
                if (!(warpData[4] = warpData[4].substring(1, warpData[4].length() - 1)).isEmpty()) {
                    String[] location = warpData[4].split(".");
                    warp.world = location[0];
                    warp.x = Double.parseDouble(location[1]);
                    warp.y = Double.parseDouble(location[2]);
                    warp.z = Double.parseDouble(location[3]);
                    warp.pitch = Float.parseFloat(location[4]);
                    warp.yaw = Float.parseFloat(location[5]);
                }

                //Load the time data of the Warp
                String[] time = (warpData[5].substring(1, warpData[5].length() - 1)).split("'");
                warp.days = Integer.parseInt(time[0]);
                warp.hours = Integer.parseInt(time[1]);
                warp.minutes = Integer.parseInt(time[2]);
                warp.seconds = Integer.parseInt(time[3]);

                warp.global = Boolean.parseBoolean(warpData[6]);

                //Load the access groups of the Warp
                String[] groups = (warpData[7].substring(1, warpData[7].length() - 1)).split(", ");
                warp.access.addAll(Arrays.asList(groups));

                //Load the Buttons of the Warp
                int index, x, y, z;
                String[] buttons = (warpData[8].substring(1, warpData[8].length() - 1)).split(", ");
                for (String buttonData: buttons) {
                    index = buttonData.indexOf('{');

                    //Load the Block Location data of the Button
                    String[] blockData = buttonData.substring(0, index - 1).split(".");
                    x = Integer.parseInt(blockData[1]);
                    y = Integer.parseInt(blockData[1]);
                    z = Integer.parseInt(blockData[1]);
                    Button button = new Button(blockData[0], x, y, z);

                    //Load the HashMap of Users of the Button
                    String[] users = (buttonData.substring(index, buttonData.length() - 1)).split(", ");
                    for (String user: users) {
                        index = user.indexOf('=');
                        button.users.put(user.substring(0, index - 1), user.substring(index));
                    }

                    warp.buttons.add(button);
                }

                warps.add(warp);
            }

            bReader.close();
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
            System.out.println("[ButtonWarp] Updating old save file");

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
        try {
            //Cancel if saving is turned off
            if (!save) {
                System.out.println("[ButtonWarp] Warning! Data is not being saved.");
                return;
            }

            //Open save file for writing data
            BufferedWriter bWriter = new BufferedWriter(new FileWriter("plugins/ButtonWarp/warps.save"));

            //Write the current save file version on the first line
            bWriter.write("Version="+currentVersion);
            bWriter.newLine();

            //Write each Warp data on its owwn line
            for (Warp warp: warps) {
                bWriter.write(warp.toString());
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
