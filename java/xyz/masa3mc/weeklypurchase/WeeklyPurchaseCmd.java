package xyz.masa3mc.weeklypurchase;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class WeeklyPurchaseCmd implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        WeeklyPurchase plugin = JavaPlugin.getPlugin(WeeklyPurchase.class);

        if (command.getName().equalsIgnoreCase("weeklypurchase")) {
            if (!sender.hasPermission("weeklypurchase")) {
                sender.sendMessage("§c権限がありません。");
                return true;
            }
            plugin.setPurchase();
        }
        return true;
    }
}
