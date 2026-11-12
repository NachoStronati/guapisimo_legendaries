package es.nacheet.legendaries.util;

import es.nacheet.legendaries.GuapisimoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Todo lo que tienen en común las invocaciones de legendarios.
 * Lo usan los altares, los objetos invocadores, el Cofre Melocotón y el Farol del Festival.
 */
public final class Invocacion {

	// IVs perfectos con los que salen TODOS los legendarios del mod
	public static final int IVS_PERFECTOS = 3;
	// Distancia (en bloques) a la que se avisa a los jugadores cuando aparece un legendario desde un bloque
	public static final int RADIO_AVISO = 48;

	private Invocacion() {}

	/**
	 * Hace aparecer un legendario con el comando de Cobblemon.
	 * @return true si ha salido shiny
	 */
	public static boolean invocar(ServerLevel mundo, BlockPos sitio, String especie, int nivel) {
		boolean esShiny = mundo.random.nextFloat() < GuapisimoConfig.get().probabilidadShiny;

		String comando = "spawnpokemonat " + sitio.getX() + " " + sitio.getY() + " " + sitio.getZ()
			+ " " + especie + " level=" + nivel + " min_perfect_ivs=" + IVS_PERFECTOS
			+ (esShiny ? " shiny=true" : "");
		CommandSourceStack fuente = mundo.getServer().createCommandSourceStack()
			.withLevel(mundo)
			.withPosition(Vec3.atCenterOf(sitio))
			.withPermission(4)
			.withSuppressedOutput();
		mundo.getServer().getCommands().performPrefixedCommand(fuente, comando);

		return esShiny;
	}

	/** El mensaje dorado de "¡ha salido shiny!". */
	public static void avisarShiny(Player jugador) {
		jugador.sendSystemMessage(Textos.mensaje("shiny").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	/** Manda un mensaje a todos los jugadores cerca de una posición (y el aviso de shiny si toca). */
	public static void avisarCercanos(ServerLevel mundo, BlockPos pos, Component mensaje, boolean esShiny) {
		Vec3 centro = Vec3.atCenterOf(pos);
		for (ServerPlayer jugador : mundo.getPlayers(p -> p.distanceToSqr(centro) < RADIO_AVISO * RADIO_AVISO)) {
			jugador.sendSystemMessage(mensaje);
			if (esShiny) {
				avisarShiny(jugador);
			}
		}
	}

	/** Gasta uno del objeto, salvo en modo creativo. */
	public static void gastar(Player jugador, ItemStack objeto) {
		if (!jugador.getAbilities().instabuild) {
			objeto.shrink(1);
		}
	}
}
