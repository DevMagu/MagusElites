package com.magu.maguselites;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Aqui se decide que les pasa a los monstruos al aparecer: su nivel y si se convierten en elite.
 *
 * Cada vez que un mob termina de aparecer en el mundo, NeoForge lanza un FinalizeSpawnEvent y nos llama.
 */
public final class EliteSpawns {

    /**
     * NeoForge llama a este metodo por cada mob que aparece (lo apuntamos en el constructor de MagusElites).
     * Es "static" porque no necesita guardar datos propios: todo lo que necesita viene dentro del evento.
     *
     * @param event el evento: trae el mob, el mundo, la posicion y el motivo de la aparicion.
     */
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        // El mob que acaba de aparecer (zombi, vaca, esqueleto...).
        Mob mob = event.getEntity();

        // ¿Le afecta el mod? Enemigos si, animales no, y las listas del datapack mandan (ver EliteTags).
        if (!EliteTags.isAffected(mob)) {
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
            // nextDouble() da un decimal al azar entre 0 y 1: es menor que 0.06 el 6 % de las veces.
            // La probabilidad sale de la config: .getAsDouble() lee el valor actual del archivo.
            // Y nunca dentro de la zona segura del spawn (ver SafeZone): "!" = "no".
            hacerElite = !SafeZone.isInside(event.getLevel().getLevel(), mob.getX(), mob.getZ())
                    && mob.getRandom().nextDouble() < MagusConfig.ELITE_CHANCE.getAsDouble();
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
