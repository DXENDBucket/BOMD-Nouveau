# Native payload audit — 0.1.0-dev.6

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

Only 1.21.1 NeoForge is implemented. Four projectile replacements
and seven effects have native Scribe recipes and English/Chinese localization. Glyphs
are grouped through Core after normal Ars glyphs; projectile modifiers remain at the
end of this provider's augments. Native icons are referenced, not copied.

Focused GameTests exercise configured Harm amplification, real damage/slow,
unchanged unmanaged damage, all four projectiles with ordinary/orbit carriers,
continuations, repeated fee depletion, creative casting, delayed native poison and
spore removal, independent explosion damage/terrain preservation and recipe presence.
Development coexistence includes Iron & Nouveau and Goety & Nouveau with one Core.
Client visuals still require in-game inspection; a server test cannot prove rendering.

Remaining Boss attacks (gauntlet shielding/body actions, Void Blossom spike animations,
minion summoning and arena pillars) are not represented by the current glyphs.

## Damage and cost review — dev.2

Figures below assume the native default Boss configuration, player casting, no
equipment modifiers or defenses. Amplify follows Harm's configured +2 for direct
attack-value damage; comet Amplify instead adds 0.5 native explosion strength.

| Payload | Unamplified damage | Default payload Mana |
| --- | --- | ---: |
| Missile | 13.5 direct, plus configured 5-second Slowness III | 40 |
| Petal | 18 direct | 50 |
| Comet / Comet Burst | Strength 4 explosion, theoretical center maximum 57; distance and exposure reduce it | 300 / 300 |
| Spore / Spore Burst | 18 on an entity hit; 18 per accepted eruption hit after 30 ticks, plus 140-tick Poison I | 200 / 200 |

The spore circle is a filled radius-7 disk (149 possible ground columns), not a
single ring. Placement still requires native suitable ground. Vanilla hurt immunity
prevents equal overlapping hits from simply multiplying damage by the column count.
A target taking both direct and delayed hits ordinarily receives 36 raw hit damage
plus roughly 5 poison damage if susceptible and able to lose that health. Ground-only
targets ordinarily receive 18 plus poison. These are scenario estimates, not universal
damage caps: movement, immunity, armor, hurt timing and repeated casts change them.
Poison does not normally kill; piercing means bypassing shields, not bypassing armor.

Local default comparison, excluding equipment/config discounts:

- Ars projectile + Harm: 5 damage for 25 Mana (10 method + 15 effect); ten separate
  successful hits total 50 damage for 250 Mana. Harm permits two Amplify by default,
  giving 9 damage for 65 Mana per projectile. This is a simple reference spell, not
  a claim about the cheapest possible Ars composition.
- Iron Magic Missile level 1: 6 damage for 10 Iron Mana, 1-second cooldown. Nine
  separate casts total 54 damage for 90 Mana. Iron Fireball level 4: up to 25 damage
  per target for 105 Mana, 2-second base cast time and 25-second cooldown; two casts
  total up to 50 per target for 210 Mana.
- Goety Soul Bolt: 4 damage for 4 Soul Energy, no base charge and 1-second cooldown;
  thirteen casts total 52 damage for 52 Soul Energy. Soul Energy is not equivalent
  to either Mana pool and has its own acquisition/storage rules.

Evidence: installed Ars 5.13.3 Harm/method/augment configs and bytecode; installed
Iron spell constructors and power/cost formulas; Goety 3.2.0 Soul Bolt source and
test-profile spell config; installed Minecraft ExplosionDamageCalculator and poison
effect source. This balance-only revision uses compilation and artifact checks;
the three runtime tests remain the dev.1 baseline and are not rerun for price changes.

## Obsidilith effects and earthdive — dev.3

`GroundEffects` adapts the native action orchestration without creating a Boss:

- Burst uses the native filled radius-7 disk, 30-tick warning, burst particles/sounds,
  shield-piercing damage and vertical launch. Wave uses native lineCallback geometry,
  radius-4 disks, a 20-tick initial delay, 8-tick spacing and 20-tick column warning.
- Wave retains the actual native ignition value of **5 ticks**, not five seconds.
  This is a very brief ignition; changing it to several seconds would alter balance.
- Spikes retain three rounds beginning at ticks 30/60/90, each with a 20-tick warning,
  radius-2 ground coverage and 120-tick Slowness III. Entity selections follow current
  position plus ten ticks of velocity, capped at six blocks; a block selection stays
  fixed. This replaces Boss-specific ServerPlayer history prediction to support mobs.
- Native RiftBurst still supplies the ground columns and effects. Its default 14-block
  upward search / 28-block height tolerance is reproduced through its custom placement
  constructor, adding caster and chunk guards. Damage uses the explicit damage owner,
  while the caster remains the excluded actor and lifetime owner. Default raw damage
  is 16 x 1.5 = 24, plus configured Harm amplification. Non-player launch is added.
- An activation charges once for its complete native attack sequence. Subsequent Ars
  hits activate and charge separate sequences; creative casters are exempt. No added
  chant/cooldown. Defaults are 200/300/240 Mana, plus the configured Amplify surcharge.
- Earthdive selects a living ally or uses the caster for a non-entity hit. Entity
  hits are rejected unless the recipient is the caster or either entity's native
  isAlliedTo predicate accepts the other; non-living entities and spectators are
  rejected too. Rejected hits do not move anyone or spend Mana. Self selection uses
  that caster's facing; otherwise incoming trajectory direction takes priority, then
  the direction from caster to hit. Native WallTeleport chooses an exit, with additional
  bounding-box collision, loaded-path and world-border checks. It supports player and
  creature teleportation, does not damage walls and charges 100 only on success.
  Its icon/recipe use the native earthdive spear; attacks use the obsidian heart.

The added focused server case checks actual delayed burst damage/launch, all three
spike hits and slow, captured wave direction, full-sequence payment, player/creature
wall teleportation, bedrock rejection, failed-cast nonpayment and creative exemption.
Native client particles/sounds are dispatched; client appearance still needs manual
inspection. No Void Blossom spike renderer or Boss-only client actor is used here.

## Gauntlet Laser — dev.5

- `LaserAction` starts after 25 ticks, collects an 8-sample aim history and runs a
  60-tick phase at 30-block range. The adapter retains that warm-up, phase and lag,
  using past facing rather than the Boss target's past position. The beam appears
  after two history samples; damage waits for all eight, retaining the native
  separation of visual appearance and first damage. A self/allied host follows
  its eye position and look; a point trigger holds its origin and incoming
  direction. Host/caster loss and loaded-path checks guard every tick.
- Use native `BMDUtils.findEntitiesInLine` after server block clipping. Native laser
  damage is attack × .75; fixed hard baseline is × 1.5 again, default 18. Harm
  amplification is added once. A normal, non-shield-piercing damage type with
  scaling `never` preserves armor and hurt immunity without reapplying difficulty.
  Damage owner remains the actual caster; host and native allies are excluded.
- Conflux owns the transient session and cleanup. Upkeep is prepaid in 20-tick
  blocks before active damage/visuals, default 150 per block, at most three blocks.
  Amplify surcharge applies to each payment. First-second affordability is required
  at start; charging costs nothing. Creative bypass comes from the resource account.
- Client packets carry server-clipped geometry, phase and a six-tick visual lease,
  refreshed every two ticks with explicit closure and world-switch cleanup. Client
  particles use native red full-bright sparkles; beam drawing invokes the native
  helper/texture with both its origin translation and endpoint coordinate adjusted
  together. The native helper's `eyeHeight - 1.7` offset is compensated explicitly.
- Native block destruction is omitted deliberately, not approximated. Boss body
  animations cannot appear on arbitrary recipients. No added channel lock is
  applied; dev.6 adds a cooldown at the active-release boundary. Later Ars effects retain their ordinary trigger semantics;
  individual laser damage ticks do not execute the remaining recipe again.

The fifth focused test exercises no warm-up damage/cost, opaque clipping, actual
turn lag and hard-mode damage, upkeep depletion, allied eyes/facing precedence,
damage ownership, host loss, fixed-point incoming geometry, creative exemption
and natural expiry. Dedicated-server startup with all three addons also verifies
client class separation. Visual inspection is still needed for first/third person.

## Active cooldowns — dev.6

`BomdActiveCastProvider` registers only BOMD recipes with Conflux's public
`ActiveCastProvider` API. It never performs per-hit cooldown checks. Provider
queries read deadlines; an acknowledged release commits each distinct glyph once.
Other providers retain their own policies and storage; pure Ars recipes are unchanged.

Deadlines belong to the living caster's persistent compound, keyed by glyph ID,
using the server Overworld game clock across dimensions. Configuration disables
checks/writes globally or for individual zero-duration glyphs; creative bypasses
both by default. Normal cooldowns start at release, including the laser's warm-up.

Earthdive is the result-dependent exception. A successful paid teleport calls
`BomdCooldowns.succeeded`, which queues one keyed `ActiveCasts.afterRelease` action
if an enclosing active use exists. This leaves all synchronous branches eligible
until release; failures queue nothing. Passive/late successes commit immediately
without denying their ongoing execution. There is no early cooldown for failed
teleportation and no cooldown refund on ordinary missed projectiles.

The sixth focused case verifies real Ars launch through Conflux, recipe/weapon
sharing, complete denial with no action/payment, passive repeated conversion,
pure Ars isolation, keyed callback deduplication/drop on unacknowledged uses,
failed Earthdive, two successful same-use branches, creative and global switches,
and clock expiry. The shared Core callback API also compiles for Forge 1.20.1.
