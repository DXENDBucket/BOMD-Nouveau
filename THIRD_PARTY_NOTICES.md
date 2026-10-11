# Third-party notices

BOMD & Nouveau is licensed under LGPL v3, copyright 2026 EndXiom. The LGPL
text and the GPL v3 text it incorporates accompany binary and source JARs.

- **Bosses of Mass Destruction**: barribob / miyo6032; Forge/NeoForge port:
  CerbonXD. LGPL v3. [Port source and license](https://github.com/CERBON-MODS/Bosses-of-Mass-Destruction-FORGE).
  Audited against NeoForge 1.3.3; source snapshot `1a7bd955d201d1b4d066157b8335cd967677e209`.
  Native callbacks in `NativePayloads` adapt `VolleyAction` / `CometAction`.
  `GroundEffects` adapts Obsidilith `BurstAction` / `WaveAction` / `SpikeAction`
  orchestration and invokes native `RiftBurst` / `WallTeleport` helpers.
  `GauntletLaser` adapts `LaserAction` timing and line-hit orchestration. Client
  visuals adapt `GauntletClientLaserHandler` / `LaserParticleRenderer` particle
  placement and invoke native `VanillaCopies.renderBeam` using the original material.
  Modifying interfaces and native impact hooks does not alter ordinary Boss projectiles.
- **Ars Conflux**: EndXiom, MIT. Separately installed dependency. This addon
  uses its public carrier, execution, context, resource and presentation APIs.
- **Ars Nouveau**: separately installed dependency. No source or art bundled;
  its registered Harm configuration is read to share damage amplification.

All dependency JARs in `libs/` are development-only, ignored by Git and not
included in distributions. Users may replace BOMD with an interface-compatible
modified version. No restriction on reverse engineering to debug such changes.

Publish the corresponding source JAR / repository revision alongside each
release. Any future copied native code or assets must retain their own notices.
