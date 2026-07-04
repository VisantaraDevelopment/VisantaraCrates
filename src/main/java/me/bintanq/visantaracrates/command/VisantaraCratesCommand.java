package me.bintanq.visantaracrates.command;

import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.model.Crate;
import me.bintanq.visantaracrates.util.MessageManager;
import me.bintanq.visantaracrates.util.ReloadUtil;
import me.bintanq.visantaracrates.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.bintanq.visantaracrates.util.PhysicalCrateItem;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.stream.Collectors;

public class VisantaraCratesCommand implements CommandExecutor, TabCompleter {

    private final VisantaraCrates plugin;

    public VisantaraCratesCommand(VisantaraCrates plugin) {
        this.plugin = plugin;
        var cmd = plugin.getCommand("VisantaraCrates");
        Objects.requireNonNull(cmd).setExecutor(this);
        Objects.requireNonNull(cmd).setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "reload"    -> cmdReload(sender);
            case "give"      -> cmdGive(sender, args);
            case "open"      -> cmdOpen(sender, args);
            case "info"      -> cmdInfo(sender, args);
            case "list"      -> cmdList(sender);
            case "setloc"    -> cmdSetLoc(sender, args);
            case "delloc" -> cmdDelLoc(sender, args);
            case "pity"      -> cmdPity(sender, args);
            case "resetpity" -> cmdResetPity(sender, args);
            case "resetlifetime" -> cmdResetLifetime(sender, args);
            default          -> sendHelp(sender);
        }
        return true;
    }

    private void cmdReload(CommandSender sender) {
        if (!sender.hasPermission("VisantaraCrates.admin")) { MessageManager.sendNoPermission(sender); return; }
        ReloadUtil.reloadAll(plugin);
        MessageManager.send(sender, "reload-success");
    }

    private void cmdGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("VisantaraCrates.key.give") && !sender.hasPermission("VisantaraCrates.admin")) {
            MessageManager.sendNoPermission(sender); return;
        }
        if (args.length < 5 || !args[1].equalsIgnoreCase("physical")) {
            sender.sendMessage(colorize("&cUsage: /vc give physical <player> <crateId> <amount> [free/premium]"));
            return;
        }

        String targetInput = args[2];
        String crateId = args[3];
        int amount;
        try {
            amount = Integer.parseInt(args[4]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MessageManager.sendInvalidNumber(sender); return;
        }

        Crate crate = plugin.getCrateManager().getCrate(crateId);
        if (crate == null) {
            sender.sendMessage(colorize("&cCrate not found: " + crateId));
            return;
        }

        Player online = Bukkit.getPlayer(targetInput);
        if (online == null) {
            MessageManager.sendPlayerNotFound(sender, targetInput);
            return;
        }

        String type = crate.getCrateType(); // Default type from the yml definition
        if (args.length >= 6) {
            String typeInput = args[5].toUpperCase();
            if (typeInput.equals("FREE") || typeInput.equals("PREMIUM")) {
                type = typeInput;
            } else {
                sender.sendMessage(colorize("&cInvalid type. Must be 'free' or 'premium'."));
                return;
            }
        }
        if (crateId.equalsIgnoreCase("VIPCrate")) {
            type = "FREE";
        }

        ItemStack item = PhysicalCrateItem.create(plugin, crate, amount, type);
        if (online.getInventory().firstEmpty() == -1) {
            online.getWorld().dropItemNaturally(online.getLocation(), item);
        } else {
            online.getInventory().addItem(item);
        }

        sender.sendMessage(colorize("&aSuccessfully gave " + amount + "x physical " + type + " " + crate.getId() + " crate(s) to " + online.getName()));
    }

    private String colorize(String s) {
        return s == null ? "" : s.replace("&", "\u00A7");
    }

    private void cmdOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { MessageManager.sendPlayerOnly(sender); return; }
        if (!player.hasPermission("VisantaraCrates.admin")) { MessageManager.sendNoPermission(sender); return; }
        if (args.length < 2) { MessageManager.send(sender, "usage-open"); return; }
        crateManager().openCrate(player, args[1]);
    }

    private void cmdInfo(CommandSender sender, String[] args) {
        if (args.length < 2) { MessageManager.send(sender, "usage-info"); return; }
        Crate crate = crateManager().getCrate(args[1]);
        if (crate == null) { MessageManager.sendCrateNotFound(sender, args[1]); return; }

        String crateName = crate.getDisplayName() != null ? crate.getDisplayName() : crate.getId();
        MessageManager.send(sender, "info-header", "{crate}", crateName);
        MessageManager.send(sender, "info-id", "{crate}", crate.getId());
        MessageManager.send(sender, crate.isEnabled() ? "info-status-on" : "info-status-off");
        MessageManager.send(sender, "info-rewards", "{count}", String.valueOf(crate.getRewards().size()));
        MessageManager.send(sender, "info-total-weight", "{weight}", String.format("%.2f", crate.getTotalWeight()));
        MessageManager.send(sender, "info-cooldown", "{time}",
                crate.getCooldownMs() > 0 ? TimeUtil.formatDuration(crate.getCooldownMs()) : MessageManager.getRaw("cooldown-none"));

        if (crate.getPity().isEnabled()) {
            MessageManager.send(sender, "info-pity-on",
                    "{max}", String.valueOf(crate.getPity().getThreshold()),
                    "{soft}", String.valueOf(crate.getPity().getSoftPityStart()));
        } else {
            MessageManager.send(sender, "info-pity-off");
        }

        MessageManager.send(sender, crate.isMassOpenEnabled() ? "info-massopen-on" : "info-massopen-off",
                "{limit}", crate.getMassOpenLimit() < 0 ? "unlimited" : String.valueOf(crate.getMassOpenLimit()));
        MessageManager.send(sender, "info-schedule",
                "{schedule}", crate.getSchedule() != null ? crate.getSchedule().getNextOpenDescription() : MessageManager.getRaw("schedule-always"));
        MessageManager.send(sender, crate.isCurrentlyOpenable() ? "info-openable" : "info-not-openable");

        if (!crate.getLocations().isEmpty()) {
            MessageManager.send(sender, "info-location-count",
                    "{count}", String.valueOf(crate.getLocations().size()));
            crate.getLocations().forEach(l ->
                    MessageManager.send(sender, "info-location",
                            "{world}", l.world,
                            "{x}", String.valueOf((int) l.x),
                            "{y}", String.valueOf((int) l.y),
                            "{z}", String.valueOf((int) l.z)));
        } else {
            MessageManager.send(sender, "info-no-location", "{crate}", crate.getId());
        }

    }

    private void cmdList(CommandSender sender) {
        Collection<Crate> crates = crateManager().getAllCrates();
        if (crates.isEmpty()) { MessageManager.send(sender, "list-empty"); return; }
        MessageManager.send(sender, "list-header", "{count}", String.valueOf(crates.size()));
        crates.forEach(c -> MessageManager.send(sender, "list-entry",
                "{id}", c.getId(),
                "{name}", c.getDisplayName() != null ? c.getDisplayName() : c.getId(),
                "{status}", c.isEnabled() ? MessageManager.getRaw("list-status-on") : MessageManager.getRaw("list-status-off"),
                "{rewards}", String.valueOf(c.getRewards().size()),
                "{keys}", "0"));
    }

    private void cmdSetLoc(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { MessageManager.sendPlayerOnly(sender); return; }
        if (!player.hasPermission("VisantaraCrates.admin")) { MessageManager.sendNoPermission(sender); return; }
        if (args.length < 2) { MessageManager.send(sender, "usage-setloc"); return; }

        Crate crate = crateManager().getCrate(args[1]);
        if (crate == null) { MessageManager.sendCrateNotFound(sender, args[1]); return; }

        var targeted = player.getTargetBlockExact(5);
        if (targeted == null) { MessageManager.send(sender, "setloc-no-target"); return; }

        var loc = targeted.getLocation();

        // Check no other crate already owns this block
        for (Crate other : crateManager().getAllCrates()) {
            if (other.getId().equals(crate.getId())) continue;
            if (other.hasLocationAt(loc.getWorld().getName(),
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                MessageManager.send(sender, "setloc-already-taken", "{crate}", other.getId());
                return;
            }
        }

        Crate.SerializableLocation newLoc = new Crate.SerializableLocation(
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        boolean added = crate.addLocation(newLoc);
        if (!added) {
            // Already exists — inform the admin
            sender.sendMessage(MessageManager.color("&eThis block is already a location for &6" + crate.getId() + "&e."));
            return;
        }
        crateManager().saveCrate(crate);

        if (plugin.getParticleManager()  != null) plugin.getParticleManager().startIdleParticles(crate);

        MessageManager.send(sender, "setloc-success",
                "{crate}", crate.getId(),
                "{x}", String.valueOf(loc.getBlockX()),
                "{y}", String.valueOf(loc.getBlockY()),
                "{z}", String.valueOf(loc.getBlockZ()),
                "{world}", loc.getWorld().getName());
    }

    private void cmdDelLoc(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { MessageManager.sendPlayerOnly(sender); return; }
        if (!sender.hasPermission("VisantaraCrates.admin")) { MessageManager.sendNoPermission(sender); return; }
        if (args.length < 2) { MessageManager.send(sender, "usage-delloc"); return; }

        Crate crate = crateManager().getCrate(args[1]);
        if (crate == null) { MessageManager.sendCrateNotFound(sender, args[1]); return; }
        if (crate.getLocations().isEmpty()) {
            MessageManager.send(sender, "delloc-no-location", "{crate}", crate.getId()); return;
        }

        List<Crate.SerializableLocation> locs = crate.getLocations();

        if (locs.size() == 1 && args.length < 3) {
            performDelLoc(player, crate, 0);
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(MessageManager.color("&8&l━━━ &e" + crate.getId() + " &7Locations &8━━━"));
            for (int i = 0; i < locs.size(); i++) {
                Crate.SerializableLocation loc = locs.get(i);
                var component = net.kyori.adventure.text.Component.text()
                        .append(net.kyori.adventure.text.Component.text("  [" + i + "] ")
                                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD))
                        .append(net.kyori.adventure.text.Component.text(
                                        loc.world + " " + (int)loc.x + ", " + (int)loc.y + ", " + (int)loc.z)
                                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY))
                        .append(net.kyori.adventure.text.Component.text(" [Click to remove]")
                                .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                                .decorate(net.kyori.adventure.text.format.TextDecoration.ITALIC)
                                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(
                                        "/qc delloc " + crate.getId() + " " + i))
                                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                        net.kyori.adventure.text.Component.text("Remove location " + i)
                                                .color(net.kyori.adventure.text.format.NamedTextColor.RED))))
                        .build();
                plugin.adventure().player(player).sendMessage(component);
            }
            sender.sendMessage(MessageManager.color("&7Or: &e/qc delloc " + crate.getId() + " all &7to remove all"));
            return;
        }

        String indexArg = args[2];
        if (indexArg.equalsIgnoreCase("all")) {
            crate.setLocations(new java.util.ArrayList<>());
            crateManager().saveCrate(crate);
            if (plugin.getParticleManager() != null) plugin.getParticleManager().stopIdleParticles(crate.getId());
            MessageManager.send(sender, "delloc-success", "{crate}", crate.getId());
            return;
        }

        int idx;
        try { idx = Integer.parseInt(indexArg); }
        catch (NumberFormatException e) {
            MessageManager.send(sender, "delloc-index-invalid"); return;
        }
        performDelLoc(player, crate, idx);
    }

    private void performDelLoc(Player player, Crate crate, int idx) {
        if (!crate.removeLocation(idx)) {
            MessageManager.send(player, "delloc-index-invalid"); return;
        }
        crateManager().saveCrate(crate);
        if (plugin.getParticleManager() != null) plugin.getParticleManager().stopIdleParticles(crate.getId());
        if (!crate.getLocations().isEmpty()) {
            if (plugin.getParticleManager() != null) plugin.getParticleManager().startIdleParticles(crate);
        }
        MessageManager.send(player, "delloc-success", "{crate}", crate.getId());
    }

    private void cmdPity(CommandSender sender, String[] args) {
        if (!sender.hasPermission("VisantaraCrates.admin")) { MessageManager.sendNoPermission(sender); return; }
        if (!plugin.isPityEnabled()) {
            sender.sendMessage(MessageManager.color("&cThe pity system is globally disabled."));
            return;
        }
        if (args.length < 3) { MessageManager.send(sender, "usage-pity"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageManager.sendPlayerNotFound(sender, args[1]); return; }

        String crateId = args[2];
        Crate crate = crateManager().getCrate(crateId);
        if (crate == null) { MessageManager.sendCrateNotFound(sender, crateId); return; }

        int pity = plugin.getPlayerDataManager().getPity(target.getUniqueId(), crateId);
        int max  = crate.getPity().getThreshold();
        int soft = crate.getPity().getSoftPityStart();
        boolean softActive = pity >= soft;
        boolean hardActive = pity >= max;

        MessageManager.send(sender, "pity-info",
                "{player}", target.getName(),
                "{crate}", crateId,
                "{current}", String.valueOf(pity),
                "{max}", String.valueOf(max),
                "{soft}", String.valueOf(soft),
                "{status}", hardActive
                        ? MessageManager.getRaw("pity-status-hard")
                        : softActive
                        ? MessageManager.getRaw("pity-status-soft")
                        : MessageManager.getRaw("pity-status-normal"));
    }

    private void cmdResetPity(CommandSender sender, String[] args) {
        if (!sender.hasPermission("VisantaraCrates.admin")) { MessageManager.sendNoPermission(sender); return; }
        if (!plugin.isPityEnabled()) {
            sender.sendMessage(MessageManager.color("&cThe pity system is globally disabled."));
            return;
        }
        if (args.length < 3) { MessageManager.send(sender, "usage-resetpity"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageManager.sendPlayerNotFound(sender, args[1]); return; }

        String crateId = args[2];
        if (crateManager().getCrate(crateId) == null) { MessageManager.sendCrateNotFound(sender, crateId); return; }

        plugin.getPlayerDataManager().resetPity(target.getUniqueId(), crateId);
        MessageManager.send(sender, "pity-reset-done", "{player}", target.getName(), "{crate}", crateId);
    }



    private void cmdResetLifetime(CommandSender sender, String[] args) {
        if (!sender.hasPermission("VisantaraCrates.admin")) { MessageManager.sendNoPermission(sender); return; }
        if (args.length < 3) { MessageManager.send(sender, "usage-resetlifetime"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageManager.sendPlayerNotFound(sender, args[1]); return; }

        String crateId = args[2];
        if (crateManager().getCrate(crateId) == null) { MessageManager.sendCrateNotFound(sender, crateId); return; }

        String playerName = target.getName();
        plugin.getDatabaseManager().loadPlayerData(target.getUniqueId())
                .thenAccept(data -> {
                    int before = data.getLifetimeOpens(crateId);
                    plugin.getPlayerDataManager().resetLifetimeOpens(target.getUniqueId(), crateId);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            MessageManager.send(sender, "lifetime-reset-done",
                                    "{player}", playerName, "{crate}", crateId, "{count}", String.valueOf(before)));
                });
    }

    private void sendHelp(CommandSender sender) {
        MessageManager.send(sender, "help-header");

        boolean admin = sender.hasPermission("VisantaraCrates.admin");
        boolean give = sender.hasPermission("VisantaraCrates.key.give");

        if (admin) MessageManager.send(sender, "help-reload");
        if (admin || give) MessageManager.send(sender, "help-give");
        if (admin) MessageManager.send(sender, "help-open");
        MessageManager.send(sender, "help-info");
        MessageManager.send(sender, "help-list");
        if (admin) MessageManager.send(sender, "help-setloc");
        if (admin) MessageManager.send(sender, "help-delloc");
        if (admin && plugin.isPityEnabled()) MessageManager.send(sender, "help-pity");
        if (admin && plugin.isPityEnabled()) MessageManager.send(sender, "help-resetpity");
        if (admin) MessageManager.send(sender, "help-resetlifetime");

        if (admin) {
            for (String key : List.of("controls-header","ctrl-left","ctrl-right","ctrl-shift")) {
                MessageManager.send(sender, "help-" + key);
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        boolean admin = sender.hasPermission("VisantaraCrates.admin");
        boolean give = sender.hasPermission("VisantaraCrates.key.give");

        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("info", "list"));
            if (admin) {
                options.addAll(List.of("reload", "open", "setloc", "delloc", "resetlifetime"));
                if (plugin.isPityEnabled()) {
                    options.addAll(List.of("pity", "resetpity"));
                }
            }
            if (admin || give) options.add("give");
            return filter(options, args[0]);
        }

        return switch (args[0].toLowerCase()) {
            case "open","setloc","delloc" ->
                    admin && args.length == 2 ? filter(crateIds(), args[1]) : List.of();
            case "info" ->
                    args.length == 2 ? filter(crateIds(), args[1]) : List.of();
            case "give" ->
                    (admin || give) ? (
                            args.length == 2 ? filter(List.of("physical"), args[1])
                            : args[1].equalsIgnoreCase("physical") ? (
                                    args.length == 3 ? filter(onlinePlayers(), args[2])
                                    : args.length == 4 ? filter(crateIds(), args[3])
                                    : args.length == 5 ? filter(List.of("1","5","10","32","64"), args[4])
                                    : args.length == 6 ? filter(List.of("free","premium"), args[5])
                                    : List.of()
                              )
                            : List.of()
                    ) : List.of();
            case "pity","resetpity" ->
                    admin && plugin.isPityEnabled() ? (args.length == 2 ? filter(onlinePlayers(), args[1])
                            : args.length == 3 ? filter(crateIds(), args[2])
                            : List.of()) : List.of();
            case "resetlifetime" ->
                    admin ? (args.length == 2 ? filter(onlinePlayers(), args[1])
                            : args.length == 3 ? filter(crateIds(), args[2])
                            : List.of()) : List.of();
            default -> List.of();
        };
    }

    private me.bintanq.visantaracrates.manager.CrateManager crateManager() { return plugin.getCrateManager(); }

    private List<String> filter(List<String> opts, String input) {
        return opts.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> crateIds()      { return new ArrayList<>(crateManager().getCrateRegistry().keySet()); }
    private List<String> onlinePlayers() { return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()); }
    private List<String> mergeLists(List<String> a, List<String> b) {
        List<String> res = new ArrayList<>(a);
        res.addAll(b);
        return res;
    }
}