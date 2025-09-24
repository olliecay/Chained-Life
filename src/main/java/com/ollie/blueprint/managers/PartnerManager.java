package com.ollie.blueprint.managers;

import com.ollie.blueprint.ChainedLife;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PartnerManager {

    // single thread-safe set used for sync guards
    private final Set<UUID> syncing = ConcurrentHashMap.newKeySet();

    private final int defaultLives;
    private final Map<UUID, UUID> partners = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lives = new ConcurrentHashMap<>();
    private final ChainedLife plugin;

    private File livesFile;
    private FileConfiguration livesConfig;

    public PartnerManager(int defaultLives, ChainedLife plugin) {
        this.defaultLives = defaultLives;
        this.plugin = plugin;

        createLivesFile();
        loadLives();
    }

    private void createLivesFile() {
        livesFile = new File(plugin.getDataFolder(), "lives.yml");
        if (!livesFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                livesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create lives.yml", e);
            }
        }
        livesConfig = YamlConfiguration.loadConfiguration(livesFile);
    }

    public void saveLives() {
        // clear file to avoid stale entries
        for (String key : new HashSet<>(livesConfig.getKeys(false))) {
            livesConfig.set(key, null);
        }

        for (Map.Entry<UUID, Integer> entry : lives.entrySet()) {
            livesConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            livesConfig.save(livesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save lives.yml", e);
        }
    }

    public void loadLives() {
        if (livesConfig == null) return;
        for (String key : livesConfig.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                int amount = livesConfig.getInt(key, defaultLives);
                lives.put(id, amount);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void beginSync(Player player) {
        if (player == null) return;
        syncing.add(player.getUniqueId());
    }

    public void endSync(Player player) {
        if (player == null) return;
        syncing.remove(player.getUniqueId());
    }

    public boolean isSyncing(Player player) {
        return player != null && syncing.contains(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        if (player == null) return;
        lives.putIfAbsent(player.getUniqueId(), defaultLives);
    }

    public void removePlayer(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        // remove reciprocal mapping
        UUID pid = partners.remove(id);
        if (pid != null) {
            // remove reciprocal only if it points back to this player
            UUID back = partners.get(pid);
            if (back != null && back.equals(id)) {
                partners.remove(pid);
            }
        }
    }

    /**
     * Pair two players together (unpairs previous partners for them).
     */
    public void pairPlayers(Player a, Player b) {
        if (a == null || b == null || a.equals(b)) return;

        // remove previous partners
        Player aPrev = getPartner(a);
        Player bPrev = getPartner(b);
        if (aPrev != null) {
            partners.remove(aPrev.getUniqueId());
            partners.remove(a.getUniqueId());
        }
        if (bPrev != null) {
            partners.remove(bPrev.getUniqueId());
            partners.remove(b.getUniqueId());
        }

        partners.put(a.getUniqueId(), b.getUniqueId());
        partners.put(b.getUniqueId(), a.getUniqueId());

        a.sendMessage("§aYou are now linked with §e" + b.getName() + "!");
        b.sendMessage("§aYou are now linked with §e" + a.getName() + "!");
    }

    public void switchPartner(Player player) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.remove(player);

        if (online.isEmpty()) {
            player.sendMessage("§cNo other players to pair with!");
            return;
        }

        int playerLives = getLives(player);

        // try to find candidate with same lives
        List<Player> candidates = online.stream()
                .filter(p -> getLives(p) == playerLives)
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            player.sendMessage("§cNo available player with the same number of lives to switch to.");
            return;
        }

        Player newPartner = candidates.get(new Random().nextInt(candidates.size()));
        pairPlayers(player, newPartner);

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
        }
    }

    public Player getPartner(Player player) {
        if (player == null) return null;
        UUID id = partners.get(player.getUniqueId());
        return id != null ? Bukkit.getPlayer(id) : null;
    }

    public int getLives(Player player) {
        if (player == null) return defaultLives;
        return lives.getOrDefault(player.getUniqueId(), defaultLives);
    }

    public void setLives(Player player, int amount) {
        if (player == null) return;
        if (amount < 0) amount = 0;
        plugin.getLogger().info("[ChainedLife] Setting " + player.getName() + " lives to " + amount);
        lives.put(player.getUniqueId(), amount);
        saveLives();
    }

    public void reduceLives(Player player) {
        if (player == null) return;
        Player partner = getPartner(player);

        int current = getLives(player);
        if (current <= 0) return;

        int newLives = current - 1;

        lives.put(player.getUniqueId(), newLives);
        if (partner != null) {
            lives.put(partner.getUniqueId(), newLives);
        }
        saveLives();
    }

    public void mirrorState(Player source) {
        Player partner = getPartner(source);
        if (partner == null) return;
        if (isSyncing(source) || isSyncing(partner)) return;

        // Use sync guards to avoid recursion
        beginSync(source);
        beginSync(partner);
        try {
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
        } finally {
            endSync(source);
            endSync(partner);
        }
    }

    public void shutdown() {
        saveLives();
    }

    public void clearPartners() {
        partners.clear();
    }

}