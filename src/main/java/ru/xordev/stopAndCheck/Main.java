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
            getServer().getLogger().log(Level.INFO,"[StopAndCheck] Plugin is ready to work!");
            getServer().getLogger().log(Level.INFO," ");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE,"[StopAndCheck] Error when enabling plugin!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("[StopAndCheck]  Cleaning up metadata...");

        for (Player player : getServer().getOnlinePlayers()) {
            removeMetadataByPrefix(player, "sac_");
        }

        getLogger().info("[StopAndCheck] See you next time!");
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
