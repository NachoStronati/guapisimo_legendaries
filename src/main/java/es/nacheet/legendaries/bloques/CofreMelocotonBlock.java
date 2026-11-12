package es.nacheet.legendaries.bloques;

import es.nacheet.legendaries.registro.ModObjetos;
import es.nacheet.legendaries.util.Invocacion;
import es.nacheet.legendaries.util.Momento;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Cofre Melocotón: de noche en un pantano se carga con un Mochi de Melocotón.
 * Al amanecer se abre y aparece Pecharunt.
 */
public class CofreMelocotonBlock extends Block {

	// ¿Tiene un mochi dentro?
	public static final BooleanProperty CARGADO = BooleanProperty.create("cargado");
	// Hacia dónde mira el frente del cofre (norte, sur, este u oeste)
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	// Cada cuántos ticks revisa si ya amaneció (100 ticks = 5 segundos)
	private static final int REVISION = 100;

	private static final TagKey<Biome> PANTANOS =
		TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("cobblemon", "is_swamp"));

	public CofreMelocotonBlock(Properties propiedades) {
		super(propiedades);
		registerDefaultState(stateDefinition.any().setValue(CARGADO, false).setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
		constructor.add(CARGADO, FACING);
	}

	// Al colocarlo, el frente mira hacia el jugador (como un cofre normal)
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext contexto) {
		return defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite());
	}

	// --- Clic derecho con el mochi: cargar el cofre ---
	@Override
	protected ItemInteractionResult useItemOn(ItemStack objeto, BlockState estado, Level mundo, BlockPos pos,
											  Player jugador, InteractionHand mano, BlockHitResult golpe) {
		if (!objeto.is(ModObjetos.MOCHI_MELOCOTON)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;   // no es el mochi
		}
		if (mundo.isClientSide()) {
			return ItemInteractionResult.SUCCESS;
		}

		// 1. ¿Se puede cargar ahora?
		String fallo = null;
		if (estado.getValue(CARGADO)) {
			fallo = "cofre_ya_cargado";
		} else if (!Momento.esDeNoche(mundo)) {
			fallo = "cofre_hora";
		} else if (!mundo.getBiome(pos).is(PANTANOS)) {
			fallo = "cofre_lugar";
		}
		if (fallo != null) {
			jugador.displayClientMessage(Textos.mensaje(fallo).withStyle(ChatFormatting.GRAY), true);
			return ItemInteractionResult.FAIL;
		}

		// 2. ¡Cargamos el cofre y nos apuntamos en la "agenda" para revisar la hora en 5 segundos!
		mundo.setBlock(pos, estado.setValue(CARGADO, true), Block.UPDATE_ALL);
		mundo.scheduleTick(pos, this, REVISION);

		Invocacion.gastar(jugador, objeto);
		mundo.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
		jugador.sendSystemMessage(Textos.mensaje("cofre_cargado").withStyle(ChatFormatting.LIGHT_PURPLE));
		return ItemInteractionResult.CONSUME;
	}

	// --- Lo que pasa cuando llega la hora apuntada en la agenda ---
	@Override
	protected void tick(BlockState estado, ServerLevel mundo, BlockPos pos, RandomSource aleatorio) {
		if (!estado.getValue(CARGADO)) return;

		if (!Momento.esAmanecer(mundo)) {
			mundo.scheduleTick(pos, this, REVISION);   // todavía no: volvemos a mirar en 5 segundos
			return;
		}

		// ¡Amaneció! El cofre se abre (vuelve a estar sin cargar) y sale Pecharunt
		mundo.setBlock(pos, estado.setValue(CARGADO, false), Block.UPDATE_ALL);
		BlockPos sitio = pos.above();
		boolean esShiny = Invocacion.invocar(mundo, sitio, "pecharunt", 75);

		// Efectos
		mundo.sendParticles(ParticleTypes.WITCH, sitio.getX() + 0.5, sitio.getY() + 0.5, sitio.getZ() + 0.5,
			100, 1.0, 1.0, 1.0, 0.1);
		mundo.sendParticles(ParticleTypes.CHERRY_LEAVES, sitio.getX() + 0.5, sitio.getY() + 1, sitio.getZ() + 0.5,
			40, 1.5, 1.0, 1.5, 0.05);
		mundo.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
		mundo.playSound(null, pos, SoundEvents.WITCH_CELEBRATE, SoundSource.BLOCKS, 1.0F, 0.8F);

		Invocacion.avisarCercanos(mundo, pos,
			Textos.mensaje("pecharunt_aparece").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), esShiny);
	}

	// --- Humo tóxico mientras está cargado (solo visual) ---
	@Override
	public void animateTick(BlockState estado, Level mundo, BlockPos pos, RandomSource aleatorio) {
		if (estado.getValue(CARGADO) && aleatorio.nextInt(3) == 0) {
			mundo.addParticle(ParticleTypes.WITCH,
				pos.getX() + aleatorio.nextDouble(), pos.getY() + 1.05, pos.getZ() + aleatorio.nextDouble(),
				0, 0.02, 0);
		}
	}
}
