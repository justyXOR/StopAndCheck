package ru.xordev.stopAndCheck.commands;

import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.xordev.stopAndCheck.Utils;

import java.util.List;
import java.util.logging.Level;

public class CheckerCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;

    public CheckerCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player sender = (Player) commandSender;

        if (!(sender.hasPermission("sac.command"))) {
            sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.moderator.no-permission")));
            return true;
        }

        if (strings.length == 0 || strings[0].equalsIgnoreCase("help")) {
            send_help(sender);
            return true;
        }

        if (strings[0].equalsIgnoreCase("check")) {
            if (!(commandSender instanceof Player)) {
                plugin.getServer().getLogger().log(Level.INFO, "Console can't check player!");
                return true;
            }
            if (!(sender.hasPermission("sac.moder.check"))) {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.moderator.no-permission")));
                return true;
            }

            if (strings.length < 2) {
                sender.sendMessage(Utils.color("&c/sac check <player>"));
                return true;
            }

            String playername = strings[1];
            Player player = plugin.getServer().getPlayer(playername);

            if (player == null) {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.moderator.player-offline")));
                return true;
            }

            if (isPlayerOnCheck(player)) {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.moderator.already-check")));
                return true;
            }

            player.setMetadata("sac_oncheck", new FixedMetadataValue(plugin, true));
            player.setMetadata("sac_check_moderator", new FixedMetadataValue(plugin, sender.getUniqueId().toString()));
            player.setMetadata("sac_beforecheck_pos", new FixedMetadataValue(plugin, player.getLocation()));

            sender.setMetadata("sac_check_player", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

            String title = Utils.color(plugin.getConfig().getString("messages.player.check-title", "Проверка на читы"));
            String subtitle = Utils.color(plugin.getConfig().getString("messages.player.check-subtitle", "Инструкции в чате"));

            player.teleport(sender.getLocation());

            player.sendTitle(title, subtitle, 10, 999999, 0);

            List<String> instructions = plugin.getConfig().getStringList("messages.player.check-chatmsg");
            instructions.forEach(line -> {
                String formatted = line
                        .replace("{moderator}", sender.getName());
                player.sendMessage(Utils.color(formatted));
            });

            sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.moderator.successful-called").replace("{player}", playername)));
        } else if (strings[0].equalsIgnoreCase("free")) {
            if (!(sender.hasPermission("sac.moder.free"))) {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.moderator.no-permission")));
                return true;
            }

            if (strings.length < 2) {
                sender.sendMessage(Utils.color("&c/sac free <player>"));
                return true;
            }

            String playername = strings[1];
            Player player = plugin.getServer().getPlayer(playername);

            if (player == null) {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.moderator.player-offline")));
                return true;
            }

            if (!(isPlayerOnCheck(player))) {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.moderator.already-free").replace("{player}", player.getName())));
                return true;
            }

            player.removeMetadata("sac_oncheck", plugin);
            player.removeMetadata("sac_check_moderator", plugin);

            sender.removeMetadata("sac_check_player", plugin);

            Location oldPos = (Location) player.getMetadata("sac_beforecheck_pos").get(0).value();

            player.removeMetadata("sac_beforecheck_pos", plugin);

            player.teleport(oldPos);

            player.sendTitle("", "", 0, 0, 10);

            player.sendMessage(Utils.color(plugin.getConfig().getString("messages.player.free-msg").replace("{moderator}", sender.getName())));
            sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.moderator.successful-free").replace("{player}", playername)));
        } else if (strings[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("sac.moder.reload")) {
                try {
                    plugin.reloadConfig();
                    if (commandSender instanceof ConsoleCommandSender) {
                        plugin.getLogger().log(Level.INFO, "[StopAndCheck] Successful config reload");
                    } else {
                        sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.cfg-reload")));
                    }
                } catch (Exception err) {
                    if (commandSender instanceof ConsoleCommandSender) {
                        plugin.getLogger().log(Level.SEVERE, "[StopAndCheck] Error when reloading plugin!");
                        plugin.getLogger().log(Level.SEVERE, err.getMessage());
                    } else {
                        sender.sendMessage(Utils.color("&c&lError when reloading plugin: &7" + err.getMessage()));
                    }
                    return true;
                }
            } else {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("messages.moderator.no-permission")));
                return true;
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }

    private void send_help(Player ply) {
        List<String> help_text = plugin.getConfig().getStringList("messages.player.help");
        help_text.forEach(line -> {
            ply.sendMessage(Utils.color(line));
        });
    }

    private boolean isPlayerOnCheck(Player player) {
        if (!player.hasMetadata("sac_oncheck")) {
            return false;
        }

        try {
            List<MetadataValue> metadata = player.getMetadata("sac_oncheck");
            if (metadata.isEmpty()) {
                return false;
            }
            return metadata.get(0).asBoolean();
        } catch (Exception e) {
            return false;
        }
    }
}
