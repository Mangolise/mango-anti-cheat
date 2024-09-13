package net.mangolise.anticheat.checks.movement;

import net.mangolise.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;

import java.util.*;

public class UnaidedLevitationCheck extends ACCheck {
    private final static int THRESHOLD = 3;

    private final HashMap<UUID, Double> blocksRaised = new HashMap<>();

    public UnaidedLevitationCheck() {
        super("UnaidedLevitation");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (isBypassing(p)) return;
        UUID playerId = p.getUuid();

        if (isBypassFly(p)) {
            return;
        }

        if (isOnGround(p)) {
            blocksRaised.remove(playerId);
            return;
        }

        Pos from = e.getPlayer().getPosition();
        Pos to = e.getNewPosition();
        if (from.y() > to.y()) {
            return;
        }

        // add the distance between the two y values to the hashmap
        double newRaised = blocksRaised.getOrDefault(playerId, 0.0) + (to.y() - from.y());
        blocksRaised.put(playerId, newRaised);

        // check if the player has gone up more than 3 blocks without going down then fail
        if (newRaised > THRESHOLD) {
            float certainty = Math.min(1f, (float) ((newRaised - THRESHOLD) / 10d) + 0.5f);
            flag(p, certainty);
            if (!config.passive()) {
                e.setCancelled(true);
            }
        }
    }
}
