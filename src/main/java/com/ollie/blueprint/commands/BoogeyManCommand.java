package com.ollie.blueprint.commands;

import com.ollie.blueprint.managers.BoogeymanManager;
import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BoogeyManCommand implements CommandExecutor {

    private final BoogeymanManager boogeymanManager;
    private final PartnerManager partnerManager;

    public BoogeyManCommand(BoogeymanManager boogeymanManager, PartnerManager partnerManager) {
        this.boogeymanManager = boogeymanManager;
        this.partnerManager = partnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /boogeyman <start|end>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                int count = 1;
                if (args.length > 1) {
                    try {
                        count = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                boogeymanManager.chooseBoogeymen(count);
                Bukkit.broadcastMessage("§cA Boogeyman is now among you...");
            }

            case "end" -> {
                boogeymanManager.punishFailures(partnerManager);
                Bukkit.broadcastMessage("§cThe Boogeyman session has ended.");
            }

            default -> sender.sendMessage("§cUsage: /boogeyman <start|end>");
        }

        return true;
    }
}