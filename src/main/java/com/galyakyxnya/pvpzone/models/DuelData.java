package com.galyakyxnya.pvpzone.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DuelData {
    private final UUID duelId;
    private final Player challenger;
    private final Player target;
    private String zoneName;
    private DuelState state;
    private Location challengerLocation;
    private Location targetLocation;

    // ДОБАВЛЯЕМ ПОЛЯ ДЛЯ СОХРАНЕНИЯ ИНВЕНТАРЕЙ
    private ItemStack[] challengerOriginalInventory;
    private ItemStack[] challengerOriginalArmor;
    private ItemStack[] targetOriginalInventory;
    private ItemStack[] targetOriginalArmor;

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
        this.challengerOriginalInventory = new ItemStack[0];
        this.challengerOriginalArmor = new ItemStack[0];
        this.targetOriginalInventory = new ItemStack[0];
        this.targetOriginalArmor = new ItemStack[0];
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

    // Новые геттеры и сеттеры для инвентарей
    public ItemStack[] getChallengerOriginalInventory() { return challengerOriginalInventory; }
    public void setChallengerOriginalInventory(ItemStack[] inventory) {
        this.challengerOriginalInventory = inventory != null ? inventory.clone() : new ItemStack[0];
    }

    public ItemStack[] getChallengerOriginalArmor() { return challengerOriginalArmor; }
    public void setChallengerOriginalArmor(ItemStack[] armor) {
        this.challengerOriginalArmor = armor != null ? armor.clone() : new ItemStack[0];
    }

    public ItemStack[] getTargetOriginalInventory() { return targetOriginalInventory; }
    public void setTargetOriginalInventory(ItemStack[] inventory) {
        this.targetOriginalInventory = inventory != null ? inventory.clone() : new ItemStack[0];
    }

    public ItemStack[] getTargetOriginalArmor() { return targetOriginalArmor; }
    public void setTargetOriginalArmor(ItemStack[] armor) {
        this.targetOriginalArmor = armor != null ? armor.clone() : new ItemStack[0];
    }

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