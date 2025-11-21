package ro.maleficent.tunnelertnt.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.system.NonnullDefault;
import ro.maleficent.tunnelertnt.block.TunnelerTntBlock;
import ro.maleficent.tunnelertnt.registry.ModBlocks;

public class TunnelerTntEntity extends PrimedTnt {

    private static final float BLAST_POWER = 4.0F; // normal TNT power
    private static final int TUNNEL_LENGTH = 16;
    private static final int TUNNEL_RADIUS = 4;

    private static final EntityDataAccessor<Integer> DATA_TUNNEL_FACING =
            SynchedEntityData.defineId(TunnelerTntEntity.class, EntityDataSerializers.INT);

    public TunnelerTntEntity(EntityType<? extends TunnelerTntEntity> type, Level level) {
        super(type, level);
    }

    @SuppressWarnings("unused")
    public TunnelerTntEntity(EntityType<? extends TunnelerTntEntity> type,
                             Level level,
                             double x,
                             double y,
                             double z,
                             Direction facing) {
        super(type, level);
        this.setPos(x, y, z);
        this.setDirection(facing);

        // Slightly longer, 6 seconds fuse
        this.setFuse(120);

        // small random push like vanilla TNT
        double angle = level.random.nextDouble() * (Math.PI * 2.0);
        this.setDeltaMovement(
                -Math.sin(angle) * 0.02,
                0.2,
                -Math.cos(angle) * 0.02
        );

        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TUNNEL_FACING, Direction.NORTH.get3DDataValue());
    }

    public void setDirection(Direction facing) {
        this.entityData.set(DATA_TUNNEL_FACING, facing.get3DDataValue());
    }

    @NonnullDefault
    public Direction getDirection() {
        return Direction.from3DDataValue(this.entityData.get(DATA_TUNNEL_FACING));
    }

    @Override
    public void tick() {
        if (!this.isNoGravity()) {
            // Apply Gravity
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }

        // Handle Movement
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));

        // Ground friction
        if (this.onGround()) {
            this.setDeltaMovement(
                    this.getDeltaMovement().multiply(0.7, -0.5, 0.7)
            );
        }

        // Countdown
        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);

        if (fuse <= 0) {
            // TIME'S UP!
            this.discard(); // Remove entity
            if (!this.level().isClientSide()) {
                this.explodeAndCarve(); // BOOM
            }
        } else {
            // Visuals while waiting
            this.updateInWaterStateAndDoFluidPushing();
            if (this.level().isClientSide()) {
                this.level().addParticle(
                        ParticleTypes.SMOKE,
                        this.getX(),
                        this.getY() + 0.5,
                        this.getZ(),
                        0.0,
                        0.0,
                        0.0
                );
            }
        }
    }

    private void explodeAndCarve() {
        Level level = this.level();

        // 1) Vanilla-strength TNT explosion (power 4)
        level.explode(
                this,
                this.getX(),
                this.getY(0.0625D),
                this.getZ(),
                BLAST_POWER,
                Level.ExplosionInteraction.TNT
        );

        // 2) Tunnel carve in facing direction, skipping bedrock and ores
        carveTunnel(level);
    }

    private void carveTunnel(Level level) {
        Direction dir = getDirection();
        if (!dir.getAxis().isHorizontal()) {
            dir = Direction.NORTH;
        }

        BlockPos origin = this.blockPosition();
        RandomSource random = level.getRandom();

        // SETTING: Drop Rate (e.g., 0.3 means only 30% of blocks drop items)
        // This prevents server overload and lag while still giving loot
        float dropChance = 0.3f;

        // Loop 1: LENGTH (Forward 0 to 16)
        for (int distance = 1; distance <= TUNNEL_LENGTH; distance++) {
            BlockPos centerSlice = origin.relative(dir, distance);

            // Loop 2: WIDTH(Radius -4 to 4)
            for (int dx = -TUNNEL_RADIUS; dx <= TUNNEL_RADIUS; dx++) {
                for (int dy = -TUNNEL_RADIUS; dy <= TUNNEL_RADIUS; dy++) {

                    // Orient the circle based on facing direction
                    BlockPos targetPos;
                    if (dir.getAxis() == Direction.Axis.X) {
                        // EAST/WEST: forward is X, side is Z
                        targetPos = centerSlice.offset(0, dy, dx);
                    } else {
                        // NORTH/SOUTH: forward is Z, side is X
                        targetPos = centerSlice.offset(dx, dy, 0);
                    }

                    BlockState state = level.getBlockState(targetPos);

                    // Checks
                    // Skip air, bedrock and ores
                    if (state.isAir() || state.is(Blocks.BEDROCK) || isPrecious(state)) {
                        continue;
                    }

                    // JITTER MATH
                    double distSq = dx * dx + dy * dy;
                    double maxRadius = TUNNEL_RADIUS;

                    // Noise: 0.0 to 1.2 (Rough walls)
                    double noise = random.nextDouble() * 1.2;
                    double effectiveRadius = maxRadius - noise;

                    if (distSq > (effectiveRadius * effectiveRadius)) {
                        continue;
                    }

                    // Roll the dice for block destruction
                    // If random value is < 0.3, we drop (true). Otherwise, we destroy (false)
                    boolean shouldDrop = random.nextFloat() < dropChance;

                    level.destroyBlock(targetPos,shouldDrop, this);
                }
            }
        }
    }

    private boolean isPrecious(BlockState state) {
        // Stone & deepslate ores
        if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)) return true;
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) return true;
        if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) return true;
        if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)) return true;
        if (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)) return true;
        if (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)) return true;
        if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) return true;
        if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) return true;

        // Nether ores / netherite
        if (state.is(Blocks.NETHER_QUARTZ_ORE)) return true;
        if (state.is(Blocks.NETHER_GOLD_ORE)) return true;
        if (state.is(Blocks.ANCIENT_DEBRIS)) return true;

        // Storage / “precious” blocks (optional but usually desired)
        if (state.is(Blocks.COAL_BLOCK)) return true;
        if (state.is(Blocks.IRON_BLOCK) || state.is(Blocks.COPPER_BLOCK)) return true;
        if (state.is(Blocks.GOLD_BLOCK) || state.is(Blocks.RAW_GOLD_BLOCK)) return true;
        if (state.is(Blocks.REDSTONE_BLOCK)) return true;
        if (state.is(Blocks.LAPIS_BLOCK)) return true;
        if (state.is(Blocks.DIAMOND_BLOCK)) return true;
        if (state.is(Blocks.EMERALD_BLOCK)) return true;
        if (state.is(Blocks.RAW_IRON_BLOCK) || state.is(Blocks.RAW_COPPER_BLOCK)) return true;
        if (state.is(Blocks.NETHERITE_BLOCK)) return true;

        // Dungeon blocks
        if (state.is(Blocks.SPAWNER) || state.is(Blocks.CHEST)) return true;

        return false;
    }

    @Override
    @NonnullDefault
    public BlockState getBlockState() {
        return ModBlocks.TUNNELER_TNT.defaultBlockState().setValue(TunnelerTntBlock.FACING, getDirection());
    }
}
