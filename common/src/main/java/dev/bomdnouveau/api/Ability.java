package dev.bomdnouveau.api;

public enum Ability {
    MISSILE("lich_missile", "Lich Missile", 40, 40),
    PETAL("petal_blade", "Petal Blade", 50, 40),
    COMET("lich_comet", "Lich Comet", 300, 300),
    SPORE("spore_ball", "Spore Ball", 200, 200),
    COMET_BURST("comet_burst", "Comet Burst", 300, 300),
    SPORE_BURST("spore_burst", "Spore Burst", 200, 200),
    OBSIDIAN_BURST("obsidian_burst", "Obsidian Burst", 200, 200),
    FIRE_WAVE("fire_wave", "Fire Wave", 300, 240),
    OBSIDIAN_SPIKES("obsidian_spikes", "Obsidian Spikes", 240, 240),
    EARTHDIVE("earthdive", "Earthdive", 100, 100),
    GAUNTLET_LASER("gauntlet_laser", "Gauntlet Laser", 150, 200);
    public final String id, name;
    public final int cost, cooldownTicks;
    Ability(String id, String name, int cost, int cooldownTicks) { this.id = id; this.name = name; this.cost = cost; this.cooldownTicks = cooldownTicks; }
    public Ability payload() { return this == COMET_BURST ? COMET : this == SPORE_BURST ? SPORE : this; }
    public boolean groundEffect() { return this == OBSIDIAN_BURST || this == FIRE_WAVE || this == OBSIDIAN_SPIKES || this == EARTHDIVE; }
}
