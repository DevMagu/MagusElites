package com.magu.maguselites;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Botin extra de los elites al morir.
 *
 * QUE sueltan no esta aqui, sino en las "loot tables": archivos JSON en
 * src/main/resources/data/magus_elites/loot_table/elites/ (strong.json, elite.json y legendary.json).
 * Como las listas de la leccion 7, cualquier datapack puede cambiarlas sin tocar el codigo.
 */
public final class EliteLoot {

    /** NeoForge llama a esto cuando un mob muere, justo antes de soltar sus objetos (apuntado en MagusElites). */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        EliteTier tier = Elites.getTier(mob);
        if (tier == null) {
            return;     // no es elite: solo suelta lo normal de vanilla
        }
        // "Recently hit" = un jugador le ha pegado en los ultimos 5 segundos. Es lo mismo que usa Minecraft para
        // dar experiencia: si muere por lava o caida sin que nadie le pegue (una granja), no hay botin especial.
        if (!event.isRecentlyHit()) {
            return;
        }

        // Buscamos la loot table de su rango (magus_elites:elites/legendary...) entre las que ha cargado el servidor.
        LootTable table = level.getServer().reloadableRegistries().getLootTable(tier.lootTable);

        // Los "parametros" son la informacion que la loot table puede consultar: quien murio, donde, de que...
        // La suerte (luck) es el nivel del mob menos 1: en los JSON, "bonus_rolls" da tiradas extra con suerte.
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, event.getSource())
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, event.getSource().getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, event.getSource().getDirectEntity())
                .withLuck(MobLevels.getLevel(mob) - 1)
                .create(LootContextParamSets.ENTITY);

        // Tiramos los dados de la loot table y añadimos cada objeto a lo que suelta el mob.
        for (ItemStack stack : table.getRandomItems(params)) {
            event.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY(), mob.getZ(), stack));
        }
    }
}
