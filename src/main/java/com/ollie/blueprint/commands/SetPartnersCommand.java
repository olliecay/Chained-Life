package com.ollie.blueprint.commands;

import com.ollie.blueprint.ChainedLife;
import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SetPartnersCommand implements CommandExecutor {
    private final PartnerManager partnerManager;
    private final ChainedLife plugin;

    public SetPartnersCommand(PartnerManager partnerManager, ChainedLife plugin) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.remove(player);

        if (online.isEmpty()) {
            player.sendMessage("§cNo other players to pair with!");
            return true;
        }

        new BukkitRunnable() {
            final Random random = new Random();
            final int ITERATIONS = 30;
            int ticks = 0;
            Player lastCandidate = null;

            @Override
            public void run() {
                if (ticks >= ITERATIONS) {
                    Player partner = lastCandidate;
                    cancel();

                    if (partner == null || !partner.isOnline()) {
                        player.sendMessage("§cNo valid partner found.");
                        return;
                    }

                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        String revealName;
                        if (viewer.equals(player)) {
                            revealName = partner.getName();
                        } else if (viewer.equals(partner)) {
                            revealName = player.getName();
                        } else {
                            revealName = partner.getName();
                        }

                        viewer.sendTitle("§a" + revealName, "", 10, 60, 20);
                        viewer.playSound(viewer.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            partnerManager.switchPartner(player);
                            Player newPartner = partnerManager.getPartner(player);
                            if (newPartner != null && newPartner.isOnline()) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join Green " + player.getName());
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join Green " + newPartner.getName());
                                player.teleport(newPartner.getLocation());
                            }
                        }
                    }.runTaskLater(plugin, 60L);

                    return;
                }

                lastCandidate = online.get(random.nextInt(online.size()));

                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    String rollingName = lastCandidate.getName();

                    viewer.sendTitle("§aYour link is...", "§e" + rollingName, 0, ITERATIONS * 4, 0);

                    float pitch = 0.8f + (float) ticks / ITERATIONS * 0.4f;
                    viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, pitch);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 4L);

        return true;
    }
}
