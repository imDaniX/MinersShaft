package me.imdanix.mine.shaft;

import lombok.Getter;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Getter
public class Mineshaft {
    private final String world;
    private final String region;
    private final Map<Material, MineableResource> resources;

    public Mineshaft(String world, String region) {
        this.world = world;
        this.region = region;
        this.resources = new EnumMap<>(Material.class);
    }

    public void addResource(Material type, MineableResource resource) {
        resources.put(type, resource);
    }

    public MineableResource getResource(Material type) {
        return resources.get(type);
    }

}
