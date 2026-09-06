package com.vanishtp.plugin.listener;

import com.vanishtp.plugin.manager.GodManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class GodListener implements Listener {

    private final GodManager godManager;

    public GodListener(GodManager godManager) {
        this.godManager = godManager;
    }

    /** 無敵狀態下：自己完全免疫任何傷害 */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageTaken(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && godManager.isGod(player)) {
            event.setCancelled(true);
        }
    }

    /** 需求邏輯：無敵狀態下，只要對他人造成傷害，就自動解除無敵 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;
        if (!godManager.isGod(attacker)) return;
        if (event.getFinalDamage() <= 0) return;

        godManager.disable(attacker);
        attacker.sendMessage("§e你造成了傷害，無敵狀態已自動解除。");
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
