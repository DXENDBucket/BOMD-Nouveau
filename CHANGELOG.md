# Changelog

## 0.1.0-dev.5

- Add Gauntlet Laser effect glyph and native Scribe recipe using Blazing Eye + Source Gem + 27 XP.
- Retain native 25-tick charge, 60-tick active phase, 8-tick aim history, 30-block range and line intersections. Damage uses fixed hard-mode Gauntlet laser baseline (18 by default) plus Harm amplification, without modifying caster melee attributes or resetting hurt immunity.
- Use Conflux execution lifetime and resource transactions: each active second prepays 150 Mana by default, with at most three payments. Depletion, missing host/caster, unloaded path, natural expiry and server/world shutdown end the beam. Creative is exempt.
- Self/allied hosts emit from their eyes; point triggers use incoming geometry. Allies and host are excluded from damage, blocks clip the beam without being destroyed.
- Reuse BOMD's beam texture/render helper, native red sparkles and charge sound through short-lived client visual packets; no surrogate Boss or native casting-state mutation.
- Five focused server GameTests pass with Iron / Goety coexistence. Client appearance remains subject to manual in-game inspection.

## 0.1.0-dev.4

- Treat Earthdive as a beneficial action: only move the caster or a living ally recognized by vanilla alliance checks, never a non-allied or non-living entity target. Rejected targets do not charge Mana.
- Extend the existing wall-teleport GameTest to cover non-allied rejection, allied movement and their resource costs.
- All four existing focused server GameTests and release artifact checks pass with Iron / Goety coexistence.

## 0.1.0-dev.3

- Add Obsidian Burst, Fire Wave, Obsidian Spikes and Earthdive effect glyphs, native Scribe recipes and English/Chinese descriptions.
- Reuse native RiftBurst ground placement, indicators, columns and sounds with hard-mode Obsidilith damage and Harm amplification. Retain native wave timing and three spike rounds; support living targets using bounded velocity prediction.
- Apply launch motion to non-player victims as well as native player motion packets; retain native short wave ignition and spike Slowness III.
- Reuse native WallTeleport for player and creature earthdive, with loaded-chunk, collision and border checks. Failed teleports do not charge Mana. Earthdive has no damage augments.
- Default full-sequence Mana costs: burst 200, wave 300, spikes 240 and earthdive 100; all configurable.
- Four focused server GameTests passed with Iron / Goety coexistence; verify all ten recipes, icon references and licensed source/binary artifacts. Native client appearance remains subject to in-game inspection.

## 0.1.0-dev.2

- Raise both Lich Comet and Comet Burst default payload costs to 300 Mana to account for their native area explosion damage and lack of added cast time/cooldown.
- Raise both Spore Ball and Spore Burst default payload costs to 200 Mana for the native radius-7 ground eruption, shield piercing and poison. Keep all native payload damage unchanged.

## 0.1.0-dev.1

- Add Lich Missile, Petal Blade, Lich Comet and Spore Ball projectile replacements, with native impacts and Ars/Conflux trajectories.
- Add independent Comet Burst and Spore Burst effects sharing the projectile aftermath implementation.
- Use fixed hard-mode Boss damage for managed attack-value payloads and the actual Ars Harm amplification configuration. Preserve missile slow, comet explosion and delayed spore visuals/columns/poison.
- Add configurable Ars trigger costs, repeat-payment depletion, creative exemption and conservative default terrain protection. No added cast time or cooldown.
- Add native Scribe recipes, runtime native icon references and English/Chinese descriptions.
- Start a separate LGPL v3 project, source artifacts, license notices and dependency hashes. First target: Minecraft 1.21.1 NeoForge.
- Route comet shoot-down through paid impact resolution and prevent reentrant explosion activation.
- Verify three focused server GameTests with Iron & Nouveau / Goety & Nouveau coexistence, plus source and binary artifact checks. Client rendering remains subject to in-game inspection.
