package com.fkc.catchme;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CatchCommand implements CommandExecutor {

    private final CatchMePlugin plugin;
    private final CatchManager manager;

    public CatchCommand(CatchMePlugin plugin, CatchManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("這個指令只能由玩家使用。");
            return true;
        }

        if (!player.hasPermission("catchme.use")) {
            player.sendMessage(Component.text("你沒有權限使用這個指令。", NamedTextColor.RED));
            return true;
        }

        if (label.equalsIgnoreCase("uncatch")) {
            handleUncatch(player);
            return true;
        }

        // label.equalsIgnoreCase("catch")
        if (args.length == 0) {
            player.sendMessage(Component.text("用法: /catch <玩家名稱> | /catch accept | /catch deny", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            handleAccept(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("deny")) {
            handleDeny(player);
            return true;
        }

        handleRequest(player, args[0]);
        return true;
    }

    private void handleRequest(Player requester, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            requester.sendMessage(Component.text("找不到玩家 " + targetName + "。", NamedTextColor.RED));
            return;
        }

        String error = manager.requestCarry(requester, target);
        if (error != null) {
            requester.sendMessage(Component.text(error, NamedTextColor.RED));
            return;
        }

        requester.sendMessage(Component.text("已經送出背起 " + target.getName() + " 的請求，等待對方同意（30 秒內有效）。", NamedTextColor.GREEN));

        Component acceptButton = Component.text("[接受]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/catch accept"))
                .hoverEvent(HoverEvent.showText(Component.text("點擊同意被背起")));
        Component denyButton = Component.text("[拒絕]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/catch deny"))
                .hoverEvent(HoverEvent.showText(Component.text("點擊拒絕")));

        target.sendMessage(Component.text(requester.getName() + " 想要背起你！ ", NamedTextColor.AQUA)
                .append(acceptButton)
                .append(Component.text("  "))
                .append(denyButton));
    }

    private void handleAccept(Player target) {
        UUID requesterId = manager.getPendingRequester(target.getUniqueId());
        if (requesterId == null) {
            target.sendMessage(Component.text("目前沒有待處理的背人請求。", NamedTextColor.RED));
            return;
        }

        manager.clearPendingRequest(target.getUniqueId());

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null) {
            target.sendMessage(Component.text("對方已經離線了。", NamedTextColor.RED));
            return;
        }

        String error = manager.startCarry(requester, target);
        if (error != null) {
            target.sendMessage(Component.text(error, NamedTextColor.RED));
            requester.sendMessage(Component.text(error, NamedTextColor.RED));
            return;
        }

        target.sendMessage(Component.text("你現在被 " + requester.getName() + " 背著。輸入 /uncatch 隨時可以下來（由背你的人操作）。", NamedTextColor.GREEN));
        requester.sendMessage(Component.text("你現在背著 " + target.getName() + "。輸入 /uncatch 可以放下。", NamedTextColor.GREEN));
    }

    private void handleDeny(Player target) {
        UUID requesterId = manager.getPendingRequester(target.getUniqueId());
        if (requesterId == null) {
            target.sendMessage(Component.text("目前沒有待處理的背人請求。", NamedTextColor.RED));
            return;
        }
        manager.clearPendingRequest(target.getUniqueId());

        target.sendMessage(Component.text("已拒絕請求。", NamedTextColor.YELLOW));
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester != null) {
            requester.sendMessage(Component.text(target.getName() + " 拒絕了你的背人請求。", NamedTextColor.YELLOW));
        }
    }

    private void handleUncatch(Player sender) {
        // Case 1: sender is the one carrying someone.
        if (manager.isCarrying(sender.getUniqueId())) {
            manager.stopCarry(sender);
            sender.sendMessage(Component.text("已放下。", NamedTextColor.GREEN));
            return;
        }

        // Case 2: sender is the one being carried, wants down themselves.
        if (manager.isBeingCarried(sender.getUniqueId())) {
            var carrierId = manager.getCarrierOf(sender.getUniqueId());
            Player carrier = carrierId != null ? Bukkit.getPlayer(carrierId) : null;
            if (carrier != null) {
                manager.stopCarry(carrier);
                sender.sendMessage(Component.text("你已經自己下來了。", NamedTextColor.GREEN));
                carrier.sendMessage(Component.text(sender.getName() + " 自己下來了。", NamedTextColor.YELLOW));
            } else {
                // Carrier went offline unexpectedly; just clean up our own state.
                manager.forceRelease(sender.getUniqueId());
                sender.sendMessage(Component.text("已下來。", NamedTextColor.GREEN));
            }
            return;
        }

        sender.sendMessage(Component.text("你目前沒有背著任何人，也沒有被任何人背著。", NamedTextColor.RED));
    }
}
