package com.ollie.blueprint.listeners;

import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class HealListener implements Listener {
    private final PartnerManager partnerManager;

    public HealListener(PartnerManager partnerManager) {
        this.partnerManager = partnerManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Player partner = partnerManager.getPartner(player);
        if (partner == null) return;

        double healed = Math.min(player.getHealth() + event.getAmount(),
                player.getAttribute(Attribute.MAX_HEALTH).getValue());
        double partnerHealed = Math.min(healed, partner.getAttribute(Attribute.MAX_HEALTH).getValue());

        if (partner.getHealth() < partnerHealed) {
            partner.setHealth(partnerHealed);
        }
    }

}