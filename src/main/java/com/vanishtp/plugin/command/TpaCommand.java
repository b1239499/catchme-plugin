package com.vanishtp.plugin.command;

import com.vanishtp.plugin.manager.TpaManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpaCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
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
        if (args.length != 1) {
            player.sendMessage("§c用法：/tpa <玩家>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§c找不到玩家 " + args[0]);
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage("§c不能對自己發送請求。");
            return true;
        }

        tpaManager.sendRequest(player, target);
        return true;
    }
}
