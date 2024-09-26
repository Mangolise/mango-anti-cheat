package net.mangolise.anticheat.checks.combat;

import net.mangolise.anticheat.ACCheck;
import net.mangolise.anticheat.Tuple;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.tag.Tag;

import java.util.*;

public class HitConsistencyCheck extends ACCheck {
    private static final double THRESHOLD = 1;
    private static final long SAMPLE_TIME = 1000;
    private static final Tag<List<Long>> HITS_TAG = Tag.<List<Long>>Transient("anticheat_hitconsis_hits").defaultValue(ArrayList::new);
    private static final Tag<List<Tuple<Long, Double>>> HIT_STDS_TAG = Tag.<List<Tuple<Long, Double>>>Transient("anticheat_hitconsis_hitstds").defaultValue(ArrayList::new);

    public HitConsistencyCheck() {
        super("HitConsistency");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, e -> {
            if (!(e.getEntity() instanceof Player player)) {
                return;
            }

            if (isBypassing(player)) return;

            List<Long> pHits = player.getTag(HITS_TAG);
            pHits.add(System.currentTimeMillis());
            pHits.removeIf(time -> time < System.currentTimeMillis() - SAMPLE_TIME);
            player.setTag(HITS_TAG, pHits);

            double std = standardDeviationLong(pHits);

            List<Tuple<Long, Double>> pHitStds = player.getTag(HIT_STDS_TAG);
            pHitStds.add(new Tuple<>(System.currentTimeMillis(), std));
            pHitStds.removeIf(val -> val.getFirst() < System.currentTimeMillis() - SAMPLE_TIME);
            player.setTag(HIT_STDS_TAG, pHitStds);

            if (pHitStds.size() < 5) {
                debug(player, "No hit stds");
                return;
            }

            double stdStd = standardDeviationDouble(pHitStds.stream().map(Tuple::getSecond).toList());
            debug(player, "SDSD: " + stdStd);

            if (stdStd <= THRESHOLD) {
                float certainty = Math.max(0.1f, 1f - (float) (stdStd / THRESHOLD));
                flag(player, certainty);
            }
        });
    }

    private double standardDeviationLong(List<Long> vals) {
        double sum = 0;
        for (Long hit : vals) {
            sum += hit;
        }
        double mean = sum / vals.size();
        double squaredDifferenceSum = 0;
        for (Long hit : vals) {
            squaredDifferenceSum += Math.pow(hit - mean, 2);
        }
        return Math.sqrt(squaredDifferenceSum / vals.size());
    }

    private double standardDeviationDouble(List<Double> vals) {
        double sum = 0;
        for (Double hit : vals) {
            sum += hit;
        }
        double mean = sum / throwIfZero(vals.size());
        double squaredDifferenceSum = 0;
        for (Double hit : vals) {
            squaredDifferenceSum += Math.pow(hit - mean, 2);
        }
        return Math.sqrt(squaredDifferenceSum / vals.size());
    }

    private double throwIfZero(double d) {
        if (d == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return d;
    }
}
