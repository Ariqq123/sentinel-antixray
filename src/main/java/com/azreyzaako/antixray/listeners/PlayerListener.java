package com.azreyzaako.antixray.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import com.azreyzaako.antixray.SentinelAntiXray;
import com.azreyzaako.antixray.data.PlayerData;
import com.azreyzaako.antixray.data.VectorialLocation;
import com.azreyzaako.antixray.tasks.RayTraceCallable;
import com.azreyzaako.antixray.tasks.UpdateBukkitRunnable;

public final class PlayerListener implements Listener {
    private final SentinelAntiXray plugin;

    public PlayerListener(SentinelAntiXray plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!plugin.validatePlayer(player)) {
            return;
        }

        if (player.hasPermission("sentinel.antixray.bypass")) {
            return;
        }

        PlayerData playerData = new PlayerData(SentinelAntiXray.getLocations(player, new VectorialLocation(player.getEyeLocation())));
        playerData.setCallable(new RayTraceCallable(plugin, playerData));
        plugin.getPlayerData().put(player.getUniqueId(), playerData);

        if (plugin.isFolia()) {
            player.getScheduler().runAtFixedRate(plugin, new UpdateBukkitRunnable(plugin, player), null, 1L, plugin.getUpdateTicks());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerData().remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerData().get(player.getUniqueId());

        if (player.hasPermission("sentinel.antixray.bypass")) {
            return;
        }

        if (!plugin.validatePlayerData(player, playerData, "onPlayerMove")) {
            return;
        }

        Location to = event.getTo();

        if (to.getWorld().equals(playerData.getLocations()[0].getWorld())) {
            VectorialLocation location = new VectorialLocation(to);
            Vector vector = location.getVector();
            vector.setY(vector.getY() + player.getEyeHeight());
            playerData.setLocations(SentinelAntiXray.getLocations(player, location));
        }
    }
}
