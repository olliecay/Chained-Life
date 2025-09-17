package com.ollie.blueprint.listeners;

import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageListener implements Listener {
    private final PartnerManager partnerManager;

    public DamageListener(PartnerManager partnerManager) {
        this.partnerManager = partnerManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Player partner = partnerManager.getPartner(player);
        if (partner == null) return;

        double health = Math.max(0, player.getHealth() - event.getFinalDamage());
        double clamped = Math.min(health, partner.getAttribute(Attribute.MAX_HEALTH).getValue());

        if (partner.getHealth() > clamped) {
            partner.setNoDamageTicks(10);
            partner.setHealth(clamped);
        }
    }

}