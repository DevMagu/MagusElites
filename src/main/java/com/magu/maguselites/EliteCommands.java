package com.magu.maguselites;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Los comandos del mod:
 *   /elite spawn <rango> <mob> [poder] [poder] [poder]   (solo OP) invoca un elite a tu lado
 *   /elite info                                          (todos) rango, nivel, vida y poderes del mob al que miras
 *   /elite level                                         (todos) dia del mundo y nivel de los monstruos
 *
 * Minecraft hace los comandos con una libreria llamada Brigadier: un comando es un "arbol". Cada palabra fija es un
 * literal ("elite", "spawn") y cada hueco que rellena el jugador es un argument ("rango", "mob"). executes() dice
 * que hacer si el comando acaba en ese punto; then() cuelga la siguiente palabra.
 *
 * Solo usamos argumentos de vanilla (palabras y ids de mobs): asi los clientes sin el mod entienden el comando.
 */
public final class EliteCommands {

    /** Hasta donde llega /elite info (en bloques). */
    private static final double INFO_RANGE = 32.0;

    /** NeoForge llama a esto cuando el servidor prepara sus comandos (apuntado en MagusElites). */
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("elite")
                // /elite spawn: requires() esconde el comando a quien no tenga permiso 2 (OP, como /give).
                .then(Commands.literal("spawn")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("tier", StringArgumentType.word())
                                // suggests() = lo que sale al pulsar TAB
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(tierNames(), builder))
                                .then(Commands.argument("mob", ResourceArgument.resource(event.getBuildContext(), Registries.ENTITY_TYPE))
                                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        // Se puede acabar aqui (poderes al azar) o seguir con 1, 2 o 3 poderes.
                                        .executes(context -> spawn(context, 0))
                                        .then(powerArgument("power1")
                                                .executes(context -> spawn(context, 1))
                                                .then(powerArgument("power2")
                                                        .executes(context -> spawn(context, 2))
                                                        .then(powerArgument("power3")
                                                                .executes(context -> spawn(context, 3))))))))
                // /elite info y /elite level: sin requires(), los puede usar cualquiera.
                .then(Commands.literal("info").executes(EliteCommands::info))
                .then(Commands.literal("level").executes(EliteCommands::level)));
    }

    /** Un hueco para un poder, con los nombres de los poderes al pulsar TAB. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> powerArgument(String name) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(powerNames(), builder));
    }

    /**
     * /elite spawn. Los comandos devuelven un numero: 1 = ha funcionado, 0 = no (lo usan los bloques de comandos).
     *
     * @param powerCount cuantos poderes se han escrito (0 a 3).
     */
    private static int spawn(CommandContext<CommandSourceStack> context, int powerCount) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        EliteTier tier = findTier(StringArgumentType.getString(context, "tier"));
        if (tier == null) {
            source.sendFailure(Component.literal("Unknown tier. Use strong, elite or legendary."));
            return 0;
        }

        List<ElitePower> powers = new ArrayList<>();
        for (int i = 1; i <= powerCount; i++) {
            String name = StringArgumentType.getString(context, "power" + i);
            ElitePower power = findPower(name);
            if (power == null) {
                source.sendFailure(Component.literal("Unknown power: " + name));
                return 0;
            }
            if (!powers.contains(power)) {      // si alguien escribe el mismo dos veces, cuenta una
                powers.add(power);
            }
        }

        // Lo hacemos aparecer como MOB_SUMMONED: nuestro EliteSpawns le pone el nivel pero NO lo hace elite al azar
        // (eso solo pasa con NATURAL y COMMAND). Asi el rango y los poderes los elegimos nosotros justo despues.
        EntityType<?> type = ResourceArgument.getSummonableEntityType(context, "mob").value();
        ServerLevel level = source.getLevel();
        Entity entity = type.spawn(level, BlockPos.containing(source.getPosition()), MobSpawnType.MOB_SUMMONED);

        if (!(entity instanceof Mob mob) || !EliteTags.isAffected(mob)) {
            if (entity != null) {
                entity.discard();               // lo quitamos: no queremos dejar una vaca a medias
            }
            source.sendFailure(Component.literal("That mob can't be an elite (only monsters, and not the blacklist)."));
            return 0;
        }

        if (powers.isEmpty()) {
            Elites.makeElite(mob, tier);                // poderes al azar segun el rango
        } else {
            Elites.makeElite(mob, tier, powers);        // los que ha escrito el jugador
        }
        mob.setHealth(mob.getMaxHealth());
        Elites.updateName(mob);

        // sendSuccess(texto, true): el "true" tambien avisa a los demas OP (como hace /summon).
        source.sendSuccess(() -> Component.literal("Spawned ").append(mob.getDisplayName()), true);
        return 1;
    }

    /** /elite info: el mob al que mira el jugador. */
    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        // getPlayerOrException: si lo escribe la consola o un bloque de comandos (no hay jugador), da error.
        ServerPlayer player = source.getPlayerOrException();

        Mob mob = lookedAtMob(player);
        if (mob == null) {
            source.sendFailure(Component.literal("Look at a mob first (up to " + (int) INFO_RANGE + " blocks)."));
            return 0;
        }

        EliteTier tier = Elites.getTier(mob);
        MutableComponent text = mob.getType().getDescription().copy();
        if (tier != null) {
            text.append(Component.literal(" - " + tier.displayName).withStyle(tier.color));
        } else {
            text.append(" - not an elite");
        }
        text.append(", level " + MobLevels.getLevel(mob));
        // Math.round redondea: 51.6 de vida se ve como 52.
        text.append("\nHealth: " + Math.round(mob.getHealth()) + " / " + Math.round(mob.getMaxHealth()));

        List<ElitePower> powers = ElitePower.of(mob);
        if (!powers.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (ElitePower power : powers) {
                names.add(power.displayName);
            }
            // String.join pega los textos de la lista con ", " entre medias: "Venomous, Blazing".
            text.append("\nPowers: " + String.join(", ", names));
        }

        source.sendSuccess(() -> text, false);
        return 1;
    }

    /** /elite level: en que dia va el mundo y que nivel tienen los monstruos que aparecen ahora. */
    private static int level(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel overworld = source.getServer().overworld();

        long day = MobLevels.currentDay(overworld);
        int mobLevel = MobLevels.currentLevel(overworld);
        // En el nivel maximo ya no suben mas: no tiene sentido decir cuantos dias faltan
        if (mobLevel >= MagusConfig.MAX_LEVEL.getAsInt()){
            source.sendSuccess(() -> Component.literal("Day " + (day + 1) + ": monsters are at the max level (" + mobLevel + ")."), false);
            return 1;
        }
        int daysPerLevel = MagusConfig.DAYS_PER_LEVEL.getAsInt();
        // % = el resto de la division: en el dia 7 con 3 dias por nivel, 7 % 3 = 1 → faltan 3 - 1 = 2 dias.
        long daysLeft = daysPerLevel - day % daysPerLevel;

        // Para las personas el primer dia es el 1, no el 0: por eso "day + 1".
        source.sendSuccess(() -> Component.literal("Day " + (day + 1) + ": monsters are level " + mobLevel
                + ". Next level in " + daysLeft + " day(s)."), false);
        return 1;
    }

    /**
     * El mob que hay delante de la mira del jugador, o null si no hay ninguno. No atraviesa paredes: primero
     * buscamos el bloque que tiene delante (pick) y solo miramos mobs hasta ahi.
     */
    private static Mob lookedAtMob(ServerPlayer player) {
        Vec3 eyes = player.getEyePosition();
        Vec3 end = player.pick(INFO_RANGE, 1.0F, false).getLocation();
        // La "caja" donde buscar: desde el jugador hasta el final de la mirada, un poco mas ancha.
        AABB area = player.getBoundingBox().expandTowards(end.subtract(eyes)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eyes, end, area,
                entity -> entity instanceof Mob, INFO_RANGE * INFO_RANGE);
        if (hit != null && hit.getEntity() instanceof Mob mob) {
            return mob;
        }
        return null;
    }

    // ---- nombres para las sugerencias del TAB, y buscar por nombre lo que ha escrito el jugador ----

    private static List<String> tierNames() {
        List<String> names = new ArrayList<>();
        for (EliteTier tier : EliteTier.values()) {
            names.add(tier.configName);
        }
        return names;
    }

    private static List<String> powerNames() {
        List<String> names = new ArrayList<>();
        for (ElitePower power : ElitePower.values()) {
            names.add(power.configName);
        }
        return names;
    }

    /** equalsIgnoreCase: compara sin mirar mayusculas, asi "Legendary" y "legendary" valen igual. */
    private static EliteTier findTier(String name) {
        for (EliteTier tier : EliteTier.values()) {
            if (tier.configName.equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return null;
    }

    private static ElitePower findPower(String name) {
        for (ElitePower power : ElitePower.values()) {
            if (power.configName.equalsIgnoreCase(name)) {
                return power;
            }
        }
        return null;
    }
}
