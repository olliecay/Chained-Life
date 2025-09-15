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

            @Override
            public void run() {
                if (ticks >= 40) {
                    Player partner = online.get(random.nextInt(online.size()));

                    player.sendTitle("§aYour partner is...", "§e" + partner.getName(), 10, 60, 20);

                    partnerManager.switchPartner(player);

                    cancel();
                    return;
                }

                Player randomCandidate = online.get(random.nextInt(online.size()));
                player.sendTitle("§aChoosing partner...", "§e" + randomCandidate.getName(), 0, 5, 0);
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f + (ticks / 40.0f));

                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);

        return true;
    }
}
