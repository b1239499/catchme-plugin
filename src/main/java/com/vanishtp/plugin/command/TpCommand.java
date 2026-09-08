package com.vanishtp.plugin.command;

import com.vanishtp.plugin.manager.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpCommand implements CommandExecutor {

    private final VanishManager vanishManager;

    public TpCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此指令只能由玩家執行。");
            return true;
        }
        if (!player.hasPermission("vanishtp.use")) {
            player.sendMessage("§c你沒有權限使用此指令。");
            return true;
        }
        if (!vanishManager.isVanished(player)) {
            player.sendMessage("§c你必須先使用 /fkcvanish 進入隱身狀態，才能直接傳送到玩家身邊。");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage("§c用法：/fkcvtp <玩家>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§c找不到玩家 " + args[0]);
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage("§c不能傳送到自己身上。");
            return true;
        }

        // Folia 安全的跨 region 非同步傳送
        player.teleportAsync(target.getLocation()).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                player.sendMessage("§a已隱身傳送到 " + target.getName() + " 身邊。");
            } else {
                player.sendMessage("§c傳送失敗。");
            }
        });
        return true;
    }
}
