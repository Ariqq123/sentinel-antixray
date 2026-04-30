package com.azreyzaako.antixray.heuristics;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.azreyzaako.antixray.SentinelAntiXray;

public class MiningListener implements Listener {
    private final SentinelAntiXray plugin;

    public MiningListener(SentinelAntiXray plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // Ignore creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Ignore players with bypass permission
        if (player.hasPermission("sentinel.antixray.bypass")) {
            return;
        }

        plugin.getMiningTracker().recordBlockBreak(player, event.getBlock().getType());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Optional: clear stats on quit, or keep them to track across relogs. 
        // We'll clear them to avoid memory leaks over long uptimes for now, 
        // but this could be configurable.
        if (plugin.getConfig().getBoolean("heuristics.clear-on-quit", true)) {
            plugin.getMiningTracker().resetStats(event.getPlayer().getUniqueId());
        }
    }
}
