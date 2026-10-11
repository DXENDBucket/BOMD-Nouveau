package dev.bomdnouveau.api;

public enum Ability {
    MISSILE("lich_missile", "Lich Missile", 40),
    PETAL("petal_blade", "Petal Blade", 50),
    COMET("lich_comet", "Lich Comet", 300),
    SPORE("spore_ball", "Spore Ball", 200),
    COMET_BURST("comet_burst", "Comet Burst", 300),
    SPORE_BURST("spore_burst", "Spore Burst", 200);
    public final String id, name;
    public final int cost;
    Ability(String id, String name, int cost) { this.id = id; this.name = name; this.cost = cost; }
    public Ability payload() { return this == COMET_BURST ? COMET : this == SPORE_BURST ? SPORE : this; }
}
