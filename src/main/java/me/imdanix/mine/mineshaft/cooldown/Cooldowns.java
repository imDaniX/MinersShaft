package me.imdanix.mine.mineshaft.cooldown;

import lombok.Setter;
import me.imdanix.mine.mineshaft.Effect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class Cooldowns {
    private final Plugin plugin;
    private final SQLite database;
    private final SortedSet<CooledBlock> cooledBlocks;
    private final Map<Location, CooledBlock> byLocation;

    @Setter
    private Effect regenEffect;

    public Cooldowns(Plugin plugin) {
        this.plugin = plugin;
        database = new SQLite(plugin, "cooldowns");
        cooledBlocks = new TreeSet<>();
        byLocation = new HashMap<>();
        loadBlocks();
    }

    public long getCooldown(Block block) {
        CooledBlock cache = byLocation.get(block.getLocation());
        if (cache != null) {
            return cache.check();
        }
        return 0;
    }

    public void removeBlock(Block block) {
        Location location = block.getLocation();
        CooledBlock cache = byLocation.remove(location);
        if (cache != null) {
            cooledBlocks.remove(cache);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    database.delete(location)
            );
        }
    }

    public long addBlock(Block block, long cooldown) {
        Location location = block.getLocation();
        CooledBlock cache = byLocation.get(location);
        if (cache != null) {
            long timeLeft = cache.update();
            if (timeLeft <= 0) {
                byLocation.remove(location);
                cooledBlocks.remove(cache);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                        database.delete(location)
                );
                return 1;
            }
            return timeLeft;
        }
        long resultCooldown = System.currentTimeMillis() + cooldown;
        Material type = block.getType();
        cache = new CooledBlock(location, type, resultCooldown);
        cooledBlocks.add(cache);
        byLocation.put(location, cache);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                database.insert(location, type, resultCooldown)
        );
        return 0;
    }

    public void updateBlocks() {
        Iterator<CooledBlock> iter = cooledBlocks.iterator();
        List<Location> toRemove = new ArrayList<>();
        while (iter.hasNext()) {
            CooledBlock block = iter.next();
            if(block.update() > 0) {
                break;
            }
            iter.remove();
            Location loc = block.getLocation();
            regenEffect.play(loc);
            byLocation.remove(loc);
            toRemove.add(loc);
        }
        if (!toRemove.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    database.delete(toRemove)
            );
        }
    }

    public void loadBlocks() {
        Collection<CooledBlock> blocks = database.select();
        plugin.getLogger().info("Loaded " + blocks.size() + " scheduled blocks.");
        for (CooledBlock block : blocks) {
            cooledBlocks.add(block);
            byLocation.put(block.getLocation(), block);
        }
    }
}
