package es.nacheet.legendaries.altar;

import es.nacheet.legendaries.util.Invocacion;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Altar de un legendario.
 * - ACTIVO = true: brilla y acepta la ofrenda.
 * - ACTIVO = false: ya se usó y no hace nada (solo se puede usar una vez).
 * Es irrompible en supervivencia para que nadie se lo lleve a otro sitio.
 */
public class AltarBlock extends Block implements EntityBlock {

	public static final BooleanProperty ACTIVO = BooleanProperty.create("activo");

	// Forma del altar (para el recuadro de selección y para chocar con él).
	// Block.box usa píxeles (0-16), igual que Blockbench.
	private static final VoxelShape FORMA = Shapes.or(
		Block.box(1, 0, 1, 15, 2, 15),    // Base
		Block.box(4, 2, 4, 12, 10, 12),   // Columna
		Block.box(2, 10, 2, 14, 12, 14)   // Tapa
	);

	private final Item ofrenda;
	private final String especie;
	private final int nivel;
	private final boolean conGema;

	public AltarBlock(Item ofrenda, String especie, int nivel, boolean conGema) {
		super(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(-1.0F, 3600000.0F)   // irrompible, como la roca madre
			.noLootTable()
			.sound(SoundType.STONE)
			.noOcclusion()                  // no es un cubo entero: no tapar las caras de al lado
			.lightLevel(estado -> estado.getValue(ACTIVO) ? 10 : 0));
		this.ofrenda = ofrenda;
		this.especie = especie;
		this.nivel = nivel;
		this.conGema = conGema;
		registerDefaultState(this.stateDefinition.any().setValue(ACTIVO, true));
	}

	public boolean tieneGema() {
		return conGema;
	}

	@Override
	protected VoxelShape getShape(BlockState estado, BlockGetter mundo, BlockPos pos, CollisionContext contexto) {
		return FORMA;
	}

	// Crea el "cerebro" del altar cuando se coloca el bloque
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
		return new AltarBlockEntity(pos, estado);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVO);
	}

	// Clic derecho con un objeto en la mano
	@Override
	protected ItemInteractionResult useItemOn(ItemStack objeto, BlockState estado, Level nivelMundo, BlockPos pos,
											  Player jugador, InteractionHand mano, BlockHitResult golpe) {
		if (!(nivelMundo instanceof ServerLevel mundo)) {
			return ItemInteractionResult.SUCCESS;   // en tu pantalla solo se ve la animación
		}

		// ¿Ya se usó?
		if (!estado.getValue(ACTIVO)) {
			jugador.displayClientMessage(Textos.mensaje("usado"), true);
			return ItemInteractionResult.SUCCESS;
		}
		// ¿Es la ofrenda correcta?
		if (!objeto.is(ofrenda)) {
			jugador.displayClientMessage(Textos.mensaje("necesita", ofrenda.getDescription()), true);
			return ItemInteractionResult.SUCCESS;
		}

		// Gastar la ofrenda y apagar el altar para siempre
		Invocacion.gastar(jugador, objeto);
		mundo.setBlock(pos, estado.setValue(ACTIVO, false), Block.UPDATE_ALL);

		// Efectos: sonido y partículas
		mundo.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
		mundo.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
			60, 0.6, 1.0, 0.6, 0.05);

		// ¡Aparece el legendario encima del altar!
		boolean esShiny = Invocacion.invocar(mundo, pos.above(), especie, nivel);
		if (esShiny) {
			Invocacion.avisarShiny(jugador);
			// Sonido de logro y lluvia de partículas
			mundo.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
			mundo.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
				80, 0.8, 1.2, 0.8, 0.3);
		} else {
			jugador.displayClientMessage(Textos.mensaje("invocado"), true);
		}
		return ItemInteractionResult.SUCCESS;
	}

	// Clic derecho con la mano vacía: da una pista
	@Override
	protected InteractionResult useWithoutItem(BlockState estado, Level nivelMundo, BlockPos pos,
											   Player jugador, BlockHitResult golpe) {
		if (!nivelMundo.isClientSide()) {
			String clave = estado.getValue(ACTIVO) ? "necesita" : "usado";
			jugador.displayClientMessage(Textos.mensaje(clave, ofrenda.getDescription()), true);
		}
		return InteractionResult.sidedSuccess(nivelMundo.isClientSide());
	}

	// Partículas mientras el altar está activo (solo se ven, no hacen nada)
	@Override
	public void animateTick(BlockState estado, Level nivelMundo, BlockPos pos, RandomSource azar) {
		if (!estado.getValue(ACTIVO)) return;
		if (azar.nextInt(3) == 0) {
			double x = pos.getX() + 0.5 + (azar.nextDouble() - 0.5) * 0.6;
			double y = pos.getY() + 1.0;
			double z = pos.getZ() + 0.5 + (azar.nextDouble() - 0.5) * 0.6;
			nivelMundo.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.02, 0);
		}
	}
}
