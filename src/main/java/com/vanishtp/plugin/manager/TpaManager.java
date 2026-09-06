package com.vanishtp.plugin.manager;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TpaManager {

    private record Request(UUID requester, UUID target, ScheduledTask timeoutTask) {}

    private final Plugin plugin;
    private final VanishManager vanishManager;
    // key = 被請求的目標玩家 UUID
    private final Map<UUID, Request> pendingByTarget = new ConcurrentHashMap<>();

    public TpaManager(Plugin plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
    }

    public void sendRequest(Player requester, Player target) {
        cancel(target.getUniqueId());

        long timeoutSeconds = plugin.getConfig().getLong("tpa-request-timeout-seconds", 30);

        ScheduledTask timeoutTask = Bukkit.getAsyncScheduler().runDelayed(plugin, t -> {
            Request req = pendingByTarget.remove(target.getUniqueId());
            if (req != null) {
                Player r = Bukkit.getPlayer(req.requester());
                Player tp = Bukkit.getPlayer(req.target());
                if (r != null) r.sendMessage("§c傳送請求已逾時。");
                if (tp != null) tp.sendMessage("§c傳送請求已逾時。");
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        pendingByTarget.put(target.getUniqueId(), new Request(requester.getUniqueId(), target.getUniqueId(), timeoutTask));

        requester.sendMessage("§a已送出傳送請求給 " + target.getName() + "，等待對方接受。");
        target.sendMessage("§e" + requester.getName() + " 想要傳送到你身邊。輸入 §a/tpaccept §e接受，或 §c/tpdeny §e拒絕。");
    }

    public void accept(Player target) {
        Request req = pendingByTarget.remove(target.getUniqueId());
        if (req == null) {
            target.sendMessage("§c目前沒有待處理的傳送請求。");
            return;
        }
        req.timeoutTask().cancel();

        Player requester = Bukkit.getPlayer(req.requester());
        if (requester == null) {
            target.sendMessage("§c對方已離線。");
            return;
        }

        requester.teleportAsync(target.getLocation()).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                requester.sendMessage("§a" + target.getName() + " 已接受你的傳送請求。");
                target.sendMessage("§a已接受 " + requester.getName() + " 的傳送請求。");

                // 需求邏輯：/tpa 被接受後，如果請求者處於隱身狀態，就解除隱身
                if (vanishManager.isVanished(requester)) {
                    vanishManager.unvanish(requester);
                    requester.sendMessage("§e隱身狀態已解除。");
                }
            } else {
                requester.sendMessage("§c傳送失敗，請再試一次。");
            }
        });
    }

    public void deny(Player target) {
        Request req = pendingByTarget.remove(target.getUniqueId());
        if (req == null) {
            target.sendMessage("§c目前沒有待處理的傳送請求。");
            return;
        }
        req.timeoutTask().cancel();
        Player requester = Bukkit.getPlayer(req.requester());
        target.sendMessage("§c已拒絕傳送請求。");
        if (requester != null) requester.sendMessage("§c" + target.getName() + " 拒絕了你的傳送請求。");
    }

    private void cancel(UUID targetId) {
        Request old = pendingByTarget.remove(targetId);
        if (old != null) old.timeoutTask().cancel();
    }
}
