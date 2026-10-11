# Changelog

## 0.1.0-dev.1

- Add Lich Missile, Petal Blade, Lich Comet and Spore Ball projectile replacements, with native impacts and Ars/Conflux trajectories.
- Add independent Comet Burst and Spore Burst effects sharing the projectile aftermath implementation.
- Use fixed hard-mode Boss damage for managed attack-value payloads and the actual Ars Harm amplification configuration. Preserve missile slow, comet explosion and delayed spore visuals/columns/poison.
- Add configurable Ars trigger costs, repeat-payment depletion, creative exemption and conservative default terrain protection. No added cast time or cooldown.
- Add native Scribe recipes, runtime native icon references and English/Chinese descriptions.
- Start a separate LGPL v3 project, source artifacts, license notices and dependency hashes. First target: Minecraft 1.21.1 NeoForge.
- Route comet shoot-down through paid impact resolution and prevent reentrant explosion activation.
- Verify three focused server GameTests with Iron & Nouveau / Goety & Nouveau coexistence, plus source and binary artifact checks. Client rendering remains subject to in-game inspection.
