package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.easyplugin.gui.paged.AutoPagedGUI;
import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.utils.ResidenceFlagCategory;
import com.artformgames.plugin.residencelist.utils.ResidenceUtils;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ResidenceFlagUI extends AutoPagedGUI {

    public static void open(@NotNull Player player, @NotNull ClaimedResidence residence, @Nullable GUI previousGUI) {
        new ResidenceFlagUI(player, residence, previousGUI, null).openGUI(player);
    }

    protected final @NotNull Player viewer;
    protected final @NotNull ClaimedResidence residence;
    protected final @Nullable GUI previousGUI;
    protected final @Nullable ResidenceFlagCategory category;

    public ResidenceFlagUI(@NotNull Player viewer, @NotNull ClaimedResidence residence,
                           @Nullable GUI previousGUI, @Nullable ResidenceFlagCategory category) {
        this(viewer, residence, previousGUI, category, 1);
    }

    public ResidenceFlagUI(@NotNull Player viewer, @NotNull ClaimedResidence residence,
                           @Nullable GUI previousGUI, @Nullable ResidenceFlagCategory category, int page) {
        super(GUIType.SIX_BY_NINE, computeTitle(residence, category), 10, 52);
        this.viewer = viewer;
        this.residence = residence;
        this.previousGUI = previousGUI;
        this.category = category;

        setPreviousPageSlot(36);
        setNextPageSlot(44);
        setPreviousPageUI(PluginConfig.ICON.PAGE.PREVIOUS_PAGE.get(viewer));
        setNextPageUI(PluginConfig.ICON.PAGE.NEXT_PAGE.get(viewer));
        setNoPreviousPageUI(PluginConfig.ICON.PAGE.NO_PREVIOUS_PAGE.get(viewer));
        setNoNextPageUI(PluginConfig.ICON.PAGE.NO_NEXT_PAGE.get(viewer));
        setEmptyItem(PluginConfig.ICON.EMPTY.get(viewer));

        initItems();
        loadContent();
        if (page > 1 && page <= getLastPageNumber()) {
            setCurrentPage(page);
        }
    }

    private static @NotNull String computeTitle(@NotNull ClaimedResidence residence, @Nullable ResidenceFlagCategory category) {
        if (category == null) {
            return ColorParser.parse("&a&l全局权限设置 &7#&f" + residence.getName());
        }
        return ColorParser.parse("&a&l全局权限 &7#&f" + category.getDisplayName() + " &7#&f" + residence.getName());
    }

    public @NotNull Player getViewer() {
        return viewer;
    }

    public @NotNull ClaimedResidence getResidence() {
        return residence;
    }

    public void initItems() {
        ItemStack backItem = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ColorParser.parse(category == null ? "&c返回" : "&c返回分类"));
            backItem.setItemMeta(backMeta);
        }
        setItem(0, new GUIItem(backItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (category == null) {
                    if (previousGUI != null) {
                        previousGUI.openGUI(player);
                    }
                } else {
                    new ResidenceFlagUI(player, residence, previousGUI, null).openGUI(player);
                }
            }
        });

        if (category == null) {
            ItemStack advancedItem = new ItemStack(Material.COMMAND_BLOCK);
            ItemMeta advancedMeta = advancedItem.getItemMeta();
            if (advancedMeta != null) {
                advancedMeta.setDisplayName(ColorParser.parse("&d&l高级版编辑"));
                List<String> lore = new ArrayList<>();
                lore.add(ColorParser.parse("&7"));
                lore.add(ColorParser.parse("&7使用Residence自带GUI编辑全部权限"));
                lore.add(ColorParser.parse("&7"));
                lore.add(ColorParser.parse("&e▶ 左键 &8| &f打开高级版编辑"));
                advancedMeta.setLore(lore);
                advancedItem.setItemMeta(advancedMeta);
            }
            setItem(45, new GUIItem(advancedItem) {
                @Override
                public void onClick(Player player, ClickType clickType) {
                    if (!clickType.isLeftClick()) return;
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    player.closeInventory();
                    Residence.getInstance().getFlagUtilManager()
                            .openSetFlagGui(player, residence, ResidenceUtils.isResAdmin(player), 1);
                }
            });
        }
    }

    public void loadContent() {
        this.container.clear();
        this.page = 1;

        if (category == null) {
            loadCategories();
        } else {
            loadFlags();
        }
        goFirstPage();
    }

    private void loadCategories() {
        for (ResidenceFlagCategory cat : ResidenceFlagCategory.all()) {
            if (cat.getVisibleGlobalFlags(viewer).isEmpty()) continue;
            addItem(createCategoryItem(cat));
        }
    }

    private void loadFlags() {
        for (Flags flag : category.getVisibleGlobalFlags(viewer)) {
            addItem(createFlagItem(flag));
        }
    }

    protected GUIItem createCategoryItem(@NotNull ResidenceFlagCategory cat) {
        ItemStack item = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&e&l" + cat.getDisplayName()));

            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7包含 &f" + cat.getGlobalFlags().size() + " &7项可设置权限"));
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&e▶ 左键点击 &8| &f打开该分类权限"));
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return new GUIItem(item) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                if (!clickType.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                new ResidenceFlagUI(player, residence, previousGUI, cat).openGUI(player);
            }
        };
    }

    protected GUIItem createFlagItem(@NotNull Flags flag) {
        String flagName = flag.name();
        Boolean value = residence.getPermissions().getFlags().get(flagName);

        boolean isSet = value != null;
        boolean isTrue = Boolean.TRUE.equals(value);
        boolean isFalse = Boolean.FALSE.equals(value);

        Material material;
        String symbol;
        String stateName;
        if (isTrue) {
            material = Material.LIME_WOOL;
            symbol = "&a✔";
            stateName = "&a允许";
        } else if (isFalse) {
            material = Material.RED_WOOL;
            symbol = "&c✘";
            stateName = "&c拒绝";
        } else {
            material = Material.GRAY_WOOL;
            symbol = "&7?";
            stateName = "&7未设置(默认)";
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse(symbol + " &f" + flag.getName()));

            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7" + flag.getDesc()));
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&7当前状态: " + stateName));
            lore.add(ColorParser.parse("&8默认值: " + (flag.isEnabled() ? "&atrue" : "&cfalse")));
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&a▶ 左键 &8| &f设为允许(true)"));
            lore.add(ColorParser.parse("&c▶ 右键 &8| &f设为拒绝(false)"));
            lore.add(ColorParser.parse("&7▶ Shift+左键 &8| &f移除(恢复默认)"));
            meta.setLore(lore);

            if (isTrue) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addEnchant(Enchantment.LURE, 1, true);
            }

            item.setItemMeta(meta);
        }

        return new GUIItem(item) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                String state;
                if (clickType.isShiftClick() && clickType.isLeftClick()) {
                    state = "remove";
                } else if (clickType.isRightClick()) {
                    state = "false";
                } else if (clickType.isLeftClick()) {
                    state = "true";
                } else {
                    return;
                }

                if (!ResidenceUtils.canManage(player, residence)) {
                    PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    return;
                }

                boolean success = ResidenceUtils.setGlobalFlag(player, residence, flagName, state);
                if (success) {
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    ResidenceFlagUI newUI = new ResidenceFlagUI(player, residence, previousGUI, category, getCurrentPage());
                    newUI.openGUI(player);
                } else {
                    PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                }
            }
        };
    }
}
