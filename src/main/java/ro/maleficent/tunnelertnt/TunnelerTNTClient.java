package ro.maleficent.tunnelertnt;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.TntRenderer;
import ro.maleficent.tunnelertnt.registry.ModEntities;

public class TunnelerTNTClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register Heavy TNT entity renderer (same as vanilla TNT)
        EntityRenderers.register(
                ModEntities.HEAVY_TNT,
                TntRenderer::new
        );
        EntityRenderers.register(
                ModEntities.TUNNELER_TNT,
                TntRenderer::new
        );
    }
}
