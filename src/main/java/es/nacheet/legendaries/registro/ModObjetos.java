package es.nacheet.legendaries.registro;

import com.mojang.serialization.Codec;
import es.nacheet.legendaries.altar.AltarBlock;
import es.nacheet.legendaries.altar.AltarBlockEntity;
import es.nacheet.legendaries.bloques.ArmazonMagearnaBlock;
import es.nacheet.legendaries.bloques.CofreMelocotonBlock;
import es.nacheet.legendaries.bloques.FarolFestivalBlock;
import es.nacheet.legendaries.objetos.*;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static es.nacheet.legendaries.AltaresLegendarios.id;

/**
 * Registra TODOS los objetos y bloques del mod, y la pestaña del modo creativo.
 * Para usarlos desde otras clases: ModObjetos.CADENA_TOXICA, ModObjetos.FAROL_FESTIVAL...
 */
public final class ModObjetos {

	private ModObjetos() {}

	// ===================== Datos de la lista de legendarios con altar =====================

	/** Un legendario con altar: su ofrenda, su nombre en el mod, su especie en Cobblemon, nivel y si lleva gema. */
	private record Legendario(String ofrenda, String nombre, String especie, int nivel, boolean conGema) {}

	/** Para añadir un legendario con altar, añade una línea aquí. */
	private static final Legendario[] LEGENDARIOS = {
		new Legendario("ofrenda_tapu_koko", "tapu_koko", "tapukoko", 60, true),
		new Legendario("ofrenda_tapu_lele", "tapu_lele", "tapulele", 60, true),
		new Legendario("ofrenda_tapu_bulu", "tapu_bulu", "tapubulu", 60, true),
		new Legendario("ofrenda_tapu_fini", "tapu_fini", "tapufini", 60, true),
		new Legendario("scarlet_book", "koraidon", "koraidon", 70, false),
		new Legendario("violet_book", "miraidon", "miraidon", 70, false)
	};

	/** Sellos de las islas: su tipo y el legendario cuya ofrenda dan. */
	private record Sello(String isla, String tipo, String legendario) {}

	private static final Sello[] SELLOS = {
		new Sello("melemele", "electric", "tapu_koko"),
		new Sello("akala", "psychic", "tapu_lele"),
		new Sello("ulaula", "grass", "tapu_bulu"),
		new Sello("poni", "water", "tapu_fini")
	};

	/** Regalo al capturar a un Pokémon: el objeto y la clave del mensaje. */
	public record Regalo(Item objeto, String mensaje) {}

	// ===================== Objetos y bloques =====================

	// Componente que guarda el progreso de sellos y cadenas
	public static DataComponentType<Integer> PROGRESO;

	// Altares (Tapus, Koraidon, Miraidon)
	public static Item GEMA;
	public static Item BRUJULA;
	public static BlockEntityType<AltarBlockEntity> ALTAR_BLOCK_ENTITY;
	public static Item PAGINA_ESCARLATA;
	public static Item PAGINA_VIOLETA;

	// Okidogi, Fezandipiti, Munkidori
	public static Item CADENA_TOXICA;
	public static Item PLUMA_VANIDOSA;
	public static Item FRAGMENTO_IDOLO;
	public static Item IDOLO_MUNKIDORI;

	// Pecharunt y Ogerpon
	public static Item MOCHI_MELOCOTON;
	public static Block COFRE_MELOCOTON;
	public static Item FLOR_RECUERDO;
	public static Block FAROL_FESTIVAL;

	// Diancie
	public static Item FRAGMENTO_DIAMANTE_ROSA;
	public static Item CORAZON_ROSA;

	// Jirachi
	public static Item FRAGMENTO_ESTRELLA;
	public static Item ESTRELLA_DESEOS;

	// Magearna
	public static Block ARMAZON_MAGEARNA;
	public static Item ENGRANAJE_ANTIGUO;
	public static Item PLACA_ORNAMENTADA;
	public static Item MUELLE_CUERDA;
	public static Item CORAZON_ALMA;

	// Mapas
	private static final Map<String, Item> OFRENDAS = new HashMap<>();                 // legendario -> ofrenda
	public static final Map<Item, String> LEGENDARIO_DE_OFRENDA = new HashMap<>();     // ofrenda -> legendario
	public static final Map<String, Regalo> REGALOS_CAPTURA = new HashMap<>();         // especie -> regalo

	// Lo que sale en la pestaña del creativo, en orden
	private static final List<ItemStack> PESTANA = new ArrayList<>();

	// ===================== Registro =====================

	public static void registrar() {
		PROGRESO = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("progreso"),
			DataComponentType.<Integer>builder()
				.persistent(Codec.INT)                      // se guarda al salir del mundo
				.networkSynchronized(ByteBufCodecs.VAR_INT) // se envía al jugador para que vea la barra
				.build());

		registrarAltares();
		registrarTresFieles();
		registrarPecharuntYOgerpon();
		registrarDiancie();
		registrarJirachi();
		registrarMagearna();
		registrarPestana();
	}

	private static void registrarAltares() {
		// La gema que flota encima de los altares (oculta: no sale en el creativo)
		GEMA = Registry.register(BuiltInRegistries.ITEM, id("gema_altar"), new Item(new Item.Properties().stacksTo(1)));
		BRUJULA = objeto("brujula_ancestral", new BrujulaAncestralItem(unico(Rarity.RARE)));

		List<Block> altares = new ArrayList<>();
		for (Legendario leg : LEGENDARIOS) {
			// Primero la ofrenda (el altar necesita saber cuál es la suya) y después el altar
			Item ofrenda = new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant());
			AltarBlock altar = new AltarBlock(ofrenda, leg.especie(), leg.nivel(), leg.conGema());
			bloque("altar_" + leg.nombre(), altar, new Item.Properties().rarity(Rarity.EPIC));
			objeto(leg.ofrenda(), ofrenda);

			altares.add(altar);
			OFRENDAS.put(leg.nombre(), ofrenda);
			LEGENDARIO_DE_OFRENDA.put(ofrenda, leg.nombre());
		}

		// El "cerebro" que comparten todos los altares (después de crearlos)
		ALTAR_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("altar"),
			BlockEntityType.Builder.of(AltarBlockEntity::new, altares.toArray(new Block[0])).build(null));

		// Sellos de los Tapus
		for (Sello s : SELLOS) {
			objeto("sello_" + s.isla(), new SelloItem(unico(Rarity.RARE), s.tipo(), OFRENDAS.get(s.legendario())));
		}

		// Páginas de los libros de Koraidon y Miraidon
		PAGINA_ESCARLATA = simple("pagina_escarlata", 16, Rarity.UNCOMMON);
		PAGINA_VIOLETA = simple("pagina_violeta", 16, Rarity.UNCOMMON);
	}

	private static void registrarTresFieles() {
		CADENA_TOXICA = objeto("cadena_toxica", new CadenaToxicaItem(unico(Rarity.EPIC)));
		regalo("okidogi", simple("eslabon_okidogi", 1, Rarity.RARE), "eslabon");

		PLUMA_VANIDOSA = objeto("pluma_vanidosa", new PlumaVanidosaItem(unico(Rarity.EPIC)));
		regalo("fezandipiti", simple("eslabon_fezandipiti", 1, Rarity.RARE), "eslabon");

		FRAGMENTO_IDOLO = simple("fragmento_idolo", 16, Rarity.UNCOMMON);
		IDOLO_MUNKIDORI = objeto("idolo_munkidori", new IdoloMunkidoriItem(unico(Rarity.EPIC)));
		regalo("munkidori", simple("eslabon_munkidori", 1, Rarity.RARE), "eslabon");
	}

	private static void registrarPecharuntYOgerpon() {
		MOCHI_MELOCOTON = simple("mochi_melocoton", 1, Rarity.EPIC);
		COFRE_MELOCOTON = bloque("cofre_melocoton",
			new CofreMelocotonBlock(BlockBehaviour.Properties.of()
				.strength(2.5F)
				.sound(SoundType.WOOD)
				.lightLevel(estado -> estado.getValue(CofreMelocotonBlock.CARGADO) ? 10 : 0)),   // brilla si está cargado
			new Item.Properties().rarity(Rarity.EPIC));

		// Pecharunt da la Flor del Recuerdo al capturarlo
		FLOR_RECUERDO = simple("flor_recuerdo", 1, Rarity.RARE);
		regalo("pecharunt", FLOR_RECUERDO, "flor_recuerdo");

		FAROL_FESTIVAL = bloque("farol_festival",
			new FarolFestivalBlock(BlockBehaviour.Properties.of()
				.strength(0.5F)
				.sound(SoundType.WOOL)
				.lightLevel(estado -> 15)    // da luz como una antorcha
				.noOcclusion()),             // no tapa los bloques de alrededor
			unico(Rarity.EPIC));
	}

	private static void registrarDiancie() {
		FRAGMENTO_DIAMANTE_ROSA = simple("fragmento_diamante_rosa", 16, Rarity.UNCOMMON);
		CORAZON_ROSA = objeto("corazon_rosa", new CorazonRosaItem(unico(Rarity.EPIC)));
	}

	private static void registrarJirachi() {
		FRAGMENTO_ESTRELLA = simple("fragmento_estrella", 16, Rarity.UNCOMMON);
		ESTRELLA_DESEOS = objeto("estrella_deseos", new EstrellaDeseosItem(unico(Rarity.EPIC)));
	}

	private static void registrarMagearna() {
		ENGRANAJE_ANTIGUO = simple("engranaje_antiguo", 1, Rarity.RARE);
		PLACA_ORNAMENTADA = simple("placa_ornamentada", 1, Rarity.RARE);
		MUELLE_CUERDA = simple("muelle_cuerda", 1, Rarity.RARE);
		CORAZON_ALMA = simple("corazon_alma", 1, Rarity.EPIC);
		ARMAZON_MAGEARNA = bloque("armazon_magearna",
				new ArmazonMagearnaBlock(BlockBehaviour.Properties.of()
						.strength(3.0F)
						.sound(SoundType.METAL)),
				unico(Rarity.EPIC));
	}

	private static void registrarPestana() {
		CreativeModeTab pestana = FabricItemGroup.builder()
			.icon(() -> new ItemStack(BRUJULA))
			.title(Component.translatable("itemGroup.guapisimo.legendarios"))
			.displayItems((parametros, salida) -> PESTANA.forEach(s -> salida.accept(s.copy())))
			.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("legendarios"), pestana);
	}

	// ===================== Ayudas para no repetir código =====================

	/** Propiedades de un objeto que no se apila (máximo 1) con su rareza. */
	private static Item.Properties unico(Rarity rareza) {
		return new Item.Properties().stacksTo(1).rarity(rareza);
	}

	/** Registra un objeto y lo añade a la pestaña del creativo. */
	private static <T extends Item> T objeto(String nombre, T objeto) {
		Registry.register(BuiltInRegistries.ITEM, id(nombre), objeto);
		PESTANA.add(new ItemStack(objeto));
		return objeto;
	}

	/** Registra un objeto sencillo (sin clase propia). */
	private static Item simple(String nombre, int maximo, Rarity rareza) {
		return objeto(nombre, new Item(new Item.Properties().stacksTo(maximo).rarity(rareza)));
	}

	/** Registra un bloque y su versión como objeto (con el mismo nombre). */
	private static <T extends Block> T bloque(String nombre, T bloque, Item.Properties propiedadesObjeto) {
		Registry.register(BuiltInRegistries.BLOCK, id(nombre), bloque);
		objeto(nombre, new BlockItem(bloque, propiedadesObjeto));
		return bloque;
	}

	/** Apunta qué objeto regala un Pokémon al capturarlo. */
	private static void regalo(String especie, Item objeto, String mensaje) {
		REGALOS_CAPTURA.put(especie, new Regalo(objeto, mensaje));
	}
}
