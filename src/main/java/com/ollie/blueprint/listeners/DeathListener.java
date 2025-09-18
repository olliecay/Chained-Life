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

    // Tracks players processed for the current death-cycle to avoid double-processing / loops
    private final Set<UUID> deathProcessing = new HashSet<>();

    public DeathListener(PartnerManager partnerManager, ChainedLife plugin) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        // If already handling this player in this cycle, skip
        if (!deathProcessing.add(playerId)) {
            Bukkit.getLogger().info("[ChainedLife] Skipping duplicate death handling for " + player.getName());
            return;
        }

        int beforeLives = partnerManager.getLives(player);
        if (beforeLives <= 0) {
            Bukkit.getLogger().info("[ChainedLife] Ignoring death for " + player.getName() + " (0 lives).");
            // remove marker because nothing to do
            deathProcessing.remove(playerId);
            return;
        }

        Player partner = partnerManager.getPartner(player);
        if (partner != null && partner.isOnline()) {
            deathProcessing.add(partner.getUniqueId()); // mark partner too so their death handler won't double-process
        }

        Bukkit.getLogger().info("[ChainedLife] Pre-death lives: " + player.getName() + "=" + beforeLives +
                (partner != null ? (", partner " + partner.getName() + "=" + partnerManager.getLives(partner)) : ""));

        // Decrement lives (PartnerManager.reduceLives will also update the partner's lives if partner is online)
        partnerManager.reduceLives(player);

        int remainingLives = partnerManager.getLives(player);
        int partnerLives = partner != null ? partnerManager.getLives(partner) : -1;

        Bukkit.getLogger().info("[ChainedLife] Post-death lives: " + player.getName() + "=" + remainingLives +
                (partner != null ? (", partner " + partner.getName() + "=" + partnerLives) : ""));

        // Notify everyone
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendTitle("§cA life was taken...", "§7" + player.getName() + " lost a life!", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
        });

        // Kill the partner visually if they still have lives > 0 (we already reduced lives above)
        if (partner != null && partner.isOnline() && partnerManager.getLives(partner) > 0) {
            Bukkit.getLogger().info("[ChainedLife] Scheduling partner death for " + partner.getName());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (partner.isOnline() && partner.getHealth() > 0) {
                    partner.setHealth(0.0);
                }
            });
        }

        // Team assignment
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

        // If out of lives, set spectator and unchain after a small delay
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

    /**
     * Clear the death-processing mark so a player can be handled again on future deaths.
     * Keep this public so RespawnListener can call it.
     */
    public void clearDeathMark(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        if (deathProcessing.remove(id)) {
            Bukkit.getLogger().info("[ChainedLife] Cleared deathProcessing for " + player.getName());
        }
    }

    // convenience to clear by UUID (optional)
    public void clearDeathMark(UUID id) {
        if (id == null) return;
        deathProcessing.remove(id);
    }
}