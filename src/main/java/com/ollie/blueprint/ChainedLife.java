package com.ollie.blueprint;

import com.ollie.blueprint.commands.*;
import com.ollie.blueprint.listeners.*;
import com.ollie.blueprint.managers.BoogeymanManager;
import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChainedLife extends JavaPlugin {

    private PartnerManager partnerManager;
    private BoogeymanManager boogeymanManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        int defaultLives = getConfig().getInt("default-lives", 3);
        partnerManager = new PartnerManager(defaultLives, this);
        boogeymanManager = new BoogeymanManager();

        // Guard command registration: check plugin.yml presence
        registerCommand("switchpartner", new SwitchPartnerCommand(partnerManager));
        registerCommand("setpartners", new SetPartnersCommand(partnerManager, this));
        registerCommand("setfixedpartners", new SetFixedPartnersCommand(partnerManager, this));
        registerCommand("endsession", new EndSessionCommand(partnerManager, this));
        registerCommand("lives", new LivesCommand(partnerManager));
        registerCommand("boogey", new BoogeyManCommand(boogeymanManager, partnerManager));

        DeathListener deathListener = new DeathListener(partnerManager, boogeymanManager, this);

        getServer().getPluginManager().registerEvents(deathListener, this);
        getServer().getPluginManager().registerEvents(new RespawnListener(partnerManager, this, deathListener), this);

        getServer().getPluginManager().registerEvents(new DamageListener(partnerManager), this);
        getServer().getPluginManager().registerEvents(new HealListener(partnerManager), this);
        getServer().getPluginManager().registerEvents(new FoodLevelChangeListener(partnerManager), this);
        getServer().getPluginManager().registerEvents(new PotionEffectListener(partnerManager), this);
        getServer().getPluginManager().registerEvents(new SessionListener(partnerManager, this), this);

        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(PlayerJoinEvent event) {
                partnerManager.addPlayer(event.getPlayer());
            }

            @org.bukkit.event.EventHandler
            public void onQuit(PlayerQuitEvent event) {
                partnerManager.removePlayer(event.getPlayer());
            }
        }, this);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        if (getCommand(name) == null) {
            getLogger().warning("Command '" + name + "' is not defined in plugin.yml — skipping registration.");
            return;
        }
        getCommand(name).setExecutor(executor);
    }

    @Override
    public void onDisable() {
        if (partnerManager != null) {
            partnerManager.shutdown();
            getLogger().info("[CHAINED-LIFE] Plugin disabled and lives saved.");
        } else {
            getLogger().info("[CHAINED-LIFE] Plugin disabled (partnerManager was null).");
        }
    }
}