package com.ollie.blueprint.listeners;

import com.ollie.blueprint.ChainedLife;
import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SessionListener implements Listener {

    private final PartnerManager partnerManager;
    private final ChainedLife plugin;

    public SessionListener(PartnerManager partnerManager, ChainedLife plugin) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player partner = partnerManager.getPartner(player);
            if (partner != null && partner.isOnline() && partnerManager.getLives(player) > 0) {
                partner.teleport(player.getLocation());

                String cmd = String.format("chain %s %s", player.getName(), partner.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

                partnerManager.mirrorState(player);
                partnerManager.enforceDistance(player);
            }
        }, 10L);
    }

//    @EventHandler
//    public void onQuit(PlayerQuitEvent event) {
//        Player player = event.getPlayer();
//
//        Player partner = partnerManager.getPartner(player);
//        if (partner != null && partner.isOnline()) {
//            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "unchain " + partner.getName());
//        }
//    }
}
