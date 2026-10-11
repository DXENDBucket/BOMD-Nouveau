package dev.bomdnouveau.api;

public enum Ability {
    MISSILE("lich_missile", "Lich Missile", 40),
    PETAL("petal_blade", "Petal Blade", 50),
    COMET("lich_comet", "Lich Comet", 100),
    SPORE("spore_ball", "Spore Ball", 90),
    COMET_BURST("comet_burst", "Comet Burst", 80),
    SPORE_BURST("spore_burst", "Spore Burst", 70);
    public final String id, name;
    public final int cost;
    Ability(String id, String name, int cost) { this.id = id; this.name = name; this.cost = cost; }
    public Ability payload() { return this == COMET_BURST ? COMET : this == SPORE_BURST ? SPORE : this; }
}
