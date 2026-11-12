package es.nacheet.legendaries.util;

import net.minecraft.world.level.Level;

/**
 * Ayudas para saber la hora, el día y la luna.
 * Así las cuentas de ticks están en UN solo sitio y no repetidas en cada objeto.
 *
 * Un día de Minecraft dura 24000 ticks:
 *   0     = amanece (6:00)
 *   6000  = mediodía
 *   13000 = anochece
 *   18000 = medianoche
 *   23000 = empieza a salir el sol
 */
public final class Momento {

	public static final int TICKS_DIA = 24000;

	private Momento() {}   // es una clase de ayuda: no se crean objetos de ella

	/** Hora dentro del día (de 0 a 23999). */
	public static long hora(Level mundo) {
		return mundo.getDayTime() % TICKS_DIA;
	}

	/** Número de día desde que empezó el mundo (0, 1, 2...). */
	public static long dia(Level mundo) {
		return mundo.getDayTime() / TICKS_DIA;
	}

	/** De noche: del tick 13000 al 23000. */
	public static boolean esDeNoche(Level mundo) {
		long hora = hora(mundo);
		return hora >= 13000 && hora < 23000;
	}

	/** Amanecer: del tick 23000 al 1000 (cuando sale el sol). */
	public static boolean esAmanecer(Level mundo) {
		long hora = hora(mundo);
		return hora >= 23000 || hora < 1000;
	}

	/** Minecraft tiene 8 fases de luna (0 a 7) y avanza una cada día. La 0 es la luna llena. */
	public static boolean esLunaLlena(Level mundo) {
		return mundo.getMoonPhase() == 0;
	}
}
