package net.mangolise.anticheat;

import net.mangolise.anticheat.events.PlayerFlagEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.block.Block;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.timer.TaskSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class ACCheck {
    private final String name;
    protected MangoAC.Config config;
    protected MangoAC ac;
    protected final List<UUID> disabledPlayers = new ArrayList<>();

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
        if (!config.debugChecks().contains(message)) {
            return;
        }
        player.sendMessage("[" + name() + "] " + message);
    }

    protected boolean isBypassing(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }
        if (disabledPlayers.contains(player.getUuid())) {
            return true;
        }
        return false;
    }

    public void flag(Player player, float certainty) {
        PlayerFlagEvent event = new PlayerFlagEvent(name(), player, certainty);
        MinecraftServer.getGlobalEventHandler().call(event);
    }

    public List<Block> getBlocksPlayerIsStandingOn(Player p, int yOffset) {
        List<Block> materialList = new ArrayList<>();
        Pos playerLocation = p.getPosition();
        double x = playerLocation.x();
        double y = playerLocation.y() + yOffset;
        double z = playerLocation.z();

        // get 4 corners of the player hitbox
        Pos corner1 = new Pos(x + 0.3, y-1, z + 0.3);
        Pos corner2 = new Pos(x - 0.3, y-1, z + 0.3);
        Pos corner3 = new Pos(x + 0.3, y-1, z - 0.3);
        Pos corner4 = new Pos(x - 0.3, y-1, z - 0.3);

        Block block1 = ac.getBlockAt(p, corner1);
        Block block2 = ac.getBlockAt(p, corner2);
        Block block3 = ac.getBlockAt(p, corner3);
        Block block4 = ac.getBlockAt(p, corner4);

        // add the blocks to the list
        materialList.add(block1);
        materialList.add(block2);
        materialList.add(block3);
        materialList.add(block4);
        return materialList;
    }

    public List<Block> getBlocksPlayerIsStandingOn(Player p) {
        return getBlocksPlayerIsStandingOn(p, 0);
    }

    public List<Block> getBlocksPlayerIsStandingOnAndAbove(Player p, int upAmount, boolean ignoreFeet) {
        List<Block> materialList = new ArrayList<>();
        for (int i = ignoreFeet ? 1 : 0; i < upAmount; i++) {
            materialList.addAll(getBlocksPlayerIsStandingOn(p, i));
        }
        return materialList;
    }

    public boolean isOnGround(Player p) {
        return getBlocksPlayerIsStandingOn(p).stream().anyMatch(b -> !b.compare(Block.AIR));
    }

    public boolean isNegateFallDamage(Player p) {
        List<Block> standingOn = getBlocksPlayerIsStandingOnAndAbove(p, 2, false);
        return isInBlock(Block.WATER, standingOn) ||
                isInBlock(Block.LAVA, standingOn) ||
                isInBlock(Block.SLIME_BLOCK, standingOn) ||
                isInBlock(Block.HONEY_BLOCK, standingOn) ||
                isInBlock(Block.COBWEB, standingOn) ||
                isInBlock(Block.VINE, standingOn) ||
                isInBlock(Block.LADDER, standingOn) ||
                isInBlock(Block.WEEPING_VINES, standingOn) ||
                isInBlock(Block.TWISTING_VINES, standingOn) ||
                isInBlock(Block.WEEPING_VINES_PLANT, standingOn) ||
                isInBlock(Block.TWISTING_VINES_PLANT, standingOn);
    }

    public boolean isInBlock(Player p, Block material) {
        List<Block> materials = getBlocksPlayerIsStandingOn(p);
        return materials.stream().anyMatch(b -> b == material);
    }

    public boolean isInBlock(Block material, List<Block> standingOn) {
        return standingOn.stream().anyMatch(b -> b.compare(material));
    }

    public boolean isBypassSpeed(Player p) {
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) {
            return true;
        }
        //TODO: Riptide
        if (p.isFlying()) {
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
            List<Block> standingOn = getBlocksPlayerIsStandingOn(p);
            if ((isInBlock(Block.SOUL_SAND, standingOn) || isInBlock(Block.SOUL_SOIL, standingOn)) && ACUtils.isUsingSoulSpeed(p)) {
                return true;
            }
            if (isInBlock(Block.ICE, standingOn)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBypassFly(Player p) {
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) {
            return true;
        }
        if (p.isFlying()) {
            return true;
        }
        if (p.isFlyingWithElytra()) {
            return true;
        }
        // TODO: Check riptiding
        if (p.getVehicle() != null) {
            return true;
        }
        if (p.hasEffect(PotionEffect.JUMP_BOOST)) {
            return true;
        }
        return false;
    }

    public boolean isInBubbleColumn(Player p) {
        List<Block> standingOn = getBlocksPlayerIsStandingOn(p);
        return isInBlock(Block.BUBBLE_COLUMN, standingOn) ||
                isInBlock(Block.BUBBLE_CORAL_BLOCK, standingOn) ||
                isInBlock(Block.BUBBLE_CORAL_FAN, standingOn) ||
                isInBlock(Block.BUBBLE_CORAL_WALL_FAN, standingOn) ||
                isInBlock(Block.BUBBLE_CORAL, standingOn) ||
                isInBlock(Block.BUBBLE_CORAL_FAN, standingOn) ||
                isInBlock(Block.BUBBLE_CORAL_WALL_FAN, standingOn);
    }

    public double getRunSpeed(Player p) {
        return p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() * 1.3;
    }
}
