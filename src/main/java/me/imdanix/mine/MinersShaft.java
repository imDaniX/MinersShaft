package me.imdanix.mine;

import lombok.Getter;
import me.imdanix.mine.shaft.ShaftsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinersShaft extends JavaPlugin {
    @Getter
    private ShaftsManager shafts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.shafts = new ShaftsManager(this);
        this.shafts.reload(getConfig());
        Bukkit.getPluginManager().registerEvents(shafts, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        reloadConfig();
        shafts.reload(getConfig());
        sender.sendMessage(shafts.getMessages().get("reloaded"));
        return true;
    }
}
