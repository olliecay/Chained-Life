package com.ollie.blueprint.commands;

import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SwitchPartnerCommand implements CommandExecutor {
    private final PartnerManager partnerManager;

    public SwitchPartnerCommand(PartnerManager partnerManager) {
        this.partnerManager = partnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        partnerManager.switchPartner(player);
        return true;
    }
}
