package es.nacheet.legendaries;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuración del mod. Se guarda en config/guapisimo.json
 * Los valores escritos aquí son los de por defecto.
 */
public class GuapisimoConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("guapisimo");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path RUTA = FabricLoader.getInstance().getConfigDir().resolve("guapisimo.json");

    // La configuración que usa el juego
    private static GuapisimoConfig instancia = new GuapisimoConfig();

    // ===== General =====
    public double probabilidadShiny = 0.05;     // 0.05 = 5%
    public int esperaInvocacionTicks = 24000;   // 24000 = 1 día de Minecraft

    // ===== Tapus (sellos) =====
    public int capturasSello = 30;

    // ===== Tres Fieles =====
    public Okidogi okidogi = new Okidogi();
    public Fezandipiti fezandipiti = new Fezandipiti();

    public static class Okidogi {
        public int victoriasNecesarias = 25;
        public int alturaMinima = 120;
    }

    public static class Fezandipiti {
        public int amistadNecesaria = 200;
        public int tamanoEquipo = 4;
    }
    /** Para usar la configuración desde cualquier clase: GuapisimoConfig.get() */
    public static GuapisimoConfig get() {
        return instancia;
    }

    /** Lee el archivo. Si no existe, lo crea con los valores por defecto. */
    public static void cargar() {
        try {
            if (Files.exists(RUTA)) {
                try (Reader lector = Files.newBufferedReader(RUTA)) {
                    GuapisimoConfig leida = GSON.fromJson(lector, GuapisimoConfig.class);
                    if (leida != null) {
                        instancia = leida;
                    }
                }
            }
            // Lo volvemos a guardar: así se crea si no existía,
            // y si en una versión nueva añadimos opciones, aparecen en el archivo
            guardar();
            LOGGER.info("Configuración de Guapisimo cargada");
        } catch (Exception error) {
            // Si el jugador escribió mal el JSON, usamos los valores por defecto
            // y NO sobrescribimos su archivo, para que pueda arreglarlo
            LOGGER.error("Error leyendo config/guapisimo.json, se usan los valores por defecto", error);
        }
    }

    private static void guardar() throws IOException {
        Files.createDirectories(RUTA.getParent());
        try (Writer escritor = Files.newBufferedWriter(RUTA)) {
            GSON.toJson(instancia, escritor);
        }
    }
}