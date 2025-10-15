package ru.xordev.stopAndCheck;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class Utils {
    private final JavaPlugin plugin;

    public Utils(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
