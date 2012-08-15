package com.codisimus.plugins.buttonwarp;

/**
 * Holds messages that are displayed to users of this plugin
 *
 * @author Codisimus
 */
public class ButtonWarpMessages {
    static String broadcast;
    static String permission;
    static String insufficentFunds;
    static String sourceInsufficentFunds;
    static String delay;
    static String cancel;
    static String cannotUseWarps;
    static String noAccess;
    static String cannotTakeItems;
    static String cannotTakeArmor;
    static String worldMissing;
    static String cannotHaveAnotherReward;
    static String cannotUseAgain;
    static String timeRemainingReward;
    static String timeRemainingUse;
    
    /**
     * Formats all PhatLoots messages
     * 
     */
    static void formatAll() {
        broadcast = format(broadcast);
        permission = format(permission);
        insufficentFunds = format(insufficentFunds);
        sourceInsufficentFunds = format(sourceInsufficentFunds);
        delay = format(delay);
        cancel = format(cancel);
        cannotUseWarps = format(cannotUseWarps);
        noAccess = format(noAccess);
        cannotTakeItems = format(cannotTakeItems);
        cannotTakeArmor = format(cannotTakeArmor);
        worldMissing = format(worldMissing);
        cannotHaveAnotherReward = format(cannotHaveAnotherReward);
        cannotUseAgain = format(cannotUseAgain);
        timeRemainingReward = format(timeRemainingReward);
        timeRemainingUse = format(timeRemainingUse);
    }
    
    /**
     * Adds various Unicode characters and colors to a string
     * 
     * @param string The string being formated
     * @return The formatted String
     */
    static String format(String string) {
        return string.replace("&", "§").replace("<ae>", "æ").replace("<AE>", "Æ")
                .replace("<o/>", "ø").replace("<O/>", "Ø")
                .replace("<a>", "å").replace("<A>", "Å");
    }

    /**
     * Changes Unicode characters back
     *
     * @param string The string being unformated
     * @return The unformatted String
     */
    static String unformat(String string) {
        return string.replace("§", "&").replace("æ", "<ae>").replace("Æ", "<AE>")
                .replace("ø", "<o/>").replace("Ø", "<O/>")
                .replace("å", "<a>").replace("Å", "<A>");
    }
}
