package com.galyakyxnya.pvpzone.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DuelData {
    private final UUID duelId;
    private final Player challenger;
    private final Player target;
    private String zoneName;
    private DuelState state;
    private Location challengerLocation;
    private Location targetLocation;

    public enum DuelState {
        PENDING,
        ACTIVE,
        CANCELLED,
        FINISHED
    }

    public DuelData(Player challenger, Player target, String zoneName) {
        this.duelId = UUID.randomUUID();
        this.challenger = challenger;
        this.target = target;
        this.zoneName = zoneName;
        this.state = DuelState.PENDING;
    }

    public UUID getDuelId() { return duelId; }
    public Player getChallenger() { return challenger; }
    public Player getTarget() { return target; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public DuelState getState() { return state; }
    public void setState(DuelState state) { this.state = state; }
    public Location getChallengerLocation() { return challengerLocation; }
    public void setChallengerLocation(Location loc) { this.challengerLocation = loc; }
    public Location getTargetLocation() { return targetLocation; }
    public void setTargetLocation(Location loc) { this.targetLocation = loc; }

    public boolean isPlayerInDuel(UUID playerId) {
        return challenger.getUniqueId().equals(playerId) ||
                target.getUniqueId().equals(playerId);
    }

    public Player getOpponent(UUID playerId) {
        if (challenger.getUniqueId().equals(playerId)) return target;
        if (target.getUniqueId().equals(playerId)) return challenger;
        return null;
    }
}