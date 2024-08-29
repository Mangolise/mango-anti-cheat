package net.mangolise.anticheat.checks.movement;

import net.mangolise.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.PlayerMoveEvent;

import java.util.*;

public class TeleportSpamCheck extends ACCheck {
    private static final double TELEPORT_DISTANCE_THRESHOLD = 1.5;
    private static final double SAMPLING_PERIOD = 1000;
    private static final double MAX_TELEPORTS = 2;

    private final Map<UUID, List<Long>> teleportTimes = new HashMap<>();

    public TeleportSpamCheck() {
        super("TeleportSpam");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, e -> {
            if (isBypassing(e.getPlayer())) return;

            Pos pos1 = e.getPlayer().getPosition().withY(0);
            Pos pos2 = e.getNewPosition().withY(0);

            if (pos1.distance(pos2) < TELEPORT_DISTANCE_THRESHOLD) {
                return;
            }

            UUID uuid = e.getPlayer().getUuid();
            if (!teleportTimes.containsKey(uuid)) {
                teleportTimes.put(uuid, new ArrayList<>());
            }

            List<Long> times = teleportTimes.get(uuid);
            long currentTime = System.currentTimeMillis();
            times.add(currentTime);

            while (!times.isEmpty() && times.getFirst() < currentTime - SAMPLING_PERIOD) {
                times.removeFirst();
            }

            debug(e.getPlayer(), "Teleports: " + times.size());

            if (times.size() > MAX_TELEPORTS) {
                float certainty = Math.min(1f, ((float) (times.size() - MAX_TELEPORTS) / 10f) + 0.5f);
                flag(e.getPlayer(), certainty);
                if (!config.passive()) e.setCancelled(true);
            }
        });
    }
}
