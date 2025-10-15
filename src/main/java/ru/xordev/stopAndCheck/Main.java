package ru.xordev.stopAndCheck;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xordev.stopAndCheck.commands.CheckerCommand;
import ru.xordev.stopAndCheck.handlers.CheckerEventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            PluginManager pm = getServer().getPluginManager();

            pm.addPermission(new Permission("sac.command", "Access to /sac command", PermissionDefault.OP));
            pm.addPermission(new Permission("sac.immunity", "Deny send player to checks", PermissionDefault.OP));
            pm.addPermission(new Permission("sac.help", "Access to /help command", PermissionDefault.OP));

            Permission moderPerm = new Permission("sac.moder.*", "Allow all moderator commands", PermissionDefault.OP);
            moderPerm.getChildren().put("sac.moder.reload", true);
            moderPerm.getChildren().put("sac.moder.check", true);
            moderPerm.getChildren().put("sac.moder.free", true);
            moderPerm.getChildren().put("sac.moder.info", true);

            pm.addPermission(moderPerm);

            pm.registerEvents(new CheckerEventHandler(this), this);
            getServer().getPluginCommand("sac").setExecutor(new CheckerCommand(this));

            getServer().getLogger().log(Level.INFO," ");
            getServer().getLogger().log(Level.INFO,"-| StopAndCheck 1.0beta by XOR |-");
            getServer().getLogger().log(Level.INFO,"All releases here: https://github.com/justyXOR/StopAndCheck");
            getServer().getLogger().log(Level.INFO," ");
            getServer().getLogger().log(Level.INFO,"Plugin is ready to work!");
            getServer().getLogger().log(Level.INFO," ");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE,"Error when enabling plugin!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Cleaning up metadata...");

        for (Player player : getServer().getOnlinePlayers()) {
            removeMetadataByPrefix(player, "sac_");
            player.sendTitle("", "", 0, 0, 10);
        }

        getLogger().info("Cleaning up permissions...");

        unregisterPermissions();

        getLogger().info("See you next time!");
    }

    private void unregisterPermissions() {
        PluginManager pm = getServer().getPluginManager();
        String[] permissionsToRemove = {
                "sac.command", "sac.immunity", "sac.help",
                "sac.moder.reload", "sac.moder.check", "sac.moder.free", "sac.moder.info",
                "sac.moder.*"
        };

        for (String permName : permissionsToRemove) {
            Permission perm = pm.getPermission(permName);
            if (perm != null) {
                pm.removePermission(perm);
            }
        }
    }

    private void removeMetadataByPrefix(Player player, String prefix) {
        List<MetadataValue> metadata = player.getMetadata(prefix);
        for (MetadataValue meta : new ArrayList<>(player.getMetadata(prefix))) {
            if (meta.getOwningPlugin().equals(this)) {
                player.removeMetadata(prefix, this);
            }
        }
    }
}
