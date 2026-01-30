package at.helpch.papi.expansion.world;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class WorldData {
    private PlayerRef recentJoin;
    private PlayerRef recentQuit;

    public PlayerRef getRecentJoin() {
        return recentJoin;
    }

    public void setRecentJoin(PlayerRef recentJoin) {
        this.recentJoin = recentJoin;
    }

    public PlayerRef getRecentQuit() {
        return recentQuit;
    }

    public void setRecentQuit(PlayerRef recentQuit) {
        this.recentQuit = recentQuit;
    }
}
