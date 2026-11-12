package es.nacheet.legendaries.util;

import es.nacheet.legendaries.AltaresLegendarios;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Ayudas para crear textos traducibles sin escribir siempre "mensaje.guapisimo." delante.
 *   Textos.mensaje("pluma_hora")  ->  "mensaje.guapisimo.pluma_hora"
 *   Textos.tooltip("pluma_pista1") ->  "tooltip.guapisimo.pluma_pista1"
 */
public final class Textos {

	private Textos() {}

	public static MutableComponent mensaje(String clave, Object... argumentos) {
		return Component.translatable("mensaje." + AltaresLegendarios.MOD_ID + "." + clave, argumentos);
	}

	public static MutableComponent tooltip(String clave, Object... argumentos) {
		return Component.translatable("tooltip." + AltaresLegendarios.MOD_ID + "." + clave, argumentos);
	}
}
