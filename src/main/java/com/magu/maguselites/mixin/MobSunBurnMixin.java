package com.magu.maguselites.mixin;

import com.magu.maguselites.EliteTier;
import com.magu.maguselites.Elites;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin: un trozo de codigo que se "inyecta" DENTRO de una clase de Minecraft al arrancar el juego.
 *
 * Los zombis, esqueletos y phantoms preguntan cada tick a Mob.isSunBurnTick() "¿me quemo ahora?". No hay ningun
 * evento de NeoForge para esto, asi que nos colamos al principio de ese metodo: si el mob es Legendario,
 * respondemos "no" antes de que vanilla llegue a mirar el sol.
 *
 * Solo afecta al sol: si un Legendario pisa lava o le pegan con Aspecto ígneo, arde igual que siempre.
 */
@Mixin(Mob.class)
public abstract class MobSunBurnMixin {

    /**
     * @Inject = añadir codigo a un metodo de vanilla. method = cual; at HEAD = al principio del todo;
     * cancellable = podemos cortar el metodo y dar nosotros la respuesta.
     *
     * @param cir "CallbackInfoReturnable": el mando para devolver un valor en lugar de vanilla (setReturnValue).
     */
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void magusElites$legendariesIgnoreSun(CallbackInfoReturnable<Boolean> cir) {
        // Dentro de un mixin, "this" ES el Mob de vanilla. Java no lo sabe, asi que se lo decimos con este doble cast.
        Mob self = (Mob) (Object) this;

        if (Elites.getTier(self) == EliteTier.LEGENDARY) {
            cir.setReturnValue(false);   // "no te quemas" (y el resto del metodo de vanilla ya no se ejecuta)
        }
    }
}
