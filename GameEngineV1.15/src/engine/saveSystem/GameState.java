package engine.saveSystem;

import com.fasterxml.jackson.annotation.JsonProperty;
import engine.combat.StatusEffectInstance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete serializable snapshot of the entire game state.
 * Saved to checkpoint files for full game restoration.
 */
public class GameState {
    @JsonProperty("player")
    private PlayerState playerState;

    @JsonProperty("rooms")
    private Map<String, RoomState> roomStates;

    @JsonProperty("worldInteractionStage")
    private int worldInteractionStage;

    @JsonProperty("worldInCombat")
    private boolean worldInCombat;

    @JsonProperty("worldPendingTraderId")
    private String worldPendingTraderId;

    @JsonProperty("worldPendingDialogueNpcId")
    private String worldPendingDialogueNpcId;

    // Default constructor for Jackson
    public GameState() {
        this.playerState = new PlayerState();
        this.roomStates = new HashMap<>();
    }

    // Getters and setters
    public PlayerState getPlayerState() { return playerState; }
    public void setPlayerState(PlayerState playerState) { this.playerState = playerState; }

    public Map<String, RoomState> getRoomStates() { return roomStates; }
    public void setRoomStates(Map<String, RoomState> roomStates) { this.roomStates = roomStates; }

    public int getWorldInteractionStage() { return worldInteractionStage; }
    public void setWorldInteractionStage(int stage) { this.worldInteractionStage = stage; }

    public boolean isWorldInCombat() { return worldInCombat; }
    public void setWorldInCombat(boolean inCombat) { this.worldInCombat = inCombat; }

    public String getWorldPendingTraderId() { return worldPendingTraderId; }
    public void setWorldPendingTraderId(String id) { this.worldPendingTraderId = id; }

    public String getWorldPendingDialogueNpcId() { return worldPendingDialogueNpcId; }
    public void setWorldPendingDialogueNpcId(String id) { this.worldPendingDialogueNpcId = id; }

    /**
     * Player state subset
     */
    public static class PlayerState {
        @JsonProperty("hp")
        public int hp;

        @JsonProperty("maxHp")
        public int maxHp;

        @JsonProperty("attackDamage")
        public int attackDamage;

        @JsonProperty("baseEscapeChance")
        public int baseEscapeChance;

        @JsonProperty("carryCapacity")
        public int carryCapacity;

        @JsonProperty("money")
        public int money;

        @JsonProperty("currentRoomId")
        public String currentRoomId;

        @JsonProperty("previousRoomId")
        public String previousRoomId;

        @JsonProperty("equippedWeaponId")
        public String equippedWeaponId;

        @JsonProperty("equippedArmorId")
        public String equippedArmorId;

        @JsonProperty("inventoryItemIds")
        public List<String> inventoryItemIds = new ArrayList<>();

        @JsonProperty("statusEffects")
        public List<StatusEffectInstance> statusEffects = new ArrayList<>();
    }

    /**
     * Room state subset
     */
    public static class RoomState {
        @JsonProperty("roomId")
        public String roomId;

        @JsonProperty("itemIds")
        public List<String> itemIds = new ArrayList<>();

        @JsonProperty("monsterIds")
        public List<String> monsterIds = new ArrayList<>();

        @JsonProperty("targetIds")
        public List<String> targetIds = new ArrayList<>();

        @JsonProperty("currencyAmount")
        public int currencyAmount;

        @JsonProperty("exits")
        public Map<String, String> exits;
    }
}