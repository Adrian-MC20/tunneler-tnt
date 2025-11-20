package ro.maleficent.tunnelertnt.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import ro.maleficent.tunnelertnt.TunnelerTNT;
import ro.maleficent.tunnelertnt.entity.MegaTntEntity;

public final class ModEntities {

    private static final ResourceLocation MEGA_TNT_ID = ResourceLocation.fromNamespaceAndPath(TunnelerTNT.MOD_ID, "mega_tnt");

    private static final ResourceKey<EntityType<?>> MEGA_TNT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, MEGA_TNT_ID);

    public static final EntityType<MegaTntEntity> MEGA_TNT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            MEGA_TNT_ID,
            EntityType.Builder
                    .<MegaTntEntity>of(MegaTntEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .build(MEGA_TNT_KEY)
    );

    private ModEntities() {

    }

    public static void init() {
        // ensure class load
    }
}