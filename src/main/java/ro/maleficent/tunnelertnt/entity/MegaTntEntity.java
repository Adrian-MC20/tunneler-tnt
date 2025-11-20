package ro.maleficent.tunnelertnt.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class MegaTntEntity extends PrimedTnt {

    // Vanilla TNT ≈ 80 ticks. We want 8 seconds: 20 * 8 = 160
    private static final int MEGA_FUSE_TICKS = 160;

    // Vanilla TNT uses power 4.0. We want roughly radius 10, so power 10 is a good starting point.
    private static final float MEGA_POWER = 10.0F;

    public MegaTntEntity(EntityType<? extends MegaTntEntity> type, Level world) {
        super(type, world);
        this.setFuse(MEGA_FUSE_TICKS);
    }

    @SuppressWarnings("unused")
    public MegaTntEntity(EntityType<? extends  MegaTntEntity> type,
                         Level level,
                         double x,
                         double y,
                         double z,
                         @Nullable LivingEntity owner) {
        super(level, x, y, z, owner);

        // small random push like vanilla TNT
        double angle = level.random.nextDouble() * (Math.PI * 2.0);
        this.setDeltaMovement(
                -Math.sin(angle) * 0.02,
                0.2,
                -Math.cos(angle) * 0.02
        );

        this.setFuse(MEGA_FUSE_TICKS);
    }

    @SuppressWarnings("resource")
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
                        MEGA_POWER,
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
