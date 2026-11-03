package com.magu.maguselites;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.entity.Mob;

/**
 * Los poderes de combate de los elites. Lo que HACE cada uno esta en EliteCombat; aqui solo su nombre y como se
 * guarda en el mob.
 *
 * Cada poder se guarda como un tag de entidad (como la marca de elite de la leccion 3): "magus_elites_power_venomous".
 * Se pueden ver en el juego con /tag @e[type=zombie,limit=1,sort=nearest] list
 */
public enum ElitePower {
    //        adjetivo del nombre   seccion de la config
    // --- de ataque: se activan cuando el elite pega ---
    VENOMOUS("Venomous", "venomous"),     // veneno
    BLAZING("Blazing", "blazing"),        // fuego
    FREEZING("Freezing", "freezing"),     // lentitud y fatiga de minado
    HURLING("Hurling", "hurling"),        // te lanza por los aires
    VAMPIRIC("Vampiric", "vampiric"),     // se cura con el daño que hace
    // --- de defensa y especiales ---
    THORNED("Thorned", "thorned"),        // devuelve parte del daño que recibe
    BLINKING("Blinking", "blinking"),     // se teletransporta a tu lado si te alejas
    SUMMONING("Summoning", "summoning");  // llama a mobs de su tipo al recibir daño

    /** Adjetivo que sale en el nombre: "[Elite] Venomous Zombie [Lv. 4]". */
    public final String displayName;
    /** Nombre de su opcion en la config: [powers] venomous = true... */
    public final String configName;

    ElitePower(String displayName, String configName) {
        this.displayName = displayName;
        this.configName = configName;
    }

    /** El tag que lleva el mob con este poder. */
    public String tag() {
        // name() = "VENOMOUS"; toLowerCase() lo pasa a minusculas: "venomous".
        return "magus_elites_power_" + name().toLowerCase();
    }

    /** ¿Tiene el mob este poder? */
    public boolean has(Mob mob) {
        return mob.getTags().contains(tag());
    }

    /** Todos los poderes que tiene el mob, en el orden del enum. */
    public static List<ElitePower> of(Mob mob) {
        List<ElitePower> powers = new ArrayList<>();
        for (ElitePower power : values()) {
            if (power.has(mob)) {
                powers.add(power);
            }
        }
        return powers;
    }

    /**
     * Le da al mob "count" poderes distintos al azar, solo entre los activados en la config.
     * Truco: barajamos la lista (como un mazo de cartas) y cogemos las primeras: asi nunca se repite ninguno.
     */
    public static void giveRandom(Mob mob, int count) {
        List<ElitePower> available = new ArrayList<>();
        for (ElitePower power : values()) {
            if (MagusConfig.powerEnabled(power)) {
                available.add(power);
            }
        }
        // Collections.shuffle necesita un java.util.Random: el del mob (RandomSource) no vale, asi que creamos uno
        // con una "semilla" sacada del mob.
        Collections.shuffle(available, new java.util.Random(mob.getRandom().nextLong()));

        // Math.min: si piden 3 pero solo hay 2 activados, damos 2.
        for (int i = 0; i < Math.min(count, available.size()); i++) {
            mob.addTag(available.get(i).tag());
        }
    }
}
