package es.nacheet.legendaries.objetos;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import es.nacheet.legendaries.GuapisimoConfig;
import es.nacheet.legendaries.util.Momento;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

/**
 * Pluma Vanidosa: invoca a Fezandipiti al amanecer, en un bioma de flores,
 * si tu equipo está completo y todos te tienen mucha amistad.
 */
public class PlumaVanidosaItem extends ObjetoInvocador {

	// La etiqueta de biomas "de flores" de Cobblemon
	private static final TagKey<Biome> BIOMAS_FLORES =
		TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("cobblemon", "is_floral"));

	public PlumaVanidosaItem(Properties propiedades) {
		super(propiedades, "fezandipiti", 70, "pluma", ChatFormatting.LIGHT_PURPLE);
	}

	private static int amistadNecesaria() {
		return GuapisimoConfig.get().fezandipiti.amistadNecesaria;
	}

	private static int tamanoEquipo() {
		return GuapisimoConfig.get().fezandipiti.tamanoEquipo;
	}

	@Override
	protected Component comprobar(ServerLevel mundo, ServerPlayer jugador, ItemStack pluma) {
		// 1. ¿Es el amanecer?
		if (!Momento.esAmanecer(mundo)) {
			return Textos.mensaje("pluma_hora");
		}
		// 2. ¿Estamos en un bioma de flores?
		if (!mundo.getBiome(jugador.blockPosition()).is(BIOMAS_FLORES)) {
			return Textos.mensaje("pluma_lugar");
		}
		// 3. ¿El equipo está completo y con suficiente amistad?
		PlayerPartyStore equipo = Cobblemon.INSTANCE.getStorage().getParty(jugador);
		int total = 0;
		int sinAmistad = 0;
		for (Pokemon pokemon : equipo) {
			total++;
			if (pokemon.getFriendship() < amistadNecesaria()) {
				sinAmistad++;
			}
		}
		if (total < tamanoEquipo()) {
			return Textos.mensaje("pluma_equipo", total, tamanoEquipo());
		}
		if (sinAmistad > 0) {
			return Textos.mensaje("pluma_amistad", sinAmistad);
		}
		return null;   // todo correcto
	}

	@Override
	protected void efectos(ServerLevel mundo, BlockPos sitio) {
		// Pétalos, corazones y un sonido de cristal
		mundo.sendParticles(ParticleTypes.CHERRY_LEAVES, sitio.getX() + 0.5, sitio.getY() + 1.5, sitio.getZ() + 0.5,
			60, 1.5, 1.0, 1.5, 0.05);
		mundo.sendParticles(ParticleTypes.HEART, sitio.getX() + 0.5, sitio.getY() + 1.5, sitio.getZ() + 0.5,
			12, 1.0, 0.8, 1.0, 0.1);
		mundo.playSound(null, sitio, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 1.2F);
	}
}
