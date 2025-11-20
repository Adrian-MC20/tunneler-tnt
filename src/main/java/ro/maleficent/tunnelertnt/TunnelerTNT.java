package ro.maleficent.tunnelertnt;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.maleficent.tunnelertnt.registry.ModBlocks;
import ro.maleficent.tunnelertnt.registry.ModEntities;

public class TunnelerTNT implements ModInitializer {
	public static final String MOD_ID = "tunnelertnt";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        LOGGER.info("Initializing Tunneler TNT");

        ModBlocks.init();
        ModEntities.init();
	}
}