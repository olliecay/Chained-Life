package com.ollie.blueprint.listeners;

import com.ollie.blueprint.ChainedLife;
import com.ollie.blueprint.managers.BoogeymanManager;
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
import java.util.logging.Level;

public class DeathListener implements Listener {

    private final PartnerManager partnerManager;
    private final BoogeymanManager boogeymanManager;
    private final ChainedLife plugin;
    private final Set<UUID> deathProcessing = new HashSet<>();
    private final Set<UUID> skipReduce = new HashSet<>();

    public DeathListener(PartnerManager partnerManager, BoogeymanManager boogeymanManager, ChainedLife plugin) {
        this.partnerManager = partnerManager;
        this.boogeymanManager = boogeymanManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = event.getEntity().getKiller();
        UUID playerId = player.getUniqueId();

        // If killer is a boogeyman, mark cleansed
        if (killer != null && boogeymanManager.isBoogeyman(killer)) {
            boogeymanManager.markCleansed(killer);
        }

        // prevent re-entry
        if (!deathProcessing.add(playerId)) {
            return;
        }

        try {
            // If this was a forced partner death, skip life reduction
            if (skipReduce.remove(playerId)) {
                plugin.getLogger().info("[ChainedLife] Skipped life reduction for partner " + player.getName());
                return;
            }

            int livesBefore = partnerManager.getLives(player);
            if (livesBefore <= 0) {
                plugin.getLogger().info("[ChainedLife] " + player.getName() + " is already at 0 lives. Ignoring death.");
                return;
            }

            // Reduce player’s life (handles both partners)
            partnerManager.reduceLives(player);
            int remainingLives = partnerManager.getLives(player);

            plugin.getLogger().info("[ChainedLife] " + player.getName() + " died. Lives left: " + remainingLives);

            // Notify everyone
            Bukkit.getOnlinePlayers().forEach(p -> {
                p.sendTitle("§cA life was taken...", "§7" + player.getName() + " lost a life!", 10, 70, 20);
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
            });

            // Handle partner forced death
            Player partner = partnerManager.getPartner(player);
            if (partner != null && partner.isOnline() && partnerManager.getLives(partner) > 0) {
                UUID partnerId = partner.getUniqueId();

                // Mark partner to avoid double processing, and mark skipReduce to avoid extra life reduction
                deathProcessing.add(partnerId);
                skipReduce.add(partnerId);

                plugin.getLogger().info("[ChainedLife] Killing partner " + partner.getName() + " because " + player.getName() + " died.");

                // kill partner on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        partner.setHealth(0.0);
                    } catch (Throwable t) {
                        plugin.getLogger().log(Level.WARNING, "Failed to set partner health to 0 for " + partner.getName(), t);
                        partner.damage(1000.0);
                    }
                });

                // schedule cleanup of partner marks after a few ticks to avoid stuck state
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    skipReduce.remove(partnerId);
                    deathProcessing.remove(partnerId);
                }, 40L); // 2 seconds: enough time for death/respawn handling to happen
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

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "unchain " + player.getName());
                    if (partner != null && partner.isOnline()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "unchain " + partner.getName());
                    }
                }, 3L);
            }
        } finally {
            // Clean up initial player's deathProcessing mark (partner cleanup handled separately above)
            Bukkit.getScheduler().runTask(plugin, () -> deathProcessing.remove(playerId));
        }
    }

    public void clearDeathMark(Player player) {
        deathProcessing.remove(player.getUniqueId());
    }
}
