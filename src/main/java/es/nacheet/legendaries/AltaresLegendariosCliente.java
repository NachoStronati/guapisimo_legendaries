package es.nacheet.legendaries;

import es.nacheet.legendaries.altar.AltarRenderer;
import es.nacheet.legendaries.registro.ModObjetos;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.LodestoneTracker;

/**
 * Parte visual del mod (solo se ejecuta en el juego del jugador, no en servidores).
 */
public class AltaresLegendariosCliente implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// Los altares se dibujan con nuestro "pintor" (para la gema flotante)
		BlockEntityRenderers.register(ModObjetos.ALTAR_BLOCK_ENTITY, AltarRenderer::new);

		// La aguja de la Brújula Ancestral apunta a la posición guardada en la brújula
		ItemProperties.register(ModObjetos.BRUJULA, ResourceLocation.withDefaultNamespace("angle"),
			new CompassItemPropertyFunction((nivel, objeto, entidad) -> {
				LodestoneTracker rastreador = objeto.get(DataComponents.LODESTONE_TRACKER);
				return rastreador != null ? rastreador.target().orElse(null) : null;
			}));

		// El farol se dibuja en X con transparencias recortadas (sin cuadrado negro)
		BlockRenderLayerMap.INSTANCE.putBlock(ModObjetos.FAROL_FESTIVAL, RenderType.cutout());
	}
}
