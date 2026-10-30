package com.magu.maguselites;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Particulas alrededor de los elites, del color de su rango: verde (Strong), rojo (Elite) y amarillo (Legendary).
 *
 * Las crea el servidor y se las manda a los jugadores cercanos (hasta 32 bloques), asi que se ven sin instalar nada.
 */
public final class EliteParticles {
    /** Cada cuantos ticks salen particulas (5 ticks = 4 veces por segundo). */
    private static final int EVERY_TICKS = 5;

    /** NeoForge llama a esto despues de cada tick de cada entidad (apuntado en MagusElites). */
    public static void onEntityTick(EntityTickEvent.Post event) {
        
        // Si en la config estan desactivadas, no hacemos nada.
        if (!MagusConfig.SHOW_PARTICLES.getAsBoolean()) {
            return;
        }
        // "mob.level() instanceof ServerLevel level" hace dos cosas: comprueba que estamos en el servidor
        // (en el cliente el mundo es un ClientLevel) y nos da el mundo ya como ServerLevel para usarlo abajo.
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (mob.tickCount % EVERY_TICKS != 0) {
            return;
        }
        EliteTier tier = Elites.getTier(mob);
        if (tier == null || mob.isInvisible()) {
            return;     // no es elite, o es invisible (las particulas lo delatarian)
        }

        // sendParticles(particula, x, y, z, cuantas, dispersion x, dispersion y, dispersion z, velocidad)
        // Salen a media altura del mob y repartidas por su cuerpo: getBbWidth/getBbHeight = ancho y alto del mob.
        level.sendParticles(tier.particle,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                tier.particleCount,
                mob.getBbWidth() * 0.6, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.6,
                0.0);
    }
}
