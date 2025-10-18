package ru.xordev.stopAndCheck;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Utils {
    private final JavaPlugin plugin;

    public Utils(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String get_str(String path, @Nullable Player moderator, @Nullable Player player) {
        String msg = plugin.getConfig().getString(path);

        if (moderator != null) {
            msg = msg.replace("{moderator}", moderator.getName());
        } else {
            msg = msg.replace("{moderator}", "&7CONSOLE&r");
        }

        if (player != null) {
            msg = msg.replace("{player}", player.getName());
        } else {
            msg = msg.replace("{player}", "&7???&r");
        }

        return color(msg);
    }

    public List<String> get_str_list(String path, @Nullable Player executor, @Nullable Player player) {
        List<String> msgs = plugin.getConfig().getStringList(path);
        List<String> result = new ArrayList<>();

        for (String msg : msgs) {
            if (executor != null) {
                msg = msg.replace("{executor}", executor.getName());
            } else {
                msg = msg.replace("{executor}", "&7CONSOLE&r");
            }

            if (player != null) {
                msg = msg.replace("{player}", player.getName());
            } else {
                msg = msg.replace("{player}", "&7???&r");
            }

            result.add(color(msg));
        }

        return result;
    }

    public void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            sender.sendMessage(color(message));
        } else if (sender instanceof ConsoleCommandSender) {
            plugin.getLogger().log(Level.INFO, message.replaceAll("§[0-9a-fk-or]", ""));
        }
    }

    public void sendMessage(CommandSender sender, String key, Player moderator, Player target) {
        String message = get_str(key, moderator, target);
        sendMessage(sender, color(message));
    }
}
