package dev.stardust.gui.widgets.meteorites.entity;

import net.minecraft.util.StringIdentifiable;

public enum Powerups implements StringIdentifiable {
    NONE, PRECISION_AIM, BULLET_HELL, REINFORCED_HULL, PIERCING_SHOTS,
    THRUSTER_UPGRADES, RAPID_FIRE, SUPERCHARGED_FSD, CALIBRATED_FSD, DOUBLE_POINTS,
    SHOTGUN, SNIPER, HIGH_TECH_HULL, ENTROPY, GRAVITY_WELL, HOMING_SHOTS, PHASE_SHIFT, MIDAS_TOUCH, STARDUST;

    @Override
    public String asString() {
        return switch (this) {
            case NONE -> "None";
            case SNIPER -> "Sniper"; // fast-moving piercing rounds, long cooldown
            case ENTROPY -> "Entropy"; // random powerup every round excluding stardust
            case SHOTGUN -> "Shotgun"; // many pellets with random spread; player recoil, long cooldown
            case STARDUST -> "Stardust"; // many powerups combined (exceedingly rare and capped at 3-rounds to balance)
            case RAPID_FIRE -> "Rapid Fire"; // fire rate doubled
            case BULLET_HELL -> "Bullet Hell"; // bullets naturally live longer
            case PHASE_SHIFT -> "Phase Shift"; // replaces right-click warp ability, briefly phase through meteorites
            case MIDAS_TOUCH -> "Midas Touch"; // lol: shooting a bullet now costs 10 points, but is refunded with interest if it kills a meteorite
            case GRAVITY_WELL -> "Gravity Well"; // only granted if mouse aim enabled, deploys at cursor to draw in nearby meteorites
            case HOMING_SHOTS -> "Homing Rounds"; // bullets track towards nearby meteorites
            case DOUBLE_POINTS -> "Double Points"; // kills award double points
            case PRECISION_AIM -> "Precision Aim"; // only granted if mouse aim enabled, no rotation delay towards cursor
            case HIGH_TECH_HULL -> "High Tech Hull"; // reinforced hull + hull health regen
            case PIERCING_SHOTS -> "Piercing Shots"; // bullets may not be removed when killing a meteorite
            case REINFORCED_HULL -> "Reinforced Hull"; // take hull damage instead of instantly dying when hit
            case THRUSTER_UPGRADES -> "Thruster Upgrades"; // extra thrust and rotation speed
            case CALIBRATED_FSD -> "Calibrated Warp Drive"; // only granted if mouse aim enabled, hyperjump with cursor-accurate precision
            case SUPERCHARGED_FSD -> "Supercharged Warp Drive"; // no right-click warp cooldown or point cost
        };
    }
}
