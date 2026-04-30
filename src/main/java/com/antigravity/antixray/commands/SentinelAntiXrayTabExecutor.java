package com.antigravity.antixray.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.antigravity.antixray.SentinelAntiXray;
import com.antigravity.antixray.heuristics.MiningTracker;

public final class SentinelAntiXrayTabExecutor implements TabExecutor {
    private final SentinelAntiXray plugin;

    public SentinelAntiXrayTabExecutor(SentinelAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        LinkedList<String> completions = new LinkedList<>();

        if (command.getName().equalsIgnoreCase("sentinelantixray")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase(Locale.ROOT);
                if (sender.hasPermission("sentinel.antixray.timings") && "timings".startsWith(partial)) {
                    completions.add("timings");
                }
                if (sender.hasPermission("sentinel.antixray.command") && "stats".startsWith(partial)) {
                    completions.add("stats");
                }
                if (sender.hasPermission("sentinel.antixray.command") && "reset".startsWith(partial)) {
                    completions.add("reset");
                }
                if (sender.hasPermission("sentinel.antixray.admin") && "gui".startsWith(partial)) {
                    completions.add("gui");
                }
                if (sender.hasPermission("sentinel.antixray.admin") && "reload".startsWith(partial)) {
                    completions.add("reload");
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("timings")) {
                    if (sender.hasPermission("sentinel.antixray.timings")) {
                        if ("on".startsWith(args[1].toLowerCase(Locale.ROOT))) completions.add("on");
                        if ("off".startsWith(args[1].toLowerCase(Locale.ROOT))) completions.add("off");
                    }
                } else if (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("reset")) {
                    if (sender.hasPermission("sentinel.antixray.command")) {
                        String partial = args[1].toLowerCase(Locale.ROOT);
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getName().toLowerCase(Locale.ROOT).startsWith(partial)) {
                                completions.add(player.getName());
                            }
                        }
                    }
                }
            }
        }

        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("sentinelantixray")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "Sentinel Anti-Xray v" + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.GRAY + "Commands: timings, stats, reset, gui, reload");
            return true;
        }

        String sub = args[0];

        if (sub.equalsIgnoreCase("timings")) {
            return handleTimings(sender, args);
        } else if (sub.equalsIgnoreCase("stats")) {
            return handleStats(sender, args);
        } else if (sub.equalsIgnoreCase("reset")) {
            return handleReset(sender, args);
        } else if (sub.equalsIgnoreCase("gui")) {
            return handleGUI(sender);
        } else if (sub.equalsIgnoreCase("reload")) {
            return handleReload(sender);
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown argument. Use: timings, stats, reset, gui, reload");
            return true;
        }
    }

    private boolean handleTimings(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sentinel.antixray.timings")) {
            sender.sendMessage(ChatColor.RED + "You don't have permissions.");
            return true;
        }
        if (args.length == 2) {
            if (args[1].equalsIgnoreCase("on")) {
                plugin.setTimingsEnabled(true);
                sender.sendMessage(ChatColor.GREEN + "Timings turned on.");
                return true;
            } else if (args[1].equalsIgnoreCase("off")) {
                plugin.setTimingsEnabled(false);
                sender.sendMessage(ChatColor.RED + "Timings turned off.");
                return true;
            }
        }
        sender.sendMessage(ChatColor.RED + "Usage: /sax timings <on|off>");
        return true;
    }

    private boolean handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sentinel.antixray.command")) {
            sender.sendMessage(ChatColor.RED + "You don't have permissions.");
            return true;
        }
        if (args.length == 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                plugin.getMiningTracker().printStats(sender, target);
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            }
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Usage: /sax stats <player>");
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("sentinel.antixray.command")) {
            sender.sendMessage(ChatColor.RED + "You don't have permissions.");
            return true;
        }
        if (args.length == 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                plugin.getMiningTracker().resetStats(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Reset mining stats for " + target.getName());
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            }
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Usage: /sax reset <player>");
        return true;
    }

    private boolean handleGUI(CommandSender sender) {
        if (!sender.hasPermission("sentinel.antixray.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permissions.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }
        plugin.getGUIListener().getGUI().openMainMenu(player);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("sentinel.antixray.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permissions.");
            return true;
        }
        plugin.reloadPlugin();
        sender.sendMessage(ChatColor.GREEN + "[Sentinel] " + ChatColor.GRAY + "Configuration reloaded.");
        sender.sendMessage(ChatColor.GRAY + "Block lists and thresholds updated.");
        sender.sendMessage(ChatColor.GRAY + "Note: World ray-trace changes require a restart.");
        return true;
    }
}
