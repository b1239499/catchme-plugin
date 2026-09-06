package com.vanishtp.plugin.command;

import com.vanishtp.plugin.manager.VanishManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements CommandExecutor {

    private final VanishManager vanishManager;

    public VanishCommand(VanishManager vanishManager) {
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

        if (vanishManager.isVanished(player)) {
            if (player.hasPermission("vanishtp.forceoff")) {
                vanishManager.unvanish(player);
                player.sendMessage("§e已強制解除隱身。");
            } else {
                player.sendMessage("§c隱身狀態只能透過 /tpa 被接受，或使用 /spawn 才能解除。");
            }
            return true;
        }

        vanishManager.vanish(player);
        player.sendMessage("§a你已進入隱身狀態，現在可以使用 /fkcvtp <玩家> 並繞過保護區限制。");
        return true;
    }
}
