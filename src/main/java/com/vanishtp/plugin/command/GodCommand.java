package com.vanishtp.plugin.command;

import com.vanishtp.plugin.manager.GodManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GodCommand implements CommandExecutor {

    private final GodManager godManager;

    public GodCommand(GodManager godManager) {
        this.godManager = godManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此指令只能由玩家執行。");
            return true;
        }
        if (!player.hasPermission("vanishtp.god")) {
            player.sendMessage("§c你沒有權限使用此指令。");
            return true;
        }

        if (godManager.isGod(player)) {
            godManager.disable(player);
            player.sendMessage("§e無敵狀態已關閉。");
        } else {
            godManager.enable(player);
            player.sendMessage("§a無敵狀態已開啟，注意：只要你造成傷害就會自動解除。");
        }
        return true;
    }
}
