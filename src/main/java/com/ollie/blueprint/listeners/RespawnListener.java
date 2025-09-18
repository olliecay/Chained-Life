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
    private final DeathListener deathListener; // reference to clear death marks

    public RespawnListener(PartnerManager partnerManager, ChainedLife plugin, DeathListener deathListener) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
        this.deathListener = deathListener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        int lives = partnerManager.getLives(player);

        // Always clear death tracking once they respawn
        if (lives > 0) {
            deathListener.clearDeathMark(player);
            Bukkit.getLogger().info("[ChainedLife] Cleared death tracking for " + player.getName());
        } else {
            Bukkit.getLogger().info("[ChainedLife] " + player.getName() + " respawned with 0 lives, keeping death mark.");
        }

        if (lives <= 0) {
            // Force spectator if they are out
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }, 2L);
            return;
        }

        // Reset player state
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);

        // Re-chain partner after respawn
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