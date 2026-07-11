package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.api.residence.ResidenceData;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.listener.AnvilNameInput;
import com.artformgames.plugin.residencelist.utils.ResidenceUtils;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ResidenceRentSettingsUI extends GUI {

    public static void open(@NotNull Player player, @NotNull ResidenceData data, @Nullable GUI previousGUI) {
        new ResidenceRentSettingsUI(player, data, previousGUI).openGUI(player);
    }

    public static void open(@NotNull Player player, @NotNull ResidenceData data,
                            @Nullable GUI previousGUI, int cost, int days,
                            boolean allowRenewing, boolean stayInMarket, boolean allowAutoPay) {
        new ResidenceRentSettingsUI(player, data, previousGUI, cost, days, allowRenewing, stayInMarket, allowAutoPay)
                .openGUI(player);
    }

    protected final @NotNull Player viewer;
    protected final @NotNull ResidenceData residenceData;
    protected final @Nullable GUI previousGUI;

    private int cost = 100;
    private int days = 7;
    private boolean allowRenewing = true;
    private boolean stayInMarket = true;
    private boolean allowAutoPay = false;

    public ResidenceRentSettingsUI(@NotNull Player viewer, @NotNull ResidenceData residenceData,
                                   @Nullable GUI previousGUI) {
        this(viewer, residenceData, previousGUI, 100, 7, true, true, false);
    }

    public ResidenceRentSettingsUI(@NotNull Player viewer, @NotNull ResidenceData residenceData,
                                   @Nullable GUI previousGUI, int cost, int days,
                                   boolean allowRenewing, boolean stayInMarket, boolean allowAutoPay) {
        super(GUIType.THREE_BY_NINE, ColorParser.parse("&b&l设置出租参数"));
        this.viewer = viewer;
        this.residenceData = residenceData;
        this.previousGUI = previousGUI;
        this.cost = cost;
        this.days = days;
        this.allowRenewing = allowRenewing;
        this.stayInMarket = stayInMarket;
        this.allowAutoPay = allowAutoPay;

        setEmptyItem(PluginConfig.ICON.EMPTY.get(viewer));
        initItems();
    }

    public void initItems() {
        setItem(0, new GUIItem(buildBackItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                if (previousGUI != null) {
                    previousGUI.openGUI(clicker);
                } else {
                    ResidenceManageUI.open(clicker, residenceData, null);
                }
            }
        });

        setItem(11, new GUIItem(buildCostItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                clicker.closeInventory();
                AnvilNameInput.open(clicker, "输入日租金", String.valueOf(cost), (player, text) -> {
                    if (text == null || text.trim().isEmpty()) {
                        ResidenceRentSettingsUI.open(player, residenceData, previousGUI,
                                cost, days, allowRenewing, stayInMarket, allowAutoPay);
                        return;
                    }
                    try {
                        int newCost = Integer.parseInt(text.trim());
                        if (newCost <= 0) {
                            PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                            ResidenceRentSettingsUI.open(player, residenceData, previousGUI,
                                    cost, days, allowRenewing, stayInMarket, allowAutoPay);
                            return;
                        }
                        ResidenceRentSettingsUI.open(player, residenceData, previousGUI,
                                newCost, days, allowRenewing, stayInMarket, allowAutoPay);
                    } catch (NumberFormatException e) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                        ResidenceRentSettingsUI.open(player, residenceData, previousGUI,
                                cost, days, allowRenewing, stayInMarket, allowAutoPay);
                    }
                });
            }
        });

        setItem(13, new GUIItem(buildDaysItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                clicker.closeInventory();
                AnvilNameInput.open(clicker, "输入租期天数", String.valueOf(days), (player, text) -> {
                    if (text == null || text.trim().isEmpty()) {
                        ResidenceRentSettingsUI.open(player, residenceData, previousGUI,
                                cost, days, allowRenewing, stayInMarket, allowAutoPay);
                        return;
                    }
                    try {
                        int newDays = Integer.parseInt(text.trim());
                        if (newDays <= 0) {
                            PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                            ResidenceRentSettingsUI.open(player, residenceData, previousGUI,
                                    cost, days, allowRenewing, stayInMarket, allowAutoPay);
                            return;
                        }
                        ResidenceRentSettingsUI.open(player, residenceData, previousGUI,
                                cost, newDays, allowRenewing, stayInMarket, allowAutoPay);
                    } catch (NumberFormatException e) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                        ResidenceRentSettingsUI.open(player, residenceData, previousGUI,
                                cost, days, allowRenewing, stayInMarket, allowAutoPay);
                    }
                });
            }
        });

        setItem(15, new GUIItem(buildRenewingItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                ResidenceRentSettingsUI.open(clicker, residenceData, previousGUI,
                        cost, days, !allowRenewing, stayInMarket, allowAutoPay);
            }
        });

        setItem(21, new GUIItem(buildStayInMarketItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                ResidenceRentSettingsUI.open(clicker, residenceData, previousGUI,
                        cost, days, allowRenewing, !stayInMarket, allowAutoPay);
            }
        });

        setItem(22, new GUIItem(buildAutoPayItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                ResidenceRentSettingsUI.open(clicker, residenceData, previousGUI,
                        cost, days, allowRenewing, stayInMarket, !allowAutoPay);
            }
        });

        setItem(26, new GUIItem(buildConfirmItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                ClaimedResidence residence = residenceData.getResidence();
                boolean success = ResidenceUtils.setForRent(clicker, residence,
                        cost, days, allowRenewing, stayInMarket, allowAutoPay);
                if (success) {
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(clicker);
                    ResidenceManageUI.open(clicker, residenceData, previousGUI);
                } else {
                    PluginMessages.EDIT.FAILED_SOUND.playTo(clicker);
                }
            }
        });
    }

    protected ItemStack buildBackItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&c返回"));
            meta.setLore(List.of(ColorParser.parse("&e▶ 左键 &8| &f返回上级菜单")));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack buildCostItem() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&6&l日租金"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7当前设置: &e" + cost));
            lore.add(ColorParser.parse(""));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f修改日租金"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack buildDaysItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&a&l租期天数"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7当前设置: &e" + days + " 天"));
            lore.add(ColorParser.parse(""));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f修改租期天数"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack buildRenewingItem() {
        ItemStack item = new ItemStack(allowRenewing ? Material.SLIME_BALL : Material.MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&d&l自动续租"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7当前状态: " + (allowRenewing ? "&a开启" : "&c关闭")));
            lore.add(ColorParser.parse("&7开启后租客到期可自动续租"));
            lore.add(ColorParser.parse(""));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f切换状态"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack buildStayInMarketItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&e&l租出后保留在市场"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7当前状态: " + (stayInMarket ? "&a是" : "&c否")));
            lore.add(ColorParser.parse("&7开启后领地被租出后仍可被其他玩家租用"));
            lore.add(ColorParser.parse(""));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f切换状态"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack buildAutoPayItem() {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&6&l允许自动付费"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7当前状态: " + (allowAutoPay ? "&a开启" : "&c关闭")));
            lore.add(ColorParser.parse("&7开启后租客可设置自动付费续租"));
            lore.add(ColorParser.parse(""));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f切换状态"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected ItemStack buildConfirmItem() {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&a&l确认出租"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7日租金: &e" + cost));
            lore.add(ColorParser.parse("&7租期: &e" + days + " 天"));
            lore.add(ColorParser.parse("&7自动续租: " + (allowRenewing ? "&a是" : "&c否")));
            lore.add(ColorParser.parse("&7保留在市场: " + (stayInMarket ? "&a是" : "&c否")));
            lore.add(ColorParser.parse("&7允许自动付费: " + (allowAutoPay ? "&a是" : "&c否")));
            lore.add(ColorParser.parse(""));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f确认设置并出租"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
