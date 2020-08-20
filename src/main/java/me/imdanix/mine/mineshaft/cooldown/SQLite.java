package me.imdanix.mine.mineshaft.cooldown;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SQLite {
    private Connection connection;

    public SQLite(Plugin plugin, String name) {
        plugin.getDataFolder().mkdir();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + File.separator + name + ".db");
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS `cooldowns` (" +
                    "`location` VARCHAR(32) PRIMARY KEY," +
                    "`type` VARCHAR(32) NOT NULL," +
                    "`until` INTEGER NOT NULL" +
                    ");");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void insert(Location location, Material type, long time) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO `cooldowns` (location,type,until) VALUES (?,?,?);");
            statement.setString(1, locationToString(location));
            statement.setString(2, type.toString());
            statement.setLong(3, time);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(Collection<Location> locations) {
        StringBuilder builder = new StringBuilder(locations.size()*2);
        for (int i = 0; i < locations.size(); i++) {
            builder.append("?,");
        }
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `cooldowns` WHERE location IN (" + builder.substring(0, builder.length() - 1) + ")");
            int i = 0;
            for (Location location : locations) {
                statement.setString(++i, locationToString(location));
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(Location location) {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `cooldowns` WHERE location = ?;");
            statement.setString(1, locationToString(location));
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Collection<CooledBlock> select() {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `cooldowns`;");
            ResultSet e = statement.executeQuery();
            List<CooledBlock> blocks = new ArrayList<>();
            while (e.next()) {
                blocks.add(new CooledBlock(
                        strngToLocation(e.getString("location")),
                        Material.getMaterial(e.getString("type")),
                        e.getLong("until"))
                );
            }
            return blocks;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }

    public static String locationToString(Location location) {
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ();
    }

    private static Location strngToLocation(String string) {
        String[] split = string.split(",");
        return new Location(Bukkit.getWorld(split[0]),
                Double.valueOf(split[1]),
                Double.valueOf(split[2]),
                Double.valueOf(split[3]));
    }
}