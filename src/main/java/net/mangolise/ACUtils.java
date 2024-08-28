package net.mangolise;

import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.registry.DynamicRegistry;

public class ACUtils {

    public static boolean isUsingSoulSpeed(Player p) {
        return hasEnchantment(p.getEquipment(EquipmentSlot.BOOTS), Enchantment.SOUL_SPEED);
    }

    public static boolean hasEnchantment(ItemStack item, DynamicRegistry.Key<Enchantment> enchantment) {
        EnchantmentList enchantments = item.get(ItemComponent.ENCHANTMENTS);
        return enchantments != null && enchantments.has(enchantment);
    }
}
