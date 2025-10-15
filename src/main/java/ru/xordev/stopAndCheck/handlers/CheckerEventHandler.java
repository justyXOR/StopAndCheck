package ru.xordev.stopAndCheck.handlers;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xordev.stopAndCheck.Utils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CheckerEventHandler implements Listener {
    private final JavaPlugin plugin;

    public CheckerEventHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void checkPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();

        if (isPlayerOnCheck(player)) {
            if (!(plugin.getConfig().getBoolean("check.can-move"))) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void checkPlayerCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();

        if (isPlayerOnCheck(player)) {
            if (!(plugin.getConfig().getBoolean("check.can-command"))) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void checkPlayerDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();

        if (isPlayerOnCheck(player)) {
            if (!(plugin.getConfig().getBoolean("check.can-drop-items"))) {
                e.setCancelled(true);
            }
        }
    }



    @EventHandler
    public void checkPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        if (isPlayerOnCheck(player)) {
            String text = e.getMessage();
            UUID mod_uuid = UUID.fromString(player.getMetadata("sac_check_moderator").get(0).asString());

            Player moderator = plugin.getServer().getPlayer(mod_uuid);

            e.setCancelled(true);

            String playerMsg = Utils.color(plugin.getConfig().getString("messages.check-chat-tag") + "&7" + player.getName() + " ") + text;
            String targetMsg = Utils.color(plugin.getConfig().getString("messages.you-tag") + plugin.getConfig().getString("messages.check-chat-tag")) + text;

            moderator.sendMessage(playerMsg);
            player.sendMessage(targetMsg);
        } else if (player.hasMetadata("sac_check_player") && player.getMetadata("sac_check_player").get(0).value() != null) {
            String text = e.getMessage();
            UUID ply_uuid = UUID.fromString(player.getMetadata("sac_check_player").get(0).asString());

            Player ply = plugin.getServer().getPlayer(ply_uuid);

            e.setCancelled(true);

            String playerMsg = Utils.color(plugin.getConfig().getString("messages.check-chat-tag") + "&7" + player.getName() + ": ") + text;
            String targetMsg = Utils.color(plugin.getConfig().getString("messages.you-tag") + plugin.getConfig().getString("messages.check-chat-tag")) + text;

            ply.sendMessage(playerMsg);
            player.sendMessage(targetMsg);
        }
    }

    private void executeBan(Player player) {
        try {
            String command = plugin.getConfig().getString("check.on-disconnect.command", "ban {player} 30d Уход с проверки");
            String finalCommand = command.replace("{player}", player.getName());

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);

            plugin.getLogger().info(player.getName() + " leaved from check");

        } catch (Exception e) {
            plugin.getLogger().warning(e.getMessage());
        }
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent e) {
        Player quited_player = e.getPlayer();

        if (isPlayerOnCheck(quited_player)) {
            if (plugin.getConfig().getBoolean("check.on-disconnect.enabled", true)) {
                quited_player.removeMetadata("sac_oncheck", plugin);
                quited_player.removeMetadata("sac_check_moderator", plugin);
                quited_player.removeMetadata("sac_beforecheck_pos", plugin);

                quited_player.sendTitle("", "", 0, 0, 10);

                executeBan(quited_player);
            }
        } else if (quited_player.hasMetadata("sac_check_player") && quited_player.getMetadata("sac_check_player").get(0).asString() != "") {
            UUID moder_player_uuid = UUID.fromString(quited_player.getMetadata("sac_check_player").get(0).asString());
            Player player = plugin.getServer().getPlayer(moder_player_uuid);

            if (Objects.equals(quited_player.getUniqueId().toString(), moder_player_uuid.toString())) return;

            if (isPlayerOnCheck(player)) releasePlayer(player, quited_player);
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

    private void releasePlayer(Player player, Player moderator) {
        player.removeMetadata("sac_oncheck", plugin);
        player.removeMetadata("sac_check_moderator", plugin);

        moderator.removeMetadata("sac_check_player", plugin);

        player.sendTitle("", "", 0, 0, 10);

        player.sendMessage(Utils.color(plugin.getConfig().getString("messages.player.free-msg").replace("{moderator}", moderator.getName())));
    }
}
