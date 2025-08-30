package com.ollie.blueprint.listeners;

import com.ollie.blueprint.ChainedLife;
import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class DeathListener implements Listener {

    private final PartnerManager partnerManager;
    private final ChainedLife plugin;

    public DeathListener(PartnerManager partnerManager, ChainedLife plugin) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (player.hasMetadata("deathHandled")) return;
        player.setMetadata("deathHandled", new FixedMetadataValue(plugin, true));

        partnerManager.reduceLives(player);
        int remainingLives = partnerManager.getLives(player);

        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendTitle("§cA life was taken...", "§7" + player.getName() + " lost a life!", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
        });
        player.getWorld().strikeLightningEffect(player.getLocation());

        Player partner = partnerManager.getPartner(player);

        if (partner != null && partner.isOnline() && !partner.isDead()) {
            partner.setMetadata("deathHandled", new FixedMetadataValue(plugin, true));
            partner.setHealth(0.0);
        }

        if (remainingLives <= 0) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage("§cYou are out of lives!");

            if (partner != null && partner.isOnline()) {
                partner.setGameMode(GameMode.SPECTATOR);
                partner.sendMessage("§cYour partner ran out of lives too!");
            }
        }
    }
}
