package com.antigravity.antixray.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.antigravity.antixray.SentinelAntiXray;

/**
 * Handles all click events inside the Sentinel GUI inventories
 * and chat input for adding new blocks.
 */
public final class GUIListener implements Listener {

    private final SentinelAntiXray plugin;
    private final SentinelGUI gui;

    /**
     * Tracks players currently in "add block" mode.
     * Value: "valuable" or "common" to indicate which list they are adding to.
     */
    private final ConcurrentHashMap<UUID, String> pendingBlockInput = new ConcurrentHashMap<>();

    public GUIListener(SentinelAntiXray plugin, SentinelGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    public SentinelGUI getGUI() {
        return gui;
    }

    // ─── Inventory Click ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        if (title.equals(SentinelGUI.TITLE_MAIN)) {
            event.setCancelled(true);
            handleMainMenu(player, event.getRawSlot());
        } else if (title.equals(SentinelGUI.TITLE_BLOCKS)) {
            event.setCancelled(true);
            handleBlockManager(player, event.getRawSlot(), event.getCurrentItem());
        } else if (title.equals(SentinelGUI.TITLE_WORLDS)) {
            event.setCancelled(true);
            handleWorldManager(player, event.getRawSlot(), event.getCurrentItem());
        }
    }

    private void handleMainMenu(Player player, int slot) {
        if (slot == 11) {
            gui.openBlockManager(player);
        } else if (slot == 15) {
            gui.openWorldManager(player);
        }
    }

    private void handleBlockManager(Player player, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        // Back button
        if (slot == 49) {
            gui.openMainMenu(player);
            return;
        }

        // Add Valuable Block button
        if (slot == 48) {
            player.closeInventory();
            pendingBlockInput.put(player.getUniqueId(), "valuable");
            player.sendMessage(ChatColor.GREEN + "[Sentinel] " + ChatColor.GRAY + "Type a block material name in chat (e.g. " + ChatColor.WHITE + "IRON_ORE" + ChatColor.GRAY + ").");
            player.sendMessage(ChatColor.GRAY + "Type " + ChatColor.RED + "cancel" + ChatColor.GRAY + " to abort.");
            // Auto-cancel after 30 seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (pendingBlockInput.remove(player.getUniqueId()) != null) {
                    player.sendMessage(ChatColor.RED + "[Sentinel] " + ChatColor.GRAY + "Block input timed out.");
                }
            }, 600L); // 30 seconds
            return;
        }

        // Add Common Block button
        if (slot == 50) {
            player.closeInventory();
            pendingBlockInput.put(player.getUniqueId(), "common");
            player.sendMessage(ChatColor.BLUE + "[Sentinel] " + ChatColor.GRAY + "Type a block material name in chat (e.g. " + ChatColor.WHITE + "SANDSTONE" + ChatColor.GRAY + ").");
            player.sendMessage(ChatColor.GRAY + "Type " + ChatColor.RED + "cancel" + ChatColor.GRAY + " to abort.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (pendingBlockInput.remove(player.getUniqueId()) != null) {
                    player.sendMessage(ChatColor.RED + "[Sentinel] " + ChatColor.GRAY + "Block input timed out.");
                }
            }, 600L);
            return;
        }

        // Clicking an existing block — remove it
        if (slot < 45) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasLore()) return;

            // Determine which list this block belongs to by checking lore
            List<String> lore = meta.getLore();
            String blockName = extractMaterialName(meta.getDisplayName());
            Material mat = Material.matchMaterial(blockName);
            if (mat == null) return;

            boolean isValuable = lore.stream().anyMatch(l -> l.contains("Valuable"));

            String configKey = isValuable ? "heuristics.valuable-blocks" : "heuristics.common-blocks";
            FileConfiguration config = plugin.getConfig();
            List<String> blocks = new ArrayList<>(config.getStringList(configKey));
            blocks.removeIf(b -> b.equalsIgnoreCase(mat.name()));
            config.set(configKey, blocks);
            plugin.saveConfig();
            plugin.getMiningTracker().reloadBlockLists();

            player.sendMessage(ChatColor.RED + "[Sentinel] " + ChatColor.GRAY + "Removed " + ChatColor.WHITE + SentinelGUI.formatMaterial(mat) + ChatColor.GRAY + " from " + (isValuable ? "valuable" : "common") + " list.");

            // Refresh the GUI
            gui.openBlockManager(player);
        }
    }

    private void handleWorldManager(Player player, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        // Back button
        if (item.getType() == Material.BARRIER) {
            gui.openMainMenu(player);
            return;
        }

        // Toggle world
        if (item.getType() == Material.LIME_STAINED_GLASS_PANE || item.getType() == Material.RED_STAINED_GLASS_PANE) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            String worldName = ChatColor.stripColor(meta.getDisplayName());
            FileConfiguration config = plugin.getConfig();
            boolean currentState = config.getBoolean("world-settings." + worldName + ".anti-xray.ray-trace",
                    config.getBoolean("world-settings.default.anti-xray.ray-trace", true));

            config.set("world-settings." + worldName + ".anti-xray.ray-trace", !currentState);
            plugin.saveConfig();

            String newState = !currentState ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
            player.sendMessage(ChatColor.GOLD + "[Sentinel] " + ChatColor.GRAY + "Ray-trace for " + ChatColor.WHITE + worldName + ChatColor.GRAY + " is now " + newState + ChatColor.GRAY + ".");
            player.sendMessage(ChatColor.GRAY + "Note: World changes take full effect after restart or chunk reload.");

            // Refresh the GUI
            gui.openWorldManager(player);
        }
    }

    // ─── Chat Input ──────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String listType = pendingBlockInput.remove(uuid);
        if (listType == null) return;

        // Consume the chat message — don't broadcast it
        event.setCancelled(true);

        Player player = event.getPlayer();
        String input = event.getMessage().trim().toUpperCase();

        if (input.equalsIgnoreCase("CANCEL")) {
            player.sendMessage(ChatColor.RED + "[Sentinel] " + ChatColor.GRAY + "Block input cancelled.");
            return;
        }

        Material mat = Material.matchMaterial(input);
        if (mat == null) {
            player.sendMessage(ChatColor.RED + "[Sentinel] " + ChatColor.GRAY + "Unknown material: " + ChatColor.WHITE + input);
            player.sendMessage(ChatColor.GRAY + "Use the exact Bukkit material name (e.g. IRON_ORE, DEEPSLATE_GOLD_ORE).");
            return;
        }

        if (!mat.isBlock()) {
            player.sendMessage(ChatColor.RED + "[Sentinel] " + ChatColor.WHITE + SentinelGUI.formatMaterial(mat) + ChatColor.GRAY + " is not a block.");
            return;
        }

        String configKey = listType.equals("valuable") ? "heuristics.valuable-blocks" : "heuristics.common-blocks";

        // Run config modification on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            FileConfiguration config = plugin.getConfig();
            List<String> blocks = new ArrayList<>(config.getStringList(configKey));

            if (blocks.stream().anyMatch(b -> b.equalsIgnoreCase(mat.name()))) {
                player.sendMessage(ChatColor.RED + "[Sentinel] " + ChatColor.WHITE + SentinelGUI.formatMaterial(mat) + ChatColor.GRAY + " is already in the " + listType + " list.");
                return;
            }

            blocks.add(mat.name());
            config.set(configKey, blocks);
            plugin.saveConfig();
            plugin.getMiningTracker().reloadBlockLists();

            String color = listType.equals("valuable") ? ChatColor.GREEN.toString() : ChatColor.BLUE.toString();
            player.sendMessage(color + "[Sentinel] " + ChatColor.GRAY + "Added " + ChatColor.WHITE + SentinelGUI.formatMaterial(mat) + ChatColor.GRAY + " to " + listType + " list.");

            // Reopen GUI
            gui.openBlockManager(player);
        });
    }

    // ─── Cleanup ─────────────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingBlockInput.remove(event.getPlayer().getUniqueId());
    }

    // ─── Utilities ───────────────────────────────────────────────────────────────

    /**
     * Strips color codes and symbol prefixes from a display name to recover the material name.
     * "♦ Deepslate Diamond Ore" → "DEEPSLATE_DIAMOND_ORE"
     */
    private String extractMaterialName(String displayName) {
        String clean = ChatColor.stripColor(displayName);
        // Remove leading symbols (♦, ▪, etc.)
        clean = clean.replaceAll("^[^a-zA-Z]+", "").trim();
        return clean.toUpperCase().replace(' ', '_');
    }
}
