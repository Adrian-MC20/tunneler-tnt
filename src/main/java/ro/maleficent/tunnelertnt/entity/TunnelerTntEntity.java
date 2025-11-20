package ro.maleficent.tunnelertnt.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class TunnelerTntEntity extends PrimedTnt {

    private static final float BASE_POWER = 4.0F; // normal TNT power
    private static final int TUNNEL_LENGTH = 16;
    private static final int TUNNEL_RADIUS = 4;

    private Direction facing = Direction.NORTH;

    public TunnelerTntEntity(EntityType<? extends TunnelerTntEntity> type, Level level) {
        super(type, level);
    }

    @SuppressWarnings("unused")
    public TunnelerTntEntity(EntityType<? extends TunnelerTntEntity> type,
                             Level level,
                             double x,
                             double y,
                             double z,
                             Direction facing,
                             @Nullable LivingEntity owner) {
        super(level, x, y, z, owner);
        this.facing = facing;

        // small random push like vanilla TNT
        double angle = level.random.nextDouble() * (Math.PI * 2.0);
        this.setDeltaMovement(
                -Math.sin(angle) * 0.02,
                0.2,
                -Math.cos(angle) * 0.02
        );
    }

    @SuppressWarnings("unused")
    public void setDirection(Direction facing) {
        this.facing = facing;
    }

    public Direction getDirection() {
        return this.facing;
    }

    @Override
    public void tick() {
        Level level = this.level();

        // TNT physics
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));

        if (this.onGround()) {
            this.setDeltaMovement(
                    this.getDeltaMovement().multiply(0.7, -0.5, 0.7)
            );
        }

        // fuse countdown
        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);

        if (fuse <= 0) {
            this.discard();

            if (!level.isClientSide()) {
                // custom helper: normal TNT explosion + tunnel carve
                performExplosionAndTunnel(level);
            }
        } else {
            this.updateInWaterStateAndDoFluidPushing();

            if (level.isClientSide()) {
                level.addParticle(
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

    private void performExplosionAndTunnel(Level level) {
        // 1) Vanilla-strength TNT explosion (power 4)
        level.explode(
                this,
                this.getX(),
                this.getY(),
                this.getZ(),
                BASE_POWER,
                Level.ExplosionInteraction.TNT
        );

        // 2) Tunnel carve in facing direction, skipping bedrock and ores
        carveTunnel(level);
    }

    private void carveTunnel(Level level) {
        Direction dir = this.facing;
        if (!dir.getAxis().isHorizontal()) {
            dir = Direction.NORTH;
        }

        BlockPos origin = this.blockPosition();
        for (int distance = 1; distance <= TUNNEL_LENGTH; distance++) {
            BlockPos center = origin.relative(dir, distance);

            for (int dx = -TUNNEL_RADIUS; dx <= TUNNEL_RADIUS; dx++) {
                for (int dy = -TUNNEL_RADIUS; dy <= TUNNEL_RADIUS; dy++) {
                    if (dx * dx + dy * dy > TUNNEL_RADIUS*TUNNEL_RADIUS) {
                        continue;
                    }

                    BlockPos targetPos;
                    if (dir.getAxis() == Direction.Axis.X) {
                        // EAST/WEST: forward is X, side is Z
                        targetPos = center.offset(0, dy, dx);
                    } else {
                        // NORTH/SOUTH: forward is Z, side is X
                        targetPos = center.offset(dx, dy, 0);
                    }

                    BlockState state = level.getBlockState(targetPos);

                    // 1) Never break bedrock
                    if (state.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    // 2) Skip ores - keep all ore blocks intact and exposed
                    if (isPrecious(state)) {
                        continue;
                    }

                    // 3) Shape + noise: clean core, noisy edge
                    int innerR = TUNNEL_RADIUS - 1; // core radius that is always cleared

                    int distSq = dx * dx + dy * dy;
                    int innerSq = innerR * innerR;

                    // Inside the inner core: always break
                    if (distSq <= innerSq) {
                        level.destroyBlock(targetPos, true, this);
                        continue;
                    }

                    // We are in the outer ring: innerR < distance <= r
                    // We add noise so the edge blocks remain as little protrusions
                    RandomSource random = level.getRandom();

                    // Chance that a block STAYS (is NOT broken) in the edge ring
                    // 0.2 = 20% of edge blocks remain -> small teeth, no big blobs
                    double keepChance = 0.2;

                    if (random.nextDouble() < keepChance) {
                        // Leave this block to create a jagged edge
                        continue;
                    }

                    level.destroyBlock(targetPos,true, this);
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

        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        //save the direction as an integer (0-5)
        output.putInt("TunnelFacing", this.facing.get3DDataValue());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int facingId = input.getIntOr("TunnelFacing", Direction.NORTH.get3DDataValue());
        this.facing = Direction.from3DDataValue(facingId);
    }
}
