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
import net.minecraft.world.item.ItemStack;

/**
 * Estrella de los Deseos: de noche y a cielo abierto, invoca a Jirachi.
 */
public class EstrellaDeseosItem extends ObjetoInvocador {

	public EstrellaDeseosItem(Properties propiedades) {
		super(propiedades, "jirachi", 70, "deseo", ChatFormatting.YELLOW);
	}

	@Override
	protected Component comprobar(ServerLevel mundo, ServerPlayer jugador, ItemStack estrella) {
		if (!Momento.esDeNoche(mundo)) {
			return Textos.mensaje("deseo_hora");
		}
		// No puede haber nada encima del jugador
		if (!mundo.canSeeSky(jugador.blockPosition().above())) {
			return Textos.mensaje("deseo_cielo");
		}
		return null;   // todo correcto
	}

	// Jirachi aparece un poco en el aire
	@Override
	protected BlockPos sitio(ServerPlayer jugador) {
		return super.sitio(jugador).above();
	}

	@Override
	protected void efectos(ServerLevel mundo, BlockPos sitio) {
		// Lluvia de estrellas y campanitas
		mundo.sendParticles(ParticleTypes.END_ROD, sitio.getX() + 0.5, sitio.getY() + 3, sitio.getZ() + 0.5,
			80, 2.0, 2.0, 2.0, 0.05);
		mundo.sendParticles(ParticleTypes.FIREWORK, sitio.getX() + 0.5, sitio.getY() + 1, sitio.getZ() + 0.5,
			40, 1.0, 1.0, 1.0, 0.1);
		mundo.playSound(null, sitio, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 1.5F);
		mundo.playSound(null, sitio, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
	}
}
