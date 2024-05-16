package net.hynse.bladehue;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.HashMap;
import java.util.Map;

public final class BladeHue extends JavaPlugin implements Listener {

    private Map<Material, DyeColor> dyeColorMap;
    private Map<DyeColor, NamespacedKey> recipeKeys;
    public Map<String, String> playerTeams;

    @Override
    public void onEnable() {
        initializeRecipes();
        initializeTeams();
        // Register PlaceholderAPI as a dependency
//        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
//            // PlaceholderAPI is available, continue with initialization
//            new BladeHuePlaceholder(this).register();
//            // Other initialization code...
//        } else {
//            // PlaceholderAPI is not available, disable the plugin
//            getLogger().warning("PlaceholderAPI not found! Disabling BladeHue...");
//            Bukkit.getPluginManager().disablePlugin(this);
//        }
    }


    @Override
    public void onDisable() {
        getLogger().info("BladeHue has been disabled.");
    }

    private void initializeTeams() {
        playerTeams = new HashMap<>();
    }

    private void initializeRecipes() {
        dyeColorMap = new HashMap<>();
        recipeKeys = new HashMap<>();
        for (DyeColor dyeColor : DyeColor.values()) {
            Material dyeMaterial = Material.valueOf(dyeColor.name() + "_DYE");
            dyeColorMap.put(dyeMaterial, dyeColor);
            NamespacedKey recipeKey = createShapelessSwordRecipe(dyeColor, dyeMaterial);
            recipeKeys.put(dyeColor, recipeKey);
        }
    }

    private NamespacedKey createShapelessSwordRecipe(DyeColor dyeColor, Material dyeMaterial) {
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(dyeColor.name().toUpperCase() + " Team's");
        sword.setItemMeta(meta);

        NamespacedKey key = new NamespacedKey(this, dyeColor.name().toLowerCase() + "_wooden_sword");
        ShapelessRecipe recipe = new ShapelessRecipe(key, sword);
        recipe.addIngredient(dyeMaterial);
        recipe.addIngredient(Material.WOODEN_SWORD);
        Bukkit.getServer().addRecipe(recipe);

        return key;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory craftingInventory = event.getInventory();
        ItemStack[] ingredients = craftingInventory.getMatrix();

        for (ItemStack ingredient : ingredients) {
            if (ingredient != null && ingredient.getType() == Material.WOODEN_SWORD && ingredient.hasItemMeta()) {
                ItemStack result = event.getRecipe().getResult();
                if (result != null && result.getType() == Material.WOODEN_SWORD && result.hasItemMeta()) {
                    ItemMeta resultMeta = result.getItemMeta();
                    resultMeta.setLore(ingredient.getItemMeta().getLore());
                    resultMeta.setDisplayName(ingredient.getItemMeta().getDisplayName());
                    for (Map.Entry<Enchantment, Integer> enchantmentEntry : ingredient.getItemMeta().getEnchants().entrySet()) {
                        resultMeta.addEnchant(enchantmentEntry.getKey(), enchantmentEntry.getValue(), true);
                    }
                    resultMeta.setCustomModelData(ingredient.getItemMeta().getCustomModelData());
                    result.setItemMeta(resultMeta);
                    craftingInventory.setResult(result);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        ItemStack craftedItem = event.getCurrentItem();
        if (craftedItem != null && craftedItem.getType() == Material.WOODEN_SWORD && craftedItem.hasItemMeta()) {
            ItemMeta meta = craftedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                DyeColor dyeColor = null;
                for (Map.Entry<Material, DyeColor> entry : dyeColorMap.entrySet()) {
                    if (meta.getDisplayName().equalsIgnoreCase(entry.getValue().name().toUpperCase() + " Wooden Sword")) {
                        dyeColor = entry.getValue();
                        break;
                    }
                }
                if (dyeColor != null) {
                    Player player = event.getWhoClicked() instanceof Player ? (Player) event.getWhoClicked() : null;
                    if (player != null) {
                        String teamName = dyeColor.name().toUpperCase();
                        if (dyeColor == DyeColor.WHITE) {
                            removePlayerFromTeam(player.getName());
                            player.sendMessage(ChatColor.GREEN + "You have left your team!");
                        } else {
                            // Use PlaceholderAPI to dynamically get the team name and color
                            addPlayerToTeam(player.getName(), teamName);
                            player.sendMessage(ChatColor.GREEN + "You have been added to the " + teamName + " team!");
                        }
                    }
                }
            }
        }
    }

    private void removePlayerFromTeam(String playerName) {
        playerTeams.remove(playerName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (NamespacedKey recipeKey : recipeKeys.values()) {
            event.getPlayer().discoverRecipe(recipeKey);
        }
    }

    private void addPlayerToTeam(String playerName, String teamName) {
        playerTeams.put(playerName, teamName);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player damaged = (Player) event.getEntity();
            if (playerTeams.containsKey(attacker.getName()) && playerTeams.containsKey(damaged.getName())) {
                String attackerTeam = playerTeams.get(attacker.getName());
                String damagedTeam = playerTeams.get(damaged.getName());
                if (attackerTeam.equals(damagedTeam)) {
                    event.setCancelled(true); // Cancel the event if they are on the same team
                }
            }
        }
    }
}

