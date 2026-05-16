package com.pluginforge.rtp13tov8yohh5qplugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RtpCommand implements CommandExecutor, TabCompleter, Listener {
    private static final String GUI_TITLE = "Rtp Terminal";
    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();

    public RtpCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("reload".equals(sub) && player.hasPermission("rtp.admin")) {
                plugin.reloadConfig();
                player.sendMessage("Configuration reloaded.");
                return true;
            }
            if (("admin".equals(sub) || "gui".equals(sub)) && player.hasPermission("rtp.admin")) {
                openMenu(player);
                return true;
            }
            if ("help".equals(sub)) {
                player.sendMessage("/rtp - generated from your prompt.");
                return true;
            }
        }
        if (!player.hasPermission("rtp.use")) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }
        long cooldownSeconds = plugin.getConfig().getLong("cooldown-seconds", 45L);
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null) {
            long remaining = cooldownSeconds - ((now - last) / 1000L);
            if (remaining > 0) {
                player.sendMessage("Slow down — " + remaining + " seconds before using this again.");
                return true;
            }
        }
        cooldowns.put(player.getUniqueId(), now);
        int warmupSeconds = plugin.getConfig().getInt("warmup-seconds", 1);
        if (warmupSeconds > 0) {
            player.sendMessage("Starting warmup...");
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) performMainAction(player);
                }
            }.runTaskLater(plugin, warmupSeconds * 20L);
        } else {
            performMainAction(player);
        }
        return true;
    }

    private void performMainAction(Player player) {
        int min = plugin.getConfig().getInt("radius-min", 426);
        int max = plugin.getConfig().getInt("radius-max", 2869);
        Location safe = findSafeLocation(player.getWorld(), Math.max(1, min), Math.max(min + 1, max));
        if (safe == null) {
            player.sendMessage("Could not find a safe random teleport location. Try again.");
            return;
        }
        player.teleport(safe);
        player.sendMessage("Teleported to a safe random location.");
    }

    private Location findSafeLocation(World world, int min, int max) {
        int range = Math.max(1, max - min + 1);
        for (int i = 0; i < 32; i++) {
            int distance = min + random.nextInt(range);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int x = (int) Math.round(Math.cos(angle) * distance);
            int z = (int) Math.round(Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z);
            Location location = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
            if (isSafe(location)) return location;
        }
        return null;
    }

    private boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);
        Material groundType = ground.getType();
        return feet.getType().isAir()
                && head.getType().isAir()
                && groundType.isSolid()
                && !ground.isLiquid()
                && groundType != Material.LAVA
                && groundType != Material.MAGMA_BLOCK
                && groundType != Material.CACTUS;
    }

    private void openMenu(Player player) {
        Inventory inventory = plugin.getServer().createInventory(null, 9, GUI_TITLE);
        inventory.setItem(1, menuItem(Material.ENDER_PEARL, "Run Command"));
        inventory.setItem(4, menuItem(Material.CLOCK, "Cooldown: " + plugin.getConfig().getLong("cooldown-seconds", 45L) + "s"));
        inventory.setItem(5, menuItem(Material.COMPASS, "Prompt Settings"));
        player.openInventory(inventory);
    }

    private ItemStack menuItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.ENDER_PEARL) return;
        player.closeInventory();
        performMainAction(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> choices = new ArrayList<>(Arrays.asList("help"));
        if (sender.hasPermission("rtp.admin")) choices.add("reload");
        String prefix = args[0].toLowerCase(Locale.ROOT);
        choices.removeIf(value -> !value.startsWith(prefix));
        return choices;
    }
}
