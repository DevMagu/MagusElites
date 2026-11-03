package com.magu.maguselites;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Convierte un monstruo normal en elite (mas vida, daño y velocidad, y una marca para reconocerlo) y pone los nombres.
 */
public final class Elites {

    /**
     * Marca invisible que dejamos en cada elite. Los "tags" de entidad son textos que el juego guarda con el mob
     * (sobreviven a cerrar el mundo). Tambien se pueden ver con comandos: /tag @e[type=zombie,limit=1,sort=nearest] list
     */
    public static final String TAG = "magus_elites_elite";

    /** Clave con la que guardamos el rango dentro del mob. */
    private static final String TIER_KEY = "magus_elites_tier";

    /** ¿Este mob ya es elite? */
    public static boolean isElite(Mob mob) {
        return mob.getTags().contains(TAG);
    }

    /** Rango del mob, o null si no es elite. */
    public static EliteTier getTier(Mob mob) {
        // Guardamos el rango como texto ("LEGENDARY"); aqui buscamos el valor del enum que se llama asi.
        String saved = mob.getPersistentData().getString(TIER_KEY);
        for (EliteTier tier : EliteTier.values()) {
            if (tier.name().equals(saved)) {     // los textos se comparan con equals, no con ==
                return tier;
            }
        }
        return null;
    }

    /**
     * Convierte al mob en elite del rango que le pasemos. Se llama DESPUES de MobLevels.applyLevel: el bonus de elite
     * se multiplica encima del de nivel (ADD_MULTIPLIED_TOTAL).
     */
    public static void makeElite(Mob mob, EliteTier tier) {
        // Los porcentajes salen del rango: un Legendary lleva +200 % de vida (x3), un Strong +50 %...
        addModifier(mob, Attributes.MAX_HEALTH, "elite_health", tier.health(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(mob, Attributes.ATTACK_DAMAGE, "elite_damage", tier.damage(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addModifier(mob, Attributes.MOVEMENT_SPEED, "elite_speed", tier.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        mob.addTag(TAG);
        // name() da el nombre del valor en el codigo: "STRONG", "ELITE" o "LEGENDARY".
        mob.getPersistentData().putString(TIER_KEY, tier.name());
        // El brillo de los Legendarios ya no va aqui: lo hace LegendaryGlow, solo para los jugadores cercanos.

        // Poderes de combate: Strong 0, Elite 1, Legendary 2 o 3 (lo que diga la config). Lo que hacen: EliteCombat.
        ElitePower.giveRandom(mob, tier.rollPowerCount(mob.getRandom()));
    }

    /**
     * Pone el nombre encima de la cabeza: "[Legendary] Venomous Blazing Zombi [Lv. 4]" en los elites (siempre visible,
     * con el color del rango y sus poderes) y "Zombi [Lv. 4]" en los normales (blanco, solo al apuntarles con la mira).
     */
    public static void updateName(Mob mob) {
        EliteTier tier = getTier(mob);
        MutableComponent name;
        if (tier != null) {
            // Lo que se pega con append hereda el color: el tipo de mob y el nivel salen del color del rango.
            name = Component.literal("[" + tier.displayName + "] ").withStyle(tier.color);
            // Los poderes van delante del mob, como adjetivos en ingles: "Venomous Blazing Zombie".
            for (ElitePower power : ElitePower.of(mob)) {
                name.append(power.displayName + " ");
            }
            name.append(mob.getType().getDescription());
        } else {
            // getDescription() da un Component que no se puede modificar; copy() hace una copia que si.
            name = mob.getType().getDescription().copy();
        }
        name.append(Component.literal(" [Lv. " + MobLevels.getLevel(mob) + "]"));

        mob.setCustomName(name);
        // true = siempre visible; false = solo al mirarle de cerca.
        mob.setCustomNameVisible(tier != null);
    }

    /**
     * Sube un atributo del mob un porcentaje.
     *
     * @param attribute qué atributo (vida, daño, armadura, velocidad...).
     * @param name      nombre del modificador; junto con nuestro mod_id forma su id unico.
     * @param percent   cuánto sube: 0.5 = +50 %, 1.0 = +100 %.
     * @param operation ADD_MULTIPLIED_BASE = porcentaje de la base (se suman entre ellos);
     *                  ADD_MULTIPLIED_TOTAL = porcentaje del total, aplicado al final (multiplica lo anterior).
     */
    static void addModifier(Mob mob, Holder<Attribute> attribute, String name, double percent,
                            AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);

        // No todos los mobs tienen todos los atributos: un creeper no tiene ATTACK_DAMAGE (no pega, explota).
        // Si no lo tiene, getAttribute devuelve null, y usar un null hace petar el juego (NullPointerException).
        if (instance == null) {
            return;
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MagusElites.MOD_ID, name);
        // "Permanent" = se guarda con el mob.
        instance.addPermanentModifier(new AttributeModifier(id, percent, operation));
    }
}
