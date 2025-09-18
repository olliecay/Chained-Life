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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeathListener implements Listener {
    private final PartnerManager partnerManager;
    private final ChainedLife plugin;

    // Tracks which players have been processed for their current death
    private final Set<UUID> handledDeaths = new HashSet<>();

    public DeathListener(PartnerManager partnerManager, ChainedLife plugin) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        int lives = partnerManager.getLives(player);
        if (lives <= 0) {
            Bukkit.getLogger().info("[ChainedLife] Ignoring death for " + player.getName() + " (0 lives).");
            return;
        }

        // Prevent duplicate handling
        if (!handledDeaths.add(player.getUniqueId())) {
            Bukkit.getLogger().info("[ChainedLife] Skipping duplicate death handling for " + player.getName());
            return;
        }

        // Reduce player’s lives
        partnerManager.reduceLives(player);
        int remainingLives = partnerManager.getLives(player);

        Bukkit.getLogger().info("[ChainedLife] " + player.getName() + " died. Lives left: " + remainingLives);

        // Global notification
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendTitle("§cA life was taken...", "§7" + player.getName() + " lost a life!", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
        });

        // Handle partner death
        Player partner = partnerManager.getPartner(player);
        if (partner != null && partner.isOnline() && partnerManager.getLives(partner) > 0) {
            if (handledDeaths.add(partner.getUniqueId())) {
                Bukkit.getLogger().info("[ChainedLife] Killing partner " + partner.getName() +
                        " because " + player.getName() + " died.");
                Bukkit.getScheduler().runTask(plugin, () -> partner.setHealth(0.0));
            }
        }

        // Assign teams
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

        // Handle elimination
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

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "unchain " + player.getName());
                if (partner != null && partner.isOnline()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "unchain " + partner.getName());
                }
            }, 3L);
        }
    }

    /** Clear death flag so player can die again */
    public void clearDeathMark(Player player) {
        handledDeaths.remove(player.getUniqueId());
    }
}