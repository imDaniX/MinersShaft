package me.imdanix.mine.mineshaft;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import me.imdanix.mine.WeightedCollection;
import me.imdanix.mine.mineshaft.cooldown.Cooldowns;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class ShaftsManager implements Listener {

    private final Plugin plugin;
    private final Map<Material, Set<Mineshaft>> shaftsByType;
    @Getter
    private final Map<String, Mineshaft> shafts;
    @Getter
    private final Cooldowns cooldowns;
    private final Set<BlockBreakEvent> inUse;
    private final Random rng;

    @Getter
    private final Map<String, String> messages;

    public ShaftsManager(Plugin plugin) {
        this.plugin = plugin;
        shaftsByType = new EnumMap<>(Material.class);
        shafts = new HashMap<>();
        cooldowns = new Cooldowns(plugin);
        inUse = new HashSet<>();
        messages = new HashMap<>();
        rng = ThreadLocalRandom.current();
        Bukkit.getScheduler().runTaskTimer(plugin, cooldowns::updateBlocks, 20, 20);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void wgBypass(BlockBreakEvent event) {
        if (inUse.remove(event)) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            cooldowns.removeBlock(block);
            return;
        }
        Player player = event.getPlayer();
        Set<Mineshaft> shafts = this.shaftsByType.get(block.getType());
        if (shafts == null) {
            long cooldown = cooldowns.getCooldown(block);
            if (cooldown > 0) {
                event.setCancelled(true);
                player.sendMessage(messages.get("cooldown").replace("{seconds}", Long.toString(cooldown/1000)));
            }
            return;
        }
        World world = player.getWorld();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        for (Mineshaft shaft : shafts) {
            if(!world.getName().equalsIgnoreCase(shaft.getWorld())) {
                continue;
            }
            ProtectedRegion region = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)).getRegion(shaft.getRegion());
            if (region == null || !region.contains(x, y, z)) {
                continue;
            }
            event.setCancelled(true);
            MineableResource resource = shaft.getResource(block.getType());
            long cooldown = cooldowns.addBlock(block, resource.getCooldown());
            if (cooldown > 0) {
                player.sendMessage(messages.get("cooldown").replace("{seconds}", Long.toString(cooldown/1000)));
                return;
            } else {
                inUse.add(event);
                Location location = block.getLocation();
                if (!resource.getReplaces().isEmpty()) {
                    Bukkit.getScheduler().runTaskLater(
                            plugin,
                            () -> location.getBlock().setType(randomElement(resource.getReplaces())),
                            1
                    );
                }
                if (resource.isInstant()) {
                    event.setDropItems(false);
                    player.giveExp(event.getExpToDrop());
                    event.setExpToDrop(0);
                    player.getInventory().addItem(getCustomDrop(resource, block, player));
                } else if (!resource.getReplaces().isEmpty()) {
                    event.setDropItems(false);
                    world.dropItemNaturally(location.clone().add(0.5, 0.5, 0.5), getCustomDrop(resource, block, player));
                }
                return;
            }
        }
    }

    // such a mess
    public void reload(ConfigurationSection cfg) {
        ConfigurationSection messagesCfg = section("messages", cfg);
        messages.put("cooldown", clr(messagesCfg.getString("cooldown", "&4ERROR")));
        messages.put("reloaded", clr(messagesCfg.getString("reloaded", "&4ERROR")));

        shaftsByType.clear();
        shafts.clear();
        ConfigurationSection worldsCfg = section("worlds", cfg);
        for (String world : worldsCfg.getKeys(false)) {

            ConfigurationSection regionsCfg = section(world, worldsCfg);
            for (String region : regionsCfg.getKeys(false)) {

                Mineshaft shaft = new Mineshaft(world, region);
                ConfigurationSection materialsCfg = section(region, regionsCfg);
                for (String typeStr : materialsCfg.getKeys(false)) {
                    ConfigurationSection resourceCfg = section(typeStr, materialsCfg);
                    Material type = getMaterial(typeStr);
                    if (type == null) {
                        continue;
                    }
                    List<String> replacesStr = resourceCfg.getStringList("replace-with");
                    List<Material> replaces = replacesStr.isEmpty() ? Collections.emptyList() : new ArrayList<>();
                    for (String replaceStr : replacesStr) {
                        Material replace = getMaterial(replaceStr);
                        if (replace != null) {
                            replaces.add(replace);
                        }
                    }
                    List<String> dropsStr = resourceCfg.getStringList("custom-drop");
                    WeightedCollection<ItemStack> drops = new WeightedCollection<>();
                    for (String dropStr : dropsStr) {
                        String[] split = dropStr.split(" ");
                        Material dropType = getMaterial(split[0]);
                        if (dropType == null) {
                            continue;
                        }
                        int amount = split.length > 1 ? Integer.parseInt(split[1]) : 1;
                        int weight = split.length > 2 ? Integer.parseInt(split[2]) : 1;
                        drops.add(new ItemStack(dropType, amount), weight);
                    }
                    shaft.addResource(type,
                            new MineableResource(
                                    drops,
                                    replaces,
                                    resourceCfg.getLong("cooldown", 60L) * 1000,
                                    resourceCfg.getBoolean("instant", false)
                            )
                    );
                    if (!shaftsByType.containsKey(type)) {
                        shaftsByType.put(type, new HashSet<>());
                    }
                    shaftsByType.get(type).add(shaft);
                }
                shafts.put(region, shaft);
            }
        }
    }

    private  <T> T randomElement(List<T> list) {
        return list.get(rng.nextInt(list.size()));
    }

    private static ItemStack getCustomDrop(MineableResource resource, Block block, Player player) {
        return resource.getDrops().isEmpty() ?
                getFirst(block.getDrops(player.getInventory().getItemInMainHand()), new ItemStack(Material.STONE)) :
                resource.getDrops().next();
    }

    private static <T> T getFirst(Collection<T> coll, T def) {
        Iterator<T> iter = coll.iterator();
        return iter.hasNext() ? iter.next() : def;
    }

    private static ConfigurationSection section(String path, ConfigurationSection cfg) {
        return cfg.isConfigurationSection(path) ? cfg.getConfigurationSection(path) : cfg.createSection(path);
    }

    private static Material getMaterial(String typeStr) {
        return Material.getMaterial(typeStr.toUpperCase(Locale.ENGLISH));
    }

    private static String clr(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}
