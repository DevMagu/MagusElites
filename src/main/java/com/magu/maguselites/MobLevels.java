package com.magu.maguselites;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Niveles de los monstruos: todos (normales y elites) suben un nivel cada 3 dias de Minecraft.
 *
 * Dia 0-2 = nivel 1, dia 3-5 = nivel 2, dia 6-8 = nivel 3... hasta el nivel 20.
 * Cuentan los dias del reloj de Minecraft: dormir pasa al dia siguiente y cuenta. Ojo: /time set lo cambia.
 */
public final class MobLevels {
    /** Cada cuantos dias sube un nivel. */
    public static final int DAYS_PER_LEVEL = 3;
    /** Nivel maximo: a partir de aqui ya no suben mas. */
    public static final int MAX_LEVEL = 20;
    /** Vida extra por cada nivel por encima del 1: 0.10 = +10 %. */
    public static final double HEALTH_PER_LEVEL = 0.10;
    /** Daño extra por cada nivel por encima del 1: 0.05 = +5 %. */
    public static final double DAMAGE_PER_LEVEL = 0.05;

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
        long mobLevel = 1 + day / DAYS_PER_LEVEL;

        // Math.min se queda con el mas pequeño de los dos: asi nunca pasa de MAX_LEVEL.
        return (int) Math.min(mobLevel, MAX_LEVEL);
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
                extraLevels * HEALTH_PER_LEVEL, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        Elites.addModifier(mob, Attributes.ATTACK_DAMAGE, "level_damage",
                extraLevels * DAMAGE_PER_LEVEL, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    /** Nivel guardado en el mob (1 si no tiene, por ejemplo los que ya existian antes de instalar el mod). */
    public static int getLevel(Mob mob) {
        return Math.max(1, mob.getPersistentData().getInt(LEVEL_KEY));
    }
}
