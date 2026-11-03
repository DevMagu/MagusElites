package com.magu.maguselites;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * La zona segura: un circulo alrededor del spawn del mundo donde no aparecen elites naturales, para que los
 * jugadores nuevos no se encuentren un Legendario nada mas empezar. El radio esta en la config (safeZoneRadius).
 *
 * Solo cuenta la distancia en horizontal (x y z): da igual si estas en una cueva profunda o en lo alto de un monte.
 */
public final class SafeZone {

    /** ¿Esta este punto (x, z) del mundo "level" dentro de la zona segura? */
    public static boolean isInside(ServerLevel level, double x, double z) {
        int radius = MagusConfig.SAFE_ZONE_RADIUS.getAsInt();
        if (radius <= 0) {
            return false;       // 0 = sin zona segura
        }

        double centerX;
        double centerZ;
        if (level.dimension() == Level.END) {
            // En el End se mide desde el centro (0, 0), donde esta la isla del dragon.
            centerX = 0;
            centerZ = 0;
        } else {
            // En el resto, desde el spawn del Overworld. getSharedSpawnPos() = donde aparecen los jugadores nuevos.
            BlockPos spawn = level.getServer().overworld().getSharedSpawnPos();
            centerX = spawn.getX();
            centerZ = spawn.getZ();

            // coordinateScale: cuantos bloques del Overworld vale un bloque de esta dimension. En el Nether es 8
            // (como los portales: 100 bloques alli = 800 aqui); en el Overworld es 1. Lo pasamos a "bloques del
            // Overworld" para compararlo con el spawn.
            double scale = level.dimensionType().coordinateScale();
            x = x * scale;
            z = z * scale;
        }

        // Pitagoras: la distancia en linea recta es la raiz de (dx² + dz²).
        double dx = x - centerX;
        double dz = z - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        return distance < radius;
    }
}
