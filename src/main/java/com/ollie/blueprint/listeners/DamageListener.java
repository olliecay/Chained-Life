package com.ollie.blueprint.listeners;

import com.ollie.blueprint.managers.PartnerManager;
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
        if (event.getEntity() instanceof Player player) {
            partnerManager.mirrorState(player);
        }
    }

}
