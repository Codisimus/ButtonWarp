package com.codisimus.plugins.buttonwarp;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.entity.Player;

/**
 * Manages payment/rewards of using Warps
 * 
 * @author Codisimus
 */
public class Econ {
    public static Economy economy;

    /**
     * Charges a Player a given amount of money, which goes to a Player/Bank
     * 
     * @param player The name of the Player to be charged
     * @param source The Player/Bank that will receive the money
     * @param amount The amount that will be charged
     * @return True if the transaction was successful
     */
    public static boolean charge(Player player, String source, double amount) {
        String name = player.getName();
        
        //Cancel if the Player cannot afford the transaction
        if (!economy.has(name, amount)) {
            player.sendMessage("You need "+economy.format(amount)+" to activate that");
            return false;
        }
        
        economy.withdrawPlayer(name, amount);
        
        //Money does not go to anyone if the source is the server
        if (source.equalsIgnoreCase("server"))
            return true;
        
        if (source.startsWith("bank:"))
            //Send money to a bank account
            economy.bankDeposit(source.substring(5), amount);
        else
            //Send money to a Player
            economy.depositPlayer(source, amount);
        
        return true;
    }
    
    /**
     * Gives the given Player the given amount of money from the given source
     * 
     * @param player The Player being rewarded
     * @param source The Player/Bank that will give the reward
     * @param amount The amount that will be rewarded
     * @return false if source does not have enough money
     */
    public static void reward(Player player, String source, double amount) {
        //Charge the source if it is not the server
        if (!source.equalsIgnoreCase("server")) {
            //Check if money comes from a Player or a Bank
            if (source.startsWith("bank:")) {
                source = source.substring(5);
                
                //Cancel if the Bank does not have enough money
                if (economy.bankHas(source, amount).type != (ResponseType.SUCCESS)) {
                    player.sendMessage("The Source Bank has insufficient funds");
                    return;
                }
                
                economy.bankWithdraw(source, amount);
            }
            else {
                //Cancel if the Source Player cannot afford the transaction
                if (!economy.has(source, amount)) {
                    player.sendMessage("The Source Player has insufficient funds");
                    return;
                }
                
                economy.withdrawPlayer(source, amount);
            }
        }
        
        //Send money to the Player
        economy.depositPlayer(player.getName(), amount);
    }
    
    /**
     * Formats the money amount by adding the unit
     * 
     * @param amount The amount of money to be formatted
     * @return The String of the amount + currency name
     */
    public static String format(double amount) {
        return economy.format(amount).replace(".00", "");
    }
}
