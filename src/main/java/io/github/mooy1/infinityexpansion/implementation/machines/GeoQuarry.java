package io.github.mooy1.infinityexpansion.implementation.machines;

import io.github.mooy1.infinityexpansion.InfinityExpansion;
import io.github.mooy1.infinityexpansion.lists.Categories;
import io.github.mooy1.infinityexpansion.lists.Items;
import io.github.mooy1.infinitylib.misc.Pair;
import io.github.mooy1.infinitylib.objects.AbstractMachine;
import io.github.mooy1.infinitylib.presets.MenuPreset;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.cscorelib2.collections.RandomizedSet;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GeoQuarry extends AbstractMachine implements RecipeDisplayItem {

    public static final int ENERGY = 180;
    private static final int STATUS = 4;
    private static final int[] BORDER = { 0, 1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 26, 27, 35, 36, 44, 45, 53 };
    private static final int[] OUTPUT_BORDER = { 19, 20, 21, 22, 23, 24, 25, 28, 34, 37, 43, 46, 47, 48, 49, 50, 51, 52 };
    private static final int[] OUTPUT_SLOTS = { 29, 30, 31, 32, 33, 38, 39, 40, 41, 42 };
    private static final Map<Pair<Biome, World.Environment>, RandomizedSet<ItemStack>> recipes = new HashMap<>();
    
    public GeoQuarry() {
        super(Categories.ADVANCED_MACHINES, Items.GEO_QUARRY, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                Items.MACHINE_PLATE, SlimefunItems.COBALT_PICKAXE, Items.MACHINE_PLATE,
                Items.MACHINE_CIRCUIT, Items.ADVANCED_GEO_MINER, Items.MACHINE_CIRCUIT,
                Items.MACHINE_PLATE, SlimefunItems.COBALT_PICKAXE, Items.MACHINE_PLATE,
        }, STATUS, ENERGY);
        
        registerBlockHandler(getId(), (p, b, item, reason) -> {
            BlockMenu menu = BlockStorage.getInventory(b);
            if (menu != null) {
                menu.dropItems(b.getLocation(), OUTPUT_SLOTS);
            }
            return true;
        });
    }
    
    @Override
    public boolean process(@Nonnull Block b, @Nonnull BlockMenu inv) {
        if (!InfinityExpansion.progressEvery(60)) {
            if (inv.hasViewer()) {
                inv.replaceExistingItem(STATUS, new CustomItem(Material.LIME_STAINED_GLASS_PANE, "&aDrilling..."));
            }
            return true;
        }
        
        ItemStack output = recipes.computeIfAbsent(new Pair<>(b.getBiome(), b.getWorld().getEnvironment()), k -> {
            RandomizedSet<ItemStack> set = new RandomizedSet<>();
            for (GEOResource resource : SlimefunPlugin.getRegistry().getGEOResources().values()) {
                if (resource.isObtainableFromGEOMiner()) {
                    int supply = resource.getDefaultSupply(b.getWorld().getEnvironment(), b.getBiome());
                    if (supply > 0) {
                        set.add(resource.getItem().clone(), supply);
                    }
                }
            }
            return set;
        }).getRandom();
        
        if (!inv.fits(output, OUTPUT_SLOTS)) {
            if (inv.hasViewer()) {
                inv.replaceExistingItem(STATUS, MenuPreset.notEnoughRoom);
            }
            return false;
        }

        inv.pushItem(output, OUTPUT_SLOTS);
        if (inv.hasViewer()) {
            inv.replaceExistingItem(STATUS, new CustomItem(Material.LIME_STAINED_GLASS_PANE, "&aFound!"));
        }
        return true;
    }
    
    @Override
    public void onNewInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
    }

    @Override
    public void setupInv(@Nonnull BlockMenuPreset blockMenuPreset) {
        for (int i : BORDER) {
            blockMenuPreset.addItem(i, new CustomItem(Material.GRAY_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i : OUTPUT_BORDER) {
            blockMenuPreset.addItem(i, new CustomItem(Material.ORANGE_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        }
        blockMenuPreset.addItem(STATUS, MenuPreset.loadingItemRed, ChestMenuUtils.getEmptyClickHandler());
    }

    @Nonnull
    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> displayRecipes = new LinkedList<>();

        for (GEOResource resource : SlimefunPlugin.getRegistry().getGEOResources().values()) {
            if (resource.isObtainableFromGEOMiner()) {
                displayRecipes.add(resource.getItem());
            }
        }

        return displayRecipes;
    }
    
    @Override
    public int getCapacity() {
        return ENERGY * 4;
    }

}