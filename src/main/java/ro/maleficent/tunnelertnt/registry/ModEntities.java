package ro.maleficent.tunnelertnt.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import ro.maleficent.tunnelertnt.TunnelerTNT;
import ro.maleficent.tunnelertnt.entity.HeavyTntEntity;

public final class ModEntities {

    private static final ResourceLocation HEAVY_TNT_ID = ResourceLocation.fromNamespaceAndPath(TunnelerTNT.MOD_ID, "heavy_tnt");

    private static final ResourceKey<EntityType<?>> HEAVY_TNT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, HEAVY_TNT_ID);

    public static final EntityType<HeavyTntEntity> HEAVY_TNT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            HEAVY_TNT_ID,
            EntityType.Builder
                    .<HeavyTntEntity>of(HeavyTntEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .build(HEAVY_TNT_KEY)
    );

    private ModEntities() {

    }

    public static void init() {
        // ensure class load
    }
}