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
            int ticks = 0;
            Player lastCandidate = null;

            @Override
            public void run() {
                if (ticks >= 40) {
                    Player partner = lastCandidate;

                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        String resultLine;

                        if (viewer.equals(player)) {
                            resultLine = "§eYou ↔ " + partner.getName();
                        } else if (viewer.equals(partner)) {
                            resultLine = "§eYou ↔ " + player.getName();
                        } else {
                            resultLine = "§e" + player.getName() + " ↔ " + partner.getName();
                        }

                        viewer.sendTitle("§aPartner Selected!", resultLine, 10, 60, 20);
                        viewer.playSound(viewer.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                    }

                    partnerManager.switchPartner(player);

                    Player newPartner = partnerManager.getPartner(player);
                    if (newPartner != null && newPartner.isOnline()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join Green " + player.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join Green " + newPartner.getName());
                    }

                    cancel();
                    return;
                }

                lastCandidate = online.get(random.nextInt(online.size()));
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    String rollingLine;

                    if (viewer.equals(player)) {
                        rollingLine = "§eYou ↔ " + lastCandidate.getName();
                    } else if (viewer.equals(lastCandidate)) {
                        rollingLine = "§eYou ↔ " + player.getName();
                    } else {
                        rollingLine = "§e" + player.getName() + " ↔ " + lastCandidate.getName();
                    }

                    viewer.sendTitle("§aChoosing partner...", rollingLine, 0, 5, 0);
                    viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 4L);

        return true;
    }
}
