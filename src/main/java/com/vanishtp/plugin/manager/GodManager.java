package com.vanishtp.plugin.manager;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GodManager {

    private final Set<UUID> godPlayers = ConcurrentHashMap.newKeySet();

    public boolean isGod(Player player) {
        return godPlayers.contains(player.getUniqueId());
    }

    public void enable(Player player) {
        godPlayers.add(player.getUniqueId());
    }

    public void disable(Player player) {
        godPlayers.remove(player.getUniqueId());
    }

    public void disableAll() {
        godPlayers.clear();
    }
}
