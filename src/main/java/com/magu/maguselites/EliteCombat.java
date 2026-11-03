package com.magu.maguselites;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Lo que hace cada poder de combate (ElitePower).
 *
 * - Los de ATAQUE y Espinoso/Invocador funcionan con LivingDamageEvent.Post: "alguien acaba de recibir daño".
 *   Ahi miramos quien pego (event.getSource().getEntity()) y quien lo recibio (event.getEntity()).
 * - Teletransportista funciona con el tick, como el brillo y las particulas.
 */
public final class EliteCombat {

    // ---- numeros de cada poder (en segundos se multiplica por 20: 20 ticks = 1 segundo) ----
    private static final int POISON_TICKS = 5 * 20;
    private static final int FIRE_SECONDS = 4;
    private static final int FREEZE_TICKS = 4 * 20;
    private static final double HURL_STRENGTH = 1.2;
    private static final double HURL_UP = 0.6;
    /** Vampirico: se cura la mitad del daño que hace. */
    private static final float VAMPIRE_HEAL = 0.5F;
    /** Espinoso: devuelve el 30 % del daño que recibe. */
    private static final float THORNS_RETURN = 0.3F;
    /** Invocador: 25 % de probabilidad al recibir un golpe, y como mucho una vez cada 10 segundos. */
    private static final float SUMMON_CHANCE = 0.25F;
    private static final long SUMMON_COOLDOWN_TICKS = 10 * 20;
    private static final String LAST_SUMMON_KEY = "magus_elites_last_summon";
    /** Teletransportista: lo intenta cada 2 segundos si estas a mas de 10 bloques, con un 50 % de probabilidad. */
    private static final int BLINK_EVERY_TICKS = 2 * 20;
    private static final double BLINK_MIN_DISTANCE = 10.0;

    /** Despues de que alguien reciba daño (apuntado en MagusElites). */
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        float damage = event.getNewDamage();

        // Si el golpe no ha hecho daño (escudo, armadura...), o estamos en el cliente, no hacemos nada.
        if (damage <= 0 || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        // El daño de espinas no activa poderes: si no, dos elites espinosos se devolverian el daño para siempre.
        if (source.is(DamageTypes.THORNS)) {
            return;
        }

        // 1. ¿Ha pegado un elite? -> poderes de ataque.
        if (source.getEntity() instanceof Mob attacker && attacker != victim && Elites.isElite(attacker)) {
            onEliteAttack(attacker, victim, damage);
        }

        // 2. ¿Le han pegado a un elite? -> Espinoso e Invocador.
        if (victim instanceof Mob elite && Elites.isElite(elite)
                && source.getEntity() instanceof LivingEntity hitter && hitter != elite) {
            onEliteHurt(level, elite, hitter, damage);
        }
    }

    /** Poderes de ataque: el elite "attacker" acaba de hacerle "damage" de daño a "victim". */
    private static void onEliteAttack(Mob attacker, LivingEntity victim, float damage) {
        if (ElitePower.VENOMOUS.has(attacker)) {
            // Nivel de veneno: 0 = Poison I, 1 = Poison II.
            int amplifier = 0;
            // Los legendarios envenenan mas fuerte.
            if (Elites.getTier(attacker) == EliteTier.LEGENDARY) {
                amplifier = 1;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, amplifier), attacker);
        }
        if (ElitePower.BLAZING.has(attacker)) {
            victim.igniteForSeconds(FIRE_SECONDS);
        }
        if (ElitePower.FREEZING.has(attacker)) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS, 1), attacker);
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, FREEZE_TICKS, 0), attacker);
        }
        if (ElitePower.HURLING.has(attacker)) {
            // knockback empuja en direccion contraria al atacante; luego le sumamos un impulso hacia arriba.
            victim.knockback(HURL_STRENGTH, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
            victim.setDeltaMovement(victim.getDeltaMovement().add(0, HURL_UP, 0));
            // hurtMarked = "avisa al jugador de que se ha movido": sin esto, el jugador no sale volando en su pantalla.
            victim.hurtMarked = true;
        }
        if (ElitePower.VAMPIRIC.has(attacker)) {
            attacker.heal(damage * VAMPIRE_HEAL);
        }
    }

    /** Poderes de defensa: alguien ("hitter") le acaba de hacer "damage" de daño al elite. */
    private static void onEliteHurt(ServerLevel level, Mob elite, LivingEntity hitter, float damage) {
        if (ElitePower.THORNED.has(elite)) {
            // Daño de tipo "espinas" que viene del elite (como el encantamiento Espinas).
            hitter.hurt(level.damageSources().thorns(elite), damage * THORNS_RETURN);
        }

        // Invocador: solo contra jugadores (si no, dos mobs peleando llenarian el mundo de mobs).
        if (ElitePower.SUMMONING.has(elite) && hitter instanceof Player player) {
            long now = level.getGameTime();
            long lastSummon = elite.getPersistentData().getLong(LAST_SUMMON_KEY);
            if (now - lastSummon >= SUMMON_COOLDOWN_TICKS && elite.getRandom().nextFloat() < SUMMON_CHANCE) {
                elite.getPersistentData().putLong(LAST_SUMMON_KEY, now);
                summonHelpers(level, elite, player);
            }
        }
    }

    /** Invoca 1 o 2 mobs normales del mismo tipo que el elite al lado de el, y los manda a por el jugador. */
    private static void summonHelpers(ServerLevel level, Mob elite, Player player) {
        int count = 1 + elite.getRandom().nextInt(2);   // 1 o 2
        for (int i = 0; i < count; i++) {
            BlockPos pos = elite.blockPosition().offset(elite.getRandom().nextInt(5) - 2, 0, elite.getRandom().nextInt(5) - 2);
            // MOB_SUMMONED: nuestro EliteSpawns les pondra nivel, pero nunca los hara elite (solo NATURAL o COMMAND).
            if (elite.getType().spawn(level, pos, MobSpawnType.MOB_SUMMONED) instanceof Mob helper) {
                helper.setTarget(player);
            }
        }
    }

    /** Teletransportista: en cada tick de cada entidad (apuntado en MagusElites). */
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (mob.tickCount % BLINK_EVERY_TICKS != 0 || !ElitePower.BLINKING.has(mob)) {
            return;
        }
        LivingEntity target = mob.getTarget();      // a quien esta persiguiendo (null si a nadie)
        if (target == null || !target.isAlive() || mob.distanceTo(target) < BLINK_MIN_DISTANCE
                || mob.getRandom().nextFloat() >= 0.5F) {
            return;
        }

        // Probamos hasta 8 sitios al azar a 3 bloques o menos del objetivo. randomTeleport busca suelo firme debajo
        // y devuelve false si no cabe; con "true" se ven las particulas moradas, como un enderman.
        for (int i = 0; i < 8; i++) {
            double x = target.getX() + (mob.getRandom().nextDouble() - 0.5) * 6;
            double z = target.getZ() + (mob.getRandom().nextDouble() - 0.5) * 6;
            if (mob.randomTeleport(x, target.getY(), z, true)) {
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                        mob.getSoundSource(), 1.0F, 1.0F);
                return;
            }
        }
    }
}
