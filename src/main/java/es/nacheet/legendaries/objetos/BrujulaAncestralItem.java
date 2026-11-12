package es.nacheet.legendaries.objetos;

import es.nacheet.legendaries.AltaresLegendarios;
import es.nacheet.legendaries.registro.ModObjetos;
import es.nacheet.legendaries.util.Textos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * Brújula Ancestral: con una ofrenda en la otra mano, apunta al templo de ese legendario.
 */
public class BrujulaAncestralItem extends Item {

	private static final String[] DIRECCIONES =
		{"norte", "noreste", "este", "sureste", "sur", "suroeste", "oeste", "noroeste"};

	// Espera entre usos (40 ticks = 2 segundos), porque buscar estructuras cuesta rendimiento
	private static final int ESPERA = 40;
	// Hasta dónde busca (en "regiones" de estructuras, igual que /locate)
	private static final int RADIO_BUSQUEDA = 100;

	public BrujulaAncestralItem(Properties propiedades) {
		super(propiedades);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level nivelMundo, Player jugador, InteractionHand mano) {
		ItemStack brujula = jugador.getItemInHand(mano);

		// Solo funciona en la mano principal, y la búsqueda la hace el servidor
		if (mano != InteractionHand.MAIN_HAND) {
			return InteractionResultHolder.pass(brujula);
		}
		if (!(nivelMundo instanceof ServerLevel mundo)) {
			return InteractionResultHolder.success(brujula);
		}

		// 1. ¿Hay una ofrenda en la otra mano? El mapa nos dice de qué legendario es (o null si no lo es)
		ItemStack otraMano = jugador.getOffhandItem();
		String legendario = ModObjetos.LEGENDARIO_DE_OFRENDA.get(otraMano.getItem());
		if (legendario == null) {
			brujula.remove(DataComponents.LODESTONE_TRACKER);   // la aguja vuelve a girar sin rumbo
			jugador.displayClientMessage(Textos.mensaje("brujula_sin_ofrenda"), true);
			return InteractionResultHolder.success(brujula);
		}

		// 2. Buscar el templo más cercano con la etiqueta "templo_<legendario>" (lo mismo que hace /locate)
		TagKey<Structure> etiqueta = TagKey.create(Registries.STRUCTURE, AltaresLegendarios.id("templo_" + legendario));
		jugador.getCooldowns().addCooldown(this, ESPERA);
		BlockPos templo = mundo.findNearestMapStructure(etiqueta, jugador.blockPosition(), RADIO_BUSQUEDA, false);
		if (templo == null) {
			jugador.displayClientMessage(Textos.mensaje("brujula_no_encontrado"), true);
			return InteractionResultHolder.success(brujula);
		}

		// 3. Guardar la posición dentro de la brújula (para que la aguja apunte ahí)
		brujula.set(DataComponents.LODESTONE_TRACKER,
			new LodestoneTracker(Optional.of(GlobalPos.of(mundo.dimension(), templo)), false));

		// 4. Mensaje con la distancia y la dirección
		int dx = templo.getX() - jugador.getBlockX();
		int dz = templo.getZ() - jugador.getBlockZ();
		int distancia = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
		jugador.displayClientMessage(Textos.mensaje("brujula_encontrado",
			otraMano.getHoverName(), distancia, direccion(dx, dz)), false);
		mundo.playSound(null, jugador.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);

		return InteractionResultHolder.success(brujula);
	}

	/** Convierte una diferencia de posición en una de las 8 direcciones (norte, noreste...). */
	private static Component direccion(int dx, int dz) {
		double angulo = Math.toDegrees(Math.atan2(dx, -dz));   // 0 = norte, 90 = este
		if (angulo < 0) {
			angulo += 360;
		}
		String nombre = DIRECCIONES[(int) Math.round(angulo / 45.0) % 8];
		return Component.translatable("direccion." + AltaresLegendarios.MOD_ID + "." + nombre);
	}
}
