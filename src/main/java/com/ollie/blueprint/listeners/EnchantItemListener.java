package com.ollie.blueprint.listeners;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;

public class EnchantItemListener implements Listener {

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        ItemStack item = event.getItem();

        if (item.getType() == Material.BOW
                || item.getType() == Material.WOODEN_SWORD
                || item.getType() == Material.STONE_SWORD
                || item.getType() == Material.IRON_SWORD
                || item.getType() == Material.GOLDEN_SWORD
                || item.getType() == Material.DIAMOND_SWORD
                || item.getType() == Material.NETHERITE_SWORD) {
            event.getEnchantsToAdd().clear();

            event.setCancelled(true);
        }

        if (item.containsEnchantment(Enchantment.PROTECTION)
                || item.containsEnchantment(Enchantment.PROJECTILE_PROTECTION)
                || item.containsEnchantment(Enchantment.BLAST_PROTECTION)
                || item.containsEnchantment(Enchantment.FIRE_PROTECTION)) {
            if (item.getItemMeta().getEnchantLevel(Enchantment.PROTECTION) > 1) {
                event.setCancelled(true);
            }
            if (item.getItemMeta().getEnchantLevel(Enchantment.BLAST_PROTECTION) > 1) {
                event.setCancelled(true);
            }
            if (item.getItemMeta().getEnchantLevel(Enchantment.FIRE_PROTECTION) > 1) {
                event.setCancelled(true);
            }
            if (item.getItemMeta().getEnchantLevel(Enchantment.PROJECTILE_PROTECTION) > 1) {
                event.setCancelled(true);
            }
        }
    }
}
