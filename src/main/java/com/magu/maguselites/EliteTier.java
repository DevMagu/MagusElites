package com.magu.maguselites;

import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;

/**
 * Los rangos de elite. Un "enum" es una clase con una lista CERRADA de valores: aqui solo existen STRONG, ELITE
 * y LEGENDARY, nadie puede crear un cuarto desde fuera. Cada valor lleva sus propios datos (nombre, color, peso...).
 *
 * Los colores son verde(STRONG), rojo(ELITE) y amarillo(LEGENDARY).
 */
public enum EliteTier {
    //        nombre        color                   peso  vida  daño  velocidad
    STRONG("Strong", ChatFormatting.GREEN, 70, 0.5, 0.25, 0.10),
    ELITE("Elite", ChatFormatting.RED, 25, 1.0, 0.50, 0.20),
    LEGENDARY("Legendary", ChatFormatting.YELLOW, 5, 2.0, 1.00, 0.30);

    /** Nombre que se ve encima de la cabeza. */
    public final String displayName;
    /** Color del nombre. */
    public final ChatFormatting color;
    /**
     * Peso para el sorteo: de cada 100 elites, unos 70 seran Strong, 25 Elite y 5 Legendary.
     * (No tienen por que sumar 100; lo que cuenta es la proporcion entre ellos.)
     */
    public final int weight;
    /** Bonus de vida, daño y velocidad: 1.0 = +100 % (x2), 0.5 = +50 %... */
    public final double health;
    public final double damage;
    public final double speed;

    /** El constructor de un enum se llama una vez por cada valor de arriba, con los datos entre parentesis. */
    EliteTier(String displayName, ChatFormatting color, int weight, double health, double damage, double speed) {
        this.displayName = displayName;
        this.color = color;
        this.weight = weight;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
    }

    /** Sortea un rango teniendo en cuenta los pesos. */
    public static EliteTier random(RandomSource random) {
        // 1. Sumamos todos los pesos: 70 + 25 + 5 = 100.
        int total = 0;
        for (EliteTier tier : values()) {          // values() = todos los rangos, en orden
            total += tier.weight;
        }

        // 2. Sacamos un numero al azar entre 0 y total-1 (0..99).
        int roll = random.nextInt(total);

        // 3. Vamos restando pesos: 0-69 -> STRONG, 70-94 -> ELITE, 95-99 -> LEGENDARY.
        for (EliteTier tier : values()) {
            if (roll < tier.weight) {
                return tier;
            }
            roll -= tier.weight;
        }
        return STRONG; // Nunca llega aqui, pero Java exige devolver algo en todos los caminos.
    }
}
