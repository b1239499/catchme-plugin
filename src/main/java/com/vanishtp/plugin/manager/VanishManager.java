package com.vanishtp.plugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager {

    private final Plugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    public VanishManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    /** 開啟隱身：隱藏玩家、附加傳送與保護區 bypass 用的暫時權限 */
    public void vanish(Player player) {
        if (isVanished(player)) return;
        vanished.add(player.getUniqueId());

        // 附加暫時權限，只在隱身期間生效，解除隱身時會自動移除
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission("vanishtp.tp", true);
        for (String node : plugin.getConfig().getStringList("bypass-permissions")) {
            attachment.setPermission(node, true);
        }
        attachments.put(player.getUniqueId(), attachment);

        // Folia：其他玩家的可見度變更必須排程到該玩家自己所屬的 region 執行緒上執行，
        // 不能直接在目前執行緒對別人的 Player 物件呼叫 API
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (online.hasPermission("vanishtp.see")) continue;
            online.getScheduler().run(plugin, task -> online.hidePlayer(plugin, player), null);
        }

        player.setCollidable(false);
    }

    /** 解除隱身：只應該由 /tpa 被接受、或 /spawn 觸發（forceoff 權限可強制解除） */
    public void unvanish(Player player) {
        if (!isVanished(player)) return;
        vanished.remove(player.getUniqueId());

        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            player.removeAttachment(attachment);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            online.getScheduler().run(plugin, task -> online.showPlayer(plugin, player), null);
        }

        player.setCollidable(true);
    }

    /** 新玩家加入時，把目前所有隱身中的人對他隱藏 */
    public void applyVanishTo(Player viewer) {
        if (viewer.hasPermission("vanishtp.see")) return;
        for (UUID id : vanished) {
            Player target = Bukkit.getPlayer(id);
            if (target != null) {
                viewer.getScheduler().run(plugin, task -> viewer.hidePlayer(plugin, target), null);
            }
        }
    }

    public void unvanishAll() {
        for (UUID id : Set.copyOf(vanished)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) unvanish(p);
        }
    }
}
