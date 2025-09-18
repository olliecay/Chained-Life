package com.ollie.blueprint.commands;

import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LivesCommand implements CommandExecutor {

    private final PartnerManager partnerManager;

    public LivesCommand(PartnerManager partnerManager) {
        this.partnerManager = partnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /lives <get|set> [player] [amount]");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "get" -> handleGet(sender, args);
            case "set" -> handleSet(sender, args);
            default -> sender.sendMessage("§cInvalid action. Use /lives <get|set>.");
        }

        return true;
    }

    private void handleGet(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cYou must specify a player when using this from console.");
                return;
            }
            int lives = partnerManager.getLives(player);
            player.sendMessage("§aYou have §e" + lives + "§a lives remaining.");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        int lives = partnerManager.getLives(target);
        sender.sendMessage("§e" + target.getName() + "§a has §e" + lives + "§a lives remaining.");
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /lives set <player> <amount>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        try {
            int amount = Integer.parseInt(args[2]);
            partnerManager.setLives(target, amount);
            sender.sendMessage("§aSet §e" + target.getName() + "§a's lives to §e" + amount + "§a.");
            target.sendMessage("§aYour lives have been set to §e" + amount + "§a.");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a number.");
        }
    }
}
