package com.magu.maguselites;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Listas de mobs que se pueden cambiar con un datapack, sin tocar el codigo (las usan los modpacks).
 *
 * Un "tag" es una lista con nombre guardada en un archivo JSON. Los nuestros estan en
 * src/main/resources/data/magus_elites/tags/entity_type/:
 * - blacklist.json: mobs que el mod NO toca nunca (ni nivel ni elite). Por defecto los jefes.
 * - whitelist.json: mobs que SI toca aunque no sean enemigos (por ejemplo, un mob raro de otro mod).
 *
 * Cualquier datapack puede añadir mobs a estas listas con un archivo en la misma ruta.
 */
public final class EliteTags {

    /** magus_elites:blacklist. TagKey = el "nombre" de la lista; el contenido lo carga Minecraft de los JSON. */
    public static final TagKey<EntityType<?>> BLACKLIST =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MagusElites.MOD_ID, "blacklist"));

    /** magus_elites:whitelist. */
    public static final TagKey<EntityType<?>> WHITELIST =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MagusElites.MOD_ID, "whitelist"));

    /** ¿Le afecta el mod a este mob (niveles y elites)? */
    public static boolean isAffected(Mob mob) {
        EntityType<?> type = mob.getType();

        // 1. La lista negra gana siempre: si esta ahi, fuera.
        if (type.is(BLACKLIST)) {
            return false;
        }
        // 2. La lista blanca deja pasar a cualquiera.
        if (type.is(WHITELIST)) {
            return true;
        }
        // 3. Si no esta en ninguna lista: todos los enemigos. "Enemy" lo llevan todos los mobs hostiles,
        // tambien los que NO son Monster: slimes, phantoms, ghasts, shulkers, hoglins...
        return mob instanceof Enemy;
    }
}
