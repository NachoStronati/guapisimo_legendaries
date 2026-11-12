package es.nacheet.legendaries.altar;

import es.nacheet.legendaries.registro.ModObjetos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El "pintor" del altar: dibuja la gema encima del pedestal en cada fotograma.
 * - Altar activo: la gema flota subiendo y bajando, y gira.
 * - Altar apagado: la gema descansa quieta sobre la tapa.
 */
public class AltarRenderer implements BlockEntityRenderer<AltarBlockEntity> {

	private final ItemRenderer pintorObjetos;

	public AltarRenderer(BlockEntityRendererProvider.Context contexto) {
		this.pintorObjetos = contexto.getItemRenderer();
	}

	@Override
	public void render(AltarBlockEntity altarEntidad, float fraccionTick, PoseStack pose,
					   MultiBufferSource buffers, int luz, int superposicion) {
		BlockState estado = altarEntidad.getBlockState();
		Level mundo = altarEntidad.getLevel();
		if (mundo == null || !(estado.getBlock() instanceof AltarBlock altar) || !altar.tieneGema()) {
			return;
		}

		// Tiempo suave (los ticks van a 20 por segundo; fraccionTick rellena los huecos entre ticks)
		float tiempo = mundo.getGameTime() + fraccionTick;

		// Altura de la parte de abajo de la gema, en bloques (la tapa del altar acaba en 12/16 = 0.75)
		float alturaGema;
		float giro;
		if (estado.getValue(AltarBlock.ACTIVO)) {
			alturaGema = 0.90F + (float) Math.sin(tiempo / 10.0) * 0.06F; // sube y baja
			giro = tiempo * 2.0F;                                          // 2 grados por tick
		} else {
			alturaGema = 0.75F; // apoyada en la tapa
			giro = 0.0F;
		}

		pose.pushPose();                                 // guardar la posición
		pose.translate(0.5, alturaGema + 0.5, 0.5);      // al centro del bloque, a la altura de la gema
		pose.mulPose(Axis.YP.rotationDegrees(giro));     // girar sobre sí misma

		int luzEncima = LevelRenderer.getLightColor(mundo, altarEntidad.getBlockPos().above());
		pintorObjetos.renderStatic(new ItemStack(ModObjetos.GEMA), ItemDisplayContext.FIXED,
			luzEncima, superposicion, pose, buffers, mundo, 0);

		pose.popPose();                                  // volver a la posición guardada
	}
}
