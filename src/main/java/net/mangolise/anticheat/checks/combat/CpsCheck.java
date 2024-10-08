package net.mangolise.anticheat.checks.combat;

import net.mangolise.anticheat.ACCheck;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.tag.Tag;

import java.util.*;

public class CpsCheck extends ACCheck {
    private static final double THRESHOLD = 20;
    private static final long SAMPLE_TIME = 1000;
    private static final Tag<List<Long>> HITS_TAG = Tag.<List<Long>>Transient("anticheat_cps_hits").defaultValue(ArrayList::new);

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

            List<Long> pHits = player.getTag(HITS_TAG);
            pHits.add(System.currentTimeMillis());
            pHits.removeIf(time -> time < System.currentTimeMillis() - SAMPLE_TIME);
            player.setTag(HITS_TAG, pHits);

            int cps = pHits.size();
            debug(player, "CPS: " + cps + " (SD: " + standardDeviation(pHits) + ")");

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
