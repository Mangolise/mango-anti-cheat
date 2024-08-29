package net.mangolise.anticheat.checks.movement;

import net.mangolise.anticheat.ACCheck;
import net.mangolise.anticheat.Tuple;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.player.PlayerMoveEvent;

import java.util.*;

public class FlightCheck extends ACCheck {
    private static final long MIN_SAMPLE_TIME = 500;
    private final HashMap<UUID, List<Tuple<Long, Pos>>> playerDetails = new HashMap<>();

    public FlightCheck() {
        super("Flight");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, this::onMove);
        MinecraftServer.getGlobalEventHandler().addListener(AddEntityToInstanceEvent.class, e -> {
            if (e.getEntity() instanceof Player p) {
                playerDetails.remove(p.getUuid());
            }
        });
    }

    private void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!canCheck(p)) return;

        if (isBypassFly(p)) {
            debug(p, "bypassed");
            return;
        }

        Pos from = e.getPlayer().getPosition();
        Long fromTime = System.currentTimeMillis();
        Pos to = e.getNewPosition();
        if (playerDetails.containsKey(p.getUuid())) {
            playerDetails.get(p.getUuid()).removeIf(tuple -> tuple.getFirst() < System.currentTimeMillis() - 1000);
            playerDetails.get(p.getUuid()).add(new Tuple<>(System.currentTimeMillis(), to));

            if (playerDetails.get(p.getUuid()).size() == 1) {
                debug(p, "not enough samples");
                return;
            }
            from = playerDetails.get(p.getUuid()).getFirst().getSecond();
            fromTime = playerDetails.get(p.getUuid()).getFirst().getFirst();
        } else {
            List<Tuple<Long, Pos>> details = new java.util.ArrayList<>();
            details.add(new Tuple<>(System.currentTimeMillis(), to));
            playerDetails.put(p.getUuid(), details);
        }

        if (Objects.requireNonNull(to).y() != from.y()) {
            playerDetails.remove(p.getUuid());
            debug(p, "changed y");
            return;
        }

        // Make sure they moved on the x or z axis.
        if (to.x() == from.x() && to.z() == from.z()) {
            debug(p, "didn't move");
            return;
        }

        if (isOnGround(p)) {
            playerDetails.remove(p.getUuid());
            debug(p, "on ground");
            return;
        }

        // Return if the oldest location is newer than half a second
        if (fromTime > System.currentTimeMillis() - MIN_SAMPLE_TIME) {
            debug(p, "sample time not met");
            return;
        }

        float certainty = 0.7f;  // We are cancelling so it's binary, we can't have varying certainty.
        flag(p, certainty);  // They have moved without going up or down, and they are not on the ground.
        if (!config.passive()) {
            e.setCancelled(true);
        }
    }
}
