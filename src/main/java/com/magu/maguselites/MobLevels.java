package com.magu.maguselites;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Niveles de los monstruos: todos (normales y elites) suben un nivel cada X dias de Minecraft (3 por defecto).
 *
 * Con los valores por defecto: dia 0-2 = nivel 1, dia 3-5 = nivel 2... hasta el nivel 20.
 * Cuentan los dias del reloj de Minecraft: dormir pasa al dia siguiente y cuenta. Ojo: /time set lo cambia.
 */
public final class MobLevels {
    // Los dias por nivel, el tope y los bonus por nivel estan en la configuracion (MagusConfig, seccion [levels]).

    /** Un dia de Minecraft dura 24000 ticks (20 ticks = 1 segundo, asi que 20 minutos). */
    private static final long TICKS_PER_DAY = 24000L;

    /** Clave con la que guardamos el nivel dentro del mob. */
    private static final String LEVEL_KEY = "magus_elites_level";

    /** Nivel que les toca a los monstruos que aparecen ahora mismo. */
    public static int currentLevel(ServerLevel level) {
        // Usamos el reloj del Overworld: en el Nether y el End no hay dias, pero queremos el mismo nivel.
        long ticks = level.getServer().overworld().getDayTime();
        long day = ticks / TICKS_PER_DAY;

        // Division entera: 7 / 3 = 2 (los decimales se tiran). Por eso el nivel cambia solo cada 3 dias.
        long mobLevel = 1 + day / MagusConfig.DAYS_PER_LEVEL.getAsInt();

        // Math.min se queda con el mas pequeño de los dos: asi nunca pasa del nivel maximo.
        return (int) Math.min(mobLevel, MagusConfig.MAX_LEVEL.getAsInt());
    }

    /** Aplica el nivel al mob: le sube vida y daño y se lo apunta. */
    public static void applyLevel(Mob mob, int level) {
        // Guardamos el nivel en los datos del mob (se guardan con el mundo) para poder leerlo despues.
        mob.getPersistentData().putInt(LEVEL_KEY, level);

        int extraLevels = level - 1;   // el nivel 1 no lleva bonus
        if (extraLevels <= 0) {
            return;
        }
        // ADD_MULTIPLIED_BASE: un porcentaje de la vida base. Nivel 11 = 10 niveles extra x 10 % = +100 %.
        Elites.addModifier(mob, Attributes.MAX_HEALTH, "level_health",
                extraLevels * MagusConfig.HEALTH_PER_LEVEL.getAsDouble(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        Elites.addModifier(mob, Attributes.ATTACK_DAMAGE, "level_damage",
                extraLevels * MagusConfig.DAMAGE_PER_LEVEL.getAsDouble(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    /** Nivel guardado en el mob (1 si no tiene, por ejemplo los que ya existian antes de instalar el mod). */
    public static int getLevel(Mob mob) {
        return Math.max(1, mob.getPersistentData().getInt(LEVEL_KEY));
    }
}
