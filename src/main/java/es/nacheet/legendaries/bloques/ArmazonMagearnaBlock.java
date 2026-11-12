package es.nacheet.legendaries.bloques;

import es.nacheet.legendaries.registro.ModObjetos;
import es.nacheet.legendaries.util.Invocacion;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Armazón de Magearna: se le meten 3 piezas antiguas (en cualquier orden) y el Corazón Alma.
 * Con el corazón puesto, una señal de redstone pone la máquina en marcha:
 * a los pocos segundos aparece Magearna y el armazón desaparece.
 */
public class ArmazonMagearnaBlock extends Block {

	// Un estado por cada pieza: ¿está puesta?
	public static final BooleanProperty ENGRANAJE = BooleanProperty.create("engranaje");
	public static final BooleanProperty PLACA = BooleanProperty.create("placa");
	public static final BooleanProperty MUELLE = BooleanProperty.create("muelle");
	public static final BooleanProperty CORAZON = BooleanProperty.create("corazon");

	// Lo que tarda la máquina en arrancar (60 ticks = 3 segundos)
	private static final int TIEMPO_ARRANQUE = 60;

	public ArmazonMagearnaBlock(Properties propiedades) {
		super(propiedades);
		registerDefaultState(stateDefinition.any()
			.setValue(ENGRANAJE, false).setValue(PLACA, false).setValue(MUELLE, false).setValue(CORAZON, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
		constructor.add(ENGRANAJE, PLACA, MUELLE, CORAZON);
	}

	// ===================== Ayudas =====================

	/** ¿Qué estado corresponde a este objeto? (null si no es una pieza) */
	private static BooleanProperty piezaDe(ItemStack objeto) {
		if (objeto.is(ModObjetos.ENGRANAJE_ANTIGUO)) return ENGRANAJE;
		if (objeto.is(ModObjetos.PLACA_ORNAMENTADA)) return PLACA;
		if (objeto.is(ModObjetos.MUELLE_CUERDA)) return MUELLE;
		return null;
	}

	/** Cuántas de las 3 piezas tiene puestas. */
	public static int piezas(BlockState estado) {
		int total = 0;
		if (estado.getValue(ENGRANAJE)) total++;
		if (estado.getValue(PLACA)) total++;
		if (estado.getValue(MUELLE)) total++;
		return total;
	}

	/** Lista de las piezas que faltan, separadas por comas. Por ejemplo: "Placa Ornamentada, Muelle de Cuerda" */
	private static Component faltan(BlockState estado) {
		MutableComponent lista = Component.empty();
		boolean primera = true;
		Item[] objetos = {ModObjetos.ENGRANAJE_ANTIGUO, ModObjetos.PLACA_ORNAMENTADA, ModObjetos.MUELLE_CUERDA};
		BooleanProperty[] propiedades = {ENGRANAJE, PLACA, MUELLE};
		for (int i = 0; i < objetos.length; i++) {
			if (!estado.getValue(propiedades[i])) {
				if (!primera) lista.append(", ");
				lista.append(objetos[i].getDescription());
				primera = false;
			}
		}
		return lista;
	}

	// ===================== Meter piezas (clic derecho con un objeto) =====================

	@Override
	protected ItemInteractionResult useItemOn(ItemStack objeto, BlockState estado, Level mundo, BlockPos pos,
											  Player jugador, InteractionHand mano, BlockHitResult golpe) {
		BooleanProperty pieza = piezaDe(objeto);
		boolean esCorazon = objeto.is(ModObjetos.CORAZON_ALMA);
		if (pieza == null && !esCorazon) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;   // no es nada nuestro
		}
		if (mundo.isClientSide()) {
			return ItemInteractionResult.SUCCESS;
		}

		// A) Una de las 3 piezas
		if (pieza != null) {
			if (estado.getValue(pieza)) {
				jugador.displayClientMessage(Textos.mensaje("armazon_ya_tiene").withStyle(ChatFormatting.GRAY), true);
				return ItemInteractionResult.FAIL;
			}
			BlockState nuevo = estado.setValue(pieza, true);
			mundo.setBlock(pos, nuevo, Block.UPDATE_ALL);
			Invocacion.gastar(jugador, objeto);
			mundo.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.6F);
			jugador.displayClientMessage(Textos.mensaje("armazon_pieza", piezas(nuevo), 3)
				.withStyle(ChatFormatting.GOLD), true);
			return ItemInteractionResult.CONSUME;
		}

		// B) El Corazón Alma: solo cuando ya están las 3 piezas
		if (estado.getValue(CORAZON)) {
			jugador.displayClientMessage(Textos.mensaje("armazon_ya_tiene").withStyle(ChatFormatting.GRAY), true);
			return ItemInteractionResult.FAIL;
		}
		if (piezas(estado) < 3) {
			jugador.displayClientMessage(Textos.mensaje("armazon_faltan", faltan(estado))
				.withStyle(ChatFormatting.GRAY), true);
			return ItemInteractionResult.FAIL;
		}
		BlockState completo = estado.setValue(CORAZON, true);
		mundo.setBlock(pos, completo, Block.UPDATE_ALL);
		Invocacion.gastar(jugador, objeto);
		mundo.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.5F);
		jugador.sendSystemMessage(Textos.mensaje("armazon_completo").withStyle(ChatFormatting.LIGHT_PURPLE));

		// Si ya tenía redstone conectada (una palanca encendida, por ejemplo), arranca directamente
		intentarArrancar(completo, mundo, pos);
		return ItemInteractionResult.CONSUME;
	}

	// ===================== Clic derecho con la mano vacía: ¿qué le falta? =====================

	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (mundo.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Component mensaje;
		if (piezas(estado) < 3) {
			mensaje = Textos.mensaje("armazon_faltan", faltan(estado));
		} else if (!estado.getValue(CORAZON)) {
			mensaje = Textos.mensaje("armazon_falta_corazon");
		} else {
			mensaje = Textos.mensaje("armazon_listo");
		}
		jugador.displayClientMessage(mensaje.copy().withStyle(ChatFormatting.GRAY), true);
		return InteractionResult.CONSUME;
	}

	// ===================== Redstone =====================

	/** Se llama cuando cambia un bloque de al lado (por ejemplo, al mover una palanca). */
	@Override
	protected void neighborChanged(BlockState estado, Level mundo, BlockPos pos, Block bloqueVecino,
								   BlockPos posVecino, boolean movido) {
		super.neighborChanged(estado, mundo, pos, bloqueVecino, posVecino, movido);
		intentarArrancar(estado, mundo, pos);
	}

	/** Si le llega redstone: arranca (si está completo) o hace un ruido de fallo (si no). */
	private void intentarArrancar(BlockState estado, Level mundo, BlockPos pos) {
		if (mundo.isClientSide() || !mundo.hasNeighborSignal(pos)) return;
		// Si ya está arrancando, no hacemos nada (para no arrancarlo dos veces)
		if (mundo.getBlockTicks().hasScheduledTick(pos, this)) return;

		if (!estado.getValue(CORAZON)) {
			mundo.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 0.8F);
			return;
		}

		// ¡Arranca! Ruido de engranajes y chispas, y dentro de 3 segundos aparece Magearna
		mundo.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
		mundo.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.6F);
		if (mundo instanceof ServerLevel servidor) {
			servidor.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				40, 0.6, 0.6, 0.6, 0.2);
		}
		mundo.scheduleTick(pos, this, TIEMPO_ARRANQUE);
	}

	// ===================== Pasados 3 segundos: ¡Magearna! =====================

	@Override
	protected void tick(BlockState estado, ServerLevel mundo, BlockPos pos, RandomSource aleatorio) {
		if (!estado.getValue(CORAZON)) return;   // por si le quitaron algo mientras arrancaba

		// El armazón desaparece y en su sitio aparece Magearna
		mundo.removeBlock(pos, false);
		boolean esShiny = Invocacion.invocar(mundo, pos, "magearna", 70);

		mundo.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
			60, 0.8, 1.0, 0.8, 0.08);
		mundo.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
			8, 0.6, 0.4, 0.6, 0.1);
		mundo.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 1.2F);
		mundo.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.8F, 1.0F);

		Invocacion.avisarCercanos(mundo, pos,
			Textos.mensaje("magearna_aparece").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), esShiny);
	}

	// ===================== Efectos visuales =====================

	@Override
	public void animateTick(BlockState estado, Level mundo, BlockPos pos, RandomSource aleatorio) {
		// Con el corazón puesto, suelta chispitas rosas: está "vivo" esperando la energía
		if (estado.getValue(CORAZON) && aleatorio.nextInt(3) == 0) {
			mundo.addParticle(ParticleTypes.ELECTRIC_SPARK,
				pos.getX() + aleatorio.nextDouble(), pos.getY() + 1.05, pos.getZ() + aleatorio.nextDouble(),
				0, 0.05, 0);
		}
	}
}
