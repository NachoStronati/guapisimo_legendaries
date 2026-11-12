package es.nacheet.legendaries;

import es.nacheet.legendaries.eventos.ModEventos;
import es.nacheet.legendaries.registro.ModLoot;
import es.nacheet.legendaries.registro.ModObjetos;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

/**
 * Guapisimo Legendaries: punto de entrada del mod.
 *
 * Esta clase solo "pone en marcha" cada parte, en orden. Cada parte vive en su propia clase:
 *   - GuapisimoConfig  -> lee config/guapisimo.json
 *   - ModObjetos       -> registra todos los objetos, bloques y la pestaña del creativo
 *   - ModLoot          -> añade nuestros objetos al loot de Minecraft
 *   - ModEventos       -> capturas, combates, minería y estrellas fugaces
 */
public class AltaresLegendarios implements ModInitializer {

	public static final String MOD_ID = "guapisimo";

	@Override
	public void onInitialize() {
		GuapisimoConfig.cargar();   // 1. Primero la configuración: otras partes la usan
		ModObjetos.registrar();     // 2. Objetos y bloques (tienen que existir antes que el loot y los eventos)
		ModLoot.registrar();        // 3. Loot
		ModEventos.registrar();     // 4. Eventos
	}

	/** Crea un identificador de nuestro mod. Ejemplo: id("cadena_toxica") -> guapisimo:cadena_toxica */
	public static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, nombre);
	}
}
