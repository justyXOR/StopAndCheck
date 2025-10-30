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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CheckerCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final Utils utils;

    public CheckerCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.utils = new Utils(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player sender;

        if (commandSender instanceof Player) {
            sender = (Player) commandSender;
        } else {
            sender = null;
        }

        if (sender != null && !(sender.hasPermission("sac.command"))) {
            sender.sendMessage(utils.get_str("messages.moderator.no-permission", sender, null));
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
                sender.sendMessage(utils.get_str("messages.moderator.no-permission", sender, null));
                return true;
            }
            if (strings.length < 2) {
                sender.sendMessage(utils.get_str("messages.usage", sender, null) + " /sac check <player>");
                return true;
            }

            String playername = strings[1];
            Player player = plugin.getServer().getPlayer(playername);

            if (player == null) {
                sender.sendMessage(utils.get_str("messages.moderator.player-offline", sender, null));
                return true;
            }

            if (player == sender) {
                sender.sendMessage(utils.get_str("messages.moderator.self-check", sender, player));
                return true;
            }

            if (player.hasMetadata("sac_check_player")) {
                sender.sendMessage(utils.get_str("messages.moderator.is-checking", player, sender));
                return true;
            }

            if (isPlayerOnCheck(player)) {
                sender.sendMessage(utils.get_str("messages.moderator.already-check", sender, player));
                return true;
            }

            if (player.hasPermission("sac.immunity")) {
                sender.sendMessage(utils.get_str("messages.moderator.player-has-perm", sender, player));
                return true;
            }

            player.setMetadata("sac_oncheck", new FixedMetadataValue(plugin, true));
            player.setMetadata("sac_check_moderator", new FixedMetadataValue(plugin, sender.getUniqueId().toString()));

            sender.setMetadata("sac_check_player", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

            String title = utils.get_str("messages.player.check-title", sender, player);
            String subtitle = utils.get_str("messages.player.check-subtitle", sender, player);

            if (plugin.getConfig().getBoolean("check.teleport.enabled")) {
                player.setMetadata("sac_beforecheck_pos", new FixedMetadataValue(plugin, player.getLocation()));

                String coords = plugin.getConfig().getString("check.teleport.coordinates", "0 0 0 0 0");
                String wrld = plugin.getConfig().getString("check.teleport.world", "world");

                Location loc = utils.parse_location(coords, wrld, " ");

                sender.setMetadata("sac_beforecheck_pos", new FixedMetadataValue(plugin, sender.getLocation()));

                player.teleport(loc);
                sender.teleport(loc);
            }

            player.sendTitle(title, subtitle, 10, 999999, 0);

            List<String> instructions = utils.get_str_list("messages.player.check-chatmsg", sender, player);
            instructions.forEach(player::sendMessage);

            sender.sendMessage(utils.get_str("messages.moderator.successful-called", sender, player));
        } else if (strings[0].equalsIgnoreCase("free")) {
            if (sender != null && !(sender.hasPermission("sac.moder.free"))) {
                sender.sendMessage(utils.get_str("messages.moderator.no-permission", null, null));
                return true;
            }

            if (strings.length < 2) {
                utils.sendMessage(commandSender, utils.get_str("messages.usage", sender, null) + " /sac free <player>");
                return true;
            }

            String playername = strings[1];
            Player player = plugin.getServer().getPlayer(playername);

            if (player == null) {
                utils.sendMessage(commandSender, utils.get_str("messages.moderator.player-offline", sender, null));
                return true;
            }

            if (player == sender) {
                utils.sendMessage(commandSender, utils.get_str("messages.moderator.self-free", sender, player));
                return true;
            }

            if (!(isPlayerOnCheck(player))) {
                utils.sendMessage(commandSender, utils.get_str("messages.moderator.already-free", sender, player));
                return true;
            }

            player.removeMetadata("sac_oncheck", plugin);

            UUID playerModeratorUuid = UUID.fromString(player.getMetadata("sac_check_moderator").get(0).asString());
            player.removeMetadata("sac_check_moderator", plugin);

            Player playerModerator = (Player) plugin.getServer().getPlayer(playerModeratorUuid);

            playerModerator.removeMetadata("sac_check_player", plugin);

            player.setAllowFlight(false);

            if (plugin.getConfig().getBoolean("check.teleport.enabled")) {
                Location oldPos = (Location) player.getMetadata("sac_beforecheck_pos").get(0).value();
                Location oldModPos = (Location) Objects.requireNonNull(sender).getMetadata("sac_beforecheck_pos").get(0).value();

                player.removeMetadata("sac_beforecheck_pos", plugin);
                sender.removeMetadata("sac_beforecheck_pos", plugin);

                player.teleport(oldPos);
                sender.teleport(oldModPos);
            }

            player.sendTitle("", "", 0, 0, 10);

            player.sendMessage(utils.get_str("messages.player.free-msg", sender, player));
            utils.sendMessage(commandSender, utils.get_str("messages.moderator.successful-free", sender, player));
        } else if (strings[0].equalsIgnoreCase("reload")) {
            if (commandSender instanceof ConsoleCommandSender || sender.hasPermission("sac.moder.reload")) {
                try {
                    plugin.reloadConfig();
                    if (commandSender instanceof ConsoleCommandSender) {
                        plugin.getLogger().log(Level.INFO, "[StopAndCheck] Successful config reload");
                    } else {
                        sender.sendMessage(utils.get_str("messages.cfg-reload", sender, null));
                    }
                } catch (Exception err) {
                    if (commandSender instanceof ConsoleCommandSender) {
                        plugin.getLogger().log(Level.SEVERE, "[StopAndCheck] Error when reloading plugin!");
                        plugin.getLogger().log(Level.SEVERE, err.getMessage());
                    } else {
                        sender.sendMessage(utils.color("&c&lError when reloading plugin: &7" + err.getMessage()));
                    }
                    return true;
                }
            } else {
                sender.sendMessage(utils.get_str("messages.moderator.no-permission", sender, null));
                return true;
            }
        } else if (strings[0].equalsIgnoreCase("info")) {
            if (sender != null && !sender.hasPermission("sac.moder.free")) {
                sender.sendMessage(utils.get_str("messages.moderator.no-permission", sender, null));
                return true;
            }

            if (strings.length < 2) {
                utils.sendMessage(commandSender, utils.get_str("messages.usage", sender, null) + " /sac info <player>");
                return true;
            }

            String playername = strings[1];
            Player player = (Player) plugin.getServer().getPlayer(playername);

            if (player == null) {
                utils.sendMessage(commandSender, utils.get_str("messages.moderator.player-offline", sender, null));
                return true;
            }

            List<String> lines = utils.get_str_list("messages.moderator.info", sender, player);

            String status = plugin.getConfig().getString("messages.moderator.status-free", "Free");
            String checking_player = plugin.getConfig().getString("messages.moderator.none", "&7No");
            String checking_moderator = plugin.getConfig().getString("messages.moderator.none", "&7No");

            if (player.hasMetadata("sac_oncheck") && player.getMetadata("sac_oncheck").get(0).asBoolean()) {
                status = plugin.getConfig().getString("messages.moderator.status-oncheck", "On check");
                UUID mod_uuid = UUID.fromString(player.getMetadata("sac_check_moderator").get(0).asString());
                checking_moderator = Objects.requireNonNull(plugin.getServer().getPlayer(mod_uuid)).getName();
                checking_player = plugin.getConfig().getString("messages.moderator.none", "&7No");
            } else if (player.hasMetadata("sac_oncheck") && !player.getMetadata("sac_oncheck").get(0).asBoolean()) {
                status = plugin.getConfig().getString("messages.moderator.status-free", "Free");
                checking_moderator = plugin.getConfig().getString("messages.moderator.none", "&7No");
                checking_player = plugin.getConfig().getString("messages.moderator.none", "&7No");
            }

            if (player.hasMetadata("sac_check_player") && !player.getMetadata("sac_cheking_player").get(0).asString().isEmpty()) {
                status = plugin.getConfig().getString("messages.moderator.status-checking", "Checking");
                checking_moderator = plugin.getConfig().getString("messages.moderator.none", "&7No");
                UUID ply_uuid = UUID.fromString(player.getMetadata("sac_check_player").get(0).asString());
                checking_player = Objects.requireNonNull(plugin.getServer().getPlayer(ply_uuid)).getName();
            }

            for (String line : lines) {
                String fline = utils.color(line
                        .replace("{info_status}", status)
                        .replace("{checking_player}", checking_player)
                        .replace("{checking_moderator}", checking_moderator));

                utils.sendMessage(commandSender, fline);
            }
        } else {
            send_help(commandSender);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1) {
            return Stream.of("check", "free", "info", "reload", "help")
                    .filter(action -> action.startsWith(strings[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (strings.length == 2) {
            Set<String> commands_with_players = Set.of("check", "free", "info");

            if (commands_with_players.contains(strings[0].toLowerCase())) {
                return null;
            }
            return List.of();
        }

        return List.of();
    }

    private void send_help(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        List<String> help_text = utils.get_str_list("messages.player.help", player, null);

        for (String line : help_text) {
            utils.sendMessage(sender, line);
        }
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
