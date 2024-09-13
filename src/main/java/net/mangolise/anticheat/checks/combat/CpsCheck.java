package net.mangolise.anticheat.checks.combat;

import net.mangolise.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CpsCheck extends ACCheck {
    private static final double THRESHOLD = 20;
    private static final long SAMPLE_TIME = 1000;
    private final Map<UUID, List<Long>> hits = new HashMap<>();

    public CpsCheck() {
        super("CPS");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, e -> {
            if (!(e.getEntity() instanceof Player player)) {
                return;
            }

            if (isBypassing(player)) {
                return;
            }

            hits.computeIfAbsent(player.getUuid(), uuid -> new java.util.ArrayList<>()).add(System.currentTimeMillis());
            hits.get(player.getUuid()).removeIf(time -> time < System.currentTimeMillis() - SAMPLE_TIME);

            int cps = hits.get(player.getUuid()).size();
            debug(player, "CPS: " + cps + " (SD: " + standardDeviation(hits.get(player.getUuid())) + ")");

            if (cps >= THRESHOLD) {
                float certainty = Math.min(1f, ((float) (cps - THRESHOLD) / 10f) + 0.7f);
                flag(player, certainty);
            }
        });
    }

    private double standardDeviation(List<Long> hits) {
        double sum = 0;
        for (Long hit : hits) {
            sum += hit;
        }
        double mean = sum / hits.size();
        double squaredDifferenceSum = 0;
        for (Long hit : hits) {
            squaredDifferenceSum += Math.pow(hit - mean, 2);
        }
        return Math.sqrt(squaredDifferenceSum / hits.size());
    }
}
