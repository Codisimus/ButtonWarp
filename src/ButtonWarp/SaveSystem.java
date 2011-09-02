
package ButtonWarp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;

/**
 *
 * @author Codisimus
 */
public class SaveSystem {
    private static LinkedList<Warp> warps = new LinkedList<Warp>();
    private static boolean save = true;

    /**
     * Reads save file to load ButtonWarp data
     * Saving is turned off if an error occurs
     */
    public static void loadFromFile() {
        BufferedReader bReader = null;
        String line = "";
        try {
            new File("plugins/ButtonWarp").mkdir();
            new File("plugins/ButtonWarp/ButtonWarp.save").createNewFile();
            bReader = new BufferedReader(new FileReader("plugins/ButtonWarp/ButtonWarp.save"));
            while ((line = bReader.readLine()) != null) {
                String[] split = line.split(";");
                String name = split[0];
                String msg = split[1];
                int amount = Integer.parseInt(split[2]);
                String source = split[3];
                String time = split[10];
                String type = split[11];
                String users = "";
                if (split.length > 12)
                    users = split[12];
                Location location = null;
                Warp warp = null;
                try {
                    double x = Double.parseDouble(split[5]);
                    double y = Double.parseDouble(split[6]);
                    double z = Double.parseDouble(split[7]);
                    float pitch = Float.parseFloat(split[8]);
                    float yaw = Float.parseFloat(split[9]);
                    Environment environment = Environment.NORMAL;
                    if (split[4].endsWith("~NETHER")) {
                        environment = Environment.NETHER;
                        split[4] = split[4].replace("~NETHER", "");
                    }
                    World world = ButtonWarp.server.createWorld(split[4], environment);
                    location = new Location(world, x, y, z);
                    location.setPitch(pitch);
                    location.setYaw(yaw);
                    warp = new Warp(name, msg, location, amount, source, time, type, users);
                }
                catch (Exception e) {
                    warp = new Warp(name, msg, null, amount, source, time, type, users);
                }
                warps.add(warp);
            }
        }
        catch (Exception e) {
            save = false;
            System.out.println("[ButtonWarp] Load failed, saving turned off to prevent loss of data");
            System.out.println("[ButtonWarp] Errored line: "+line);
            e.printStackTrace();
        }
    }

    /**
     * Writes data to save file
     * Old file is overwritten
     */
    protected static void save() {
        //cancels if saving is turned off
        if (!save)
            return;
        BufferedWriter bWriter = null;
        try {
            bWriter = new BufferedWriter(new FileWriter("plugins/ButtonWarp/ButtonWarp.save"));
            for(Warp warp : warps) {
                bWriter.write(warp.name.concat(";"));
                bWriter.write(warp.msg.concat(";"));
                bWriter.write(Integer.toString(warp.amount).concat(";"));
                bWriter.write(warp.source.concat(";"));
                Location location = warp.sendTo;
                if (location == null) {
                    bWriter.write(";;;;;;");
                }
                else {
                    World world = location.getWorld();
                    String name = world.getName();
                    if (world.getEnvironment().equals(Environment.NETHER))
                        name = name.concat("~NETHER");
                    bWriter.write(name+";");
                    bWriter.write(location.getX()+";");
                    bWriter.write(location.getY()+";");
                    bWriter.write(location.getZ()+";");
                    bWriter.write(location.getPitch()+";");
                    bWriter.write(location.getYaw()+";");
                }
                bWriter.write(warp.resetTime.concat(";"));
                bWriter.write(warp.resetType.concat(";"));
                bWriter.write(warp.restrictedUsers.concat(";"));
                bWriter.newLine();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                bWriter.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
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
        for(Warp warp : warps) {
            if (warp.restrictedUsers.contains(block))
                return warp;
        }
        return null;
    }

    /**
     * Adds the Warp to the LinkedList of saved Warps
     * 
     * @param warp The Warp to be added
     */
    protected static void addWarp(Warp warp) {
        try {
            warps.add(warp);
            save();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the Warp from the LinkedList of saved Warps
     * 
     * @param warp The Warp to be removed
     */
    protected static void removeWarp(Warp warp){
        try {
            warps.remove(warp);
            save();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
