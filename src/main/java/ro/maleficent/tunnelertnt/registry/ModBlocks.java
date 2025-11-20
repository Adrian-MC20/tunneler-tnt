package ro.maleficent.tunnelertnt.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import ro.maleficent.tunnelertnt.TunnelerTNT;
import ro.maleficent.tunnelertnt.block.HeavyTntBlock;
import ro.maleficent.tunnelertnt.block.TunnelerTntBlock;

public final class ModBlocks {

    @SuppressWarnings("unused")
    public static final Block HEAVY_TNT = create("heavy_tnt", HeavyTntBlock::new);

    @SuppressWarnings("unused")
    public static final Block TUNNELER_TNT = create("tunneler_tnt", TunnelerTntBlock::new);

    private ModBlocks() {}

    private static Block create(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {

        // Build ID
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TunnelerTNT.MOD_ID, name);

        // Create block key
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);

        // Build block properties and APPLY THE ID
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .instabreak()
                .sound(Blocks.TNT.defaultBlockState().getSoundType())
                .setId(blockKey);

        // Construct block
        Block block = factory.apply(props);

        // Register block
        Registry.register(BuiltInRegistries.BLOCK, id, block);

        // Create item key and item properties WITH THE ID SET
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Item.Properties itemProps = new Item.Properties().setId(itemKey);

        // Register item
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, itemProps));

        return block;
    }

    public static void init() {}
}