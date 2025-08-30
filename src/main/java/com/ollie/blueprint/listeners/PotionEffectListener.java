package com.ollie.blueprint.listeners;

import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;

public class PotionEffectListener implements Listener {
    private final PartnerManager partnerManager;

    public PotionEffectListener(PartnerManager partnerManager) {
        this.partnerManager = partnerManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player) {
            partnerManager.mirrorState(player);
        }
    }
}
