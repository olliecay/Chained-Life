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

public class SetFixedPartnersCommand implements CommandExecutor {
    private static final String[][] FIXED_PAIRS = {
            {"saparata", "Fluixon"},
            {"LordBurney", "ItzzEnder"},
            {"PrinceZam", "Dtowncat"},
            {"cynikka", "Lingulini"},
            {"Hvyrotation", "KagL"},
            {"Thomas5200", "Microspr"},
            {"Zapynubs", "AFreakinTurkey"},
            {"Bizzy_Brit", "lightrocket2"},
            {"Benji_Button", "5pyder"},
            {"Jophiel_", "CallMeCass"},
            {"Baablu", "Jawunleashed"},
            {"Whalemilk_", "DaHouse_Panda"},
            {"ymis", "sidefall"},
            {"KaNukei", "Lampeyre"},
            {"magicsings", "meagon"},
            {"SaltySurvivalist", "awobbuffet"}
    };
    private final PartnerManager partnerManager;
    private final ChainedLife plugin;

    public SetFixedPartnersCommand(PartnerManager partnerManager, ChainedLife plugin) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        for (String[] pair : FIXED_PAIRS) {
            Player p1 = Bukkit.getPlayerExact(pair[0]);
            Player p2 = Bukkit.getPlayerExact(pair[1]);

            if (p1 == null || p2 == null) {
                sender.sendMessage("§eSkipping pair: " + pair[0] + " + " + pair[1] + " (offline)");
                continue;
            }

            startRevealAnimation(p1, p2);
        }

        return true;
    }

    private void startRevealAnimation(Player player, Player partner) {
        new BukkitRunnable() {
            final Random random = new Random();
            final int ITERATIONS = 30;
            int ticks = 0;
            Player lastCandidate = null;

            @Override
            public void run() {
                if (ticks >= ITERATIONS) {
                    cancel();

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
                            partnerManager.pairPlayers(player, partner);

                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join Green " + player.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join Green " + partner.getName());

                            if (player.isOnline() && partner.isOnline()) {
                                player.teleport(partner.getLocation());
                            }
                        }
                    }.runTaskLater(plugin, 60L);

                    return;
                }

                List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (!online.isEmpty()) {
                    lastCandidate = online.get(random.nextInt(online.size()));
                }

                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    String rollingName = (lastCandidate != null ? lastCandidate.getName() : partner.getName());
                    viewer.sendTitle("§aYour link is...", "§e" + rollingName, 0, ITERATIONS * 4, 0);

                    float pitch = 0.8f + (float) ticks / ITERATIONS * 0.4f;
                    viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, pitch);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}
