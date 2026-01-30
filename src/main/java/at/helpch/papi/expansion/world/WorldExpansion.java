package at.helpch.papi.expansion.world;

import at.helpch.placeholderapi.PlaceholderAPI;
import at.helpch.placeholderapi.PlaceholderAPIPlugin;
import at.helpch.placeholderapi.configuration.BooleanValue;
import at.helpch.placeholderapi.expansion.Cacheable;
import at.helpch.placeholderapi.expansion.PlaceholderExpansion;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.worldgen.biome.Biome;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldExpansion extends PlaceholderExpansion implements Cacheable {
    // Define a regular expression pattern to match the strings
    private final static Pattern PATTERN = Pattern.compile("(\\b\\w+\\b)|(\\b\\w+\\b)(\\B_+?\\B)(?<=\\*)\\w+(?=\\*(?:_|$))");
    private static final PermissionsModule PERMISSIONS = PermissionsModule.get();

    private static final DateTimeFormatter TIME_24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_12 = DateTimeFormatter.ofPattern("hh:mm a");

    private final Map<String, WorldData> worldData;


    public WorldExpansion() {
        this.worldData = new HashMap<>();

        Universe.get().getWorlds().keySet().forEach(name -> PlaceholderAPIPlugin.instance().getEventRegistry().register(PlayerReadyEvent.class, name, this::onJoin));
        PlaceholderAPIPlugin.instance().getEventRegistry().register(PlayerDisconnectEvent.class, this::onQuit);

    }

    @Override
    public String getIdentifier() {
        return "world";
    }

    @Override
    public String getAuthor() {
        return "HelpChat";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(final PlayerRef playerRef, final String params) {
        final String[] args = parseParams(params);
        if (args == null) {
            return null;
        }

        if (playerRef == null) {
            return "";
        }

        final Ref<EntityStore> ref = playerRef.getReference();

        if (ref == null || !ref.isValid())
            return null;

        final Store<EntityStore> store = ref.getStore();
        final Player player = (Player) store.getComponent(ref, Player.getComponentType());

        //===== All worlds =====
        switch (args[0].toLowerCase()) {
            case "total":
                return String.valueOf(Universe.get().getWorlds().size());
            case "biome":
                if (player == null) {
                    return "";
                }

                return player.getWorldMapTracker().getCurrentBiomeName();
//            case "nearbyentites":
//
////                player.getWorld().getE
//
//
//                if (player == null) {
//                    return "";
//                }
//
//                if (args.length != 2) {
//                    return null;
//                }
//
//                final Integer radius = Ints.tryParse(args[1]);
//                if (radius == null) {
//                    return "0";
//                }
//                return String.valueOf(player.getNearbyEntities(radius, radius, radius).size());
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
            case "uuid":
                return String.valueOf(world.getWorldConfig().getUuid());
            case "seed":
                return String.valueOf(world.getWorldConfig().getSeed());
//            case "sealevel":
//                return String.valueOf(world.getWorldConfig().);

            case "time":
                return TIME_24.format(world.getWorldConfig().getGameTime());
            case "timein12":
                return TIME_12.format(world.getWorldConfig().getGameTime());
            case "fulltime":
                return String.valueOf(world.getWorldConfig().getGameTime());
            case "canpvp":
                return PlaceholderAPI.booleanValue(world.getWorldConfig().isPvpEnabled());
//            case "thunder":
//                return world.getWorldConfig().
            case "spawnnpc":
                return PlaceholderAPI.booleanValue(world.getWorldConfig().isSpawningNPC());
            case "npcfrozen":
                return PlaceholderAPI.booleanValue(world.getWorldConfig().isAllNPCFrozen());
            case "falldamage":
                return PlaceholderAPI.booleanValue(world.getWorldConfig().isFallDamageEnabled());
            case "objectivemarkers":
                return PlaceholderAPI.booleanValue(world.getWorldConfig().isObjectiveMarkersEnabled());
            case "entities":
//                if (args.length < 3 || !"living".equals(args[1])) {
//                    return String.valueOf(world.getEntityStore().getStore().getEntityCount());
//                }

                return String.valueOf(world.getEntityStore().getStore().getEntityCount());
            case "players":
                if (args.length == 2) {
                    return String.valueOf(world.getPlayers().size());
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
//            case "isgamerule":
//                if (args.length < 3) {
//                    return null;
//                }
//                return String.valueOf(world.isGameRule(args[1].toUpperCase()));
            case "recentjoin":
                if (player == null || !worldData.containsKey(world.getName())) {
                    return "";
                }
                return worldData.get(world.getName()).getRecentJoin().getUsername();
            case "recentquit":
                if (player == null || !worldData.containsKey(world.getName())) {
                    return "";
                }
                return worldData.get(world.getName()).getRecentQuit().getUsername();

        }
        return null;
    }

    public World getWorld(final Player player, final String[] args) {
        final String worldName = args[args.length - 1];
        if (worldName.equals("$")) {
            if (player == null) {
                return null;
            }

            return player.getWorld();
        }
        return Universe.get().getWorld(worldName);
    }

    public void onJoin(final PlayerReadyEvent event) {
        final Player player = event.getPlayer();
        worldData.computeIfAbsent(player.getWorld().getName(), (k) -> new WorldData()).setRecentJoin(player.getPlayerRef());
    }

    public void onQuit(final PlayerDisconnectEvent event) {
        final PlayerRef player = event.getPlayerRef();
        worldData.computeIfAbsent(Universe.get().getWorld(player.getWorldUuid()).getName(), k -> new WorldData()).setRecentQuit(player);
    }

//    public void onTeleport(final AddPlayerToWorldEvent event) {
//        worldData.computeIfAbsent(event.getWorld().getName(), k -> new WorldData()).setRecentJoin(event.getHolder().);
//    }

    private boolean playerExist(final World world, final String name) {
        for (Player player : world.getPlayers()) {
            if (player.getPlayerRef().getUsername().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private int playersInGroup(final World world, final String group) {
        int i = 0;

        for (Player player : world.getPlayers()) {
            if (PERMISSIONS.getGroupsForUser(player.getUuid()).contains(group)) {
                i++;
            }
        }
        return i;
    }

    private int playersPermission(final World world, final String perm) {
        int i = 0;
        perm.replace("_", "");
        for (Player player : world.getPlayers()) {
            if (PERMISSIONS.getGroupsForUser(player.getUuid()).contains("OP") || player.hasPermission(perm)) {
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

    private String[] parseParams(final String params) {
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
    }
}
