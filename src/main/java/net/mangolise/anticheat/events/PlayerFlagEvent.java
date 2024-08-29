package net.mangolise.anticheat.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;

public class PlayerFlagEvent implements Event {
    private final String checkName;
    private final Player player;
    private final float certainty;

    public PlayerFlagEvent(String checkName, Player player, float certainty) {
        this.checkName = checkName;
        this.player = player;
        this.certainty = certainty;
    }

    public String checkName() {
        return checkName;
    }

    public Player player() {
        return player;
    }

    public float certainty() {
        return certainty;
    }
}
