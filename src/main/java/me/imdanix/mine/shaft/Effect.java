package me.imdanix.mine.shaft;

import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

@AllArgsConstructor
public class Effect {
    private final Sound sound;
    private final Particle particle;

    public void play(Location loc) {
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        World world = loc.getWorld();
        if (sound != null)      world.playSound(center, sound, 0.5f, 1f);
        if (particle != null)   world.spawnParticle(particle, center, 20, 0.6, 0.6, 0.6);
    }
}
