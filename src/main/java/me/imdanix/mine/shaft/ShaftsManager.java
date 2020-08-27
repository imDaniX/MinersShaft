package me.imdanix.mine.shaft;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import me.imdanix.mine.shaft.cooldown.Cooldowns;
import me.imdanix.mine.util.Utils;
import me.imdanix.mine.util.WeightedCollection;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    private Effect breakEffect;
    private Effect cooldownEffect;

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
                cooldownEffect.play(block.getLocation());
                player.sendMessage(messages.get("cooldown").replace("{seconds}", Long.toString(cooldown/1000)));
                event.setCancelled(true);
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
                cooldownEffect.play(block.getLocation());
                player.sendMessage(messages.get("cooldown").replace("{seconds}", Long.toString(cooldown/1000)));
                return;
            } else {
                inUse.add(event);
                Location location = block.getLocation();
                breakEffect.play(location);
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
                    player.getInventory().addItem(Utils.getCustomDrop(resource, block, player));
                } else if (!resource.getReplaces().isEmpty()) {
                    event.setDropItems(false);
                    world.dropItemNaturally(location.add(0.5, 0.5, 0.5), Utils.getCustomDrop(resource, block, player));
                }
                return;
            }
        }
    }

    // such a mess
    public void reload(ConfigurationSection cfg) {
        ConfigurationSection messagesCfg = Utils.section("messages", cfg);
        messages.put("cooldown", Utils.clr(messagesCfg.getString("cooldown", "&4ERROR")));
        messages.put("reloaded", Utils.clr(messagesCfg.getString("reloaded", "&4ERROR")));

        ConfigurationSection effectsCfg = Utils.section("effects", cfg);
        breakEffect = new Effect(
                Utils.getEnum(Sound.class, effectsCfg.getString("break.sound")),
                Utils.getEnum(Particle.class, effectsCfg.getString("break.particle")));
        cooldownEffect = new Effect(
                Utils.getEnum(Sound.class, effectsCfg.getString("cooldown.sound")),
                Utils.getEnum(Particle.class, effectsCfg.getString("cooldown.particle")));
        cooldowns.setRegenEffect(new Effect(
                Utils.getEnum(Sound.class, effectsCfg.getString("regen.sound")),
                Utils.getEnum(Particle.class, effectsCfg.getString("regen.particle"))));

        shaftsByType.clear();
        shafts.clear();
        ConfigurationSection worldsCfg = Utils.section("worlds", cfg);
        for (String world : worldsCfg.getKeys(false)) {

            ConfigurationSection regionsCfg = Utils.section(world, worldsCfg);
            for (String region : regionsCfg.getKeys(false)) {

                Mineshaft shaft = new Mineshaft(world, region);
                ConfigurationSection materialsCfg = Utils.section(region, regionsCfg);
                for (String typeStr : materialsCfg.getKeys(false)) {
                    ConfigurationSection resourceCfg = Utils.section(typeStr, materialsCfg);
                    Material type = Utils.getMaterial(typeStr);
                    if (type == null) {
                        continue;
                    }
                    List<String> replacesStr = resourceCfg.getStringList("replace-with");
                    List<Material> replaces = replacesStr.isEmpty() ? Collections.emptyList() : new ArrayList<>();
                    for (String replaceStr : replacesStr) {
                        Material replace = Utils.getMaterial(replaceStr);
                        if (replace != null) {
                            replaces.add(replace);
                        }
                    }
                    List<String> dropsStr = resourceCfg.getStringList("custom-drop");
                    WeightedCollection<ItemStack> drops = new WeightedCollection<>();
                    for (String dropStr : dropsStr) {
                        String[] split = dropStr.split(" ");
                        Material dropType = Utils.getMaterial(split[0]);
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

    private <T> T randomElement(List<T> list) {
        return list.get(rng.nextInt(list.size()));
    }

}
