package me.imdanix.mine.shaft;

import lombok.Value;
import me.imdanix.mine.util.WeightedCollection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Value
public class MineableResource {
    WeightedCollection<ItemStack> drops;
    List<Material> replaces;
    long cooldown;
    boolean instant;
}
