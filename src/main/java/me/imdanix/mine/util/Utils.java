package me.imdanix.mine.util;

import lombok.experimental.UtilityClass;
import me.imdanix.mine.shaft.MineableResource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

@UtilityClass
public class Utils {
    public ItemStack getCustomDrop(MineableResource resource, Block block, Player player) {
        return resource.getDrops().isEmpty() ?
                getFirst(block.getDrops(player.getInventory().getItemInMainHand()), new ItemStack(Material.STONE)) :
                resource.getDrops().next();
    }

    public <T> T getFirst(Collection<T> coll, T def) {
        Iterator<T> iter = coll.iterator();
        return iter.hasNext() ? iter.next() : def;
    }

    public ConfigurationSection section(String path, ConfigurationSection cfg) {
        return cfg.isConfigurationSection(path) ? cfg.getConfigurationSection(path) : cfg.createSection(path);
    }

    public Material getMaterial(String typeStr) {
        return Material.getMaterial(typeStr.toUpperCase(Locale.ENGLISH));
    }

    public String clr(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public <T extends Enum<T>> T getEnum(Class<T> clazz, String name) {
        if (name == null) {
            return null;
        }
        try {
            return Enum.valueOf(clazz, name.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String locationToString(Location location) {
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ();
    }

    public Location strngToLocation(String string) {
        String[] split = string.split(",");
        return new Location(Bukkit.getWorld(split[0]),
                Double.valueOf(split[1]),
                Double.valueOf(split[2]),
                Double.valueOf(split[3]));
    }
}
