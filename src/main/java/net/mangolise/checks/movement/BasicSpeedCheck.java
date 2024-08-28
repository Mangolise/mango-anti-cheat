package net.mangolise.checks.movement;

import net.mangolise.ACCheck;
import net.mangolise.Tuple;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * We can't get when the player teleports, so to flag for speed we need the following:
 * - Player isn't flying or otherwise bypassing
 * - All movements are consistent, the speed shouldn't spike once (e.g. teleporting)
 */
public class BasicSpeedCheck extends ACCheck {
    private final static float THRESHOLD = 4f;  // Usually starts false flagging at 0.6
    private final static int SAMPLE_TIME = 1500;
    private final static int AVERAGE_TIME_PERIOD_MS = 1000;

    private final HashMap<UUID, List<Tuple<Long, Pos>>> playerDetails = new HashMap<>();

    public BasicSpeedCheck() {
        super("BasicSpeed");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!canCheck(p)) return;

        if (isBypassSpeed(p)) {
            debug(p, "bypassed");
            return;
        }

        List<Tuple<Long, Pos>> details = playerDetails.get(p.getUuid());
        if (details == null) {
            details = new java.util.ArrayList<>();
            playerDetails.put(p.getUuid(), details);
            debug(p, "created new details");
            return;
        }

        // Check if there's any outliers
        double sum = 0;
        double sumOfSquares = 0;
        for (Tuple<Long, Pos> detail : details) {
            sum += detail.getSecond().x();
            sumOfSquares += Math.pow(detail.getSecond().x(), 2);
        }
        final double mean = sum / details.size();
        final double standardDeviation = Math.sqrt((sumOfSquares - (Math.pow(sum, 2) / details.size())) / details.size());

        if (playerDetails.get(p.getUuid()).stream().anyMatch(tuple -> Math.abs(tuple.getSecond().x() - mean) > standardDeviation * 2)) {
            debug(p, "outlier detected in speed, IGNORING ALL DATA");
            playerDetails.put(p.getUuid(), new java.util.ArrayList<>());
            return;
        }

        Pos to = e.getNewPosition();

        details.removeIf(tuple -> tuple.getFirst() < System.currentTimeMillis() - SAMPLE_TIME);
        details.add(new Tuple<>(System.currentTimeMillis(), to));
        playerDetails.put(p.getUuid(), details);

        Pos from = details.getFirst().getSecond();
        long timeSinceFrom = System.currentTimeMillis() - details.getFirst().getFirst();

        int averagingPeriod = AVERAGE_TIME_PERIOD_MS;
        if (timeSinceFrom < averagingPeriod) {
            debug(p, "averaging time not met");
            return;  // Average must be over the averaging period.
        }

        double horizontalDistance = Math.sqrt(Math.pow(to.x() - from.x(), 2) + Math.pow(to.z() - from.z(), 2));
        double speed = horizontalDistance / (timeSinceFrom / (double) averagingPeriod);
        double expectedMaxWalkSpeed = 4.317 * (getRunSpeed(p) / 0.2);
        expectedMaxWalkSpeed = expectedMaxWalkSpeed * 1.3 * 1.5;

        //p.sendActionBar(Component.text("Speed: " + String.format("%.2f", speed) + " Expected: " + String.format("%.2f", expectedMaxWalkSpeed)));

        if (speed > expectedMaxWalkSpeed + THRESHOLD) {
            flag(p, 0.5f);
            debug(p, "Lowest valid threshold: " + (speed - expectedMaxWalkSpeed));
            if (!config.passive()) {
                e.setCancelled(true);
            }
        }
    }
}
