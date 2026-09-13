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
        // PlayerInteractEntityEvent fires once for main hand and once for
        // off hand; only handle the main-hand swing to avoid double triggers.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof LivingEntity target)) {
            return;
        }
        if (target instanceof Player) {
            // Players go through /catch <name> with an accept/deny flow instead.
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            // Require an empty hand so we don't hijack feeding/breeding/leashing.
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

        // We are handling this interaction ourselves now, so cancel the
        // vanilla behaviour (which could otherwise trigger taming, breeding,
        // opening a horse's inventory, etc).
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
     * inside a claim, since that is the vanilla mechanic griefers would
     * otherwise abuse to kill or steal someone's animals.
     * <p>
     * PlayerInteractEntityEvent alone is not a reliable signal — plenty of
     * protection plugins don't bother cancelling a harmless "pet the cow"
     * interaction. So instead we synthesize a zero-damage
     * EntityDamageByEntityEvent and dispatch it (without ever actually
     * calling entity.damage(), so no real damage happens) purely to ask
     * "would this player be allowed to interact with this entity here?".
     * If any protection plugin cancels it, we treat the spot as protected.
     * <p>
     * Two ways to override this behaviour, both configurable so you don't
     * have to depend on whatever your specific land-claim plugin exposes:
     * <ul>
     *   <li>{@code animal-pickup.check-protection: false} in config.yml —
     *       turns this whole check off for everyone (anyone can carry
     *       animals anywhere, land-protected or not);</li>
     *   <li>{@code catchme.animal.bypass-protection} permission node —
     *       lets specific players (e.g. staff) always carry animals
     *       regardless of land protection, without needing to touch the
     *       land-claim plugin's own trust/permission system at all.</li>
     * </ul>
     */
    private boolean isProtectedFromPlayer(Player player, LivingEntity target) {
        if (!plugin.getConfig().getBoolean("animal-pickup.check-protection", true)) {
            return false; // protection check turned off entirely in config.yml
        }
        if (player.hasPermission("catchme.animal.bypass-protection")) {
            return false; // this player is explicitly allowed to ignore land protection
        }
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
