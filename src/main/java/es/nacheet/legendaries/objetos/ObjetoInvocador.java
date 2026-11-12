package es.nacheet.legendaries.objetos;

import es.nacheet.legendaries.GuapisimoConfig;
import es.nacheet.legendaries.util.Invocacion;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Clase "madre" de los objetos que invocan un legendario (cadena, pluma, ídolo, corazón, estrella).
 *
 * Todos hacen lo mismo:
 *   1. Comprobar sus condiciones (cada uno las suyas)  -> método comprobar()
 *   2. Invocar al legendario
 *   3. Partículas y sonidos (cada uno los suyos)       -> método efectos()
 *   4. Mensaje, gastar el objeto y poner la espera
 *
 * Así cada objeto solo escribe lo que lo hace especial.
 */
public abstract class ObjetoInvocador extends Item {

	private final String especie;          // especie en Cobblemon, por ejemplo "okidogi"
	private final int nivel;
	private final String clave;            // para los tooltips: "pluma" -> tooltip.guapisimo.pluma_pista1
	private final ChatFormatting color;    // color de los textos de este legendario

	protected ObjetoInvocador(Properties propiedades, String especie, int nivel, String clave, ChatFormatting color) {
		super(propiedades);
		this.especie = especie;
		this.nivel = nivel;
		this.clave = clave;
		this.color = color;
	}

	/**
	 * Comprueba si ahora se puede invocar.
	 * @return null si todo está bien, o el mensaje que explica qué falta
	 */
	@Nullable
	protected abstract Component comprobar(ServerLevel mundo, ServerPlayer jugador, ItemStack objeto);

	/** Las partículas y sonidos propios de cada legendario. */
	protected abstract void efectos(ServerLevel mundo, BlockPos sitio);

	/** Dónde aparece el legendario. Por defecto, 3 bloques delante del jugador. */
	protected BlockPos sitio(ServerPlayer jugador) {
		return jugador.blockPosition().relative(jugador.getDirection(), 3);
	}

	// --- Clic derecho al aire ---
	@Override
	public InteractionResultHolder<ItemStack> use(Level nivelMundo, Player jugador, InteractionHand mano) {
		ItemStack objeto = jugador.getItemInHand(mano);
		// En el cliente (tu pantalla) no se decide nada: lo hace el servidor
		if (!(nivelMundo instanceof ServerLevel mundo) || !(jugador instanceof ServerPlayer jugadorServidor)) {
			return InteractionResultHolder.success(objeto);
		}

		Component fallo = comprobar(mundo, jugadorServidor, objeto);
		if (fallo != null) {
			jugador.displayClientMessage(fallo.copy().withStyle(ChatFormatting.GRAY), true);
			return InteractionResultHolder.fail(objeto);
		}

		completarInvocacion(mundo, jugadorServidor, objeto, sitio(jugadorServidor));
		return InteractionResultHolder.consume(objeto);
	}

	/** Invoca, hace los efectos, avisa, gasta el objeto y pone la espera. */
	protected void completarInvocacion(ServerLevel mundo, ServerPlayer jugador, ItemStack objeto, BlockPos sitio) {
		boolean esShiny = Invocacion.invocar(mundo, sitio, especie, nivel);
		efectos(mundo, sitio);

		// Las claves de los mensajes siguen la regla "mensaje.guapisimo.<especie>_aparece"
		jugador.sendSystemMessage(Textos.mensaje(especie + "_aparece").withStyle(color));
		if (esShiny) {
			Invocacion.avisarShiny(jugador);
		}

		Invocacion.gastar(jugador, objeto);
		jugador.getCooldowns().addCooldown(this, GuapisimoConfig.get().esperaInvocacionTicks);
	}

	// --- Texto al pasar el ratón: dos pistas ---
	@Override
	public void appendHoverText(ItemStack objeto, TooltipContext contexto, List<Component> texto, TooltipFlag flag) {
		texto.add(Textos.tooltip(clave + "_pista1").withStyle(color));
		texto.add(Textos.tooltip(clave + "_pista2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
}
