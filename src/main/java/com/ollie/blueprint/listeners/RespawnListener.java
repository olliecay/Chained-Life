package com.ollie.blueprint.listeners;

import com.ollie.blueprint.ChainedLife;
import com.ollie.blueprint.managers.PartnerManager;
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

        player.removeMetadata("deathHandled", plugin);

        if (partnerManager.getLives(player) <= 0) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage("§cYou are out of lives!");
            return;
        }

        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);

        partnerManager.mirrorState(player);
    }
}
