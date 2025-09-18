package com.ollie.blueprint;

import com.ollie.blueprint.commands.EndSessionCommand;
import com.ollie.blueprint.commands.LivesCommand;
import com.ollie.blueprint.commands.SetPartnersCommand;
import com.ollie.blueprint.commands.SwitchPartnerCommand;
import com.ollie.blueprint.listeners.*;
import com.ollie.blueprint.managers.PartnerManager;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChainedLife extends JavaPlugin {

    private PartnerManager partnerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        int defaultLives = getConfig().getInt("default-lives", 3);
        partnerManager = new PartnerManager(defaultLives, this);

        getCommand("switchpartner").setExecutor(new SwitchPartnerCommand(partnerManager));
        getCommand("setpartners").setExecutor(new SetPartnersCommand(partnerManager, this));
        getCommand("endsession").setExecutor(new EndSessionCommand(partnerManager, this));
        getCommand("lives").setExecutor(new LivesCommand(partnerManager));

        getServer().getPluginManager().registerEvents(new DeathListener(partnerManager, this), this);
        getServer().getPluginManager().registerEvents(new RespawnListener(partnerManager, this, new DeathListener(partnerManager, this)), this);

        getServer().getPluginManager().registerEvents(new DamageListener(partnerManager), this);
        getServer().getPluginManager().registerEvents(new HealListener(partnerManager), this);
        getServer().getPluginManager().registerEvents(new FoodLevelChangeListener(partnerManager), this);
        getServer().getPluginManager().registerEvents(new PotionEffectListener(partnerManager), this);
        getServer().getPluginManager().registerEvents(new SessionListener(partnerManager, this), this);

//        getServer().getPluginManager().registerEvents(new EnchantItemListener(), this);
//        getServer().getPluginManager().registerEvents(new PrepareAnvilListener(), this);
//        getServer().getPluginManager().registerEvents(new PrepareItemEnchantListener(), this);

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

    @Override
    public void onDisable() {
        if (partnerManager != null) {
            getLogger().info("[CHAINED-LIFE] Plugin disabled and lives saved in memory.");
        }
    }
}
