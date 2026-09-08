package com.vanishtp.plugin.listener;

import com.vanishtp.plugin.manager.VanishManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerConnectionListener implements Listener {

    private final VanishManager vanishManager;

    public PlayerConnectionListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        vanishManager.applyVanishTo(event.getPlayer());
    }
}
