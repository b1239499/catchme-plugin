package com.fkc.catchme;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class CatchListener implements Listener {

    private final CatchManager manager;

    public CatchListener(CatchManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.forceRelease(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        manager.forceRelease(event.getEntity().getUniqueId());
    }

    // An animal (or any non-player passenger) dying while being carried.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            return; // handled by onDeath above
        }
        if (manager.isBeingCarried(entity.getUniqueId())) {
            manager.forceReleaseEntity(entity.getUniqueId());
        }
    }

    // Covers players/animals jumping or sneaking off, or being
    // force-dismounted by something else (fall damage, another plugin,
    // etc). We just make sure our own bookkeeping map stays in sync with
    // reality.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDismount(EntityDismountEvent event) {
        Entity passenger = event.getEntity();
        if (!manager.isBeingCarried(passenger.getUniqueId())) {
            return;
        }
        if (passenger instanceof Player) {
            manager.forceRelease(passenger.getUniqueId());
        } else {
            manager.forceReleaseEntity(passenger.getUniqueId());
        }
    }

    // Long-distance teleports (e.g. /pwarp from earlier in this server's
    // setup, or death/respawn) can easily move carrier and passenger into
    // different Folia regions. Safer to just drop the passenger rather
    // than risk a cross-region entity operation.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (manager.isCarrying(player.getUniqueId()) || manager.isBeingCarried(player.getUniqueId())) {
            manager.forceRelease(player.getUniqueId());
        }
    }
}
