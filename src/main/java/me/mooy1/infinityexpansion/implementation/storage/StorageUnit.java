package me.mooy1.infinityexpansion.implementation.storage;

import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mooy1.infinityexpansion.lists.Categories;
import me.mooy1.infinityexpansion.lists.Items;
import me.mooy1.infinityexpansion.lists.InfinityRecipes;
import me.mooy1.infinityexpansion.utils.ItemStackUtils;
import me.mooy1.infinityexpansion.utils.LoreUtils;
import me.mooy1.infinityexpansion.utils.PresetUtils;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import me.mrCookieSlime.Slimefun.cscorelib2.protection.ProtectableAction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Basically just barrels...
 *
 * @author Mooy1
 *
 * Thanks to
 * @author NCBPFluffyBear
 * for idea, a few bits of code,
 * and code to learn from
 *
 */

public class StorageUnit extends SlimefunItem {

    public static final int BASIC = 2560;
    public static final int ADVANCED = 20_480;
    public static final int REINFORCED = 81_920;
    public static final int VOID = 327_680;
    public static final int INFINITY = 1_600_000_000;

    private final Type type;
    private static final int STATUS_SLOT = 13;
    private static final int[] INPUT_SLOTS = {
            10
    };
    private static final int INPUT_SLOT = INPUT_SLOTS[0];
    private static final int[] OUTPUT_SLOTS = {
            16
    };
    private static final int OUTPUT_SLOT = OUTPUT_SLOTS[0];
    private static final int[] INPUT_BORDER = {
            0, 1, 2, 9, 11, 18, 19, 20
    };
    private static final int[] STATUS_BORDER = {
            3, 4, 5, 12, 14, 21, 22, 23
    };
    private static final int[] OUTPUT_BORDER = {
            6, 7, 8, 15, 17, 24, 25, 26
    };

    public StorageUnit(@Nonnull Type type) {
        super(type.getCategory(), type.getItem(), type.getRecipeType(), type.getRecipe());
        this.type = type;

        new BlockMenuPreset(getId(), Objects.requireNonNull(type.getItem().getDisplayName())) {
            @Override
            public void init() {
                setupInv(this);
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                if (getBlockData(b.getLocation(), "stored") == null) {
                    setStored(b, 0);
                }
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return (player.hasPermission("slimefun.inventory.bypass")
                        || SlimefunPlugin.getProtectionManager().hasPermission(
                        player, block.getLocation(), ProtectableAction.ACCESS_INVENTORIES));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow itemTransportFlow) {
                return new int[0];
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack item) {
                if (flow == ItemTransportFlow.INSERT) {
                    return INPUT_SLOTS;
                } else if (flow == ItemTransportFlow.WITHDRAW) {
                    return OUTPUT_SLOTS;
                } else {
                    return new int[0];
                }
            }
        };

        registerBlockHandler(getId(), (p, b, stack, reason) -> {
            BlockMenu inv = BlockStorage.getInventory(b);

            if (inv != null) {
                String storedItem = getStoredItem(b);
                int stored = getStored(b);

                inv.dropItems(b.getLocation(), OUTPUT_SLOTS);
                inv.dropItems(b.getLocation(), INPUT_SLOTS);

                //drop stored items
                if (stored > 0 && storedItem != null) {

                    ItemStack storedItemStack = ItemStackUtils.getItemFromID(storedItem, 1);
                    int stackSize = storedItemStack.getMaxStackSize();

                    int stacks = (int) Math.floor((float) stored / stackSize);
                    storedItemStack.setAmount(stackSize);

                    for (int i = 0; i < stacks; i++) {
                        b.getWorld().dropItemNaturally(b.getLocation(), storedItemStack);
                    }

                    int remainder = stored % stackSize;
                    storedItemStack.setAmount(remainder);

                    if (remainder > 0) {
                        b.getWorld().dropItemNaturally(b.getLocation(), storedItemStack);
                    }
                }
            }
            setStored(b, 0);
            setStoredItem(b, null);

            return true;
        });
    }

    private void setupInv(BlockMenuPreset blockMenuPreset) {
        for (int i : STATUS_BORDER) {
            blockMenuPreset.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i : INPUT_BORDER) {
            blockMenuPreset.addItem(i, PresetUtils.borderItemInput, ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i : OUTPUT_BORDER) {
            blockMenuPreset.addItem(i, PresetUtils.borderItemOutput, ChestMenuUtils.getEmptyClickHandler());
        }

        blockMenuPreset.addItem(STATUS_SLOT, PresetUtils.loadingItemBarrier, ChestMenuUtils.getEmptyClickHandler());
    }

    @Override
    public void preRegister() {
        this.addItemHandler(new BlockTicker() {
            public void tick(Block b, SlimefunItem sf, Config data) { StorageUnit.this.tick(b); }

            public boolean isSynchronized() { return false; }
        });
    }

    protected void tick(@Nonnull Block b) {

        @Nullable final BlockMenu inv = BlockStorage.getInventory(b.getLocation());
        if (inv == null) return;

        int maxStorage = getStorage(type);
        String storedItem = getStoredItem(b);

        if (inv.toInventory() != null || !inv.toInventory().getViewers().isEmpty()) {
            updateStatus(b);
        }

        //input

        ItemStack inputSlotItem = inv.getItemInSlot(INPUT_SLOT);

        if (inputSlotItem != null) { //Check if empty slot

            int slotAmount = inputSlotItem.getAmount();

            if (inputSlotItem.getMaxStackSize() != 1) { //Check if non stackable item

                String inputItemID = ItemStackUtils.getIDFromItem(inputSlotItem);
                int stored = getStored(b);

                if (stored == 0 && storedItem == null) { //store new item

                    setStoredItem(b, ItemStackUtils.getIDFromItem(inputSlotItem));
                    setStored(b, slotAmount);
                    inv.consumeItem(INPUT_SLOT, slotAmount);

                } else {

                    int maxInput = maxStorage-stored;
                    storedItem = getStoredItem(b);

                    if (storedItem.equals(inputItemID)) { //deposit item

                        if (slotAmount <= maxInput) {
                            setStored(b, stored + slotAmount);
                            inv.consumeItem(INPUT_SLOT, slotAmount);
                        } else {
                            setStored(b, stored + maxInput);
                            inv.consumeItem(INPUT_SLOT, maxInput);
                        }
                    }
                }
            } else {

                if (inv.fits(inputSlotItem, OUTPUT_SLOTS)) { //try to move to output slot to decrease timings

                    inv.pushItem(inputSlotItem, OUTPUT_SLOTS);
                    inv.consumeItem(INPUT_SLOT, inputSlotItem.getAmount());

                }
            }
        }

        //output

        storedItem = getStoredItem(b);

        if (storedItem != null) {

            ItemStack storedItemStack = ItemStackUtils.getItemFromID(storedItem, 1);
            int stored = getStored(b);
            int outputRemaining;

            if (inv.getItemInSlot(OUTPUT_SLOT) != null) {
                outputRemaining = storedItemStack.getMaxStackSize()-inv.getItemInSlot(OUTPUT_SLOT).getAmount();
            } else {
                outputRemaining = storedItemStack.getMaxStackSize();
            }

            if (stored > 1) {

                int amount = 0;

                for (int i = 0; i < outputRemaining; i++) {

                    if (stored > 1+i) {
                        storedItemStack.setAmount(1 + i);
                        if (inv.fits(storedItemStack, OUTPUT_SLOTS)) {
                            amount++;
                        }
                    }
                }
                storedItemStack.setAmount(amount);
                inv.pushItem(storedItemStack, OUTPUT_SLOTS);
                setStored(b, stored-amount);

            } else if (stored == 1 && inv.getItemInSlot(OUTPUT_SLOT) == null) {
                inv.pushItem(storedItemStack, OUTPUT_SLOTS);
                setStoredItem(b, null);
                setStored(b, 0);
            }
        }
    }

    private void updateStatus(Block b) {
        BlockMenu inv = BlockStorage.getInventory(b);

        if (inv.toInventory() == null || inv.toInventory().getViewers().isEmpty()) return;

        String storedItem = getStoredItem(b);
        int stored = getStored(b);

        if (storedItem == null || stored == 0) {
            inv.replaceExistingItem(STATUS_SLOT, new CustomItem(
                    new ItemStack(Material.BARRIER),
                    "&cInput an Item!"
            ));
        } else {
            int maxStorage = getStorage(type);
            ItemStack storedItemStack = ItemStackUtils.getItemFromID(storedItem, 1);

            String converteditemname = "";
            if (storedItemStack.getItemMeta() != null) {
                converteditemname = storedItemStack.getItemMeta().getDisplayName();
            }

            String stacks = "&7Stacks: " + LoreUtils.format(Math.round((float) stored / storedItemStack.getMaxStackSize()));

            if (this.type == Type.INFINITY) {
                inv.replaceExistingItem(STATUS_SLOT, new CustomItem(
                        storedItemStack,
                        converteditemname,
                        "&6Stored: &e" + LoreUtils.format(stored),
                        stacks
                ));
            } else {
                inv.replaceExistingItem(STATUS_SLOT, new CustomItem(
                        storedItemStack,
                        converteditemname,
                        "&6Stored: &e" + LoreUtils.format(stored) + "/" + LoreUtils.format(maxStorage) + " &7(" + Math.round((float) 100 * stored / maxStorage )  + "%)",
                        stacks
                ));
            }
        }
    }

    private void setStored(Block b, int stored) {
        setBlockData(b, "stored", String.valueOf(stored));
        updateStatus(b);
    }

    private void setStoredItem(Block b, String storedItem) {
        setBlockData(b, "storeditem", storedItem);
    }

    private int getStored(@Nonnull Block b) {
        return Integer.parseInt(getBlockData(b.getLocation(), "stored"));
    }

    private String getStoredItem(@Nonnull Block b) {
        return getBlockData(b.getLocation(), "storeditem");
    }

    private void setBlockData(Block b, String key, String data) {
        BlockStorage.addBlockInfo(b, key, data);
    }

    private int getStorage(Type type) {
        if (type == Type.BASIC) {
            return BASIC;
        } else if (type == Type.ADVANCED) {
            return ADVANCED;
        } else if (type == Type.REINFORCED) {
            return REINFORCED;
        } else if (type == Type.VOID) {
            return VOID;
        } else if (type == Type.INFINITY) {
            return INFINITY;
        } else {
            return 0;
        }
    }

    private String getBlockData(Location l, String key) {
        return BlockStorage.getLocationInfo(l, key);
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)

    public enum Type {
        BASIC(Categories.INFINITY_STORAGE, Items.BASIC_STORAGE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                new ItemStack(Material.OAK_LOG), Items.MAGSTEEL, new ItemStack(Material.OAK_LOG),
                new ItemStack(Material.OAK_LOG), new ItemStack(Material.BARREL), new ItemStack(Material.OAK_LOG),
                new ItemStack(Material.OAK_LOG), Items.MAGSTEEL, new ItemStack(Material.OAK_LOG)
        }),
        ADVANCED(Categories.INFINITY_STORAGE, Items.ADVANCED_STORAGE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                Items.MAGSTEEL, Items.MACHINE_CIRCUIT, Items.MAGSTEEL,
                Items.MAGSTEEL, Items.BASIC_STORAGE, Items.MAGSTEEL,
                Items.MAGSTEEL, Items.MACHINE_CIRCUIT, Items.MAGSTEEL
        }),
        REINFORCED(Categories.INFINITY_STORAGE, Items.REINFORCED_STORAGE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                Items.MAGSTEEL_PLATE, Items.MACHINE_PLATE, Items.MAGSTEEL_PLATE,
                Items.MAGSTEEL_PLATE, Items.ADVANCED_STORAGE, Items.MAGSTEEL_PLATE,
                Items.MAGSTEEL_PLATE, Items.MACHINE_CORE, Items.MAGSTEEL_PLATE
        }),
        VOID(Categories.INFINITY_STORAGE, Items.VOID_STORAGE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                Items.VOID_INGOT, Items.MACHINE_PLATE, Items.VOID_INGOT,
                Items.MAGNONIUM, Items.REINFORCED_STORAGE, Items.MAGNONIUM,
                Items.VOID_INGOT, Items.MACHINE_CORE, Items.VOID_INGOT
        }),
        INFINITY(Categories.INFINITY_CHEAT, Items.INFINITY_STORAGE, RecipeType.ENHANCED_CRAFTING_TABLE, InfinityRecipes.getRecipe(Items.INFINITY_STORAGE));

        @Nonnull
        private final Category category;
        private final SlimefunItemStack item;
        private final RecipeType recipeType;
        private final ItemStack[] recipe;
    }
}