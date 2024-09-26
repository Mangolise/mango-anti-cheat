package net.mangolise.anticheat;

import net.mangolise.anticheat.events.PlayerFlagEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.block.Block;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.timer.TaskSchedule;

import java.util.*;

public abstract class ACCheck {
    private final String name;
    protected MangoAC.Config config;
    protected MangoAC ac;
    protected final List<UUID> disabledPlayers = new ArrayList<>();

    private static final Set<Integer> NO_FALL_DAMAGE_BLOCKS = Set.of(
            Block.WATER.id(),
            Block.LAVA.id(),
            Block.SLIME_BLOCK.id(),
            Block.HONEY_BLOCK.id(),
            Block.COBWEB.id(),
            Block.VINE.id(),
            Block.LADDER.id(),
            Block.WEEPING_VINES.id(),
            Block.TWISTING_VINES.id(),
            Block.WEEPING_VINES_PLANT.id(),
            Block.TWISTING_VINES_PLANT.id()
    );

    private static final Set<Integer> BUBBLE_COLUMN_BLOCKS = Set.of(
            Block.BUBBLE_COLUMN.id(),
            Block.BUBBLE_CORAL_BLOCK.id(),
            Block.BUBBLE_CORAL_FAN.id(),
            Block.BUBBLE_CORAL_WALL_FAN.id(),
            Block.BUBBLE_CORAL.id()
    );

    public ACCheck(String name) {
        this.name = name;
    }

    public void enable(MangoAC ac, MangoAC.Config config) {
        this.config = config;
        this.ac = ac;
        register();
    }

    public abstract void register();

    public String name() {
        return name;
    }

    public void disableFor(Player player, int time) {
        disabledPlayers.add(player.getUuid());
        MinecraftServer.getSchedulerManager().buildTask(() ->
                disabledPlayers.remove(player.getUuid())).delay(TaskSchedule.tick(time)).schedule();
    }

    protected void debug(Player player, String message) {
        if (!config.debugChecks().contains(name)) {
            return;
        }
        player.sendMessage("[" + name() + "] " + message);
    }

    protected boolean isBypassing(Player player) {
        return player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR ||
                disabledPlayers.contains(player.getUuid());
    }

    public void flag(Player player, float certainty) {
        PlayerFlagEvent event = new PlayerFlagEvent(name(), player, certainty);
        MinecraftServer.getGlobalEventHandler().call(event);
    }

    public Set<Integer> getBlockIdsAtPlayerFeet(Player p, double yOffset) {
        Pos playerLocation = p.getPosition().add(0, yOffset, 0);
        Set<BlockVec> points = new HashSet<>();
        BoundingBox box = p.getBoundingBox();

        // get the rounded points for all four corners and add them to a set to remove duplicates
        // center isn't needed because the player is less a block wide
        points.add(new BlockVec(playerLocation.add(box.minX(), box.minY(), box.minZ())));
        points.add(new BlockVec(playerLocation.add(box.minX(), box.minY(), box.maxZ())));
        points.add(new BlockVec(playerLocation.add(box.maxX(), box.minY(), box.minZ())));
        points.add(new BlockVec(playerLocation.add(box.maxX(), box.minY(), box.maxZ())));

        Set<Integer> blockIds = new HashSet<>(points.size());
        for (Point point : points) {
            blockIds.add(ac.getBlockAt(p, point).id());
        }

        return blockIds;
    }

    public Set<Integer> getBlocksPlayerIsStandingOn(Player p) {
        return getBlockIdsAtPlayerFeet(p, -0.1);
    }

    public Set<Integer> getBlockIdsPlayerIsStandingOnAndAbove(Player p, int upAmount, boolean ignoreFeet) {
        Set<Integer> idSet = new HashSet<>();
        for (int i = ignoreFeet ? 1 : 0; i < upAmount; i++) {
            idSet.addAll(getBlockIdsAtPlayerFeet(p, i-0.1));
        }

        return idSet;
    }

    public boolean isOnGround(Player p) {
        return !getBlocksPlayerIsStandingOn(p).stream().allMatch(id -> id == Block.AIR.id());
    }

    public boolean shouldNegateFallDamage(Player p) {
        Set<Integer> standingOn = getBlockIdsPlayerIsStandingOnAndAbove(p, 2, false);
        return standingOn.stream().anyMatch(NO_FALL_DAMAGE_BLOCKS::contains);
    }

    public boolean isBypassSpeed(Player p) {
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) {
            return true;
        }
        //TODO: Riptide
        if (p.isAllowFlying()) {
            return true;
        }
        if (p.isFlyingWithElytra()) {
            return true;
        }
        if (p.getVehicle() != null) {
            return true;
        }
        if (p.hasEffect(PotionEffect.SPEED)) {
            return true;
        }
        if (p.hasEffect(PotionEffect.JUMP_BOOST)) {
            return true;
        }
        if (p.hasEffect(PotionEffect.DOLPHINS_GRACE)) {
            return true;
        }
        if (isInBubbleColumn(p)) {
            return true;
        }

        if (isOnGround(p)) {
            Set<Integer> standingOn = getBlocksPlayerIsStandingOn(p);
            if (standingOn.contains(Block.SOUL_SAND.id()) || standingOn.contains(Block.SOUL_SOIL.id()) && ACUtils.isUsingSoulSpeed(p)) {
                return true;
            }

            return standingOn.contains(Block.ICE.id());
        }
        return false;
    }

    public boolean isBypassFly(Player p) {
        // TODO: Check riptiding
        return (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) ||
                p.isAllowFlying() ||
                p.isFlyingWithElytra() ||
                p.getVehicle() != null ||
                p.hasEffect(PotionEffect.JUMP_BOOST);
    }

    public boolean isInBubbleColumn(Player p) {
        Set<Integer> standingOn = getBlocksPlayerIsStandingOn(p);
        return standingOn.stream().anyMatch(BUBBLE_COLUMN_BLOCKS::contains);
    }

    public double getRunSpeed(Player p) {
        return p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() * 1.3;
    }
}
