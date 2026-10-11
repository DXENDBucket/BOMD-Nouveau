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
            close(h,mana.getCurrentMana(),10000-BomdResources.quote(cometCast.spellContext,Ability.COMET,List.of()).amount(),"Shoot-down pays repeated projectile fee instead of bypassing billing");
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
        close(h,mana.getCurrentMana(),10000-BomdResources.quote(cast.spellContext,Ability.SPORE_BURST,List.of()).amount(),"Independent spore effect uses the same native payload and its own fee");
        h.runAfterDelay(45,()->{
            try {
                h.assertTrue(victim.getHealth()<before && victim.hasEffect(MobEffects.POISON),"Delayed native spore eruption retains actual damage and poison");
                h.assertTrue(h.getLevel().getEntitiesOfClass(SporeBallProjectile.class,new AABB(center,center).inflate(24),e->e.impacted).isEmpty(),"Native landed spore disappears after its delay");
                var target=cow(h,center.add(0,1,0));var blast=GlyphCatalog.EFFECTS.stream().filter(g->g.ability==Ability.COMET_BURST).findFirst().orElseThrow();
                mana.setMana(10000);var explode=resolver(h,p,new Spell(MethodTouch.INSTANCE,blast));explode.spellContext.nextPart();explode.onResolveEffect(h.getLevel(),block(center));
                h.assertTrue(target.getHealth()<500,"Independent comet effect preserves actual native explosion damage");
                close(h,mana.getCurrentMana(),10000-BomdResources.quote(explode.spellContext,Ability.COMET_BURST,List.of()).amount(),"Independent explosion cost");
                h.assertTrue(h.getLevel().getBlockState(BlockPos.containing(center).below()).is(Blocks.STONE),"Default comet explosion preserves terrain");
                for(var part:GlyphCatalog.ALL) h.assertTrue(h.getLevel().getRecipeManager().byKey(part.getRegistryName()).isPresent(),"Every glyph has a real native Scribe recipe");
                h.succeed();
            } finally {clean(h,p);p.discard();ExecutionSessions.tick();}
        });
    }
    @GameTest(template="empty",batch="bmd_ground",timeoutTicks=340)
    public static void groundAttacksAndWallTeleport(GameTestHelper h) {
        var p=player(h); var mana=CapabilityRegistry.getMana(p); var origin=scene(h).add(.5,0,.5);
        var burstVictim=mobileCow(h,origin);
        effect(h,p,Ability.OBSIDIAN_BURST,block(origin),Vec3.ZERO);
        close(h,mana.getCurrentMana(),10000-dev.bomdnouveau.platform.BomdConfig.cost(Ability.OBSIDIAN_BURST),"Burst prepays one native eruption");
        h.runAfterDelay(45,()->{
            close(h,burstVictim.getHealth(),476,"Native ground burst uses fixed hard-mode attack damage");
            h.assertTrue(burstVictim.getY()>origin.y,"Burst launches a non-player victim");
            burstVictim.discard();
            var spikesVictim=mobileCow(h,origin);mana.setMana(10000);
            effect(h,p,Ability.OBSIDIAN_SPIKES,new EntityHitResult(spikesVictim),Vec3.ZERO);
            h.runAfterDelay(125,()->{
                close(h,spikesVictim.getHealth(),428,"All three native pursuit rounds hit for 24 damage");
                h.assertTrue(spikesVictim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && spikesVictim.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==2,"Spike rounds retain Slowness III");
                close(h,mana.getCurrentMana(),10000-dev.bomdnouveau.platform.BomdConfig.cost(Ability.OBSIDIAN_SPIKES),"Three-round effect is charged once");
                spikesVictim.discard();
                var waveVictim=mobileCow(h,origin.add(4,0,0));mana.setMana(10000);
                effect(h,p,Ability.FIRE_WAVE,block(origin),new Vec3(1,0,0));
                h.runAfterDelay(105,()->{
                    h.assertTrue(waveVictim.getHealth()<500,"Wave uses the captured incoming direction and native ground columns");
                    close(h,mana.getCurrentMana(),10000-dev.bomdnouveau.platform.BomdConfig.cost(Ability.FIRE_WAVE),"Wave sequence is charged once");
                    waveVictim.discard();
                    var base=BlockPos.containing(origin);
                    for(int x=1;x<=2;x++)for(int y=0;y<=3;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlock(base.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
                    p.setPos(origin);p.setYRot(-90);p.setXRot(0);mana.setMana(10000);
                    effect(h,p,Ability.EARTHDIVE,new EntityHitResult(p),Vec3.ZERO);
                    h.assertTrue(p.getX()>base.getX()+3,"Self earthdive uses native wall search and actually teleports the player");
                    close(h,mana.getCurrentMana(),10000-dev.bomdnouveau.platform.BomdConfig.cost(Ability.EARTHDIVE),"Successful teleport charges once");
                    p.setPos(origin);
                    for(int x=1;x<=2;x++)for(int y=0;y<=3;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlock(base.offset(x,y,z),Blocks.BEDROCK.defaultBlockState(),2);
                    double paid=mana.getCurrentMana();effect(h,p,Ability.EARTHDIVE,new EntityHitResult(p),Vec3.ZERO);
                    close(h,p.getX(),origin.x,"Native unbreakable wall prevents teleport");close(h,mana.getCurrentMana(),paid,"Failed earthdive costs no mana");
                    for(int x=1;x<=2;x++)for(int y=0;y<=3;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlock(base.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
                    p.setPos(origin.add(-8,0,0));var creature=cow(h,origin);
                    paid=mana.getCurrentMana();
                    effect(h,p,Ability.EARTHDIVE,new EntityHitResult(creature),new Vec3(1,0,0));
                    close(h,creature.getX(),origin.x,"Earthdive cannot move a non-allied creature");
                    close(h,mana.getCurrentMana(),paid,"Rejected non-allied target costs no mana");
                    var scoreboard=h.getLevel().getScoreboard();
                    var team=scoreboard.addPlayerTeam("earthdive_"+p.getId());
                    try {
                        scoreboard.addPlayerToTeam(p.getScoreboardName(),team);
                        scoreboard.addPlayerToTeam(creature.getScoreboardName(),team);
                        effect(h,p,Ability.EARTHDIVE,new EntityHitResult(creature),new Vec3(1,0,0));
                        h.assertTrue(creature.getX()>base.getX()+3 && p.getX()<origin.x,"Earthdive moves the selected ally rather than the caster");
                        close(h,mana.getCurrentMana(),paid-dev.bomdnouveau.platform.BomdConfig.cost(Ability.EARTHDIVE),"Successful ally teleport charges once");
                    } finally {scoreboard.removePlayerTeam(team);}
                    h.assertTrue(GlyphCatalog.EFFECTS.stream().filter(g->g.ability==Ability.EARTHDIVE).findFirst().orElseThrow().compatibleAugments.isEmpty(),"Earthdive does not advertise meaningless damage augments");
                    p.setPos(origin);p.setGameMode(GameType.CREATIVE);mana.setMana(0);
                    effect(h,p,Ability.EARTHDIVE,new EntityHitResult(p),Vec3.ZERO);
                    h.assertTrue(p.getX()>base.getX()+3 && mana.getCurrentMana()==0,"Creative earthdive is free");
                    clean(h,p);p.discard();ExecutionSessions.tick();h.succeed();
                });
            });
        });
    }
    @GameTest(template="empty",batch="bmd_laser",timeoutTicks=360)
    public static void laserChargeWallLagUpkeepAlliedHostAndCreative(GameTestHelper h) {
        var p=player(h);var origin=scene(h).add(.5,0,.5);p.setPos(origin);p.setYRot(-90);p.setXRot(0);
        int price=dev.bomdnouveau.platform.BomdConfig.cost(Ability.GAUNTLET_LASER);
        var budget=new java.util.concurrent.atomic.AtomicInteger(price);
        var victim=cow(h,origin.add(5,.7,0));var base=BlockPos.containing(origin);
        for(int y=0;y<4;y++)for(int z=-1;z<=1;z++)h.getLevel().setBlock(base.offset(3,y,z),Blocks.STONE.defaultBlockState(),2);
        laser(h,p,new EntityHitResult(p),Vec3.ZERO,budget);
        h.runAfterDelay(20,()->{
            close(h,victim.getHealth(),500,"Laser charge-up cannot hurt targets");
            close(h,budget.get(),price,"No upkeep is charged during warm-up");
        });
        h.runAfterDelay(35,()->{
            close(h,victim.getHealth(),500,"Opaque wall stops the active laser");
            close(h,budget.get(),0,"Active laser prepays one second even when blocked");
            for(int y=0;y<4;y++)for(int z=-1;z<=1;z++)h.getLevel().setBlock(base.offset(3,y,z),Blocks.AIR.defaultBlockState(),2);
            p.setYRot(0);
        });
        h.runAfterDelay(40,()->{
            close(h,500-victim.getHealth(),com.cerbon.bosses_of_mass_destruction.entity.BMDEntities.mobConfig.gauntletConfig.attack*.75*1.5,
                    "Turn lag keeps the former aim briefly; damage is native hard-mode laser baseline");
            h.assertTrue(victim.getLastDamageSource().getEntity()==p,"Actual caster owns beam damage");
        });
        h.runAfterDelay(65,()->{
            var stoppedHealth=victim.getHealth();p.setYRot(-90);
            h.runAfterDelay(30,()->{
                close(h,victim.getHealth(),stoppedHealth,"Depleted upkeep ends damage rather than continuing a free beam");
                victim.discard();
                var host=cow(h,origin);host.setYRot(-90);var target=cow(h,origin.add(5,.3,0));
                p.setPos(origin.add(-8,0,8));budget.set(price*3);
                laser(h,p,new EntityHitResult(host),new Vec3(0,0,-1),budget);
                close(h,budget.get(),price*3,"Non-allied hosts are rejected for free");
                var scoreboard=h.getLevel().getScoreboard();var team=scoreboard.addPlayerTeam("laser_"+p.getId());
                scoreboard.addPlayerToTeam(p.getScoreboardName(),team);scoreboard.addPlayerToTeam(host.getScoreboardName(),team);
                laser(h,p,new EntityHitResult(host),new Vec3(0,0,-1),budget);
                h.runAfterDelay(40,()->{
                    h.assertTrue(target.getHealth()<500 && target.getLastDamageSource().getEntity()==p,"Allied host eyes/facing override incoming direction; damage remains owned by caster");
                    close(h,host.getHealth(),500,"Beam never damages its emitting host");
                    host.discard();var after=target.getHealth();
                    h.runAfterDelay(10,()->{
                        close(h,target.getHealth(),after,"Host removal cancels beam");
                        scoreboard.removePlayerTeam(team);target.discard();p.setPos(origin);p.setYRot(180);p.setGameMode(GameType.CREATIVE);
                        budget.set(0);var creativeVictim=cow(h,origin.add(5,.7,0));
                        laser(h,p,block(p.getEyePosition()),new Vec3(1,0,0),budget);
                        h.runAfterDelay(40,()->h.assertTrue(creativeVictim.getHealth()<500,"Point trigger uses incoming trajectory even when caster faces elsewhere; creative starts at zero Mana"));
                        h.runAfterDelay(95,()->{
                            float finished=creativeVictim.getHealth();
                            close(h,budget.get(),0,"Creative upkeep remains free");
                            h.assertTrue(finished<450,"Beam repeatedly damages across its native lifetime");
                            h.runAfterDelay(20,()->{
                                close(h,creativeVictim.getHealth(),finished,"Natural end stops beam damage");
                                clean(h,p);p.discard();ExecutionSessions.tick();h.succeed();
                            });
                        });
                    });
                });
            });
        });
    }
    @GameTest(template="empty",batch="bmd_cooldowns",timeoutTicks=140)
    public static void confluxCooldownReleaseBoundariesAndEarthdiveResults(GameTestHelper h) {
        var p=player(h);var mana=CapabilityRegistry.getMana(p);
        var missile=GlyphCatalog.PROJECTILES.getFirst();var spell=new Spell(MethodProjectile.INSTANCE,missile);
        var source=dev.arsconflux.api.casting.ActiveCastSource.CASTER_TOOL;
        h.assertTrue(dev.arsconflux.api.casting.ActiveCasts.perform(spell,p,net.minecraft.world.InteractionHand.MAIN_HAND,source,
                ()->resolver(h,p,spell).onCast(ItemStack.EMPTY,h.getLevel()),false,false),"Real Ars dispatch is accepted through Conflux");
        close(h,BomdCooldowns.remaining(p,Ability.MISSILE),dev.bomdnouveau.platform.BomdConfig.cooldownTicks(Ability.MISSILE),"Accepted release starts the configured cooldown");
        double after=mana.getCurrentMana();var blockedCalls=new java.util.concurrent.atomic.AtomicInteger();
        h.assertTrue(!dev.arsconflux.api.casting.ActiveCasts.execute(new Spell(MethodProjectile.INSTANCE,missile),p,
                dev.arsconflux.api.casting.ActiveCastSource.WEAPON,()->{blockedCalls.incrementAndGet();return true;},false),"Switching recipe/weapon cannot bypass a glyph cooldown");
        close(h,blockedCalls.get(),0,"Cooldown denial happens before the action");close(h,mana.getCurrentMana(),after,"Denied active use spends no Mana");
        var pure=new Spell(MethodSelf.INSTANCE,new Marker());
        h.assertTrue(dev.arsconflux.api.casting.ActiveCasts.allowed(pure,p,source),"Pure Ars recipes are unaffected");
        var passive=resolver(h,p,spell);passive.spellContext.nextPart();
        var orbit=new EntityOrbitProjectile(h.getLevel(),passive,p);orbit.setPos(scene(h).add(0,3,0));
        h.assertTrue(ProjectileDispatch.convert(orbit),"A passive/repeated carrier still converts while its glyph cools down");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(BaseThrownItemProjectile.class,new AABB(scene(h),scene(h)).inflate(24)).isEmpty(),"Payload execution remains available outside a new active use");
        clean(h,p);

        var commits=new java.util.concurrent.atomic.AtomicInteger();var commitKey=CarrierRegistry.id("bomd_nouveau:test_commit");
        dev.arsconflux.api.casting.ActiveCasts.execute(pure,p,source,()->{
            h.assertTrue(dev.arsconflux.api.casting.ActiveCasts.afterRelease(p,commitKey,commits::incrementAndGet),"Core captures active release callback");return false;
        },false);
        close(h,commits.get(),0,"Unacknowledged uses drop deferred commits");
        dev.arsconflux.api.casting.ActiveCasts.execute(pure,p,source,()->{
            dev.arsconflux.api.casting.ActiveCasts.afterRelease(p,commitKey,commits::incrementAndGet);
            dev.arsconflux.api.casting.ActiveCasts.afterRelease(p,commitKey,commits::incrementAndGet);
            close(h,commits.get(),0,"Deferred commits do not gate sibling dispatches");
            dev.arsconflux.api.casting.ActiveCasts.dispatched(resolver(h,p,pure),true);return true;
        },false);
        close(h,commits.get(),1,"Repeated success callbacks commit once per acknowledged use");
        h.assertTrue(!dev.arsconflux.api.casting.ActiveCasts.afterRelease(p,commitKey,commits::incrementAndGet),"Passive use reports no active release rather than leaking a callback");

        var origin=scene(h).add(.5,0,.5);var base=BlockPos.containing(origin);p.setPos(origin);p.setYRot(-90);p.setXRot(0);
        var earth=GlyphCatalog.EFFECTS.stream().filter(g->g.ability==Ability.EARTHDIVE).findFirst().orElseThrow();var dive=new Spell(MethodSelf.INSTANCE,earth);
        for(int x=1;x<=2;x++)for(int y=0;y<4;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlock(base.offset(x,y,z),Blocks.BEDROCK.defaultBlockState(),2);
        dev.arsconflux.api.casting.ActiveCasts.execute(dive,p,source,()->resolver(h,p,dive).onCast(ItemStack.EMPTY,h.getLevel()),false);
        close(h,p.getX(),origin.x,"Failed native wall search leaves the caster in place");close(h,BomdCooldowns.remaining(p,Ability.EARTHDIVE),0,"Failed Earthdive adds no cooldown even after an acknowledged Ars dispatch");
        for(int x=1;x<=2;x++)for(int y=0;y<4;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlock(base.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(dev.arsconflux.api.casting.ActiveCasts.execute(dive,p,source,()->{
            boolean first=resolver(h,p,dive).onCast(ItemStack.EMPTY,h.getLevel());
            h.assertTrue(p.getX()>base.getX()+3,"First synchronous Earthdive succeeds");
            close(h,BomdCooldowns.remaining(p,Ability.EARTHDIVE),0,"Successful result waits until the enclosing use releases");
            p.setPos(origin);
            boolean second=resolver(h,p,dive).onCast(ItemStack.EMPTY,h.getLevel());
            return first && second && p.getX()>base.getX()+3;
        },false),"Multiple synchronous branches complete before cooldown commitment");
        close(h,BomdCooldowns.remaining(p,Ability.EARTHDIVE),dev.bomdnouveau.platform.BomdConfig.cooldownTicks(Ability.EARTHDIVE),"Successful Earthdive commits exactly at release");
        h.assertTrue(!dev.arsconflux.api.casting.ActiveCasts.allowed(dive,p,source),"New Earthdive is blocked after successful teleport");
        p.setGameMode(GameType.CREATIVE);
        h.assertTrue(dev.arsconflux.api.casting.ActiveCasts.allowed(dive,p,source),"Creative bypasses an existing cooldown");
        long deadline=p.getPersistentData().getCompound("bomd_nouveau_cooldowns").getLong(Ability.EARTHDIVE.id);
        BomdCooldowns.released(p,Ability.EARTHDIVE);
        close(h,p.getPersistentData().getCompound("bomd_nouveau_cooldowns").getLong(Ability.EARTHDIVE.id),deadline,"Creative does not extend cooldowns");
        try {
            dev.bomdnouveau.platform.BomdConfig.CREATIVE_COOLDOWN.set(true);
            h.assertTrue(!dev.arsconflux.api.casting.ActiveCasts.allowed(dive,p,source),"Creative cooldown configuration restores checks");
            dev.bomdnouveau.platform.BomdConfig.COOLDOWNS_ENABLED.set(false);
            h.assertTrue(dev.arsconflux.api.casting.ActiveCasts.allowed(dive,p,source),"Global disable bypasses cooldown checks");
        } finally {
            dev.bomdnouveau.platform.BomdConfig.CREATIVE_COOLDOWN.set(false);
            dev.bomdnouveau.platform.BomdConfig.COOLDOWNS_ENABLED.set(true);
        }
        p.setGameMode(GameType.SURVIVAL);
        h.runAfterDelay(dev.bomdnouveau.platform.BomdConfig.cooldownTicks(Ability.MISSILE)+1,()->{
            h.assertTrue(dev.arsconflux.api.casting.ActiveCasts.allowed(spell,p,source),"Server clock expires the projectile cooldown");
            clean(h,p);p.discard();ExecutionSessions.tick();h.succeed();
        });
    }
    private static void laser(GameTestHelper h,ServerPlayer p,HitResult hit,Vec3 direction,java.util.concurrent.atomic.AtomicInteger budget) {
        var glyph=GlyphCatalog.EFFECTS.stream().filter(g->g.ability==Ability.GAUNTLET_LASER).findFirst().orElseThrow();
        var cast=new SpellResolver(new SpellContext(h.getLevel(),new Spell(MethodTouch.INSTANCE,glyph),p,new LivingCaster(p) {
            @Override public boolean enoughMana(int amount) {return budget.get()>=amount;}
            @Override public void expendMana(int amount) {budget.addAndGet(-amount);}
        },ItemStack.EMPTY));
        cast.spellContext.nextPart();dev.arsconflux.api.context.TriggerGeometry.write(cast.spellContext,direction);
        cast.onResolveEffect(h.getLevel(),hit);
    }
    private static void effect(GameTestHelper h,ServerPlayer p,Ability ability,HitResult hit,Vec3 direction) {
        var glyph=GlyphCatalog.EFFECTS.stream().filter(g->g.ability==ability).findFirst().orElseThrow();
        var cast=resolver(h,p,new Spell(MethodTouch.INSTANCE,glyph));cast.spellContext.nextPart();
        dev.arsconflux.api.context.TriggerGeometry.write(cast.spellContext,direction);
        cast.onResolveEffect(h.getLevel(),hit);
    }
    private static LivingEntity mobileCow(GameTestHelper h,Vec3 position) {
        var creature=(net.minecraft.world.entity.Mob)cow(h,position);
        creature.setNoAi(false);creature.setNoGravity(false);
        creature.goalSelector.removeAllGoals(goal->true);creature.targetSelector.removeAllGoals(goal->true);
        return creature;
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
