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

    public RespawnListener(PartnerManager partnerManager, ChainedLife plugin) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (partnerManager.getLives(player) <= 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }, 2L);
            return;
        }

        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);

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
