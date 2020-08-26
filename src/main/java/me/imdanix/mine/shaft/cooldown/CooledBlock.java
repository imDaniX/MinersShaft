package me.imdanix.mine.shaft.cooldown;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;

@Getter
@AllArgsConstructor
public class CooledBlock implements Comparable<CooledBlock> {
    private final Location location;
    private final Material block;
    private final long until;

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
