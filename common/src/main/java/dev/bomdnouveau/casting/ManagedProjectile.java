package dev.bomdnouveau.casting;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.projectile.CarrierProxy;

/** Native projectiles keep mode 0. Mode 1 is an off-world Ars trajectory's visual;
 * mode 2 is a landed spore payload that keeps native delayed visuals and effects. */
public interface ManagedProjectile extends CarrierProxy {
    int bomdNouveau$mode();
    void bomdNouveau$mode(int mode);
    float bomdNouveau$damage();
    void bomdNouveau$damage(float damage);
    void bomdNouveau$trajectory(EntityProjectileSpell trajectory);
    boolean bomdNouveau$resolving();
    void bomdNouveau$resolving(boolean resolving);
}
