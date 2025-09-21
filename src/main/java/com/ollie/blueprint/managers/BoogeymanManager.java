package com.ollie.blueprint.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class BoogeymanManager {

    private final Set<UUID> boogeymen = new HashSet<>();
    private final Set<UUID> cleansed = new HashSet<>();
    private final Random random = new Random();

    public void chooseBoogeymen(int count) {
        boogeymen.clear();
        cleansed.clear();

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) return;

        Collections.shuffle(players, random);
        for (int i = 0; i < count && i < players.size(); i++) {
            Player chosen = players.get(i);
            boogeymen.add(chosen.getUniqueId());
            chosen.sendMessage("§cYou feel a dark presence... You are the §lBoogeyman§c.");
        }
    }

    public boolean isBoogeyman(Player player) {
        return boogeymen.contains(player.getUniqueId());
    }

    public boolean isCleansed(Player player) {
        return cleansed.contains(player.getUniqueId());
    }

    public void markCleansed(Player player) {
        boogeymen.remove(player.getUniqueId());
        cleansed.add(player.getUniqueId());
        player.sendMessage("§aYou have cleansed yourself as the Boogeyman!");
    }

    public void punishFailures(PartnerManager partnerManager) {
        for (UUID id : boogeymen) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                player.sendMessage("§cYou failed as the Boogeyman... Dropping to §4Red Life!");
                partnerManager.setLives(player, 1);
            }
        }
        boogeymen.clear();
        cleansed.clear();
    }
}
