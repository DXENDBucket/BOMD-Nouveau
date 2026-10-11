# Native payload audit — 0.1.0-dev.1

Target: installed BOMD NeoForge 1.3.3, source snapshot
`1a7bd955d201d1b4d066157b8335cd967677e209`.

| Ability | Native path | Retained | Adapter boundary |
| --- | --- | --- | --- |
| Lich Missile | `MagicMissileProjectile.entityHit`, `VolleyAction` callback | Native projectile renderer/trail, hit sound, configured slow callback | Configured Lich attack ×1.5 plus Harm amplifier; no caster melee scaling |
| Petal Blade | `PetalBladeProjectile.entityHit` | Native blade renderer/trail and actual impact | Configured Void Blossom attack ×1.5 plus Harm amplifier |
| Comet | `CometProjectile.onImpact`, `CometAction` callback | Rotating native model, actual native explosion callback, launch sound | Default terrain preservation; amp adds .5 explosion strength |
| Spore Ball | `SporeBallProjectile.entityHit/onHitBlock/doExplosion`, `RiftBurst` | Direct damage, 30-tick landed model/flash, landing particles, ground search, columns, poison, delayed removal | Synced mode restores client landed state; fixed managed damage does not reapply difficulty |

Comet Burst and Spore Burst use the same `NativePayloads.impact` implementation,
not a second approximation. Each repeated collision creates a fresh impact payload;
an earlier spore's impacted flag cannot prevent later blooms. Spore's native entity
hit alone does not initiate its delayed ground bloom; the adapter explicitly performs
the native landing stage after the direct hit.

## Ownership, trajectory and payment

`CastContext` distinguishes caster, damage owner, target and account. Damage owner
is installed as native owner. Costs use Ars through Core; later effects retain their
incoming account. No native player proxy or Boss actor is spawned.

`ArsTrajectory` is used for all carriers, including the normal projectile. Flight speed,
gravity, bounce, orbit and collisions follow Ars / the supported carrier. The native
entity supplies rendering and its payload; its base movement is suppressed only when
managed. This intentionally does not reproduce BOMD's Boss salvo layout or native
launch speeds. Acceleration has already been applied to the Ars carrier.

Attacking a managed comet dispatches its shoot-down through the same Core impact
resolver and ends that trajectory, rather than invoking a free native callback.
Ordinary projectiles pay at replacement creation. Repeated carriers pay per resolved
hit. `ProjectileForm.initialCharge` gives Core a side-effect-free price for balanced
scatter budgeting. Creatives are free; unfunded replacement attempts cannot fall back
to an unpaid ordinary Ars projectile. Ordinary Ars costs remain independent.

Mode 0 native entities retain all original movement/damage/persistence. Managed
visuals are not saved, and public Core execution sessions close on owner loss,
expiration, unloaded chunk/world and server shutdown. Native landed spores retain
their 30-tick result and a 100-tick safety cleanup; native rifts cancel if the owner dies.

## Version and verification scope

Only 1.21.1 NeoForge is implemented in this first batch. Four projectile replacements
and two effects have native Scribe recipes and English/Chinese localization. Glyphs
are grouped through Core after normal Ars glyphs; projectile modifiers remain at the
end of this provider's augments. Native icons are referenced, not copied.

Focused GameTests exercise configured Harm amplification, real damage/slow,
unchanged unmanaged damage, all four projectiles with ordinary/orbit carriers,
continuations, repeated fee depletion, creative casting, delayed native poison and
spore removal, independent explosion damage/terrain preservation and recipe presence.
Development coexistence includes Iron & Nouveau and Goety & Nouveau with one Core.
Client visuals still require in-game inspection; a server test cannot prove rendering.

Remaining Boss attacks (gauntlet laser/shield, specialized spike animations, summoning,
walls, and item teleport abilities) are not represented by these six glyphs.
