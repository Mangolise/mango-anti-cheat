package net.mangolise.anticheat.checks.movement;

import net.mangolise.anticheat.ACCheck;
import net.mangolise.anticheat.Tuple;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.tag.Tag;

import java.util.*;

public class FlightCheck extends ACCheck {
    private static final long MIN_SAMPLE_TIME = 500;
    private static final int MIN_SAMPLE_SIZE = 5;
    private final Tag<List<Tuple<Long, Pos>>> PLAYER_DETAILS_TAG = Tag.<List<Tuple<Long, Pos>>>Transient("anticheat_flight_player_details").defaultValue(ArrayList::new);

    public FlightCheck() {
        super("Flight");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
        MinecraftServer.getGlobalEventHandler().addListener(AddEntityToInstanceEvent.class, e -> {
            if (e.getEntity() instanceof Player p) {
                p.removeTag(PLAYER_DETAILS_TAG);
            }
        });
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (isBypassing(p)) return;

        if (isBypassFly(p)) {
            debug(p, "bypassed");
            return;
        }

        Pos from = e.getPlayer().getPosition();
        Long fromTime = System.currentTimeMillis();
        Pos to = e.getNewPosition();
        List<Tuple<Long, Pos>> details = p.getTag(PLAYER_DETAILS_TAG);
        if (!details.isEmpty()) {  // Only bother removing stuff if there is stuff to remove
            details.removeIf(tuple -> tuple.getFirst() < System.currentTimeMillis() - 1000);
            details.add(new Tuple<>(System.currentTimeMillis(), to));

            if (details.size() == 1) {
                debug(p, "not enough samples");
                return;
            }
            from = details.getFirst().getSecond();
            fromTime = details.getFirst().getFirst();
        } else {
            details.add(new Tuple<>(System.currentTimeMillis(), to));
            p.setTag(PLAYER_DETAILS_TAG, details);
        }

        if (Objects.requireNonNull(to).y() != from.y()) {
            p.removeTag(PLAYER_DETAILS_TAG);
            debug(p, "changed y");
            return;
        }

        // Make sure they moved on the x or z axis.
        if (to.x() == from.x() && to.z() == from.z()) {
            debug(p, "didn't move");
            return;
        }

        if (isOnGround(p)) {
            p.removeTag(PLAYER_DETAILS_TAG);
            debug(p, "on ground");
            return;
        }

        // Return if the oldest location is newer than half a second
        if (fromTime > System.currentTimeMillis() - MIN_SAMPLE_TIME) {
            debug(p, "sample time not met");
            return;
        }

        if (details.size() < MIN_SAMPLE_SIZE) {
            debug(p, "sample size too low");
            return;
        }

        float certainty = 0.7f;  // We are cancelling so it's binary, we can't have varying certainty.
        flag(p, certainty);  // They have moved without going up or down, and they are not on the ground.
        if (isNotPassive()) {
            e.setCancelled(true);
        }
    }
}
