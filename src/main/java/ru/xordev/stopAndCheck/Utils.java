package ru.xordev.stopAndCheck;

//import org.bukkit.ChatColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private final JavaPlugin plugin;

    public Utils(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String color(String msg) {
        if (msg == null) return null;
        msg = processHexColors(msg);
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        return msg;
    }

    public Location parse_location(@NotNull String coordinates, String world, String split_by) {
        String[] coordArray = coordinates.split(split_by);
        double x = Double.parseDouble(coordArray[0]);
        double y = Double.parseDouble(coordArray[1]);
        double z = Double.parseDouble(coordArray[2]);

        World to = Bukkit.getWorld(world);

        if (to == null) throw new IllegalArgumentException("World " + world + " not found");

        Location loc = new Location(to, x, y, z);

        if (coordArray.length >= 5) {
            float yaw = Float.parseFloat(coordArray[3].trim());
            float pitch = Float.parseFloat(coordArray[4].trim());
            loc.setYaw(yaw);
            loc.setPitch(pitch);
        }

        return loc;
    }

    private String processHexColors(String message) {
        try {
            Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
            Matcher matcher = pattern.matcher(message);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                String hex = matcher.group(1);
                ChatColor color = ChatColor.of("#" + hex);
                matcher.appendReplacement(buffer, color.toString());
            }
            matcher.appendTail(buffer);

            return buffer.toString();
        } catch (Exception e) {
            return message;
        }
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
