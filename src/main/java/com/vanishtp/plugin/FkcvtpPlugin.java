package com.vanishtp.plugin;

import com.vanishtp.plugin.command.GodCommand;
import com.vanishtp.plugin.command.SpawnCommand;
import com.vanishtp.plugin.command.TpAcceptCommand;
import com.vanishtp.plugin.command.TpCommand;
import com.vanishtp.plugin.command.TpDenyCommand;
import com.vanishtp.plugin.command.TpaCommand;
import com.vanishtp.plugin.command.VanishCommand;
import com.vanishtp.plugin.listener.GodListener;
import com.vanishtp.plugin.listener.PlayerConnectionListener;
import com.vanishtp.plugin.manager.GodManager;
import com.vanishtp.plugin.manager.TpaManager;
import com.vanishtp.plugin.manager.VanishManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FkcvtpPlugin extends JavaPlugin {

    private VanishManager vanishManager;
    private TpaManager tpaManager;
    private GodManager godManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.vanishManager = new VanishManager(this);
        this.tpaManager = new TpaManager(this, vanishManager);
        this.godManager = new GodManager();

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(vanishManager), this);
        getServer().getPluginManager().registerEvents(new GodListener(godManager), this);

        getCommand("fkcvanish").setExecutor(new VanishCommand(vanishManager));
        getCommand("fkcvtp").setExecutor(new TpCommand(vanishManager));
        getCommand("tpa").setExecutor(new TpaCommand(tpaManager));
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(tpaManager));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(tpaManager));
        getCommand("spawn").setExecutor(new SpawnCommand(this, vanishManager));
        getCommand("fkcgod").setExecutor(new GodCommand(godManager));

        getLogger().info("fkcvtp 已啟動 (Folia 相容模式)");
    }

    @Override
    public void onDisable() {
        if (vanishManager != null) {
            vanishManager.unvanishAll();
        }
        if (godManager != null) {
            godManager.disableAll();
        }
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public GodManager getGodManager() {
        return godManager;
    }
}
