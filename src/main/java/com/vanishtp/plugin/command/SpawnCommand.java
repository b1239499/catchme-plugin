package com.vanishtp.plugin.command;

import com.vanishtp.plugin.manager.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpawnCommand implements CommandExecutor {

    private final Plugin plugin;
    private final VanishManager vanishManager;

    public SpawnCommand(Plugin plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此指令只能由玩家執行。");
            return true;
        }

        Location spawn = resolveSpawn(player);
        player.teleportAsync(spawn).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                player.sendMessage("§a已傳送回重生點。");
                // 需求邏輯：/spawn 也會解除隱身
                if (vanishManager.isVanished(player)) {
                    vanishManager.unvanish(player);
                    player.sendMessage("§e隱身狀態已解除。");
                }
            } else {
                player.sendMessage("§c傳送失敗。");
            }
        });
        return true;
    }

    private Location resolveSpawn(Player player) {
        String worldName = plugin.getConfig().getString("spawn.world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        if (world == null) world = player.getWorld();

        if (plugin.getConfig().contains("spawn.x")) {
            double x = plugin.getConfig().getDouble("spawn.x");
            double y = plugin.getConfig().getDouble("spawn.y");
            double z = plugin.getConfig().getDouble("spawn.z");
            float yaw = (float) plugin.getConfig().getDouble("spawn.yaw");
            float pitch = (float) plugin.getConfig().getDouble("spawn.pitch");
            return new Location(world, x, y, z, yaw, pitch);
        }
        return world.getSpawnLocation();
    }
}
