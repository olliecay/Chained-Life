package com.ollie.blueprint.listeners;

import com.ollie.blueprint.ChainedLife;
import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RespawnListener implements Listener {

    private final PartnerManager partnerManager;
    private final ChainedLife plugin;
    private final DeathListener deathListener;

    public RespawnListener(PartnerManager partnerManager, ChainedLife plugin, DeathListener deathListener) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
        this.deathListener = deathListener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        int lives = partnerManager.getLives(player);

        // If they still have lives, clear death processing for them and their partner (delayed to avoid race)
        if (lives > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // clear the respawning player's mark
                deathListener.clearDeathMark(player);
                // also attempt to clear their partner's mark (safe no-op if partner is null or not marked)
                Player partner = partnerManager.getPartner(player);
                if (partner != null) {
                    deathListener.clearDeathMark(partner);
                }
            }, 5L); // small delay to ensure death-cycle finished
        } else {
            Bukkit.getLogger().info("[ChainedLife] Keeping " + player.getName() + " marked handled (0 lives).");
        }

        // If out of lives, ensure spectator mode is applied
        if (lives <= 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) player.setGameMode(GameMode.SPECTATOR);
            }, 2L);
            return;
        }

        // restore health / food / saturation
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);

        // after respawn, re-chain and mirror partner state
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player partner = partnerManager.getPartner(player);
            if (partner != null && partner.isOnline() && partnerManager.getLives(partner) > 0) {
                partner.teleport(player.getLocation());
                partnerManager.mirrorState(player);
                partnerManager.enforceDistance(player);
                String cmd = String.format("chain %s %s", player.getName(), partner.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else {
                partnerManager.mirrorState(player);
            }
        }, 5L);
    }
}