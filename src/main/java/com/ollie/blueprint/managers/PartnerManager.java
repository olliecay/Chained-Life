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

    private final int defaultLives;
    private final Map<UUID, UUID> partners = new HashMap<>();
    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Set<UUID> inSync = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ChainedLife plugin;

    public PartnerManager(int defaultLives, ChainedLife plugin) {
        this.defaultLives = defaultLives;
        this.plugin = plugin;
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

        if (!inSync.add(source.getUniqueId())) return;
        try {
            syncFromTo(source, target);
        } finally {
            inSync.remove(source.getUniqueId());
        }
    }

    private void syncFromTo(Player source, Player target) {
        double srcMax = source.getAttribute(Attribute.MAX_HEALTH).getValue();
        double tgtMax = target.getAttribute(Attribute.MAX_HEALTH).getValue();

        double srcRounded = Math.round(source.getHealth() * 2.0) / 2.0;
        double tgtRounded = Math.round(target.getHealth() * 2.0) / 2.0;

        double finalHealth = Math.min(srcRounded, Math.min(srcMax, tgtMax));

        if (Math.abs(tgtRounded - finalHealth) >= 0.5) {
            target.setNoDamageTicks(1);
            target.setHealth(finalHealth);
        }

        if (Math.abs(srcRounded - finalHealth) >= 0.5) {
            source.setNoDamageTicks(1);
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

    private void syncPotionEffectsExact(Player source, Player target) {
        Set<PotionEffectType> srcTypes = new HashSet<>();
        for (PotionEffect e : source.getActivePotionEffects()) {
            srcTypes.add(e.getType());
        }

        Set<PotionEffectType> tgtTypes = new HashSet<>();
        for (PotionEffect e : target.getActivePotionEffects()) {
            tgtTypes.add(e.getType());
        }

        List<PotionEffectType> toRemove = new ArrayList<>();
        for (PotionEffectType type : tgtTypes) {
            if (!srcTypes.contains(type)) {
                toRemove.add(type);
            }
        }

        for (PotionEffectType type : toRemove) {
            target.removePotionEffect(type);
        }

        for (PotionEffect src : source.getActivePotionEffects()) {
            PotionEffect clone = new PotionEffect(
                    src.getType(),
                    src.getDuration(),
                    src.getAmplifier(),
                    src.isAmbient(),
                    src.hasParticles(),
                    src.hasIcon()
            );
            target.addPotionEffect(clone, true);
        }
    }


}
