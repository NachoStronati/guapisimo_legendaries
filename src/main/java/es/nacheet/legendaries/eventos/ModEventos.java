package es.nacheet.legendaries.eventos;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import es.nacheet.legendaries.objetos.CadenaToxicaItem;
import es.nacheet.legendaries.objetos.SelloItem;
import es.nacheet.legendaries.registro.ModObjetos;
import es.nacheet.legendaries.util.Momento;
import es.nacheet.legendaries.util.Textos;
import kotlin.Unit;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Todo lo que el mod "escucha" del juego:
 *   - Capturas de Pokémon      -> sellos y regalos (eslabones, Flor del Recuerdo)
 *   - Combates ganados         -> Cadena Tóxica
 *   - Bloques rotos            -> Fragmentos de Diamante Rosa
 *   - El paso del tiempo       -> estrellas fugaces
 */
public final class ModEventos {

	private ModEventos() {}

	// --- Cadena Tóxica: tipos de rival que cuentan ---
	private static final Set<String> TIPOS_CADENA = Set.of("fighting", "poison");

	// --- Diancie: probabilidad de fragmento al picar, y altura máxima ---
	private static final float PROB_ROSA_DIAMANTE = 0.20F;   // mena de diamante
	private static final float PROB_ROSA_AMATISTA = 0.15F;   // racimo de amatista
	private static final int ALTURA_MAXIMA_ROSA = 0;         // solo por debajo de esta altura

	// --- Jirachi: estrellas fugaces ---
	private static final long DIA_INICIO_ESTRELLAS = 10;       // a partir de este día del juego
	private static final float PROBABILIDAD_ESTRELLA = 0.01F;  // en cada revisión
	private static final int CADA_CUANTO_ESTRELLAS = 200;      // revisión cada 200 ticks (10 segundos)
	// Qué noche cayó la última estrella de cada jugador (para que caiga 1 como mucho por noche)
	private static final Map<UUID, Long> ULTIMA_ESTRELLA = new HashMap<>();

	public static void registrar() {
		CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, evento -> {
			alCapturar(evento.getPlayer(), evento.getPokemon());
			return Unit.INSTANCE;
		});
		CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, evento -> {
			alGanarCombate(evento.getWasWildCapture(), evento.getLosers(), evento.getWinners());
			return Unit.INSTANCE;
		});
		PlayerBlockBreakEvents.AFTER.register(ModEventos::alRomperBloque);
		ServerTickEvents.END_SERVER_TICK.register(ModEventos::cadaTick);
	}

	// ===================== Capturas =====================

	private static void alCapturar(ServerPlayer jugador, Pokemon pokemon) {
		// 1. Los sellos suman progreso con los tipos del Pokémon capturado
		List<String> tipos = new ArrayList<>();
		for (ElementalType tipo : pokemon.getTypes()) {
			tipos.add(tipo.getName());
		}
		SelloItem.sumarCaptura(jugador, tipos);

		// 2. Algunos Pokémon dan un regalo al capturarlos (eslabones, Flor del Recuerdo)
		String especie = pokemon.getSpecies().getResourceIdentifier().getPath();
		ModObjetos.Regalo regalo = ModObjetos.REGALOS_CAPTURA.get(especie);
		if (regalo != null) {
			ItemStack premio = new ItemStack(regalo.objeto());
			if (!jugador.getInventory().add(premio)) {
				jugador.drop(premio, false);   // inventario lleno: cae al suelo
			}
			jugador.sendSystemMessage(Textos.mensaje(regalo.mensaje(), pokemon.getSpecies().getTranslatedName())
				.withStyle(ChatFormatting.DARK_PURPLE));
		}
	}

	// ===================== Combates =====================

	private static void alGanarCombate(boolean fueCaptura, List<? extends BattleActor> perdedores,
									   List<? extends BattleActor> ganadores) {
		// Si el combate acabó capturando al Pokémon, no cuenta como victoria
		if (fueCaptura || !hayRivalLuchaOVeneno(perdedores)) return;

		for (BattleActor ganador : ganadores) {
			if (ganador instanceof PlayerBattleActor jugadorActor && jugadorActor.getEntity() != null) {
				CadenaToxicaItem.sumarVictoria(jugadorActor.getEntity());
			}
		}
	}

	/** ¿Algún perdedor es un Pokémon salvaje de tipo Lucha o Veneno? */
	private static boolean hayRivalLuchaOVeneno(List<? extends BattleActor> perdedores) {
		for (BattleActor perdedor : perdedores) {
			if (perdedor.getType() != ActorType.WILD) continue;
			for (BattlePokemon bp : perdedor.getPokemonList()) {
				for (ElementalType tipo : bp.getOriginalPokemon().getTypes()) {
					if (TIPOS_CADENA.contains(tipo.getName().toLowerCase())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// ===================== Minería (Diancie) =====================

	private static void alRomperBloque(Level mundo, Player jugador, BlockPos pos, BlockState estado, BlockEntity entidad) {
		// Solo en el servidor, en supervivencia y en lo profundo
		if (mundo.isClientSide() || jugador.isCreative() || pos.getY() >= ALTURA_MAXIMA_ROSA) return;

		float probabilidad = 0;
		if (estado.is(Blocks.DIAMOND_ORE) || estado.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
			probabilidad = PROB_ROSA_DIAMANTE;
		} else if (estado.is(Blocks.AMETHYST_CLUSTER)) {
			probabilidad = PROB_ROSA_AMATISTA;
		}

		if (probabilidad > 0 && mundo.random.nextFloat() < probabilidad) {
			// Suelta el fragmento en el sitio del bloque, como cualquier otro objeto
			Block.popResource(mundo, pos, new ItemStack(ModObjetos.FRAGMENTO_DIAMANTE_ROSA));
			mundo.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.5F);
		}
	}

	// ===================== Estrellas fugaces (Jirachi) =====================

	/** Se ejecuta 20 veces por segundo, siempre. */
	private static void cadaTick(MinecraftServer servidor) {
		// Solo miramos cada 10 segundos, para no gastar rendimiento
		if (servidor.getTickCount() % CADA_CUANTO_ESTRELLAS != 0) return;

		ServerLevel mundo = servidor.overworld();
		long dia = Momento.dia(mundo);
		if (dia < DIA_INICIO_ESTRELLAS || !Momento.esDeNoche(mundo)) return;

		for (ServerPlayer jugador : mundo.players()) {
			// ¿Ya le cayó una estrella esta noche?
			if (ULTIMA_ESTRELLA.getOrDefault(jugador.getUUID(), -1L) == dia) continue;
			// Sorteo
			if (mundo.random.nextFloat() >= PROBABILIDAD_ESTRELLA) continue;

			ULTIMA_ESTRELLA.put(jugador.getUUID(), dia);
			EstrellaFugaz.caer(mundo, jugador);
		}
	}
}
