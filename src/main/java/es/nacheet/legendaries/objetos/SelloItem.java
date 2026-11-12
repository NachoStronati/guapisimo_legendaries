package es.nacheet.legendaries.objetos;

import es.nacheet.legendaries.AltaresLegendarios;
import es.nacheet.legendaries.GuapisimoConfig;
import es.nacheet.legendaries.registro.ModObjetos;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Sello de isla: cuenta las capturas de su tipo mientras está en el inventario.
 * Al llegar al objetivo, con clic derecho se convierte en la ofrenda.
 */
public class SelloItem extends Item {

	private final String tipo;    // "electric", "psychic", "grass", "water"
	private final Item ofrenda;

	public SelloItem(Properties propiedades, String tipo, Item ofrenda) {
		super(propiedades);
		this.tipo = tipo;
		this.ofrenda = ofrenda;
	}

	private static int objetivo() {
		return GuapisimoConfig.get().capturasSello;
	}

	public static int getProgreso(ItemStack objeto) {
		return objeto.getOrDefault(ModObjetos.PROGRESO, 0);
	}

	// Lo llama el evento de captura: suma +1 a todos los sellos del jugador que coincidan con algún tipo
	public static void sumarCaptura(ServerPlayer jugador, List<String> tiposCapturado) {
		Inventory inventario = jugador.getInventory();
		for (int i = 0; i < inventario.getContainerSize(); i++) {
			ItemStack objeto = inventario.getItem(i);
			if (!(objeto.getItem() instanceof SelloItem sello)) {
				continue;   // no es un sello: pasamos al siguiente hueco
			}
			boolean coincide = tiposCapturado.stream().anyMatch(t -> t.equalsIgnoreCase(sello.tipo));
			int progreso = getProgreso(objeto);
			if (!coincide || progreso >= objetivo()) {
				continue;   // otro tipo, o ya está completo
			}

			progreso++;
			objeto.set(ModObjetos.PROGRESO, progreso);

			if (progreso >= objetivo()) {
				jugador.displayClientMessage(Textos.mensaje("sello_completo", objeto.getHoverName())
					.withStyle(ChatFormatting.GOLD), false);
				jugador.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
			} else {
				jugador.displayClientMessage(Textos.mensaje("sello_suma",
					objeto.getHoverName(), progreso, objetivo()), true);
			}
		}
	}

	// Clic derecho: si está completo, se convierte en la ofrenda
	@Override
	public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
		ItemStack sello = jugador.getItemInHand(mano);
		if (nivel.isClientSide()) {
			return InteractionResultHolder.success(sello);
		}
		int progreso = getProgreso(sello);
		if (progreso < objetivo()) {
			jugador.displayClientMessage(Textos.mensaje("sello_progreso", progreso, objetivo()), true);
			return InteractionResultHolder.success(sello);
		}
		ItemStack premio = new ItemStack(ofrenda);
		nivel.playSound(null, jugador.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
		jugador.displayClientMessage(Textos.mensaje("sello_ofrenda", premio.getHoverName())
			.withStyle(ChatFormatting.GOLD), false);
		return InteractionResultHolder.consume(premio);   // el sello de la mano se cambia por la ofrenda
	}

	// Texto al pasar el ratón
	@Override
	public void appendHoverText(ItemStack objeto, TooltipContext contexto, List<Component> lineas, TooltipFlag flag) {
		int progreso = getProgreso(objeto);
		lineas.add(Textos.tooltip("sello_mision", objetivo(),
			Component.translatable("tipo." + AltaresLegendarios.MOD_ID + "." + tipo)).withStyle(ChatFormatting.GRAY));
		if (progreso >= objetivo()) {
			lineas.add(Textos.tooltip("sello_listo").withStyle(ChatFormatting.GOLD));
		} else {
			lineas.add(Textos.tooltip("sello_progreso", progreso, objetivo()).withStyle(ChatFormatting.AQUA));
		}
	}

	// Barrita de progreso debajo del objeto (como la durabilidad)
	@Override
	public boolean isBarVisible(ItemStack objeto) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack objeto) {
		return Math.round(13.0F * Math.min(getProgreso(objeto), objetivo()) / objetivo());   // 13 = barra llena
	}

	@Override
	public int getBarColor(ItemStack objeto) {
		return getProgreso(objeto) >= objetivo() ? 0xFFD700 : 0x55FFFF;   // dorada si está lista, cian si no
	}

	// Brilla como si estuviera encantado cuando está listo
	@Override
	public boolean isFoil(ItemStack objeto) {
		return getProgreso(objeto) >= objetivo();
	}
}
