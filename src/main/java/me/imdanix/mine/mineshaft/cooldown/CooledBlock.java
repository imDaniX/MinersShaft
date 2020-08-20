package me.imdanix.mine.mineshaft.cooldown;

import lombok.Value;
import org.bukkit.Location;
import org.bukkit.Material;

@Value
public class CooledBlock implements Comparable<CooledBlock> {
    Location location;
    Material block;
    long until;

    public long check() {
        long now = System.currentTimeMillis();
        if (until > now) {
            return until - now;
        }
        return 0;
    }

    public long update() {
        long now = System.currentTimeMillis();
        if (until > now) {
            return until - now;
        }
        location.getBlock().setType(block, false);
        return 0;
    }

    @Override
    public int compareTo(CooledBlock cooledBlock) {
        return until > cooledBlock.until ? 1 : -1;
    }
}
