package com.magu.maguselites;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Brillo de los Legendarios, pero solo para los jugadores que estan a 30 bloques o menos.
 *
 * El brillo normal (setGlowingTag) es igual para todo el mundo: o lo ven todos o nadie. Para que cada jugador vea
 * algo distinto, el servidor le manda a CADA jugador su propio mensaje (un "paquete") diciendo si ese mob brilla
 * o no. El mob en el servidor no brilla de verdad: solo se lo "contamos" a los que estan cerca.
 */
public final class LegendaryGlow {
    /** Distancia maxima (en bloques) a la que se ve el brillo. */
    public static final double GLOW_DISTANCE = 20.0;

    /** Cada cuantos ticks se comprueba (10 ticks = medio segundo). Cada tick seria demasiado trabajo para nada. */
    private static final int CHECK_EVERY_TICKS = 10;

    /**
     * Las "banderas" de una entidad: un byte (8 interruptores de si/no) que el servidor manda al cliente: ardiendo,
     * agachado, invisible, brillando... Vanilla lo tiene como "protected" (no podemos usarlo desde fuera), pero es
     * el dato numero 0 de toda entidad, asi que creamos uno igual.
     */
    private static final EntityDataAccessor<Byte> SHARED_FLAGS = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
    /** El interruptor del brillo es el bit 6: 0b0100_0000 = 64. */
    private static final byte GLOWING_BIT = 1 << 6;

    /** NeoForge llama a esto despues de cada tick de cada entidad (20 veces por segundo). */
    public static void onEntityTick(EntityTickEvent.Post event) {
        // Solo nos interesan los mobs, y solo en el servidor (en un mundo de un jugador el cliente tambien tickea).
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        // % es el resto de la division: tickCount % 10 == 0 pasa una vez de cada 10 ticks.
        if (mob.tickCount % CHECK_EVERY_TICKS != 0) {
            return;
        }
        if (Elites.getTier(mob) != EliteTier.LEGENDARY) {
            return;
        }

        // Las banderas de verdad del mob (por si esta ardiendo, o brilla por una flecha espectral...).
        byte realFlags = mob.getEntityData().get(SHARED_FLAGS);

        for (Player player : mob.level().players()) {
            // Distancia al cuadrado: es mas rapido que la distancia normal (no hace raiz cuadrada),
            // asi que la comparamos con 30 x 30 = 900.
            boolean near = mob.distanceToSqr(player) <= GLOW_DISTANCE * GLOW_DISTANCE;

            // | enciende el bit del brillo sin tocar los demas. Si esta lejos mandamos las banderas tal cual.
            byte flags = near ? (byte) (realFlags | GLOWING_BIT) : realFlags;

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityDataPacket(mob.getId(),
                        List.of(SynchedEntityData.DataValue.create(SHARED_FLAGS, flags))));
            }
        }
    }
}
