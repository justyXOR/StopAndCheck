package ru.xordev.stopAndCheck.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xordev.stopAndCheck.Utils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CheckerEventHandler implements Listener {
    private final JavaPlugin plugin;
    private final Utils utils;

    public CheckerEventHandler(JavaPlugin plugin, Utils utils) {
        this.plugin = plugin;
        this.utils = utils;
    }

    @EventHandler
    public void checkPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();

        if (isPlayerOnCheck(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void checkPlayerCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();

        if (isPlayerOnCheck(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void checkPlayerDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();

        if (isPlayerOnCheck(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void checkPlayerBreakBlock(BlockBreakEvent e) {
        Player player = e.getPlayer();

        if (isPlayerOnCheck(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void checkPlayerDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player) {
            if (isPlayerOnCheck(player)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void checkPlayerAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player damager) {
            if (isPlayerOnCheck(damager)) {
                e.setCancelled(true);
                return;
            }
        }

        if (e.getEntity() instanceof Player player) {
            if (isPlayerOnCheck(player)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void checkPlayerUseItem(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (isPlayerOnCheck(player) && (
                e.getAction() == Action.RIGHT_CLICK_AIR ||
                        e.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            Material itemType = player.getInventory().getItemInMainHand().getType();
            if (itemType == Material.EGG || itemType == Material.SNOWBALL ||
                    itemType == Material.ENDER_PEARL || itemType == Material.SPLASH_POTION) {
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

            String playerMsg = utils.get_str("messages.check-chat-tag", player, null) + " &7" + player.getName() + ": &r" + text;
            String targetMsg = utils.get_str("messages.you-tag", player, null) + utils.get_str("messages.check-chat-tag", player, null) + " " + text;

            moderator.sendMessage(playerMsg);
            player.sendMessage(targetMsg);
        } else if (player.hasMetadata("sac_check_player") && player.getMetadata("sac_check_player").get(0).value() != null) {
            String text = e.getMessage();
            UUID ply_uuid = UUID.fromString(player.getMetadata("sac_check_player").get(0).asString());

            Player ply = plugin.getServer().getPlayer(ply_uuid);

            e.setCancelled(true);

            String playerMsg = utils.get_str("messages.check-chat-tag", player, null) + " &7" + player.getName() + ": &r" + text;
            String targetMsg = utils.get_str("messages.you-tag", player, null) + utils.get_str("messages.check-chat-tag", player, null) + " " + text;

            ply.sendMessage(playerMsg);
            player.sendMessage(targetMsg);
        }
    }

    private void executeBan(Player player, Player mod) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), utils.get_str("check.on-disconnect.command", mod, player));

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
                quited_player.sendTitle("", "", 0, 0, 10);
                quited_player.removeMetadata("sac_oncheck", plugin);

                UUID mod_uuid = UUID.fromString(quited_player.getMetadata("sac_check_moderator").get(0).asString());
                Player moderator = plugin.getServer().getPlayer(mod_uuid);

                quited_player.removeMetadata("sac_check_moderator", plugin);
                quited_player.removeMetadata("sac_beforecheck_pos", plugin);

                executeBan(quited_player, moderator);
            }
        } else if (quited_player.hasMetadata("sac_check_player") && quited_player.getMetadata("sac_check_player").get(0).asString() != "") {
            UUID moder_player_uuid = UUID.fromString(quited_player.getMetadata("sac_check_player").get(0).asString());
            Player player = plugin.getServer().getPlayer(moder_player_uuid);

            if (Objects.equals(quited_player.getUniqueId().toString(), moder_player_uuid.toString())) return;

            if (isPlayerOnCheck(Objects.requireNonNull(player))) releasePlayer(player, quited_player);
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

        player.sendMessage(utils.get_str("messages.player.free-msg", moderator, player));
    }
}
