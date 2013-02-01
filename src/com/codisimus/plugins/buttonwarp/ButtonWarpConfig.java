package com.codisimus.plugins.buttonwarp;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class ButtonWarpConfig {
    private static Properties p;

    public static void load() {
        //Load Config settings
        FileInputStream fis = null;
        try {
            //Copy the file from the jar if it is missing
            File file = new File(ButtonWarp.dataFolder + "/config.properties");
            if (!file.exists()) {
                ButtonWarp.plugin.saveResource("config.properties", true);
            }

            //Load config file
            p = new Properties();
            fis = new FileInputStream(file);
            p.load(fis);

            ButtonWarpListener.delay = loadInt("WarpDelay", 0);

            ButtonWarp.defaultTakeItems = loadBool("DefaultCanTakeItems", true);
            ButtonWarp.defaultRestricted = loadBool("DefaultRestricted", false);
            ButtonWarp.defaultMax = loadInt("DefaultMaxWarpsPerReset", 1);

            String[] defaultResetTime = loadString("DefaultResetTime", "0'0'0'0").split("'");
            ButtonWarp.defaultDays = Integer.parseInt(defaultResetTime[0]);
            ButtonWarp.defaultHours = Integer.parseInt(defaultResetTime[1]);
            ButtonWarp.defaultMinutes = Integer.parseInt(defaultResetTime[2]);
            ButtonWarp.defaultSeconds = Integer.parseInt(defaultResetTime[3]);

            ButtonWarpCommand.multiplier = loadInt("CommandWarpMultiplier", 5);

            Warp.log = loadBool("LogWarps", false);
            Warp.broadcast = loadBool("BroadcastWarps", false);

            Warp.sound = loadBool("EnderManSoundWhenWarping", true);

            String string = "PLUGIN CONFIG MUST BE REGENERATED!";
            ButtonWarpMessages.broadcast = loadString("WarpUsedBroadcast", string);
            ButtonWarpMessages.permission = loadString("PermissionMessage", string);
            ButtonWarpMessages.insufficentFunds = loadString("InsufficientFundsMessage", string);
            ButtonWarpMessages.sourceInsufficentFunds = loadString("SourceInsufficientFundsMessage", string);
            ButtonWarpMessages.delay = loadString("WarpDelayMessage", string);
            ButtonWarpMessages.alreadyWarping = loadString("AlreadyWarpingMessage", string);
            ButtonWarpMessages.cancel = loadString("WarpCancelMessage", string);
            ButtonWarpMessages.cannotUseWarps = loadString("CannotUseWarpsMessage", string);
            ButtonWarpMessages.noAccess = loadString("NoAccessMessage", string);
            ButtonWarpMessages.cannotTakeItems = loadString("CannotTakeItemsMessage", string);
            ButtonWarpMessages.cannotTakeArmor = loadString("CannotTakeArmorMessage", string);
            ButtonWarpMessages.worldMissing = loadString("WorldMissingMessage", string);
            ButtonWarpMessages.cannotHaveAnotherReward = loadString("CannotHaveAnotherRewardMessage", string);
            ButtonWarpMessages.cannotUseAgain = loadString("CannotUseAgainMessage", string);
            ButtonWarpMessages.timeRemainingReward = loadString("TimeRemainingRewardMessage", string);
            ButtonWarpMessages.timeRemainingUse = loadString("TimeRemainingUseMessage", string);

            ButtonWarpMessages.formatAll();
        } catch (Exception missingProp) {
            ButtonWarp.logger.severe("Failed to load ButtonWarp Config");
            missingProp.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Loads the given key and prints an error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    private static String loadString(String key, String defaultString) {
        if (p.containsKey(key)) {
            return p.getProperty(key);
        } else {
            ButtonWarp.logger.severe("Missing value for " + key);
            ButtonWarp.logger.severe("Please regenerate the config.properties file (delete the old file to allow a new one to be created)");
            ButtonWarp.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultString;
        }
    }

    /**
     * Loads the given key and prints an error if the key is not an Integer
     *
     * @param key The key to be loaded
     * @return The Integer value of the loaded key
     */
    private static int loadInt(String key, int defaultValue) {
        String string = loadString(key, null);
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            ButtonWarp.logger.severe("The setting for " + key + " must be a valid integer");
            ButtonWarp.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultValue;
        }
    }

    /**
     * Loads the given key and prints an error if the key is not a boolean
     *
     * @param key The key to be loaded
     * @return The boolean value of the loaded key
     */
    private static boolean loadBool(String key, boolean defaultValue) {
        String string = loadString(key, null);
        try {
            return Boolean.parseBoolean(string);
        } catch (Exception e) {
            ButtonWarp.logger.severe("The setting for " + key + " must be 'true' or 'false' ");
            ButtonWarp.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultValue;
        }
    }
}
