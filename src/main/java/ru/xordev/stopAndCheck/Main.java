package ru.xordev.stopAndCheck;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import ru.xordev.stopAndCheck.commands.CheckerCommand;
import ru.xordev.stopAndCheck.handlers.CheckerEventHandler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public final class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            PluginManager pm = getServer().getPluginManager();

            int currentConfigVersion = getConfig().getInt("config-version", -1);
            int latestConfigVersion = 2;

            if (currentConfigVersion < latestConfigVersion) {
                migrateConfig(currentConfigVersion, latestConfigVersion);
            }

            pm.addPermission(new Permission("sac.command", "Access to /sac command", PermissionDefault.OP));
            pm.addPermission(new Permission("sac.immunity", "Deny send player to checks", PermissionDefault.OP));
            pm.addPermission(new Permission("sac.help", "Access to /help command", PermissionDefault.OP));

            Permission moderPerm = new Permission("sac.moder.*", "Allow all moderator commands", PermissionDefault.OP);
            moderPerm.getChildren().put("sac.moder.reload", true);
            moderPerm.getChildren().put("sac.moder.check", true);
            moderPerm.getChildren().put("sac.moder.free", true);
            moderPerm.getChildren().put("sac.moder.info", true);

            pm.addPermission(moderPerm);

            pm.registerEvents(new CheckerEventHandler(this, new Utils(this)), this);
            Objects.requireNonNull(getServer().getPluginCommand("sac")).setExecutor(new CheckerCommand(this));

            getServer().getLogger().log(Level.INFO," ");
            getServer().getLogger().log(Level.INFO,"-| StopAndCheck 1.2.2 by XOR |-");
            getServer().getLogger().log(Level.INFO,"All releases here: https://github.com/justyXOR/StopAndCheck");
            getServer().getLogger().log(Level.INFO," ");
            getServer().getLogger().log(Level.INFO,"Plugin is ready to work!");
            getServer().getLogger().log(Level.INFO," ");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE,"Error when enabling plugin!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void migrateConfig(int fromVersion, int toVersion) {
        getLogger().warning(" ");
        getLogger().warning("⚠️ CONFIG MIGRATION REQUIRED!");
        getLogger().warning("Current config version: " + fromVersion);
        getLogger().warning("Latest config version: " + toVersion);
        getLogger().warning("Creating backup and generating new config...");

        try {
            File configFile = new File(getDataFolder(), "config.yml");
            File backupFile = new File(getDataFolder(), "config.yml.backup-v" + fromVersion);

            if (configFile.exists()) {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("✓ Backup created: " + backupFile.getName());
            }

            if (configFile.delete()) {
                saveDefaultConfig();
                reloadConfig();
                getLogger().info("✓ New config generated successfully!");
            }

            getLogger().warning("⚠️ Please check new config.yml and adjust settings if needed!");
            getLogger().warning(" ");

        } catch (Exception e) {
            getLogger().severe("✗ Config migration failed!");
            getLogger().severe("Error: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Cleaning up metadata...");

        for (Player player : getServer().getOnlinePlayers()) {
            removeMetadataByPrefix(player);
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

    private void removeMetadataByPrefix(@NotNull Player player) {
        List<MetadataValue> metadata = player.getMetadata("sac_");
        for (MetadataValue meta : new ArrayList<>(player.getMetadata("sac_"))) {
            if (Objects.equals(meta.getOwningPlugin(), this)) {
                player.removeMetadata("sac_", this);
            }
        }
    }
}
