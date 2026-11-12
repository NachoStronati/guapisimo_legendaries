package es.nacheet.legendaries.bloques;

import es.nacheet.legendaries.util.Invocacion;
import es.nacheet.legendaries.util.Momento;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Farol del Festival: se coloca en el suelo o colgando del techo y espera a la luna llena.
 * Esa noche aparece Ogerpon y el farol se consume.
 */
public class FarolFestivalBlock extends Block {

	// ¿Está colgando del techo? (el mismo estado que usa el farol de Minecraft)
	public static final BooleanProperty HANGING = BlockStateProperties.HANGING;

	// Cada cuántos ticks revisa la hora y la luna (100 ticks = 5 segundos)
	private static final int REVISION = 100;
	// A cuántos bloques del farol aparece Ogerpon
	private static final int DISTANCIA_LLEGADA = 6;

	// Forma de pie (apoyado en el suelo) y forma colgando (pegado al techo)
	private static final VoxelShape FORMA_SUELO = Block.box(4, 0, 4, 12, 11, 12);
	private static final VoxelShape FORMA_COLGANDO = Block.box(4, 5, 4, 12, 16, 12);

	public FarolFestivalBlock(Properties propiedades) {
		super(propiedades);
		registerDefaultState(stateDefinition.any().setValue(HANGING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
		constructor.add(HANGING);
	}

	// --- Al colocarlo: ¿techo o suelo? ---
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		// Miramos hacia dónde apunta el jugador, de lo más claro a lo menos claro
		for (Direction direccion : contexto.getNearestLookingDirections()) {
			if (direccion.getAxis() == Direction.Axis.Y) {
				// Si mira hacia arriba, colgando; si mira hacia abajo, de pie
				BlockState estado = defaultBlockState().setValue(HANGING, direccion == Direction.UP);
				if (estado.canSurvive(contexto.getLevel(), contexto.getClickedPos())) {
					return estado;
				}
			}
		}
		return null;   // ni techo ni suelo válidos: no se coloca
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
		return estado.getValue(HANGING) ? FORMA_COLGANDO : FORMA_SUELO;
	}

	/** Hacia dónde está el bloque que lo sujeta: arriba si cuelga, abajo si está de pie. */
	private static Direction apoyo(BlockState estado) {
		return estado.getValue(HANGING) ? Direction.UP : Direction.DOWN;
	}

	@Override
	protected boolean canSurvive(BlockState estado, LevelReader mundo, BlockPos pos) {
		Direction apoyo = apoyo(estado);
		return Block.canSupportCenter(mundo, pos.relative(apoyo), apoyo.getOpposite());
	}

	// Si quitan el bloque que lo sujeta, el farol se cae
	@Override
	protected BlockState updateShape(BlockState estado, Direction direccion, BlockState vecino,
									 LevelAccessor mundo, BlockPos pos, BlockPos posVecino) {
		if (direccion == apoyo(estado) && !canSurvive(estado, mundo, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(estado, direccion, vecino, mundo, pos, posVecino);
	}

	// --- Al colocarlo, empieza a esperar ---
	@Override
	protected void onPlace(BlockState estado, Level mundo, BlockPos pos, BlockState estadoAnterior, boolean movido) {
		if (!mundo.isClientSide()) {
			mundo.scheduleTick(pos, this, REVISION);
		}
	}

	// --- Cada 5 segundos: ¿es noche de luna llena? ---
	@Override
	protected void tick(BlockState estado, ServerLevel mundo, BlockPos pos, RandomSource aleatorio) {
		if (!Momento.esDeNoche(mundo) || !Momento.esLunaLlena(mundo)) {
			mundo.scheduleTick(pos, this, REVISION);   // todavía no: volvemos a mirar en 5 segundos
			return;
		}

		// ¡Ha llegado el momento! Ogerpon aparece en el suelo, a unos bloques, en una dirección al azar
		Direction direccion = Direction.Plane.HORIZONTAL.getRandomDirection(aleatorio);
		BlockPos lejos = pos.relative(direccion, DISTANCIA_LLEGADA);
		BlockPos sitio = mundo.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lejos);
		boolean esShiny = Invocacion.invocar(mundo, sitio, "ogerpon", 70);

		// El farol se consume con fuegos artificiales
		mundo.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
			60, 0.4, 0.6, 0.4, 0.15);
		mundo.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
			20, 0.3, 0.3, 0.3, 0.02);
		mundo.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 1.5F, 1.0F);
		mundo.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
		mundo.removeBlock(pos, false);

		Invocacion.avisarCercanos(mundo, pos,
			Textos.mensaje("ogerpon_aparece").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), esShiny);
	}

	// --- Clic derecho con la mano vacía: ¿cuánto falta? ---
	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level mundo, BlockPos pos, Player jugador, BlockHitResult golpe) {
		if (mundo.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (Momento.esLunaLlena(mundo) && Momento.hora(mundo) < 23000) {
			// Hoy toca luna llena (aún no ha amanecido)
			jugador.displayClientMessage(Textos.mensaje("farol_esta_noche").withStyle(ChatFormatting.DARK_AQUA), true);
		} else {
			// La fase avanza 1 cada día; la luna llena es la 0
			int fase = mundo.getMoonPhase();
			int noches = (fase == 0) ? 8 : 8 - fase;
			jugador.displayClientMessage(Textos.mensaje("farol_luna", noches).withStyle(ChatFormatting.GRAY), true);
		}
		return InteractionResult.CONSUME;
	}

	// --- Chispas mientras espera (solo visual) ---
	@Override
	public void animateTick(BlockState estado, Level mundo, BlockPos pos, RandomSource aleatorio) {
		if (aleatorio.nextInt(4) == 0) {
			mundo.addParticle(ParticleTypes.SMALL_FLAME,
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0.01, 0);
		}
		if (aleatorio.nextInt(10) == 0) {
			mundo.addParticle(ParticleTypes.END_ROD,
				pos.getX() + 0.2 + aleatorio.nextDouble() * 0.6, pos.getY() + 0.7, pos.getZ() + 0.2 + aleatorio.nextDouble() * 0.6,
				0, 0.02, 0);
		}
	}

	// Texto al pasar el ratón por encima en el inventario
	@Override
	public void appendHoverText(ItemStack objeto, Item.TooltipContext contexto, List<Component> texto, TooltipFlag flag) {
		texto.add(Textos.tooltip("farol_pista1").withStyle(ChatFormatting.DARK_AQUA));
		texto.add(Textos.tooltip("farol_pista2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
}
