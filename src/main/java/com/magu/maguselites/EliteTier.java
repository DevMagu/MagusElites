package com.magu.maguselites;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * Los rangos de elite. Un "enum" es una clase con una lista CERRADA de valores: aqui solo existen STRONG, ELITE
 * y LEGENDARY, nadie puede crear un cuarto desde fuera. Cada valor lleva sus propios datos (nombre, color, peso...).
 *
 * Los colores son verde(STRONG), rojo(ELITE) y amarillo(LEGENDARY).
 */
public enum EliteTier {
    // Estos numeros son los valores POR DEFECTO: los de verdad se leen de la configuracion (MagusConfig).
    //        nombre      seccion config  color                particulas  peso  vida  daño  velocidad  poderes min/max
    STRONG("Strong", "strong", ChatFormatting.GREEN, 1, 70, 0.5, 0.25, 0.10, 0, 0),
    ELITE("Elite", "elite", ChatFormatting.RED, 2, 25, 1.0, 0.50, 0.20, 1, 1),
    LEGENDARY("Legendary", "legendary", ChatFormatting.YELLOW, 4, 5, 2.0, 1.00, 0.30, 2, 3);

    /** Nombre que se ve encima de la cabeza. */
    public final String displayName;
    /** Nombre de su seccion en el archivo de configuracion: [elites.strong]... */
    public final String configName;
    /** Color del nombre. */
    public final ChatFormatting color;
    /** Particulas de polvo del mismo color que el nombre (las crea EliteParticles). */
    public final DustParticleOptions particle;
    /** Cuantas particulas suelta cada vez: mas cuanto mas alto el rango. */
    public final int particleCount;
    /** Su loot table: magus_elites:elites/strong, elites/elite o elites/legendary (la usa EliteLoot). */
    public final ResourceKey<LootTable> lootTable;
    /** Valores por defecto de la configuracion (peso, vida, daño y velocidad). */
    public final int defaultWeight;
    public final double defaultHealth;
    public final double defaultDamage;
    public final double defaultSpeed;
    /** Cuantos poderes de combate lleva (por defecto): Strong 0, Elite 1, Legendary 2 o 3. */
    public final int defaultMinPowers;
    public final int defaultMaxPowers;

    /** El constructor de un enum se llama una vez por cada valor de arriba, con los datos entre parentesis. */
    EliteTier(String displayName, String configName, ChatFormatting color, int particleCount,
              int defaultWeight, double defaultHealth, double defaultDamage, double defaultSpeed,
              int defaultMinPowers, int defaultMaxPowers) {
        this.displayName = displayName;
        this.configName = configName;
        this.color = color;
        // El color de chat es un numero RGB (verde = 0x55FF55). fromRGB24 lo parte en rojo, verde y azul (0 a 1),
        // que es lo que piden las particulas de polvo. 1.0F = tamaño normal de la particula.
        this.particle = new DustParticleOptions(Vec3.fromRGB24(color.getColor()).toVector3f(), 1.0F);
        this.particleCount = particleCount;
        this.lootTable = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(MagusElites.MOD_ID, "elites/" + configName));
        this.defaultWeight = defaultWeight;
        this.defaultHealth = defaultHealth;
        this.defaultDamage = defaultDamage;
        this.defaultSpeed = defaultSpeed;
        this.defaultMinPowers = defaultMinPowers;
        this.defaultMaxPowers = defaultMaxPowers;
    }

    /**
     * Peso para el sorteo: con 70/25/5, de cada 100 elites unos 70 seran Strong, 25 Elite y 5 Legendary.
     * (No tienen por que sumar 100; lo que cuenta es la proporcion entre ellos.)
     */
    public int weight() {
        return MagusConfig.tier(this).weight().getAsInt();
    }

    /** Bonus de vida, daño y velocidad: 1.0 = +100 % (x2), 0.5 = +50 %... */
    public double health() {
        return MagusConfig.tier(this).health().getAsDouble();
    }

    public double damage() {
        return MagusConfig.tier(this).damage().getAsDouble();
    }

    public double speed() {
        return MagusConfig.tier(this).speed().getAsDouble();
    }

    /** Cuantos poderes le tocan a un elite de este rango: un numero al azar entre el minimo y el maximo. */
    public int rollPowerCount(RandomSource random) {
        int min = MagusConfig.tier(this).minPowers().getAsInt();
        int max = MagusConfig.tier(this).maxPowers().getAsInt();
        if (max <= min) {
            return min;     // por si en la config alguien pone el maximo por debajo del minimo
        }
        // nextInt(n) da de 0 a n-1: con min 2 y max 3 -> 2 + (0 o 1) = 2 o 3.
        return min + random.nextInt(max - min + 1);
    }

    /** Sortea un rango teniendo en cuenta los pesos. */
    public static EliteTier random(RandomSource random) {
        // 1. Sumamos todos los pesos: 70 + 25 + 5 = 100.
        int total = 0;
        for (EliteTier tier : values()) {          // values() = todos los rangos, en orden
            total += tier.weight();
        }

        // Si en la config alguien pone todos los pesos a 0, nextInt(0) haria petar el juego: mejor no arriesgar.
        if (total <= 0) {
            return STRONG;
        }

        // 2. Sacamos un numero al azar entre 0 y total-1 (0..99).
        int roll = random.nextInt(total);

        // 3. Vamos restando pesos: 0-69 -> STRONG, 70-94 -> ELITE, 95-99 -> LEGENDARY.
        for (EliteTier tier : values()) {
            if (roll < tier.weight()) {
                return tier;
            }
            roll -= tier.weight();
        }
        return STRONG; // Nunca llega aqui, pero Java exige devolver algo en todos los caminos.
    }
}
