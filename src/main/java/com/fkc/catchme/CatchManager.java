package com.fkc.catchme;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles "sneak + empty-hand right-click" pickup of animals.
 * Kept separate from CatchListener (which only does cleanup bookkeeping)
 * to keep the two concerns readable on their own.
 */
public class CatchAnimalListener implements Listener {

    // Hard safety blacklist. These never get picked up, no matter what the
    // server owner puts in config.yml.
    private static final Set<EntityType> HARD_BLACKLIST = EnumSet.of(
            EntityType.ENDER_DRAGON,
            EntityType.WITHER,
            EntityType.WARDEN,
            EntityType.GIANT
    );

    private final Plugin plugin;
    private final CatchManager manager;

    public CatchAnimalListener(Plugin plugin, CatchManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof LivingEntity target)) {
            return;
        }
        if (target instanceof Player) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }
        if (!plugin.getConfig().getBoolean("animal-pickup.enabled", true)) {
            return;
        }
        if (!player.hasPermission("catchme.animal.use")) {
            return;
        }
        if (HARD_BLACKLIST.contains(target.getType())) {
            return;
        }
        if (!isAllowedType(target.getType())) {
            return;
        }

        if (isProtectedFromPlayer(player, target)) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                    "這隻動物受到領地保護，你沒有權限背走它。",
                    net.kyori.adventure.text.format.NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        String error = manager.startCarry(player, target);
        if (error != null) {
            player.sendMessage(net.kyori.adventure.text.Component.text(error,
                    net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        player.sendMessage(net.kyori.adventure.text.Component.text(
                "你背起了一隻 " + target.getType().name() + "。輸入 /uncatch 可以放下。",
                net.kyori.adventure.text.format.NamedTextColor.GREEN));
    }

    /**
     * Almost every land-claim / grief-protection plugin (GriefPrevention,
     * WorldGuard, Towny, PlotSquared, RedProtect, etc.) hooks into
     * EntityDamageByEntityEvent to stop non-members from harming animals
     * inside a claim. PlayerInteractEntityEvent alone is not reliable —
     * many protection plugins don't bother cancelling a harmless "pet the
     * cow" interaction. So we synthesize a zero-damage
     * EntityDamageByEntityEvent and dispatch it (without ever calling
     * entity.damage(), so no real damage happens) purely to ask "would this
     * player be allowed to interact with this entity here?".
     */
    private boolean isProtectedFromPlayer(Player player, LivingEntity target) {
        EntityDamageByEntityEvent probe = new EntityDamageByEntityEvent(
                player, target, EntityDamageEvent.DamageCause.CUSTOM, 0.0);
        Bukkit.getPluginManager().callEvent(probe);
        return probe.isCancelled();
    }

    private boolean isAllowedType(EntityType type) {
        Set<String> allowed = plugin.getConfig().getStringList("animal-pickup.allowed-types")
                .stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        return allowed.contains(type.name());
    }
}
