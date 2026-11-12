package es.nacheet.legendaries.altar;

import es.nacheet.legendaries.registro.ModObjetos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El "cerebro" del altar. De momento no guarda nada:
 * solo existe para que el AltarRenderer pueda dibujar la gema encima.
 */
public class AltarBlockEntity extends BlockEntity {

	public AltarBlockEntity(BlockPos pos, BlockState estado) {
		super(ModObjetos.ALTAR_BLOCK_ENTITY, pos, estado);
	}
}
