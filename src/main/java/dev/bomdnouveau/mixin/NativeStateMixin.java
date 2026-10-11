package dev.bomdnouveau.mixin;

import com.cerbon.bosses_of_mass_destruction.projectile.BaseThrownItemProjectile;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.bomdnouveau.casting.ManagedProjectile;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BaseThrownItemProjectile.class, remap = false)
public abstract class NativeStateMixin extends ThrowableItemProjectile implements ManagedProjectile {
    @Unique private static final EntityDataAccessor<Integer> bomdNouveau$MODE = SynchedEntityData.defineId(BaseThrownItemProjectile.class, EntityDataSerializers.INT);
    @Unique private float bomdNouveau$damage;
    @Unique private EntityProjectileSpell bomdNouveau$trajectory;
    @Unique private boolean bomdNouveau$resolving;
    protected NativeStateMixin(EntityType<? extends ThrowableItemProjectile> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(bomdNouveau$MODE, 0); }
    @Override public boolean shouldBeSaved() { return bomdNouveau$mode() == 0 && super.shouldBeSaved(); }
    @Override public int bomdNouveau$mode() { return getEntityData().get(bomdNouveau$MODE); }
    @Override public void bomdNouveau$mode(int value) { getEntityData().set(bomdNouveau$MODE, value); }
    @Override public float bomdNouveau$damage() { return bomdNouveau$damage; }
    @Override public void bomdNouveau$damage(float damage) { bomdNouveau$damage = damage; }
    @Override public EntityProjectileSpell confluxTrajectory() { return bomdNouveau$trajectory; }
    @Override public void bomdNouveau$trajectory(EntityProjectileSpell carrier) { bomdNouveau$trajectory = carrier; }
    @Override public boolean bomdNouveau$resolving() { return bomdNouveau$resolving; }
    @Override public void bomdNouveau$resolving(boolean resolving) { bomdNouveau$resolving = resolving; }
}
