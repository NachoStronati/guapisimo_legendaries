package es.nacheet.legendaries.objetos;

import es.nacheet.legendaries.GuapisimoConfig;
import es.nacheet.legendaries.registro.ModObjetos;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Cadena Tóxica: se carga ganando combates contra Pokémon salvajes de tipo Lucha o Veneno.
 * Cuando está llena, al usarla por encima de cierta altura aparece Okidogi.
 */
public class CadenaToxicaItem extends ObjetoInvocador {

	public CadenaToxicaItem(Properties propiedades) {
		super(propiedades, "okidogi", 70, "cadena", ChatFormatting.DARK_PURPLE);
	}

	private static int objetivo() {
		return GuapisimoConfig.get().okidogi.victoriasNecesarias;
	}

	private static int alturaMinima() {
		return GuapisimoConfig.get().okidogi.alturaMinima;
	}

	/** Cuántas victorias lleva la cadena (0 si es nueva). */
	public static int progreso(ItemStack cadena) {
		return cadena.getOrDefault(ModObjetos.PROGRESO, 0);
	}

	/**
	 * Se llama al ganar un combate contra un Pokémon salvaje Lucha o Veneno.
	 * Suma 1 a la primera cadena del inventario que no esté llena.
	 */
	public static void sumarVictoria(ServerPlayer jugador) {
		Inventory inventario = jugador.getInventory();
		for (int i = 0; i < inventario.getContainerSize(); i++) {
			ItemStack objeto = inventario.getItem(i);
			if (objeto.getItem() instanceof CadenaToxicaItem && progreso(objeto) < objetivo()) {
				int nuevo = progreso(objeto) + 1;
				objeto.set(ModObjetos.PROGRESO, nuevo);

				if (nuevo >= objetivo()) {
					jugador.sendSystemMessage(Textos.mensaje("cadena_completa")
						.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
					jugador.level().playSound(null, jugador.blockPosition(),
						SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.6F);
				}
				return; // solo una cadena por victoria
			}
		}
	}

	@Override
	protected Component comprobar(ServerLevel mundo, ServerPlayer jugador, ItemStack cadena) {
		if (progreso(cadena) < objetivo()) {
			return Textos.mensaje("cadena_incompleta", progreso(cadena), objetivo());
		}
		if (jugador.blockPosition().getY() < alturaMinima()) {
			return Textos.mensaje("cadena_lugar");
		}
		return null;   // todo correcto
	}

	@Override
	protected void efectos(ServerLevel mundo, BlockPos sitio) {
		// Humo tóxico y un sonido de invocación
		mundo.sendParticles(ParticleTypes.WITCH, sitio.getX() + 0.5, sitio.getY() + 1, sitio.getZ() + 0.5,
			80, 1.0, 1.0, 1.0, 0.1);
		mundo.playSound(null, sitio, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.7F);
	}

	// --- Barra de progreso (como los sellos) ---

	@Override
	public boolean isBarVisible(ItemStack cadena) {
		return progreso(cadena) < objetivo();
	}

	@Override
	public int getBarWidth(ItemStack cadena) {
		return Math.round(13.0F * progreso(cadena) / objetivo());
	}

	@Override
	public int getBarColor(ItemStack cadena) {
		return 0x9B30FF; // morado veneno
	}

	// Brilla cuando está llena
	@Override
	public boolean isFoil(ItemStack cadena) {
		return progreso(cadena) >= objetivo();
	}

	// La cadena tiene su propio texto (con el progreso), en vez de las dos pistas de siempre
	@Override
	public void appendHoverText(ItemStack cadena, TooltipContext contexto, List<Component> texto, TooltipFlag flag) {
		texto.add(Textos.tooltip("cadena_progreso", progreso(cadena), objetivo())
			.withStyle(ChatFormatting.DARK_PURPLE));
		texto.add(Textos.tooltip(progreso(cadena) >= objetivo() ? "cadena_lista" : "cadena_pista")
			.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
}
