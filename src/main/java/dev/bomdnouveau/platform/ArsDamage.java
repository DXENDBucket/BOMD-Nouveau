package dev.bomdnouveau.platform;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import dev.arsconflux.api.context.CastContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import java.util.*;
import java.util.function.*;
import net.neoforged.neoforge.common.NeoForge;

/** Native hit types stay intact; only explicitly bound spell attacks enter Ars's damage event chain. */
public final class ArsDamage {
    private static final Map<Entity,CastContext> CONTEXTS=new WeakHashMap<>();
    private static final ThreadLocal<CastContext> EXPLOSION=new ThreadLocal<>();
    private static final ThreadLocal<Boolean> DISPATCHING=ThreadLocal.withInitial(()->false);
    @FunctionalInterface public interface Hit { boolean hurt(DamageSource source,float amount); }
    public static <T extends Entity> T bind(T entity,CastContext context) {
        if(context!=null) CONTEXTS.put(entity,context);return entity;
    }
    public static CastContext context(Entity entity) {

        return CONTEXTS.get(entity);
    }
    public static CastContext explosion() {return EXPLOSION.get();}
    public static <T> T scoped(CastContext context,Supplier<T> action) {
        var previous=EXPLOSION.get();EXPLOSION.set(context);
        try {return action.get();} finally {if(previous==null) EXPLOSION.remove();else EXPLOSION.set(previous);}
    }
    public static boolean hurt(CastContext context,Entity target,DamageSource source,float amount,Hit original) {
        if(context==null || target.level().isClientSide || DISPATCHING.get()) return original.hurt(source,amount);
        var caster=context.caster();
        if(caster==null || !caster.isAlive() || !Float.isFinite(amount) || amount<=0) return original.hurt(source,amount);
        double bonus=caster.getAttributes().hasAttribute(PerkAttributes.SPELL_DAMAGE_BONUS)
                ?caster.getAttributeValue(PerkAttributes.SPELL_DAMAGE_BONUS):0;
        float adjusted=(float)Math.max(0,amount+bonus);
        DISPATCHING.set(true);
        try {
            var pre=new SpellDamageEvent.Pre(source,caster,target,adjusted,context.ars());
            NeoForge.EVENT_BUS.post(pre);
            if(pre.isCanceled() || !Float.isFinite(pre.damage) || pre.damage<=0) return false;
            boolean hit=original.hurt(pre.damageSource,pre.damage);
            if(hit) NeoForge.EVENT_BUS.post(new SpellDamageEvent.Post(pre.damageSource,caster,target,pre.damage,context.ars()));
            return hit;
        } finally {DISPATCHING.remove();}
    }
    private ArsDamage() {}
}
