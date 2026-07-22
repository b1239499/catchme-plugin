package com.fkc.catchme;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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

    private boolean isAllowedType(EntityType type) {
        Set<String> allowed = plugin.getConfig().getStringList("animal-pickup.allowed-types")
                .stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        return allowed.contains(type.name());
    }
}
