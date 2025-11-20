package ro.maleficent.tunnelertnt.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import ro.maleficent.tunnelertnt.registry.ModEntities;

public class HeavyTntEntity extends PrimedTnt {

    // Vanilla TNT ≈ 80 ticks. We want 6 seconds: 20 * 6 = 120
    private static final int HEAVY_TNT_FUSE_TICKS = 120;

    // Vanilla TNT uses power 4.0. We want roughly diameter of 15 so the power should be 7.
    private static final float HEAVY_TNT_POWER = 7.0F;

    public HeavyTntEntity(EntityType<? extends HeavyTntEntity> type, Level world) {
        super(type, world);
        this.setFuse(HEAVY_TNT_FUSE_TICKS);
    }

    @SuppressWarnings("unused")
    public HeavyTntEntity(EntityType<? extends HeavyTntEntity> type,
                          Level level,
                          double x,
                          double y,
                          double z) {
        super(ModEntities.HEAVY_TNT, level);

        this.setPos(x, y, z);

        this.xo = x;
        this.yo = y;
        this.zo = z;

        // small random push like vanilla TNT
        double angle = level.random.nextDouble() * (Math.PI * 2.0);
        this.setDeltaMovement(
                -Math.sin(angle) * 0.02,
                0.2,
                -Math.cos(angle) * 0.02
        );

        this.setFuse(HEAVY_TNT_FUSE_TICKS);
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
                level.explode(
                        this,
                        this.getX(),
                        this.getY(0.0625D),
                        this.getZ(),
                        HEAVY_TNT_POWER,
                        Level.ExplosionInteraction.TNT
                );
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
}
