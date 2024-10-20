package net.mangolise.anticheat.checks.movement;

import net.mangolise.anticheat.ACCheck;
import net.mangolise.anticheat.Tuple;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityTeleportEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * We can't get when the player teleports, so to flag for speed we need the following:
 * - Player isn't flying or otherwise bypassing
 * - All movements are consistent, the speed shouldn't spike once (e.g. teleporting)
 */
public class BasicSpeedCheck extends ACCheck {
    private static final float THRESHOLD = 4f;  // Usually starts false flagging at 0.6
    private static final int SAMPLE_TIME = 1500;
    private static final int AVERAGE_TIME_PERIOD_MS = 1000;
    private static final int MIN_SAMPLE_SIZE = 5;

    private final Tag<List<Tuple<Long, Pos>>> PLAYER_DETAILS_TAG = Tag.<List<Tuple<Long, Pos>>>Transient("anticheat_basicspeed_player_details").defaultValue(ArrayList::new);

    public BasicSpeedCheck() {
        super("BasicSpeed");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
        MinecraftServer.getGlobalEventHandler().addListener(EntityTeleportEvent.class, e -> {
            if (e.getEntity() instanceof Player player) {
                player.getTag(PLAYER_DETAILS_TAG).clear(); // Won't be accurate
            }
        });
    }

    @Override
    public void disableFor(Player player, int time) {
        super.disableFor(player, time);

        // reset details if the check is disabled
        player.getTag(PLAYER_DETAILS_TAG).clear();
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (isBypassing(p)) return;

        if (isBypassSpeed(p)) {
            debug(p, "bypassed");
            return;
        }

        List<Tuple<Long, Pos>> details = p.getTag(PLAYER_DETAILS_TAG);

        // Check if there's any outliers
//        double sum = 0;
//        double sumOfSquares = 0;
//        for (Tuple<Long, Pos> detail : details) {
//            sum += detail.getSecond().x();
//            sumOfSquares += Math.pow(detail.getSecond().x(), 2);
//        }

        // Is this check needed? It was only here to combat tp
//        final double mean = sum / details.size();
//        final double standardDeviation = Math.sqrt((sumOfSquares - (Math.pow(sum, 2) / details.size())) / details.size());

//        if (details.stream().anyMatch(tuple -> Math.abs(tuple.getSecond().x() - mean) > standardDeviation * 2)) {
//            debug(p, "outlier detected in speed, IGNORING ALL DATA");
//            p.getTag(PLAYER_DETAILS_TAG).clear();
//            return;
//        }

        Pos to = e.getNewPosition();

        details.removeIf(tuple -> tuple.getFirst() < System.currentTimeMillis() - SAMPLE_TIME);
        details.add(new Tuple<>(System.currentTimeMillis(), to));
        p.setTag(PLAYER_DETAILS_TAG, details);

        Pos from = details.getFirst().getSecond();
        long timeSinceFrom = System.currentTimeMillis() - details.getFirst().getFirst();

        int averagingPeriod = AVERAGE_TIME_PERIOD_MS;
        if (timeSinceFrom < averagingPeriod) {
            debug(p, "averaging time not met");
            return;  // Average must be over the averaging period.
        }

        if (details.size() < MIN_SAMPLE_SIZE) {
            debug(p, "min sample size not met");
            return;
        }

        double horizontalDistance = Math.sqrt(Math.pow(to.x() - from.x(), 2) + Math.pow(to.z() - from.z(), 2));
        double speed = horizontalDistance / (timeSinceFrom / (double) averagingPeriod);
        double expectedMaxWalkSpeed = 4.317 * (getRunSpeed(p) / 0.2);
        expectedMaxWalkSpeed = expectedMaxWalkSpeed * 1.3 * 1.5;

        //p.sendActionBar(Component.text("Speed: " + String.format("%.2f", speed) + " Expected: " + String.format("%.2f", expectedMaxWalkSpeed)));

        if (speed > expectedMaxWalkSpeed + THRESHOLD) {
            float certainty = (float) Math.min(1f, (speed - expectedMaxWalkSpeed) / 2f);
            flag(p, certainty);
            debug(p, "Lowest valid threshold: " + (speed - expectedMaxWalkSpeed));
            if (isNotPassive()) {
                e.setCancelled(true);
            }
        }
    }
}
