package com.fkc.catchme;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Tracks who is carrying whom (or what), and pending carry requests.
 * The passenger can be a Player (via request/accept) or any other Entity
 * (e.g. an animal, picked up directly by sneak + right-click).
 * Every mutation happens synchronously on the thread that already owns the
 * relevant entity (command execution / event handling), so this stays
 * safe on Folia: we never touch entities from an unrelated async thread.
 */
public class CatchManager {

    // carrierUUID -> passengerUUID (passenger can be a Player or any other Entity)
    private final Map<UUID, UUID> activeCarries = new HashMap<>();
    // passengerUUID -> requesterUUID (the requester wants to carry the key player)
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();

    private static final double MAX_DISTANCE = 8.0;
    private static final long REQUEST_TIMEOUT_SECONDS = 30;

    private final Plugin plugin;

    public CatchManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isCarrying(UUID carrierId) {
        return activeCarries.containsKey(carrierId);
    }

    public boolean isBeingCarried(UUID passengerId) {
        return activeCarries.containsValue(passengerId);
    }

    /**
     * @return the UUID of whoever is carrying the given passenger,
     *         or null if that entity is not being carried.
     */
    public UUID getCarrierOf(UUID passengerId) {
        for (Map.Entry<UUID, UUID> entry : activeCarries.entrySet()) {
            if (entry.getValue().equals(passengerId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Player <-> Player, request/accept flow
    // ---------------------------------------------------------------

    /**
     * @return null if request was created, or an error message if it could not be.
     */
    public String requestCarry(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            return "你不能背自己。";
        }
        if (isCarrying(requester.getUniqueId())) {
            return "你已經正在背著東西了，先用 /uncatch 放下再說。";
        }
        if (isBeingCarried(requester.getUniqueId())) {
            return "你正被別人背著，沒辦法同時背別人。";
        }
        if (isBeingCarried(target.getUniqueId())) {
            return target.getName() + " 已經正被別人背著了。";
        }
        if (!requester.getWorld().equals(target.getWorld())) {
            return "你們不在同一個世界，沒辦法背起對方。";
        }
        if (requester.getLocation().distance(target.getLocation()) > MAX_DISTANCE) {
            return "距離太遠了，靠近一點再試一次。";
        }

        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());

        Bukkit.getServer().getGlobalRegionScheduler().runDelayed(plugin, task -> {
            UUID stillPending = pendingRequests.get(target.getUniqueId());
            if (stillPending != null && stillPending.equals(requester.getUniqueId())) {
                pendingRequests.remove(target.getUniqueId());
            }
        }, secondsToTicks(REQUEST_TIMEOUT_SECONDS));

        return null;
    }

    public UUID getPendingRequester(UUID targetId) {
        return pendingRequests.get(targetId);
    }

    public void clearPendingRequest(UUID targetId) {
        pendingRequests.remove(targetId);
    }

    // ---------------------------------------------------------------
    // Generic carry logic — works for a Player passenger (after accept)
    // or any other Entity passenger (direct pickup, e.g. an animal).
    // ---------------------------------------------------------------

    /**
     * @return null on success, or an error message.
     */
    public String startCarry(Player carrier, Entity passenger) {
        if (!carrier.getWorld().equals(passenger.getWorld())) {
            return "你們已經不在同一個世界了。";
        }
        if (carrier.getLocation().distance(passenger.getLocation()) > MAX_DISTANCE) {
            return "距離太遠了，請靠近一點再試一次。";
        }
        if (isCarrying(carrier.getUniqueId()) || isBeingCarried(carrier.getUniqueId())) {
            return "你現在沒辦法背東西。";
        }
        if (isBeingCarried(passenger.getUniqueId())) {
            return passenger.getName() + " 已經被背走了。";
        }

        boolean success = carrier.addPassenger(passenger);
        if (!success) {
            return "背起 " + passenger.getName() + " 失敗，請再試一次。";
        }

        activeCarries.put(carrier.getUniqueId(), passenger.getUniqueId());
        return null;
    }

    public void stopCarry(Player carrier) {
        UUID passengerId = activeCarries.remove(carrier.getUniqueId());
        if (passengerId == null) {
            return;
        }
        Entity passenger = Bukkit.getEntity(passengerId);
        if (passenger != null && carrier.getPassengers().contains(passenger)) {
            carrier.removePassenger(passenger);
        }
    }

    /**
     * Called when a carrier (a Player) disconnects, dies, or otherwise
     * needs to be forcibly detached, without requiring a valid online
     * reference for the passenger side.
     */
    public void forceRelease(UUID playerId) {
        UUID passengerOfThisPlayer = activeCarries.remove(playerId);

        UUID carrierOfThisPlayer = getCarrierOf(playerId);
        if (carrierOfThisPlayer != null) {
            activeCarries.remove(carrierOfThisPlayer);
        }

        pendingRequests.remove(playerId);
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().equals(playerId));

        if (passengerOfThisPlayer != null) {
            Entity passenger = Bukkit.getEntity(passengerOfThisPlayer);
            Player carrier = Bukkit.getPlayer(playerId);
            if (carrier != null && passenger != null && carrier.getPassengers().contains(passenger)) {
                carrier.removePassenger(passenger);
            }
        }
        if (carrierOfThisPlayer != null) {
            Player carrier = Bukkit.getPlayer(carrierOfThisPlayer);
            Entity passenger = Bukkit.getEntity(playerId);
            if (carrier != null && passenger != null && carrier.getPassengers().contains(passenger)) {
                carrier.removePassenger(passenger);
            }
        }
    }

    /**
     * Called when a non-player passenger (an animal) dies, despawns, or is
     * otherwise removed from the world. Only needs to clear the "being
     * carried" side, since animals never carry anything themselves.
     */
    public void forceReleaseEntity(UUID entityId) {
        UUID carrierId = getCarrierOf(entityId);
        if (carrierId == null) {
            return;
        }
        activeCarries.remove(carrierId);

        Player carrier = Bukkit.getPlayer(carrierId);
        Entity passenger = Bukkit.getEntity(entityId);
        if (carrier != null && passenger != null && carrier.getPassengers().contains(passenger)) {
            carrier.removePassenger(passenger);
        }
    }

    public void releaseAll() {
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(activeCarries).entrySet()) {
            Player carrier = Bukkit.getPlayer(entry.getKey());
            Entity passenger = Bukkit.getEntity(entry.getValue());
            if (carrier != null && passenger != null && carrier.getPassengers().contains(passenger)) {
                carrier.removePassenger(passenger);
            }
        }
        activeCarries.clear();
        pendingRequests.clear();
    }

    private static long secondsToTicks(long seconds) {
        return TimeUnit.SECONDS.toMillis(seconds) / 50L; // 1 tick = 50ms
    }
}
