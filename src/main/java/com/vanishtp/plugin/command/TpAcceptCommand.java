package com.vanishtp.plugin.command;

import com.vanishtp.plugin.manager.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpAcceptCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpAcceptCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此指令只能由玩家執行。");
            return true;
        }
        tpaManager.accept(player);
        return true;
    }
}
