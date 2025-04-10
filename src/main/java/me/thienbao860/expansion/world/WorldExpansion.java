package me.thienbao860.expansion.world;

import com.google.common.primitives.Ints;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldExpansion extends PlaceholderExpansion implements Listener, Cacheable {
    // Define a regular expression pattern to match the strings
    private final static Pattern PATTERN = Pattern.compile("(\\b\\w+\\b)|(\\b\\w+\\b)(\\B_+?\\B)(?<=\\*)\\w+(?=\\*(?:_|$))");

    private final Map<String, WorldData> worldData;

    private Economy econ = null;
    private Permission perms = null;

    public WorldExpansion() {
        this.worldData = new HashMap<>();
        if (isVaultExist()) {
            setupEconomy();
            setupPermissions();
        }
    }

    @Override
    public @NotNull String getIdentifier() {
        return "world";
    }

    @Override
    public @NotNull String getAuthor() {
        return "thienbao860";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.2.2";
    }

    @Override
    public String onRequest(final @Nullable OfflinePlayer offlinePlayer, final @NotNull String params) {
        final Player player = (Player) offlinePlayer;

        final String[] args = parseParams(params);
        if (args == null) {
            return null;
        }

        //===== All worlds =====
        switch (args[0].toLowerCase()) {
            case "total":
                return String.valueOf(Bukkit.getWorlds().size());
            case "biome":
                Location loc = player.getLocation();
                return player.getWorld().getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()).name().toLowerCase();
            case "nearbyentites":
                if (args.length != 2) {
                    return null;
                }

                final Integer radius = Ints.tryParse(args[1]);
                if (radius == null) {
                    return "0";
                }
                return String.valueOf(player.getNearbyEntities(radius, radius, radius).size());
        }

        // ===== Specific world =====
        if (args.length < 2) {
            return null;
        }
        final World world = getWorld(player, args);
        if (world == null) {
            return "";
        }

        switch (args[0]) {
            case "name":
                return world.getName();
            case "seed":
                return String.valueOf(world.getSeed());
            case "sealevel":
                return String.valueOf(world.getSeaLevel());

            case "time":
                return timeFormat24(world.getTime());
            case "timein12":
                return timeFormat(world.getTime(), true);

            case "canpvp":
                return String.valueOf(world.getPVP());
            case "thunder":
                return String.valueOf(world.isThundering());
            case "animalallowed":
                return String.valueOf(world.getAllowAnimals());
            case "monsterallowed":
                return String.valueOf(world.getAllowMonsters());
            case "difficulty":
                return world.getDifficulty().name().toLowerCase();
            case "players":
                if (args.length == 2) {
                    return String.valueOf(world.getPlayers().size());
                }
                if (perms == null) {
                    return "0";
                }
                return String.valueOf(playersInGroup(world, args[1]));
            case "haspermission":
                if (args.length < 3) {
                    return null;
                }
                return String.valueOf(playersPermission(world, args[1]));

            case "playerexist":
                if (args.length < 3) {
                    return null;
                }
                return String.valueOf(playerExist(world, args[1]));
            case "isgamerule":
                if (args.length < 3) {
                    return null;
                }
                return String.valueOf(world.isGameRule(args[1].toUpperCase()));
            case "recentjoin":
                if (player == null || !worldData.containsKey(world.getName())) {
                    return "";
                }
                return worldData.get(world.getName()).getRecentJoin().getName();
            case "recentquit":
                if (player == null || !worldData.containsKey(world.getName())) {
                    return "";
                }
                return worldData.get(world.getName()).getRecentQuit().getName();
            case "totalbalance":
                if (econ == null) {
                    return null;
                }
                return String.valueOf(getTotalMoney(world));

        }
        return null;
    }

    public World getWorld(final @Nullable Player player, final @NotNull String[] args) {
        final String worldName = args[args.length - 1];
        if (worldName.equals("$")) {
            if (player == null) {
                return null;
            }

            return player.getWorld();
        }
        return Bukkit.getWorld(worldName);
    }

    @EventHandler
    public void onJoin(final @NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        worldData.computeIfAbsent(player.getWorld().getName(), (k) -> new WorldData()).setRecentJoin(player);
    }

    @EventHandler
    public void onQuit(final @NotNull PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        worldData.computeIfAbsent(player.getWorld().getName(), k -> new WorldData()).setRecentQuit(player);
    }

    @EventHandler
    public void onTeleport(final @NotNull PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        final Location from = event.getFrom();
        final Location to = event.getTo();

        if (to == null || from.getWorld() == null || to.getWorld() == null) {
            return;
        }

        if (from.getWorld().getName().equals(to.getWorld().getName())) {
            return;
        }

        worldData.computeIfAbsent(to.getWorld().getName(), k -> new WorldData()).setRecentJoin(player);
    }

    private void setupEconomy() {
        Server server = Bukkit.getServer();
        RegisteredServiceProvider<Economy> rsp = server.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;
        this.econ = rsp.getProvider();
    }

    private void setupPermissions() {
        Server server = Bukkit.getServer();
        RegisteredServiceProvider<Permission> rsp = server.getServicesManager().getRegistration(Permission.class);
        if (rsp == null) return;
        this.perms = rsp.getProvider();
    }

    private double getTotalMoney(final @NotNull World world) {
        double total = 0;

        if (econ == null) {
            return total;
        }

        for (Player player : world.getPlayers()) {
            total += econ.getBalance(player);
        }
        return total;
    }

    private boolean isVaultExist() {
        return Bukkit.getServer().getPluginManager().isPluginEnabled("Vault");
    }

    private boolean playerExist(final @NotNull World world, final @NotNull String name) {
        for (Player player : world.getPlayers()) {
            if (player.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private int playersInGroup(final @NotNull World world, final @NotNull String group) {
        int i = 0;
        if (perms == null) {
            return 0;
        }
        for (Player player : world.getPlayers()) {
            if (perms.playerInGroup(world.toString(), player, group)) {
                i++;
            }
        }
        return i;
    }

    private int playersPermission(final @NotNull World world, final @NotNull String perm) {
        int i = 0;
        perm.replace("_", "");
        for (Player player : world.getPlayers()) {
            if (player.isOp() || player.hasPermission(perm)) {
                i++;
            }
        }
        return i;
    }

    private String timeFormat24(final long tick) {
        return timeFormat(tick, false);
    }

    private String timeFormat(final long tick, final boolean is12) {
        int hour = ((int) ((tick / 1000) + 6)) % 24;
        final boolean am = hour < 12;

        if (is12) {
            if (hour > 12) {
                hour -= 12;
            }
        }
        final String minutesAsString = String.valueOf(tick);
        final int length = minutesAsString.length();

        final String newStr = length < 3 ? minutesAsString.substring(length - 1) : minutesAsString.substring(length - 3);
        final int minutes = Integer.parseInt(newStr) * 60 / 999;
        if (is12) {
            return String.format("%d:%02d%s", hour, minutes, am ? "am" : "pm");
        }
        return String.format("%d:%02d", hour, minutes);

    }

    private @Nullable String[] parseParams(final @NotNull String params) {
        final ArrayList<String> arrayList = new ArrayList<>();

        // Create a Matcher object
        final Matcher matcher = PATTERN.matcher(params);

        // Check if the pattern matches the input string
        while (matcher.find()) {
            if (matcher.group(1) != null)
                arrayList.add(matcher.group(1));
            if (matcher.group(2) != null)
                arrayList.add(matcher.group(2));
        }

        if (arrayList.isEmpty()) {
            return null;
        }

        if (arrayList.size() == 1) {
            return params.split("_");
        }

        // This will separate first input which can be only identifier in bottom switch(){}
        // That parts are then put together with world name
        String[] parts = arrayList.get(0).split("_");
        // add each part to the ArrayList
        ArrayList<String> list = new ArrayList<>(Arrays.asList(parts));
        list.add(arrayList.get(1));
        return list.toArray(new String[0]);
    }

    @Override
    public void clear() {
        this.worldData.clear();
        this.econ = null;
        this.perms = null;
    }
}
