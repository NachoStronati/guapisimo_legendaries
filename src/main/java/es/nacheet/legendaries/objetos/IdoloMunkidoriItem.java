package es.nacheet.legendaries.objetos;

import es.nacheet.legendaries.util.Momento;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;

/**
 * Ídolo de Munkidori: invoca a Munkidori de noche en una jungla.
 */
public class IdoloMunkidoriItem extends ObjetoInvocador {

	public IdoloMunkidoriItem(Properties propiedades) {
		super(propiedades, "munkidori", 70, "idolo", ChatFormatting.DARK_PURPLE);
	}

	@Override
	protected Component comprobar(ServerLevel mundo, ServerPlayer jugador, ItemStack idolo) {
		if (!Momento.esDeNoche(mundo)) {
			return Textos.mensaje("idolo_hora");
		}
		if (!mundo.getBiome(jugador.blockPosition()).is(BiomeTags.IS_JUNGLE)) {
			return Textos.mensaje("idolo_lugar");
		}
		return null;   // todo correcto
	}

	@Override
	protected void efectos(ServerLevel mundo, BlockPos sitio) {
		// Humo psíquico y un hechizo
		mundo.sendParticles(ParticleTypes.PORTAL, sitio.getX() + 0.5, sitio.getY() + 1, sitio.getZ() + 0.5,
			120, 1.0, 1.0, 1.0, 0.5);
		mundo.sendParticles(ParticleTypes.WITCH, sitio.getX() + 0.5, sitio.getY() + 1, sitio.getZ() + 0.5,
			40, 1.0, 1.0, 1.0, 0.1);
		mundo.playSound(null, sitio, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
	}
}
