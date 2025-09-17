package com.ollie.blueprint.commands;

import com.ollie.blueprint.ChainedLife;
import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EndSessionCommand implements CommandExecutor {

    private final PartnerManager partnerManager;
    private final ChainedLife plugin;

    public EndSessionCommand(PartnerManager partnerManager, ChainedLife plugin) {
        this.partnerManager = partnerManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.isOp())) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOp()) {
                player.kickPlayer("§cSession is over");
            }
        }

        partnerManager.clearPartners();

        sender.sendMessage("§aSession ended. All non-OP players have been kicked.");
        return true;
    }
}
