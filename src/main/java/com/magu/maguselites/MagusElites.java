package com.magu.maguselites;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase principal de Magu's Elites.
 *
 * NeoForge busca las clases que llevan @Mod("id") y crea una instancia al arrancar el juego.
 * Por eso el constructor es el primer codigo de nuestro mod que se ejecuta: aqui se "apunta"
 * todo lo demas (eventos, configuracion, comandos...).
 */
@Mod(MagusElites.MOD_ID)
public final class MagusElites {
    /** Tiene que ser igual que mod_id en gradle.properties. */
    public static final String MOD_ID = "magus_elites";

    /** Para escribir mensajes en el log del juego (logs/latest.log). */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * @param modBus    bus de eventos del propio mod (registros de bloques/objetos, configuracion...).
     * @param container informacion de nuestro mod (nombre, version...).
     */
    public MagusElites(IEventBus modBus, ModContainer container) {
        LOGGER.info("Magu's Elites {} cargado correctamente!", container.getModInfo().getVersion());
        LOGGER.info("Disfruta de la experiencia!");

        // Apuntamos la configuracion: NeoForge crea config/magus_elites-common.toml si no existe y lo lee.
        // COMMON = una sola config para todo (servidor y mundos de un jugador).
        container.registerConfig(ModConfig.Type.COMMON, MagusConfig.SPEC);

        // Apuntamos nuestro metodo al bus de eventos del JUEGO (NeoForge.EVENT_BUS): asi NeoForge lo llamara
        // cada vez que aparezca un mob. "EliteSpawns::onFinalizeSpawn" significa "el metodo onFinalizeSpawn de
        // la clase EliteSpawns" (se llama referencia a metodo).
        NeoForge.EVENT_BUS.addListener(EliteSpawns::onFinalizeSpawn);
        // Y este, en cada tick de cada entidad: el brillo de los Legendarios.
        NeoForge.EVENT_BUS.addListener(LegendaryGlow::onEntityTick);
    }
}
