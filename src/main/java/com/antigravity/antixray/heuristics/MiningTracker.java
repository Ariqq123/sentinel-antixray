package com.antigravity.antixray.heuristics;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.antigravity.antixray.SentinelAntiXray;

public class MiningTracker {
    private final SentinelAntiXray plugin;
    private final ConcurrentMap<UUID, MiningStats> statsMap = new ConcurrentHashMap<>();

    // Config-driven block sets — loaded from config.yml, reloadable at runtime
    private volatile Set<Material> valuableBlocks;
    private volatile Set<Material> commonBlocks;

    // Cached thresholds — avoid reading config on every block break
    private volatile int minBlocksMined;
    private volatile double alertThreshold;

    public MiningTracker(SentinelAntiXray plugin) {
        this.plugin = plugin;
        reloadBlockLists();
        reloadThresholds();
    }

    /**
     * Reloads the valuable and common block sets from config.yml.
     * Safe to call from the main thread after config changes (e.g. GUI save).
     */
    public void reloadBlockLists() {
        FileConfiguration config = plugin.getConfig();
        this.valuableBlocks = loadMaterialSet(config, "heuristics.valuable-blocks");
        this.commonBlocks = loadMaterialSet(config, "heuristics.common-blocks");
    }

    /**
     * Reloads the cached threshold values from config.yml.
     */
    public void reloadThresholds() {
        FileConfiguration config = plugin.getConfig();
        this.minBlocksMined = config.getInt("heuristics.minimum-blocks-mined", 50);
        this.alertThreshold = config.getDouble("heuristics.alert-threshold", 0.08);
    }

    /**
     * Returns an unmodifiable view of the current valuable block set.
     */
    public Set<Material> getValuableBlocks() {
        return Collections.unmodifiableSet(valuableBlocks);
    }

    /**
     * Returns an unmodifiable view of the current common block set.
     */
    public Set<Material> getCommonBlocks() {
        return Collections.unmodifiableSet(commonBlocks);
    }

    public MiningStats getStats(UUID uuid) {
        return statsMap.computeIfAbsent(uuid, k -> new MiningStats());
    }

    public void resetStats(UUID uuid) {
        statsMap.remove(uuid);
    }

    public void recordBlockBreak(Player player, Material type) {
        // Snapshot volatile fields once per call for consistency
        Set<Material> valuable = this.valuableBlocks;
        Set<Material> common = this.commonBlocks;

        if (valuable.contains(type)) {
            MiningStats stats = getStats(player.getUniqueId());
            stats.valuableBlocks.incrementAndGet();
            checkHeuristics(player, stats);
        } else if (common.contains(type)) {
            MiningStats stats = getStats(player.getUniqueId());
            stats.commonBlocks.incrementAndGet();
        }
    }

    private void checkHeuristics(Player player, MiningStats stats) {
        int total = stats.valuableBlocks.get() + stats.commonBlocks.get();

        if (total < minBlocksMined) return;

        double ratio = (double) stats.valuableBlocks.get() / total;

        if (ratio >= alertThreshold) {
            long now = System.currentTimeMillis();
            long lastAlert = stats.lastAlertTime.get();
            // Alert at most once per minute — CAS to avoid duplicate alerts under concurrency
            if (now - lastAlert > 60000 && stats.lastAlertTime.compareAndSet(lastAlert, now)) {
                alertStaff(player, ratio, stats);
            }
        }
    }

    private void alertStaff(Player player, double ratio, MiningStats stats) {
        String message = ChatColor.RED + "[Sentinel] " + ChatColor.YELLOW + player.getName() +
                         ChatColor.GRAY + " may be using X-Ray. " +
                         ChatColor.WHITE + "Ratio: " + ChatColor.RED + String.format("%.2f%%", ratio * 100) +
                         ChatColor.GRAY + " (" + stats.valuableBlocks.get() + " valuable / " + stats.commonBlocks.get() + " common)";

        plugin.getLogger().warning("X-Ray Alert: " + player.getName() + " - Ratio: " + String.format("%.2f%%", ratio * 100));
        Bukkit.broadcast(message, "sentinel.antixray.alerts");
    }

    public void printStats(CommandSender sender, Player target) {
        MiningStats stats = getStats(target.getUniqueId());
        int valuable = stats.valuableBlocks.get();
        int common = stats.commonBlocks.get();
        int total = valuable + common;
        double ratio = total == 0 ? 0 : (double) valuable / total;

        sender.sendMessage(ChatColor.AQUA + "--- Mining Stats for " + target.getName() + " ---");
        sender.sendMessage(ChatColor.GRAY + "Valuable Ores: " + ChatColor.GREEN + valuable);
        sender.sendMessage(ChatColor.GRAY + "Common Blocks: " + ChatColor.GREEN + common);
        sender.sendMessage(ChatColor.GRAY + "Suspicion Ratio: " + (ratio > alertThreshold ? ChatColor.RED : ChatColor.GREEN) + String.format("%.2f%%", ratio * 100));
    }

    /**
     * Loads a Set of Materials from a string list in config.
     * Invalid entries are logged and skipped — never crashes the plugin.
     */
    private Set<Material> loadMaterialSet(FileConfiguration config, String path) {
        List<String> names = config.getStringList(path);
        if (names.isEmpty()) {
            return EnumSet.noneOf(Material.class);
        }

        EnumSet<Material> set = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) {
                set.add(mat);
            } else {
                plugin.getLogger().log(Level.WARNING, "Invalid material in {0}: {1}", new Object[]{path, name});
            }
        }
        return set;
    }

    /**
     * Thread-safe mining statistics using AtomicInteger for lock-free concurrent increments.
     */
    public static class MiningStats {
        public final AtomicInteger valuableBlocks = new AtomicInteger(0);
        public final AtomicInteger commonBlocks = new AtomicInteger(0);
        public final AtomicLong lastAlertTime = new AtomicLong(0);
    }
}
