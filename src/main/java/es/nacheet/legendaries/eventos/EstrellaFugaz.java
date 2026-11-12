package es.nacheet.legendaries.eventos;

import es.nacheet.legendaries.registro.ModObjetos;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Hace caer una estrella fugaz cerca de un jugador.
 */
public class EstrellaFugaz {

	public static void caer(ServerLevel mundo, ServerPlayer jugador) {
		// 1. Elegimos un punto al azar, entre 16 y 32 bloques del jugador
		double angulo = mundo.random.nextDouble() * Math.PI * 2;
		int distancia = 16 + mundo.random.nextInt(17);
		int x = jugador.getBlockX() + (int) (Math.cos(angulo) * distancia);
		int z = jugador.getBlockZ() + (int) (Math.sin(angulo) * distancia);
		// Buscamos el suelo en ese punto (sin contar las hojas)
		BlockPos suelo = mundo.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));

		double finX = suelo.getX() + 0.5;
		double finY = suelo.getY() + 0.5;
		double finZ = suelo.getZ() + 0.5;

		// 2. La estela: una línea de partículas desde el cielo hasta el suelo, en diagonal
		double inicioX = finX - 25;
		double inicioY = finY + 60;
		double inicioZ = finZ - 10;
		for (int i = 0; i <= 60; i++) {
			double t = i / 60.0;   // de 0 (cielo) a 1 (suelo)
			double px = inicioX + (finX - inicioX) * t;
			double py = inicioY + (finY - inicioY) * t;
			double pz = inicioZ + (finZ - inicioZ) * t;
			// "true" = se ve aunque esté lejos
			mundo.sendParticles(jugador, ParticleTypes.END_ROD, true, px, py, pz, 2, 0.05, 0.05, 0.05, 0.0);
		}

		// 3. El impacto: chispas y sonido
		mundo.sendParticles(jugador, ParticleTypes.FIREWORK, true, finX, finY, finZ, 40, 0.5, 0.5, 0.5, 0.15);
		mundo.playSound(null, suelo, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.AMBIENT, 3.0F, 0.8F);
		mundo.playSound(null, suelo, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 2.0F, 1.5F);

		// 4. El fragmento queda en el suelo: brilla y no desaparece con el tiempo
		ItemEntity fragmento = new ItemEntity(mundo, finX, finY + 0.5, finZ,
				new ItemStack(ModObjetos.FRAGMENTO_ESTRELLA));
		fragmento.setGlowingTag(true);       // se ve a través de las paredes
		fragmento.setUnlimitedLifetime();    // no desaparece a los 5 minutos
		mundo.addFreshEntity(fragmento);

		// 5. Avisar al jugador
		jugador.sendSystemMessage(Textos.mensaje("estrella_cae")
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
	}
}
