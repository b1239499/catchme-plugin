package com.fkc.catchme;

import org.bukkit.plugin.java.JavaPlugin;

public final class CatchMePlugin extends JavaPlugin {

    private CatchManager carryManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.carryManager = new CatchManager(this);

        CatchCommand catchCommand = new CatchCommand(this, carryManager);
        getCommand("catch").setExecutor(catchCommand);
        getCommand("uncatch").setExecutor(catchCommand);

        getServer().getPluginManager().registerEvents(new CatchListener(carryManager), this);
        getServer().getPluginManager().registerEvents(new CatchAnimalListener(this, carryManager), this);

        getLogger().info("CatchMe 已啟用。/catch <玩家> 發送背人請求、/uncatch 放下、蹲下+空手右鍵可直接背起動物。");
    }

    @Override
    public void onDisable() {
        if (carryManager != null) {
            carryManager.releaseAll();
        }
    }

    public CatchManager getCatchManager() {
        return carryManager;
    }
}
