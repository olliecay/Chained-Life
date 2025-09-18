package com.ollie.blueprint.managers;

import com.ollie.blueprint.ChainedLife;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartnerManager {

    private final Set<UUID> syncing = new HashSet<>();
    private final int defaultLives;
    private final Map<UUID, UUID> partners = new HashMap<>();
    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Set<UUID> inSync = ConcurrentHashMap.newKeySet();
    private final ChainedLife plugin;

    public PartnerManager(int defaultLives, ChainedLife plugin) {
        this.defaultLives = defaultLives;
        this.plugin = plugin;
    }

    public void beginSync(Player player) {
        syncing.add(player.getUniqueId());
    }

    public void endSync(Player player) {
        syncing.remove(player.getUniqueId());
    }

    public boolean isSyncing(Player player) {
        return syncing.contains(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        if (!lives.containsKey(player.getUniqueId())) {
            lives.put(player.getUniqueId(), defaultLives);
        }
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

        enforceDistance(player);

        String cmd = String.format("chain %s %s", player.getName(), newPartner.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    public void enforceDistance(Player player) {
        Player partner = getPartner(player);
        if (partner == null || !partner.isOnline()) return;

        if (!player.getWorld().equals(partner.getWorld())) {
            partner.teleport(player.getLocation());
            return;
        }

        double distance = player.getLocation().distance(partner.getLocation());
        if (distance > 6) {
            partner.teleport(player.getLocation());
            // partner.sendMessage("§eYou were pulled closer to your partner!");
            // player.sendMessage("§eYour partner was pulled closer to you!");
        }
    }

    public Player getPartner(Player player) {
        UUID id = partners.get(player.getUniqueId());
        return id != null ? Bukkit.getPlayer(id) : null;
    }

    public int getLives(Player player) {
        return lives.getOrDefault(player.getUniqueId(), defaultLives);
    }

    public void setLives(Player player, int amount) {
        Bukkit.getLogger().info("[ChainedLife] Setting " + player.getName() + " lives to " + amount);
        lives.put(player.getUniqueId(), amount);
    }

    public void reduceLives(Player player) {
        Player partner = getPartner(player);

        int current = getLives(player);
        if (current <= 0) return;

        int newLives = current - 1;

        lives.put(player.getUniqueId(), newLives);
        if (partner != null) {
            lives.put(partner.getUniqueId(), newLives);
        }
    }

    public void mirrorState(Player source) {
        Player partner = getPartner(source);
        if (partner == null) return;

        double srcMax = source.getAttribute(Attribute.MAX_HEALTH).getValue();
        double tgtMax = partner.getAttribute(Attribute.MAX_HEALTH).getValue();

        double srcRounded = Math.round(source.getHealth() * 2.0) / 2.0;
        double tgtRounded = Math.round(partner.getHealth() * 2.0) / 2.0;

        double lowest = Math.min(srcRounded, tgtRounded);
        double clamped = Math.min(lowest, Math.min(srcMax, tgtMax));

        if (srcRounded > clamped) {
            source.setNoDamageTicks(10);
            source.setHealth(clamped);
        }
        if (tgtRounded > clamped) {
            partner.setNoDamageTicks(10);
            partner.setHealth(clamped);
        }

        int finalFood = Math.max(0, Math.min(20, source.getFoodLevel()));
        if (partner.getFoodLevel() != finalFood) partner.setFoodLevel(finalFood);
        if (source.getFoodLevel() != finalFood) source.setFoodLevel(finalFood);

        float finalSat = Math.max(0f, Math.min(finalFood, source.getSaturation()));
        if (Math.abs(partner.getSaturation() - finalSat) > 0.01f) partner.setSaturation(finalSat);
        if (Math.abs(source.getSaturation() - finalSat) > 0.01f) source.setSaturation(finalSat);
    }

    public void clearPartners() {
        partners.clear();
    }

}