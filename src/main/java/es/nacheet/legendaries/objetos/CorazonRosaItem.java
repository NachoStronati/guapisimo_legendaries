package es.nacheet.legendaries.objetos;

import es.nacheet.legendaries.util.Textos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Corazón Rosa: usado sobre una amatista brotante (dentro de una geoda) invoca a Diancie.
 *
 * Es distinto a los demás: se usa SOBRE un bloque (useOn), no al aire.
 * Aun así, aprovecha la parte común de ObjetoInvocador (completarInvocacion).
 */
public class CorazonRosaItem extends ObjetoInvocador {

	public CorazonRosaItem(Properties propiedades) {
		super(propiedades, "diancie", 70, "corazon", ChatFormatting.LIGHT_PURPLE);
	}

	// useOn = clic derecho SOBRE un bloque
	@Override
	public InteractionResult useOn(UseOnContext contexto) {
		Level nivelMundo = contexto.getLevel();
		BlockPos pos = contexto.getClickedPos();      // el bloque al que hemos dado
		Player jugador = contexto.getPlayer();
		if (jugador == null) {
			return InteractionResult.PASS;
		}

		// 1. ¿Es una amatista brotante? (va antes del isClientSide para que tu pantalla también lo sepa)
		if (!nivelMundo.getBlockState(pos).is(Blocks.BUDDING_AMETHYST)) {
			if (!nivelMundo.isClientSide()) {
				jugador.displayClientMessage(Textos.mensaje("corazon_lugar").withStyle(ChatFormatting.GRAY), true);
			}
			return InteractionResult.FAIL;
		}
		if (!(nivelMundo instanceof ServerLevel mundo) || !(jugador instanceof ServerPlayer jugadorServidor)) {
			return InteractionResult.SUCCESS;
		}

		// 2. La amatista le da su energía a Diancie y se vuelve un bloque de amatista normal
		mundo.setBlock(pos, Blocks.AMETHYST_BLOCK.defaultBlockState(), Block.UPDATE_ALL);

		// 3. Diancie aparece en el hueco de la cara a la que hemos dado
		completarInvocacion(mundo, jugadorServidor, contexto.getItemInHand(), pos.relative(contexto.getClickedFace()));
		return InteractionResult.CONSUME;
	}

	// Si lo usas al aire, te recuerda dónde hay que usarlo
	@Override
	protected Component comprobar(ServerLevel mundo, ServerPlayer jugador, ItemStack corazon) {
		return Textos.mensaje("corazon_lugar");
	}

	@Override
	protected void efectos(ServerLevel mundo, BlockPos sitio) {
		// Destellos y sonido de cristal
		mundo.sendParticles(ParticleTypes.END_ROD, sitio.getX() + 0.5, sitio.getY() + 0.5, sitio.getZ() + 0.5,
			60, 1.0, 1.0, 1.0, 0.05);
		mundo.sendParticles(ParticleTypes.CHERRY_LEAVES, sitio.getX() + 0.5, sitio.getY() + 1, sitio.getZ() + 0.5,
			30, 1.0, 1.0, 1.0, 0.05);
		mundo.playSound(null, sitio, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 1.0F);
		mundo.playSound(null, sitio, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.5F);
	}
}
