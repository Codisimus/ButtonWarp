package com.codisimus.plugins.buttonwarp;

import com.codisimus.plugins.buttonwarp.register.payment.Method;
import com.codisimus.plugins.buttonwarp.register.payment.Method.MethodAccount;
import com.codisimus.plugins.buttonwarp.register.payment.Method.MethodBankAccount;
import org.bukkit.entity.Player;

/**
 * Manages payment/rewards of using Warps
 * Uses Nijikokun's Register API
 * 
 * @author Codisimus
 */
public class Register {
    public static String economy;
    public static Method econ;

    /**
     * Charges a Player a given amount of money, which goes to a Player/Bank
     * 
     * @param player The name of the Player to be charged
     * @param source The Player/Bank that will receive the money
     * @param price The amount that will be charged
     * @return True if the transaction was successful
     */
    public static boolean charge(String player, String source, double price) {
        MethodAccount account = econ.getAccount(player);
        
        //Cancel if the Player can not afford the transaction
        if (!account.hasEnough(price))
            return false;
        
        account.subtract(price);
        
        //Money does not go to anyone if the source is the server
        if (source.equalsIgnoreCase("server"))
            return true;
        
        if (source.startsWith("bank:"))
            //Send money to a bank account
            econ.getBankAccount(source.substring(5), null).add(price);
        else
            //Send money to a Player
            econ.getAccount(source).add(price);
        
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
                MethodBankAccount bankAccount = econ.getBankAccount(source.substring(5), null);
                
                //Cancel if the Bank does not have enough money
                if (!bankAccount.hasEnough(amount)) {
                    player.sendMessage("Bank has insufficient funds");
                    return;
                }
                
                bankAccount.subtract(amount);
            }
            else {
                MethodAccount account = econ.getAccount(source);
                
                //Cancel if the Player does not have enough money
                if (!account.hasEnough(amount)) {
                    player.sendMessage("Player has insufficient funds");
                    return;
                }
                
                account.subtract(amount);
            }
        }
        
        econ.getAccount(player.getName()).add(amount);
    }
    
    /**
     * Formats the money amount by adding the unit
     *
     * @param amount The amount of money to be formatted
     * @return The String of the amount + currency name
     */
    public static String format(double amount) {
        if (econ == null)
            return "No Economy is present";
        
        return econ.format(amount);
    }
}
