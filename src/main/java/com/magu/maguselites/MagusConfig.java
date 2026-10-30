package com.magu.maguselites;

import java.util.EnumMap;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * La configuracion del mod: el archivo config/magus_elites-common.toml que cualquiera puede editar con el Bloc de notas.
 *
 * NeoForge crea el archivo solo la primera vez (con los valores por defecto y los comentarios), comprueba que los
 * valores esten dentro de su rango y lo vuelve a leer si se cambia con el juego abierto.
 *
 * Los textos de .comment(...) son los que se ven en el archivo: van en ingles, porque los leeran jugadores de todo
 * el mundo.
 */
public final class MagusConfig {

    /** Ajustes de un rango: un "record" es una clase pequeña que solo guarda datos (aqui, 4 opciones). */
    public record TierSettings(ModConfigSpec.IntValue weight, ModConfigSpec.DoubleValue health,
                               ModConfigSpec.DoubleValue damage, ModConfigSpec.DoubleValue speed) {
    }

    // ---- elites ----
    public static final ModConfigSpec.DoubleValue ELITE_CHANCE;
    /** Un mapa guarda parejas clave -> valor: aqui, cada rango -> sus ajustes. */
    private static final Map<EliteTier, TierSettings> TIERS = new EnumMap<>(EliteTier.class);

    // ---- niveles ----
    public static final ModConfigSpec.IntValue DAYS_PER_LEVEL;
    public static final ModConfigSpec.IntValue MAX_LEVEL;
    public static final ModConfigSpec.DoubleValue HEALTH_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue DAMAGE_PER_LEVEL;

    // ---- legendarios ----
    public static final ModConfigSpec.DoubleValue GLOW_DISTANCE;
    public static final ModConfigSpec.BooleanValue LEGENDARIES_IGNORE_SUN;

    /** La "especificacion": la lista de todas las opciones. Se apunta en el constructor de MagusElites. */
    public static final ModConfigSpec SPEC;

    // Un bloque "static" se ejecuta una sola vez, cuando Java carga la clase. Aqui construimos todas las opciones
    // en orden: push("x") abre la seccion [x] del archivo y pop() la cierra.
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Which monsters become elites, and how strong each tier is.").push("elites");
        ELITE_CHANCE = builder
                .comment("Chance for a naturally spawning monster to become an elite (0.06 = 6%).",
                        "Spawners and spawn eggs never create elites; /summon always does.")
                .defineInRange("eliteChance", 0.06, 0.0, 1.0);

        // Una seccion por rango: [elites.strong], [elites.elite] y [elites.legendary].
        // Los valores por defecto salen de la tabla del enum EliteTier.
        for (EliteTier tier : EliteTier.values()) {
            builder.comment("Settings for the " + tier.displayName + " tier.").push(tier.configName);
            TIERS.put(tier, new TierSettings(
                    builder.comment("Relative weight when picking a tier. With 70/25/5, about 5 of every 100 elites are Legendary.")
                            .defineInRange("weight", tier.defaultWeight, 0, 1000),
                    builder.comment("Extra max health (1.0 = +100%). Multiplies the level bonus.")
                            .defineInRange("health", tier.defaultHealth, 0.0, 100.0),
                    builder.comment("Extra attack damage (0.5 = +50%). Multiplies the level bonus.")
                            .defineInRange("damage", tier.defaultDamage, 0.0, 100.0),
                    builder.comment("Extra movement speed (0.2 = +20%).")
                            .defineInRange("speed", tier.defaultSpeed, 0.0, 5.0)));
            builder.pop();
        }
        builder.pop();

        builder.comment("Every monster (elite or not) gains a level as Minecraft days go by.",
                "Days come from the world clock: sleeping skips to the next day and counts.",
                "WARNING: /time set moves the clock back (\"/time set day\" = day 0 = level 1). Use /time add instead.")
                .push("levels");
        DAYS_PER_LEVEL = builder.comment("Minecraft days needed for each new level.")
                .defineInRange("daysPerLevel", 3, 1, 10000);
        MAX_LEVEL = builder.comment("Highest level monsters can reach.")
                .defineInRange("maxLevel", 20, 1, 1000);
        HEALTH_PER_LEVEL = builder.comment("Extra max health for each level above 1 (0.10 = +10%).")
                .defineInRange("healthPerLevel", 0.10, 0.0, 10.0);
        DAMAGE_PER_LEVEL = builder.comment("Extra attack damage for each level above 1 (0.05 = +5%).")
                .defineInRange("damagePerLevel", 0.05, 0.0, 10.0);
        builder.pop();

        builder.comment("Extra effects for Legendary elites.").push("legendary");
        GLOW_DISTANCE = builder.comment("Players within this many blocks see Legendaries glow. 0 = no glow.")
                .defineInRange("glowDistance", 20.0, 0.0, 128.0);
        LEGENDARIES_IGNORE_SUN = builder.comment("If true, Legendaries don't burn in daylight.").define("legendariesIgnoreSun", true);
        builder.pop();

        SPEC = builder.build();
    }

    /** Ajustes de un rango. */
    public static TierSettings tier(EliteTier tier) {
        return TIERS.get(tier);
    }
}
