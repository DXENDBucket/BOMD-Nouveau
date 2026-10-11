package dev.bomdnouveau;

import com.cerbon.bosses_of_mass_destruction.projectile.*;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.*;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.execution.ExecutionSessions;
import dev.bomdnouveau.api.Ability;
import dev.bomdnouveau.casting.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("bomd_nouveau") @PrefixGameTestTemplate(false)
public final class PayloadGameTests {
    private static Vec3 scene(GameTestHelper h) {
        var origin = h.absoluteVec(new Vec3(4,0,4));
        return new Vec3(origin.x, h.getLevel().getMaxBuildHeight()-48, origin.z);
    }
    private static ServerPlayer player(GameTestHelper h) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BmdStudent"),false);
        var p = new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,p,cookie) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener) {}
        };
        p.setGameMode(GameType.SURVIVAL); p.setPos(scene(h).add(-10,0,0));
        h.getLevel().addNewPlayer(p);
        var mana = CapabilityRegistry.getMana(p); mana.setMaxMana(10000); mana.setMana(10000);
        var base = BlockPos.containing(scene(h));
        for (int x=-9;x<=9;x++) for(int z=-9;z<=9;z++) for(int y=-1;y<=5;y++)
            h.getLevel().setBlock(base.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        return p;
    }
    private static LivingEntity cow(GameTestHelper h, Vec3 position) {
        var target = EntityType.COW.create(h.getLevel());
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);target.setHealth(500);
        target.setNoAi(true);target.setNoGravity(true);target.setPos(position);h.getLevel().addFreshEntity(target);return target;
    }
    private static BlockHitResult block(Vec3 point) { return new BlockHitResult(point,Direction.UP,BlockPos.containing(point),false); }
    private static SpellResolver resolver(GameTestHelper h, ServerPlayer player, Spell spell) {
        return new SpellResolver(new SpellContext(h.getLevel(),spell,player,LivingCaster.from(player),ItemStack.EMPTY));
    }
    private static void clean(GameTestHelper h, ServerPlayer player) {
        for(var entity:h.getLevel().getEntities((Entity)null,new AABB(scene(h),scene(h)).inflate(24),e->e!=player)) entity.discard();
        ExecutionSessions.tick();
    }
    private static void close(GameTestHelper h,double actual,double expected,String message) { h.assertTrue(Math.abs(actual-expected)<.001,message+": "+actual+" != "+expected); }
    @GameTest(template="empty",batch="bmd_damage",timeoutTicks=80)
    public static void nativeDamageAndConfiguredAmplification(GameTestHelper h) {
        var p=player(h);var amp=EffectHarm.INSTANCE.AMP_VALUE.get();
        try {
            close(h,NativePayloads.amplification(),amp,"Reads Harm's actual configured amplifier");
            EffectHarm.INSTANCE.AMP_VALUE.set(3.0);
            close(h,NativePayloads.damage(Ability.MISSILE,1)-NativePayloads.damage(Ability.MISSILE,0),3,"Changes to Harm amplification propagate");
            EffectHarm.INSTANCE.AMP_VALUE.set(amp);
            for(var ability:List.of(Ability.MISSILE,Ability.PETAL)) {
                var target=cow(h,scene(h).add(0,1,3));var hit=new EntityHitResult(target);
                NativePayloads.impact(ability,h.getLevel(),CastContext.detached(p,hit,null),hit,NativePayloads.damage(ability,1),1);
                close(h,500-target.getHealth(),NativePayloads.damage(ability,1),"Fixed Boss hard-mode damage plus Harm amplification: "+ability);
                if(ability==Ability.MISSILE) h.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Native missile slow callback retained");
                target.discard();
            }
            var nativeTarget=cow(h,scene(h).add(0,1,3));
            var ordinary=new MagicMissileProjectile(p,h.getLevel(),e->{},List.of());
            ordinary.entityHit(new EntityHitResult(nativeTarget));
            close(h,500-nativeTarget.getHealth(),p.getAttributeValue(Attributes.ATTACK_DAMAGE),"Unmanaged native projectiles keep original damage");
            var sample=NativePayloads.create(Ability.PETAL,h.getLevel(),CastContext.detached(p,null,null),0);
            h.assertTrue(!NativePayloads.source(sample,false).scalesWithDifficulty(),"Managed damage cannot be difficulty-scaled a second time");
            h.succeed();
        } finally { EffectHarm.INSTANCE.AMP_VALUE.set(amp);clean(h,p);p.discard();ExecutionSessions.tick(); }
    }
    @GameTest(template="empty",batch="bmd_projectiles",timeoutTicks=80)
    public static void allPayloadsContinueAndPayPerRepeatedHit(GameTestHelper h) {
        var p=player(h);var mana=CapabilityRegistry.getMana(p);
        try {
            for(var glyph:GlyphCatalog.PROJECTILES) for(boolean repeated:List.of(false,true)) {
                clean(h,p);mana.setMana(10000);var marker=new Marker();
                var cast=resolver(h,p,new Spell(MethodProjectile.INSTANCE,glyph,marker));cast.spellContext.nextPart();
                EntityProjectileSpell carrier=repeated?new EntityOrbitProjectile(h.getLevel(),cast,p):new EntityProjectileSpell(h.getLevel(),cast);
                carrier.setPos(scene(h).add(0,2,0));carrier.setDeltaMovement(new Vec3(0,0,.75));
                int fee=BomdResources.quote(cast.spellContext,glyph.ability,List.of()).amount();
                h.assertTrue(ProjectileDispatch.convert(carrier),"Core dispatches each BOMD projectile");
                var visual=h.getLevel().getEntitiesOfClass(BaseThrownItemProjectile.class,new AABB(scene(h),scene(h)).inflate(24),e->((ManagedProjectile)e).bomdNouveau$mode()==1).get(0);
                h.assertTrue(visual.getOwner()==p && ((ManagedProjectile)visual).confluxTrajectory()==carrier,"Native owner and public trajectory proxy retained");
                var before=visual.position();visual.tick();close(h,visual.position().distanceTo(before),0,"Native motion suppressed to avoid a second movement");
                close(h,mana.getCurrentMana(),10000-(repeated?0:fee),"One-shot launch pays once; repeated launch waits for impact");
                var hit=block(scene(h).add(0,0,3));
                ArsSpellAccess.resolver(carrier).onResolveEffect(h.getLevel(),hit);
                h.assertTrue(marker.hits==1,"Native aftermath still continues the Ars phrase");
                close(h,mana.getCurrentMana(),10000-fee,"One paid activation");
                if(glyph.ability==Ability.SPORE) h.assertTrue(!h.getLevel().getEntitiesOfClass(SporeBallProjectile.class,new AABB(hit.getLocation(),hit.getLocation()).inflate(3),e->e.impacted).isEmpty(),"Landed spore remains visible during the native explosion delay");
                if(repeated) {
                    ArsSpellAccess.resolver(carrier).onResolveEffect(h.getLevel(),hit);
                    h.assertTrue(marker.hits==2,"Repeated impacts create fresh native aftermath");
                    close(h,mana.getCurrentMana(),10000-2*fee,"Repeated activation pays again");
                    mana.setMana(0);ArsSpellAccess.resolver(carrier).onResolveEffect(h.getLevel(),hit);
                    h.assertTrue(marker.hits==2 && visual.isRemoved(),"Empty mana stops repeated payloads and continuation");
                }
            }
            clean(h,p);mana.setMana(10000);
            var comet=GlyphCatalog.PROJECTILES.stream().filter(g->g.ability==Ability.COMET).findFirst().orElseThrow();
            var shootDownMarker=new Marker();var cometCast=resolver(h,p,new Spell(MethodProjectile.INSTANCE,comet,shootDownMarker));cometCast.spellContext.nextPart();
            var orbitComet=new EntityOrbitProjectile(h.getLevel(),cometCast,p);orbitComet.setPos(scene(h).add(0,3,0));
            h.assertTrue(ProjectileDispatch.convert(orbitComet),"Core creates shootable orbit comet");
            var cometVisual=h.getLevel().getEntitiesOfClass(com.cerbon.bosses_of_mass_destruction.projectile.comet.CometProjectile.class,new AABB(scene(h),scene(h)).inflate(24)).get(0);
            cometVisual.hurt(h.getLevel().damageSources().playerAttack(p),1);
            h.assertTrue(cometVisual.isRemoved() && orbitComet.isRemoved() && shootDownMarker.hits==1,"Native shoot-down resolves and removes the carrier");
            close(h,mana.getCurrentMana(),9900,"Shoot-down pays repeated projectile fee instead of bypassing billing");
            clean(h,p);mana.setMana(0);
            var cast=resolver(h,p,new Spell(MethodProjectile.INSTANCE,GlyphCatalog.PROJECTILES.get(0)));cast.spellContext.nextPart();
            var unfunded=new EntityProjectileSpell(h.getLevel(),cast);unfunded.setPos(scene(h));
            h.assertTrue(ProjectileDispatch.convert(unfunded),"Unfunded payload consumes the attempted native Ars spawn");
            h.assertTrue(h.getLevel().getEntitiesOfClass(BaseThrownItemProjectile.class,new AABB(scene(h),scene(h)).inflate(24)).isEmpty(),"Unfunded cast creates no replacement");
            p.setGameMode(GameType.CREATIVE);
            var creative=new EntityProjectileSpell(h.getLevel(),cast);creative.setPos(scene(h));
            h.assertTrue(ProjectileDispatch.convert(creative) && !h.getLevel().getEntitiesOfClass(BaseThrownItemProjectile.class,new AABB(scene(h),scene(h)).inflate(24)).isEmpty(),"Creative can cast with zero mana");
            h.succeed();
        } finally { clean(h,p);p.discard();ExecutionSessions.tick(); }
    }
    @GameTest(template="empty",batch="bmd_aftereffects",timeoutTicks=120)
    public static void independentAftereffectsAndNativeDelayedPoison(GameTestHelper h) {
        var p=player(h);var mana=CapabilityRegistry.getMana(p);var center=scene(h).add(.5,0,.5);
        var victim=cow(h,center.add(7,0,0));var before=victim.getHealth();
        var glyph=GlyphCatalog.EFFECTS.stream().filter(g->g.ability==Ability.SPORE_BURST).findFirst().orElseThrow();
        var cast=resolver(h,p,new Spell(MethodTouch.INSTANCE,glyph));cast.spellContext.nextPart();
        cast.onResolveEffect(h.getLevel(),block(center));
        close(h,mana.getCurrentMana(),10000-70,"Independent spore effect uses the same native payload and its own fee");
        h.runAfterDelay(45,()->{
            try {
                h.assertTrue(victim.getHealth()<before && victim.hasEffect(MobEffects.POISON),"Delayed native spore eruption retains actual damage and poison");
                h.assertTrue(h.getLevel().getEntitiesOfClass(SporeBallProjectile.class,new AABB(center,center).inflate(24),e->e.impacted).isEmpty(),"Native landed spore disappears after its delay");
                var target=cow(h,center.add(0,1,0));var blast=GlyphCatalog.EFFECTS.stream().filter(g->g.ability==Ability.COMET_BURST).findFirst().orElseThrow();
                mana.setMana(10000);var explode=resolver(h,p,new Spell(MethodTouch.INSTANCE,blast));explode.spellContext.nextPart();explode.onResolveEffect(h.getLevel(),block(center));
                h.assertTrue(target.getHealth()<500,"Independent comet effect preserves actual native explosion damage");
                close(h,mana.getCurrentMana(),10000-80,"Independent explosion cost");
                h.assertTrue(h.getLevel().getBlockState(BlockPos.containing(center).below()).is(Blocks.STONE),"Default comet explosion preserves terrain");
                for(var part:GlyphCatalog.ALL) h.assertTrue(h.getLevel().getRecipeManager().byKey(part.getRegistryName()).isPresent(),"Every glyph has a real native Scribe recipe");
                h.succeed();
            } finally {clean(h,p);p.discard();ExecutionSessions.tick();}
        });
    }
    private static final class Marker extends AbstractEffect {
        int hits;
        Marker(){super(CarrierRegistry.id("bomd_nouveau:glyph_test_marker"),"Marker");}
        public int getDefaultManaCost(){return 0;}public int getCastingCost(){return 0;}
        public SpellTier defaultTier(){return SpellTier.ONE;}public SpellTier getConfigTier(){return SpellTier.ONE;}
        public boolean isEnabled(){return true;}public Set<AbstractAugment> getCompatibleAugments(){return Set.of();}
        public void onResolve(HitResult hit,Level world,LivingEntity caster,SpellStats stats,SpellContext context,SpellResolver resolver){hits++;}
    }
}
