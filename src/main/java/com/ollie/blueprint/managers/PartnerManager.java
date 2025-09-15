package com.ollie.blueprint.managers;

import com.ollie.blueprint.ChainedLife;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartnerManager {

    private static final ThreadLocal<Boolean> syncing = ThreadLocal.withInitial(() -> false);
    private final int defaultLives;
    private final Map<UUID, UUID> partners = new HashMap<>();
    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Set<UUID> inSync = ConcurrentHashMap.newKeySet();
    private final ChainedLife plugin;

    public PartnerManager(int defaultLives, ChainedLife plugin) {
        this.defaultLives = defaultLives;
        this.plugin = plugin;
    }

    private void syncPotionEffectsExact(Player source, Player target) {
        Map<PotionEffectType, PotionEffect> sourceEffects = new HashMap<>();
        for (PotionEffect effect : source.getActivePotionEffects()) {
            sourceEffects.put(effect.getType(), effect);
        }

        for (PotionEffect targetEffect : new ArrayList<>(target.getActivePotionEffects())) {
            if (!sourceEffects.containsKey(targetEffect.getType())) {
                target.removePotionEffect(targetEffect.getType());
            }
        }

        for (PotionEffect sourceEffect : sourceEffects.values()) {
            PotionEffect current = target.getPotionEffect(sourceEffect.getType());
            if (current == null
                    || current.getDuration() != sourceEffect.getDuration()
                    || current.getAmplifier() != sourceEffect.getAmplifier()
                    || current.isAmbient() != sourceEffect.isAmbient()
                    || current.hasParticles() != sourceEffect.hasParticles()
                    || current.hasIcon() != sourceEffect.hasIcon()) {
                target.addPotionEffect(sourceEffect, true);
            }
        }
    }

    public boolean isSyncing(Player player) {
        return inSync.contains(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        lives.putIfAbsent(player.getUniqueId(), defaultLives);
    }

    public void removePlayer(Player player) {
        partners.remove(player.getUniqueId());
    }

    public void switchPartner(Player player) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.remove(player);

        if (online.isEmpty()) {
            player.sendMessage("§cNo other players to pair with!");
            return;
        }

        Player newPartner = online.get(new Random().nextInt(online.size()));

        int playerLives = getLives(player);
        int partnerLives = getLives(newPartner);

        if (playerLives != partnerLives) {
            player.sendMessage("§cYou can only switch to a partner with the same number of lives!");
            return;
        }

        partners.put(player.getUniqueId(), newPartner.getUniqueId());
        partners.put(newPartner.getUniqueId(), player.getUniqueId());

        player.sendMessage("§aYou are now linked with §e" + newPartner.getName() + "!");
        newPartner.sendMessage("§aYou are now linked with §e" + player.getName() + "!");

        mirrorState(player);

        String cmd = String.format("chain %s %s", player.getName(), newPartner.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    public Player getPartner(Player player) {
        UUID id = partners.get(player.getUniqueId());
        return id != null ? Bukkit.getPlayer(id) : null;
    }

    public int getLives(Player player) {
        return lives.getOrDefault(player.getUniqueId(), defaultLives);
    }

    public void setLives(Player player, int amount) {
        lives.put(player.getUniqueId(), amount);
    }

    public void reduceLives(Player player) {
        int current = getLives(player);
        if (current <= 0) return;

        int newVal = current - 1;
        lives.put(player.getUniqueId(), newVal);

        Player partner = getPartner(player);
        if (partner != null && partner.isOnline()) {
            lives.put(partner.getUniqueId(), newVal);
        }
    }

    public void mirrorState(Player source) {
        Player target = getPartner(source);
        if (target == null) return;

        UUID srcId = source.getUniqueId();
        if (!inSync.add(srcId)) return;

        try {
            syncFromTo(source, target);

            if (!source.getActivePotionEffects().isEmpty() || !target.getActivePotionEffects().isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        syncPotionEffectsExact(source, target);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } finally {
            inSync.remove(srcId);
        }
    }

    private void syncFromTo(Player source, Player target) {
        double srcMax = source.getAttribute(Attribute.MAX_HEALTH).getValue();
        double tgtMax = target.getAttribute(Attribute.MAX_HEALTH).getValue();

        double srcRounded = Math.round(source.getHealth() * 2.0) / 2.0;
        double tgtRounded = Math.round(target.getHealth() * 2.0) / 2.0;

        double finalHealth = Math.min(srcRounded, Math.min(srcMax, tgtMax));

        if (Math.abs(tgtRounded - finalHealth) >= 0.5) {
            target.setNoDamageTicks(10);
            target.setHealth(finalHealth);
        }

        if (Math.abs(srcRounded - finalHealth) >= 0.5) {
            source.setNoDamageTicks(10);
            source.setHealth(finalHealth);
        }

        int finalFood = Math.max(0, Math.min(20, source.getFoodLevel()));
        if (target.getFoodLevel() != finalFood) target.setFoodLevel(finalFood);
        if (source.getFoodLevel() != finalFood) source.setFoodLevel(finalFood);

        float finalSat = Math.max(0f, Math.min(finalFood, source.getSaturation()));
        if (Math.abs(target.getSaturation() - finalSat) > 0.01f) target.setSaturation(finalSat);
        if (Math.abs(source.getSaturation() - finalSat) > 0.01f) source.setSaturation(finalSat);

        syncPotionEffectsExact(source, target);
    }
}
