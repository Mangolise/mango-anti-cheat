package net.mangolise.anticheat.checks.movement;

import net.mangolise.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;

public class TeleportCheck extends ACCheck {
    private final static float UP_THRESHOLD = 3;
    private final static float DOWN_THRESHOLD = 5;
    private final static float HORIZONTAL_THRESHOLD = 4;

    public TeleportCheck() {
        super("Teleport");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (isBypassing(p)) return;

        double yDiff = e.getNewPosition().y() - e.getPlayer().getPosition().y();
        checkAxis(e, p, yDiff, DOWN_THRESHOLD, UP_THRESHOLD);

        double xDiff = e.getNewPosition().x() - e.getPlayer().getPosition().x();
        checkAxis(e, p, xDiff, HORIZONTAL_THRESHOLD, HORIZONTAL_THRESHOLD);

        double zDiff = e.getNewPosition().z() - e.getPlayer().getPosition().z();
        checkAxis(e, p, zDiff, HORIZONTAL_THRESHOLD, HORIZONTAL_THRESHOLD);
    }

    private void checkAxis(PlayerMoveEvent e, Player p, double zDiff, float backwardThreshold, float forwardThreshold) {
        if (Math.abs(zDiff) > (zDiff < 0 ? backwardThreshold : forwardThreshold)) {  // If they are going backward, we allow a bit more
            float certainty = Math.min(1f, ((float) zDiff / 10f) + 0.5f);
            flag(p, certainty);
            if (isNotPassive()) {
                e.setCancelled(true);
            }
        }
    }
}
