package net.mangolise.checks.combat;

import net.mangolise.ACCheck;
import net.mangolise.Tuple;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HitConsistencyCheck extends ACCheck {
    private static final double THRESHOLD = 1;
    private static final long SAMPLE_TIME = 1000;
    private final Map<UUID, List<Long>> hits = new HashMap<>();
    private final Map<UUID, List<Tuple<Long, Double>>> hitStds = new HashMap<>();

    public HitConsistencyCheck() {
        super("HitConsistency");
    }

    @Override
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, e -> {
            if (!(e.getEntity() instanceof Player player)) {
                return;
            }

            if (!hits.containsKey(player.getUuid())) {
                hits.put(player.getUuid(), new java.util.ArrayList<>());
            }

            if (!hitStds.containsKey(player.getUuid())) {
                hitStds.put(player.getUuid(), new java.util.ArrayList<>());
            }

            hits.get(player.getUuid()).add(System.currentTimeMillis());
            hits.get(player.getUuid()).removeIf(time -> time < System.currentTimeMillis() - SAMPLE_TIME);

            double std = standardDeviationLong(hits.get(player.getUuid()));

            hitStds.get(player.getUuid()).add(new Tuple<>(System.currentTimeMillis(), std));
            hitStds.get(player.getUuid()).removeIf(val -> val.getFirst() < System.currentTimeMillis() - SAMPLE_TIME);

            if (hitStds.get(player.getUuid()).size() < 5) {
                debug(player, "No hit stds");
                return;
            }

            double stdStd = standardDeviationDouble(hitStds.get(player.getUuid()).stream().map(Tuple::getSecond).toList());
            debug(player, "SDSD: " + stdStd);

            if (stdStd <= THRESHOLD) {
                flag(player, 0.6f);
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
