package ro.maleficent.tunnelertnt.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import ro.maleficent.tunnelertnt.entity.TunnelerTntEntity;
import ro.maleficent.tunnelertnt.registry.ModEntities;

public class TunnelerTntBlock extends TntBlock {

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TunnelerTntBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.defaultBlockState()
                        .setValue(BlockStateProperties.UNSTABLE, false)
                        .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Player looks direction D, tunnel goes in D, so block "front" faces away
        return this.defaultBlockState()
                .setValue(BlockStateProperties.UNSTABLE, false)
                .setValue(FACING, ctx.getHorizontalDirection());
    }

    // Helper: prime Tunneler TNT (no owner)
    @SuppressWarnings("unused")
    public static boolean prime(Level level, BlockPos pos, BlockState state) {
        return prime(level, pos, state, null);
    }

    // Helper: prime Heavy TNT with an optional owner
    private static boolean prime(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity igniter) {
        if (level instanceof ServerLevel serverLevel && serverLevel.getGameRules().getBoolean(GameRules.RULE_TNT_EXPLODES)) {

            Direction facing = state.getValue(FACING);

            TunnelerTntEntity tunneler_tnt = new TunnelerTntEntity(
                    ModEntities.TUNNELER_TNT,
                    level,
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    facing
            );

            serverLevel.addFreshEntity(tunneler_tnt);
            level.playSound(
                    null,
                    tunneler_tnt.getX(), tunneler_tnt.getY(), tunneler_tnt.getZ(),
                    SoundEvents.TNT_PRIMED,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
            level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
            return true;
        }

        return false;
    }

    // Redstone / block update priming
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!oldState.is(state.getBlock())) {
            if (level.hasNeighborSignal(pos) && prime(level, pos, state, null)) {
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean moved) {
        if (level.hasNeighborSignal(pos) && prime(level, pos, state, null)) {
            level.removeBlock(pos, false);
        }
    }

    // Breaking UNSTABLE TNT
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && !player.getAbilities().instabuild
                && state.getValue(BlockStateProperties.UNSTABLE)) {
            prime(level, pos, state, player);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    // Explosion chain reactions
    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        if (level.getGameRules().getBoolean(GameRules.RULE_TNT_EXPLODES)) {
            BlockState state = level.getBlockState(pos);
            Direction facing = state.hasProperty(FACING)
                    ? state.getValue(FACING)
                    : Direction.NORTH;

            TunnelerTntEntity tunneler_tnt = new TunnelerTntEntity(
                    ModEntities.TUNNELER_TNT,
                    level,
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    facing
            );

            int baseFuse = tunneler_tnt.getFuse();
            tunneler_tnt.setFuse((short)(level.random.nextInt(baseFuse / 4) + baseFuse / 8));
            level.addFreshEntity(tunneler_tnt);
        }
    }

    // Flint & steel / fire charge
    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        if (prime(level, pos, state, player)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            Item item = stack.getItem();

            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            } else {
                stack.consume(1, player);
            }

            player.awardStat(Stats.ITEM_USED.get(item));
        } else if (level instanceof ServerLevel serverLevel && !serverLevel.getGameRules().getBoolean(GameRules.RULE_TNT_EXPLODES)) {
            player.displayClientMessage(Component.translatable("block.minecraft.tnt.disabled"), true);
            return InteractionResult.PASS;
        }

        return InteractionResult.SUCCESS;
    }

    // Burning projectiles
    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = hit.getBlockPos();

            if (projectile.isOnFire()
                    && projectile.mayInteract(serverLevel, pos)
                    && prime(level, pos, state)) {
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}