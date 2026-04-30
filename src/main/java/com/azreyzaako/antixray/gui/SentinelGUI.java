package com.azreyzaako.antixray.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.azreyzaako.antixray.SentinelAntiXray;

/**
 * Builds and manages the three admin GUI screens for Sentinel Anti-Xray.
 * All inventories use title-based routing in {@link GUIListener}.
 */
public final class SentinelGUI {

    // Inventory titles — used for routing in GUIListener. Do not change without updating both.
    public static final String TITLE_MAIN = ChatColor.DARK_RED + "✦ " + ChatColor.GOLD + "Sentinel" + ChatColor.DARK_RED + " ✦";
    public static final String TITLE_BLOCKS = ChatColor.DARK_RED + "✦ " + ChatColor.GOLD + "Block Manager" + ChatColor.DARK_RED + " ✦";
    public static final String TITLE_WORLDS = ChatColor.DARK_RED + "✦ " + ChatColor.GOLD + "World Manager" + ChatColor.DARK_RED + " ✦";

    private final SentinelAntiXray plugin;

    public SentinelGUI(SentinelAntiXray plugin) {
        this.plugin = plugin;
    }

    // ─── Main Menu ───────────────────────────────────────────────────────────────

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAIN);

        // Fill border with dark glass
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Block Manager button — slot 11
        inv.setItem(11, createItem(Material.DIAMOND_ORE, ChatColor.AQUA + "Manage Protected Blocks",
                List.of(ChatColor.GRAY + "Add or remove ores/blocks tracked",
                        ChatColor.GRAY + "by the heuristic mining analyzer.",
                        "",
                        ChatColor.YELLOW + "Click to open")));

        // World Manager button — slot 15
        inv.setItem(15, createItem(Material.GRASS_BLOCK, ChatColor.GREEN + "Manage Worlds",
                List.of(ChatColor.GRAY + "Toggle ray-trace anti-xray",
                        ChatColor.GRAY + "per world.",
                        "",
                        ChatColor.YELLOW + "Click to open")));

        player.openInventory(inv);
    }

    // ─── Block Manager ───────────────────────────────────────────────────────────

    public void openBlockManager(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_BLOCKS);

        // Fill bottom row with dark glass
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Populate valuable blocks
        Set<Material> valuable = plugin.getMiningTracker().getValuableBlocks();
        Set<Material> common = plugin.getMiningTracker().getCommonBlocks();

        int slot = 0;
        for (Material mat : valuable) {
            if (slot >= 45) break; // Guard against overflow
            Material displayMat = mat.isItem() ? mat : Material.GOLD_INGOT;
            inv.setItem(slot++, createItem(displayMat,
                    ChatColor.RED + "♦ " + ChatColor.WHITE + formatMaterial(mat),
                    List.of(ChatColor.GREEN + "Type: Valuable Ore",
                            "",
                            ChatColor.YELLOW + "Click to remove")));
        }

        for (Material mat : common) {
            if (slot >= 45) break;
            Material displayMat = mat.isItem() ? mat : Material.COBBLESTONE;
            inv.setItem(slot++, createItem(displayMat,
                    ChatColor.GRAY + "▪ " + ChatColor.WHITE + formatMaterial(mat),
                    List.of(ChatColor.BLUE + "Type: Common Block",
                            "",
                            ChatColor.YELLOW + "Click to remove")));
        }

        // Add Block button — slot 48
        inv.setItem(48, createItem(Material.EMERALD, ChatColor.GREEN + "✚ Add Valuable Block",
                List.of(ChatColor.GRAY + "Type a block name in chat",
                        ChatColor.GRAY + "to add it to the valuable list.",
                        "",
                        ChatColor.YELLOW + "Click to begin")));

        // Add Common Block button — slot 50
        inv.setItem(50, createItem(Material.BRICK, ChatColor.BLUE + "✚ Add Common Block",
                List.of(ChatColor.GRAY + "Type a block name in chat",
                        ChatColor.GRAY + "to add it to the common list.",
                        "",
                        ChatColor.YELLOW + "Click to begin")));

        // Back button — slot 49
        inv.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "← Back",
                List.of(ChatColor.GRAY + "Return to main menu")));

        player.openInventory(inv);
    }

    // ─── World Manager ───────────────────────────────────────────────────────────

    public void openWorldManager(Player player) {
        List<World> worlds = Bukkit.getWorlds();
        int size = Math.max(27, ceilToNine(worlds.size() + 9)); // +9 for bottom row
        Inventory inv = Bukkit.createInventory(null, size, TITLE_WORLDS);

        // Fill bottom row
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, filler);
        }

        FileConfiguration config = plugin.getConfig();

        for (int i = 0; i < worlds.size() && i < size - 9; i++) {
            World world = worlds.get(i);
            boolean enabled = config.getBoolean("world-settings." + world.getName() + ".anti-xray.ray-trace",
                    config.getBoolean("world-settings.default.anti-xray.ray-trace", true));

            Material pane = enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            String status = enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";

            inv.setItem(i, createItem(pane,
                    ChatColor.GOLD + world.getName(),
                    List.of(ChatColor.GRAY + "Ray-Trace: " + status,
                            ChatColor.GRAY + "Environment: " + ChatColor.WHITE + world.getEnvironment().name(),
                            "",
                            ChatColor.YELLOW + "Click to toggle")));
        }

        // Back button
        inv.setItem(size - 5, createItem(Material.BARRIER, ChatColor.RED + "← Back",
                List.of(ChatColor.GRAY + "Return to main menu")));

        player.openInventory(inv);
    }

    // ─── Utilities ───────────────────────────────────────────────────────────────

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Formats a Material enum name to human-readable form.
     * DEEPSLATE_DIAMOND_ORE → Deepslate Diamond Ore
     */
    public static String formatMaterial(Material mat) {
        String[] parts = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static int ceilToNine(int n) {
        return ((n + 8) / 9) * 9;
    }
}
