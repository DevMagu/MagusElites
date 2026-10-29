package com.magu.maguselites;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.minecraft.world.entity.monster.Monster;

/**
 * Aqui decidiremos que monstruos se convierten en elite.
 *
 * De momento solo "escucha": cada vez que un mob termina de aparecer en el mundo, NeoForge lanza un
 * FinalizeSpawnEvent y nosotros escribimos en el log que mob es y como ha aparecido.
 */
public final class EliteSpawns {

    /** Probabilidad de que un monstruo que aparece solo (de noche, en cuevas...) sea elite: 0.06 = 6 %. */
    public static final float ELITE_CHANCE = 0.06f;

    /**
     * NeoForge llama a este metodo por cada mob que aparece (lo apuntamos en el constructor de MagusElites).
     * Es "static" porque no necesita guardar datos propios: todo lo que necesita viene dentro del evento.
     *
     * @param event el evento: trae el mob, el mundo, la posicion y el motivo de la aparicion.
     */
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        // El mob que acaba de aparecer (zombi, vaca, esqueleto...).
        Mob mob = event.getEntity();


        if (!(mob instanceof Monster)) {
            return;
        }

        // Su id, por ejemplo "minecraft:zombie". Los mobs de otros mods tienen su propio id ("othermod:goblin").
        ResourceLocation id = EntityType.getKey(mob.getType());

        // Por que ha aparecido: NATURAL (de noche, en cuevas...), SPAWNER, COMMAND (/summon), SPAWN_EGG...
        MobSpawnType motivo = event.getSpawnType();

        // 1. Nivel: TODOS los monstruos, elites o no. event.getLevel().getLevel() = el mundo donde aparece.
        int level = MobLevels.currentLevel(event.getLevel().getLevel());
        MobLevels.applyLevel(mob, level);

        // 2. Elite: encima del nivel. ¿Le toca serlo?
        boolean hacerElite;
        if (motivo == MobSpawnType.COMMAND) {
            hacerElite = true;                  // /summon: siempre, para poder probar
        } else if (motivo == MobSpawnType.NATURAL) {
            // nextFloat() da un decimal al azar entre 0 y 1: es menor que 0.06 el 6 % de las veces.
            hacerElite = mob.getRandom().nextFloat() < ELITE_CHANCE;
        } else {
            hacerElite = false;                 // spawners, huevos...: nada (asi no hay granjas de elites)
        }

        if (hacerElite) {
            EliteTier tier = EliteTier.random(mob.getRandom());
            Elites.makeElite(mob, tier);
            MagusElites.LOGGER.info("¡{} es ahora {}!", id, tier.displayName);
        }

        // 3. Con todos los bonus puestos: llenarle la vida (subir la vida maxima NO cura) y ponerle el nombre.
        mob.setHealth(mob.getMaxHealth());
        Elites.updateName(mob);

        MagusElites.LOGGER.info("Ha aparecido {} nivel {} ({}) con {} de vida", id, level, motivo, mob.getMaxHealth());
    }
}
