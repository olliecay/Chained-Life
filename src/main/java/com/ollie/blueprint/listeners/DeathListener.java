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
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeathListener implements Listener {
    private final PartnerManager partnerManager;
    private final ChainedLife plugin;
    private final Set<UUID> handledDeaths = new HashSet<>();

    public DeathListener(PartnerManager partnerManager, ChainedLife plugin) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!handledDeaths.add(player.getUniqueId())) {
            Bukkit.getLogger().info("[ChainedLife] Skipping duplicate death handling for " + player.getName());
            return;
        }

        partnerManager.reduceLives(player);
        int remainingLives = partnerManager.getLives(player);

        Bukkit.getLogger().info("[ChainedLife] Handling death for " + player.getName() +
                " | Remaining lives: " + remainingLives);

        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendTitle("§cA life was taken...", "§7" + player.getName() + " lost a life!", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
        });

        Player partner = partnerManager.getPartner(player);

        if (partner != null && partner.isOnline() && !handledDeaths.contains(partner.getUniqueId())) {
            handledDeaths.add(partner.getUniqueId());
            Bukkit.getLogger().info("[ChainedLife] Killing partner " + partner.getName() + " due to " + player.getName() + "'s death");
            Bukkit.getScheduler().runTask(plugin, () -> partner.setHealth(0.0));
        }

        String team = switch (remainingLives) {
            case 3 -> "Green";
            case 2 -> "Yellow";
            case 1 -> "Red";
            default -> null;
        };

        if (team != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join " + team + " " + player.getName());
            if (partner != null && partner.isOnline()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join " + team + " " + partner.getName());
            }
        }

        if (remainingLives <= 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage("§cYou are out of lives!");
                    player.getWorld().strikeLightningEffect(player.getLocation());
                }
                if (partner != null && partner.isOnline()) {
                    partner.setGameMode(GameMode.SPECTATOR);
                    partner.sendMessage("§cYou are out of lives!");
                }
            }, 3L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        handledDeaths.remove(event.getPlayer().getUniqueId());
    }
}
