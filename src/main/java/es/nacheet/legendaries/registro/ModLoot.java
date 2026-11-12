package es.nacheet.legendaries.registro;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * Añade nuestros objetos al loot de Minecraft, sin tocar sus archivos:
 * cada vez que el juego carga una tabla de loot, la modificamos aquí.
 */
public final class ModLoot {

	private ModLoot() {}

	// --- Tablas de Minecraft que modificamos ---
	// Arqueología (arena y grava sospechosa): el pincel da 1 objeto, así que se AÑADE al sorteo que ya existe
	private static final ResourceLocation ARQ_PIRAMIDE = mc("archaeology/desert_pyramid");
	private static final ResourceLocation ARQ_POZO = mc("archaeology/desert_well");
	private static final ResourceLocation ARQ_RUINAS_SENDEROS = mc("archaeology/trail_ruins_rare");
	// Cofres: pueden dar muchos objetos, así que se añade un sorteo EXTRA con probabilidad
	private static final ResourceLocation COFRE_TEMPLO_JUNGLA = mc("chests/jungle_temple");
	private static final ResourceLocation COFRE_CIUDAD_ANTIGUA = mc("chests/ancient_city");
	private static final ResourceLocation COFRE_CAMARAS_DESAFIO = mc("chests/trial_chambers/reward");
	private static final ResourceLocation COFRE_CIUDAD_END = mc("chests/end_city_treasure");
	private static final ResourceLocation COFRE_BIBLIOTECA_FORTALEZA = mc("chests/stronghold_library");
	private static final ResourceLocation COFRE_MANSION = mc("chests/woodland_mansion");
	private static final ResourceLocation COFRE_BARCO_HUNDIDO = mc("chests/shipwreck_supply");

	public static void registrar() {
		LootTableEvents.MODIFY.register((clave, tabla, origen, registros) -> {
			ResourceLocation id = clave.location();

			// --- Munkidori: Fragmentos de Ídolo ---
			if (id.equals(ARQ_PIRAMIDE) || id.equals(ARQ_POZO) || id.equals(ARQ_RUINAS_SENDEROS)) {
				anadirAlSorteo(tabla, ModObjetos.FRAGMENTO_IDOLO, 1);
			}
			if (id.equals(COFRE_TEMPLO_JUNGLA)) {
				anadirConProbabilidad(tabla, ModObjetos.FRAGMENTO_IDOLO, 0.35F);
			}

			// --- Koraidon: Páginas Escarlata (el pasado) ---
			if (id.equals(ARQ_RUINAS_SENDEROS)) {
				anadirAlSorteo(tabla, ModObjetos.PAGINA_ESCARLATA, 1);
			}
			if (id.equals(COFRE_CIUDAD_ANTIGUA)) {
				anadirConProbabilidad(tabla, ModObjetos.PAGINA_ESCARLATA, 0.30F);
			}

			// --- Miraidon: Páginas Violeta (el futuro) ---
			if (id.equals(COFRE_CAMARAS_DESAFIO) || id.equals(COFRE_CIUDAD_END)) {
				anadirConProbabilidad(tabla, ModObjetos.PAGINA_VIOLETA, 0.30F);
			}

			// --- Magearna: las 3 piezas ---
			if (id.equals(COFRE_BIBLIOTECA_FORTALEZA)) {
				anadirConProbabilidad(tabla, ModObjetos.ENGRANAJE_ANTIGUO, 0.30F);
			}
			if (id.equals(COFRE_MANSION)) {
				anadirConProbabilidad(tabla, ModObjetos.PLACA_ORNAMENTADA, 0.30F);
			}
			if (id.equals(COFRE_BARCO_HUNDIDO)) {
				anadirConProbabilidad(tabla, ModObjetos.MUELLE_CUERDA, 0.25F);
			}
		});
	}

	/** Mete el objeto como una opción más dentro del sorteo que ya tiene la tabla (para la arqueología). */
	private static void anadirAlSorteo(LootTable.Builder tabla, Item objeto, int peso) {
		tabla.modifyPools(pool -> pool.add(LootItem.lootTableItem(objeto).setWeight(peso)));
	}

	/** Añade a una tabla un sorteo extra: el objeto sale con esa probabilidad (para los cofres). */
	private static void anadirConProbabilidad(LootTable.Builder tabla, Item objeto, float probabilidad) {
		tabla.withPool(LootPool.lootPool()
			.setRolls(ConstantValue.exactly(1))
			.when(LootItemRandomChanceCondition.randomChance(probabilidad))
			.add(LootItem.lootTableItem(objeto)));
	}

	/** Atajo para las tablas de Minecraft: mc("chests/ancient_city") -> minecraft:chests/ancient_city */
	private static ResourceLocation mc(String ruta) {
		return ResourceLocation.withDefaultNamespace(ruta);
	}
}
