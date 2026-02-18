package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.managers.ZoneManager;
import com.galyakyxnya.pvpzone.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PvpZoneCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public PvpZoneCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(Lang.get(plugin, "no_permission"));
                    return true;
                }
                createZone(sender, args);
                break;

            case "list":
                listZones(sender);
                break;

            case "delete":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(Lang.get(plugin, "no_permission"));
                    return true;
                }
                deleteZone(sender, args);
                break;

            case "info":
                zoneInfo(sender, args);
                break;

            case "bonuses":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(Lang.get(plugin, "no_permission"));
                    return true;
                }
                toggleBonuses(sender, args);
                break;

            case "kit":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(Lang.get(plugin, "no_permission"));
                    return true;
                }
                handleZoneKit(sender, args);
                break;

            case "reload":
                if (!sender.hasPermission("pvpzone.admin")) {
                    sender.sendMessage(Lang.get(plugin, "no_permission"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(Lang.get(plugin, "pvpzone_reload_ok"));
                break;

            case "status":
                showStatus(sender);
                break;

            case "help":
            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void createZone(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_create_player_only"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_create_usage"));
            return;
        }

        Player player = (Player) sender;
        String zoneName = args[1];

        var config = plugin.getConfig();

        if (!config.contains("temp.pos1") || !config.contains("temp.pos2")) {
            player.sendMessage(Lang.get(plugin, "pvpzone_set_points_first"));
            player.sendMessage(Lang.get(plugin, "pvpzone_use_pvpz"));
            return;
        }

        Location pos1 = loadLocationFromConfig("temp.pos1");
        Location pos2 = loadLocationFromConfig("temp.pos2");

        if (pos1 == null || pos2 == null) {
            player.sendMessage(Lang.get(plugin, "pvpzone_load_error"));
            return;
        }

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage(Lang.get(plugin, "pvpzone_same_world"));
            return;
        }

        boolean success = plugin.getZoneManager().createZone(zoneName, pos1, pos2, true);

        if (success) {
            player.sendMessage(Lang.get(plugin, "zone_enter_title"));
            player.sendMessage(Lang.get(plugin, "pvpzone_created_success", "%name%", zoneName));

            ZoneManager.PvpZone zone = plugin.getZoneManager().getZone(zoneName);
            if (zone != null) {
                Location zonePos1 = zone.getPos1();
                Location zonePos2 = zone.getPos2();

                double width = Math.max(zonePos1.getX(), zonePos2.getX()) - Math.min(zonePos1.getX(), zonePos2.getX());
                double length = Math.max(zonePos1.getZ(), zonePos2.getZ()) - Math.min(zonePos1.getZ(), zonePos2.getZ());

                player.sendMessage(Lang.get(plugin, "pvpzone_zone_sizes"));
                player.sendMessage(Lang.get(plugin, "pvpzone_width", "%value%", String.format("%.1f", width)));
                player.sendMessage(Lang.get(plugin, "pvpzone_length", "%value%", String.format("%.1f", length)));
                player.sendMessage(Lang.get(plugin, "pvpzone_height", "%value%", String.valueOf(zonePos1.getWorld().getMaxHeight())));
                player.sendMessage(Lang.get(plugin, "pvpzone_world", "%value%", zonePos1.getWorld().getName()));
                String bonusVal = zone.isBonusesEnabled() ? Lang.get(plugin, "pvpzone_bonuses_on") : Lang.get(plugin, "pvpzone_bonuses_off");
                player.sendMessage(Lang.get(plugin, "pvpzone_bonuses_label", "%value%", bonusVal));
                player.sendMessage(Lang.get(plugin, "pvpzone_kit_label", "%value%", zone.getKitName()));
            }

            player.sendMessage(Lang.get(plugin, "zone_enter_title"));

            config.set("temp.pos1", null);
            config.set("temp.pos2", null);
            plugin.saveConfig();

        } else {
            player.sendMessage(Lang.get(plugin, "pvpzone_exists"));
        }
    }

    private void listZones(CommandSender sender) {
        var zoneManager = plugin.getZoneManager();
        var zones = zoneManager.getAllZones();

        if (zones.isEmpty()) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_no_zones"));
            return;
        }

        sender.sendMessage(Lang.get(plugin, "pvpzone_list_header"));

        for (ZoneManager.PvpZone zone : zones) {
            Location pos1 = zone.getPos1();
            Location pos2 = zone.getPos2();
            double width = Math.max(pos1.getX(), pos2.getX()) - Math.min(pos1.getX(), pos2.getX());
            double length = Math.max(pos1.getZ(), pos2.getZ()) - Math.min(pos1.getZ(), pos2.getZ());
            String size = String.format("%.0f", width) + "×" + String.format("%.0f", length);
            sender.sendMessage(Lang.get(plugin, "pvpzone_list_entry", "%name%", zone.getName(), "%size%", size));
        }
    }

    private void deleteZone(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_delete_usage"));
            return;
        }

        String zoneName = args[1];
        boolean success = plugin.getZoneManager().removeZone(zoneName);

        if (success) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_deleted"));
        } else {
            sender.sendMessage(Lang.get(plugin, "pvpzone_not_found", "%name%", zoneName));
        }
    }

    private void zoneInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_info_usage"));
            return;
        }

        String zoneName = args[1];
        ZoneManager.PvpZone zone = plugin.getZoneManager().getZone(zoneName);

        if (zone == null) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_not_found", "%name%", zoneName));
            return;
        }

        Location pos1 = zone.getPos1();
        Location pos2 = zone.getPos2();

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        double width = maxX - minX;
        double length = maxZ - minZ;
        double area = width * length;

        sender.sendMessage(Lang.get(plugin, "pvpzone_info_header", "%name%", zoneName));
        sender.sendMessage(Lang.get(plugin, "pvpzone_info_coords"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_info_x", "%min%", String.format("%.1f", minX), "%max%", String.format("%.1f", maxX)));
        sender.sendMessage(Lang.get(plugin, "pvpzone_info_z", "%min%", String.format("%.1f", minZ), "%max%", String.format("%.1f", maxZ)));
        sender.sendMessage(Lang.get(plugin, "pvpzone_info_y", "%max%", String.valueOf(pos1.getWorld().getMaxHeight())));

        sender.sendMessage(Lang.get(plugin, "pvpzone_info_sizes"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_info_width", "%value%", String.format("%.1f", width)));
        sender.sendMessage(Lang.get(plugin, "pvpzone_info_length", "%value%", String.format("%.1f", length)));
        sender.sendMessage(Lang.get(plugin, "pvpzone_info_area", "%value%", String.format("%.0f", area)));

        sender.sendMessage(Lang.get(plugin, "pvpzone_info_settings"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_info_kit", "%value%", zone.getKitName()));
        String bonusVal = zone.isBonusesEnabled() ? Lang.get(plugin, "pvpzone_bonuses_on") : Lang.get(plugin, "pvpzone_bonuses_off");
        sender.sendMessage(Lang.get(plugin, "pvpzone_bonuses_label", "%value%", bonusVal));
        sender.sendMessage(Lang.get(plugin, "pvpzone_info_world", "%value%", pos1.getWorld().getName()));

        if (zone.getCenter() != null) {
            Location center = zone.getCenter();
            sender.sendMessage(Lang.get(plugin, "pvpzone_info_center"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_info_center_x", "%value%", String.format("%.1f", center.getX())));
            sender.sendMessage(Lang.get(plugin, "pvpzone_info_center_y", "%value%", String.format("%.1f", center.getY())));
            sender.sendMessage(Lang.get(plugin, "pvpzone_info_center_z", "%value%", String.format("%.1f", center.getZ())));
        }
    }

    private void toggleBonuses(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_bonuses_usage"));
            return;
        }

        String zoneName = args[1];
        String action = args[2].toLowerCase();

        ZoneManager.PvpZone zone = plugin.getZoneManager().getZone(zoneName);
        if (zone == null) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_not_found", "%name%", zoneName));
            return;
        }

        if (action.equals("on")) {
            zone.setBonusesEnabled(true);
            sender.sendMessage(Lang.get(plugin, "pvpzone_bonuses_on_ok", "%name%", zoneName));
        } else if (action.equals("off")) {
            zone.setBonusesEnabled(false);
            sender.sendMessage(Lang.get(plugin, "pvpzone_bonuses_off_ok", "%name%", zoneName));
        } else {
            sender.sendMessage(Lang.get(plugin, "pvpzone_bonuses_usage"));
            return;
        }

        plugin.getZoneManager().saveZones();
    }

    private void handleZoneKit(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_kit_usage"));
            return;
        }

        // Используем PvpZoneKitCommand
        String[] newArgs = new String[args.length - 1];
        newArgs[0] = args[1]; // subCommand (set/save/info)
        newArgs[1] = args[2]; // zoneName

        if (args.length > 3) {
            newArgs[2] = args[3]; // kitName (для set)
        }

        PvpZoneKitCommand kitCommand = new PvpZoneKitCommand(plugin);
        kitCommand.onCommand(sender, null, "pvpzone", newArgs);
    }

    private void showStatus(CommandSender sender) {
        var zoneManager = plugin.getZoneManager();

        sender.sendMessage(Lang.get(plugin, "pvpzone_status_header"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_status_zones", "%value%", String.valueOf(zoneManager.getAllZones().size())));
        sender.sendMessage(Lang.get(plugin, "pvpzone_status_players", "%value%", String.valueOf(zoneManager.getPlayersInZoneCount())));
        sender.sendMessage(Lang.get(plugin, plugin.getKitManager().isKitSet() ? "pvpzone_status_kit_set" : "pvpzone_status_kit_not"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_status_shop"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_status_db", "%value%", String.valueOf(plugin.getPlayerDataManager().getLoadedPlayersCount())));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Lang.get(plugin, "pvpzone_help_header"));

        if (sender.hasPermission("pvpzone.admin")) {
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_admin"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_pvpz1"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_pvpz2"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_pvpkit"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_create"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_delete"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_bonuses"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_kit_set"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_kit_save"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_kit_info"));
            sender.sendMessage(Lang.get(plugin, "pvpzone_help_reload"));
            sender.sendMessage("");
        }

        sender.sendMessage(Lang.get(plugin, "pvpzone_help_main"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_help_pvptop"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_help_pvpshop"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_help_list"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_help_info"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_help_status"));
        sender.sendMessage(Lang.get(plugin, "pvpzone_help_help"));
    }

    private Location loadLocationFromConfig(String path) {
        var config = plugin.getConfig();

        if (!config.contains(path)) {
            return null;
        }

        String worldName = config.getString(path + ".world");
        if (worldName == null) {
            return null;
        }

        var world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");

        return new Location(world, x, y, z);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("create");
            suggestions.add("list");
            suggestions.add("delete");
            suggestions.add("info");
            suggestions.add("bonuses");
            suggestions.add("kit");
            suggestions.add("reload");
            suggestions.add("status");
            suggestions.add("help");

            if (!sender.hasPermission("pvpzone.admin")) {
                suggestions.remove("create");
                suggestions.remove("delete");
                suggestions.remove("bonuses");
                suggestions.remove("kit");
                suggestions.remove("reload");
            }

        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("delete") || subCommand.equals("info") ||
                    subCommand.equals("bonuses") || subCommand.equals("kit")) {

                suggestions.addAll(plugin.getZoneManager().getZoneNames());
            }

        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("bonuses")) {
                suggestions.add("on");
                suggestions.add("off");
            } else if (subCommand.equals("kit")) {
                suggestions.add("set");
                suggestions.add("save");
                suggestions.add("info");
            }

        } else if (args.length == 4 && args[0].equalsIgnoreCase("kit") &&
                args[2].equalsIgnoreCase("set")) {
            // Предлагаем названия наборов
            suggestions.add("default");
            suggestions.add("arena");
            suggestions.add("duel");
            suggestions.add("tournament");
        }

        return suggestions;
    }
}