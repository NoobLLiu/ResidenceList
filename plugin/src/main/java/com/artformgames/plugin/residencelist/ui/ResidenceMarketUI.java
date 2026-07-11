package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.easyplugin.gui.paged.AutoPagedGUI;
import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.ResidenceListAPI;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.utils.ResidenceUtils;
import com.bekvon.bukkit.residence.Residence;
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
import java.util.Map;
import java.util.Set;

public class ResidenceMarketUI extends AutoPagedGUI {

    public static void open(@NotNull Player player, @Nullable GUI previousGUI) {
        new ResidenceMarketUI(player, previousGUI).openGUI(player);
    }

    protected final @NotNull Player viewer;
    protected final @Nullable GUI previousGUI;

    public ResidenceMarketUI(@NotNull Player viewer, @Nullable GUI previousGUI) {
        super(GUIType.SIX_BY_NINE, ColorParser.parse("&a&l领地市场 &7(购买/租借)"), 10, 52);
        this.viewer = viewer;
        this.previousGUI = previousGUI;

        setPreviousPageSlot(36);
        setNextPageSlot(44);
        setPreviousPageUI(PluginConfig.ICON.PAGE.PREVIOUS_PAGE.get(viewer));
        setNextPageUI(PluginConfig.ICON.PAGE.NEXT_PAGE.get(viewer));
        setNoPreviousPageUI(PluginConfig.ICON.PAGE.NO_PREVIOUS_PAGE.get(viewer));
        setNoNextPageUI(PluginConfig.ICON.PAGE.NO_NEXT_PAGE.get(viewer));
        setEmptyItem(PluginConfig.ICON.EMPTY.get(viewer));

        initItems();
        loadMarketResidences();
    }

    public @NotNull Player getViewer() {
        return viewer;
    }

    public void initItems() {
        setItem(0, new GUIItem(buildBackItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                if (previousGUI != null) {
                    previousGUI.openGUI(clicker);
                } else {
                    ResidenceListUI.open(clicker, null);
                }
            }
        });
    }

    public void loadMarketResidences() {
        this.container.clear();
        this.page = 1;

        Map<String, Integer> forSale = Residence.getInstance().getTransactionManager().getBuyableResidences();
        for (Map.Entry<String, Integer> entry : forSale.entrySet()) {
            ClaimedResidence residence = ResidenceListAPI.getResidence(entry.getKey());
            if (residence == null) continue;
            addItem(buildSaleItem(residence, entry.getValue()));
        }

        Set<ClaimedResidence> forRent = Residence.getInstance().getRentManager().getRentableResidences();
        for (ClaimedResidence residence : forRent) {
            if (residence == null) continue;
            if (ResidenceUtils.isRented(residence)) continue;
            addItem(buildRentItem(residence));
        }

        goFirstPage();
    }

    protected GUIItem buildSaleItem(@NotNull ClaimedResidence residence, int price) {
        ItemStack item = new ItemStack(Material.GREEN_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&a&l" + residence.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7主人: &e" + residence.getOwner()));
            lore.add(ColorParser.parse("&7售价: &e" + price));
            lore.add(ColorParser.parse(""));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f购买此领地"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return new GUIItem(item) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                boolean success = ResidenceUtils.buyResidence(clicker, residence);
                if (success) {
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(clicker);
                    ResidenceListUI.open(clicker, clicker.getName());
                } else {
                    PluginMessages.EDIT.FAILED_SOUND.playTo(clicker);
                    ResidenceMarketUI.open(clicker, previousGUI);
                }
            }
        };
    }

    protected GUIItem buildRentItem(@NotNull ClaimedResidence residence) {
        int cost = ResidenceUtils.getRentCost(residence);
        int days = ResidenceUtils.getRentDays(residence);

        ItemStack item = new ItemStack(Material.BLUE_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&b&l" + residence.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7主人: &e" + residence.getOwner()));
            lore.add(ColorParser.parse("&7日租金: &e" + cost));
            lore.add(ColorParser.parse("&7租期: &e" + days + " 天"));
            lore.add(ColorParser.parse(""));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f租用此领地"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return new GUIItem(item) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                boolean success = ResidenceUtils.rentResidence(clicker, residence, false);
                if (success) {
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(clicker);
                    ResidenceListUI.open(clicker, null);
                } else {
                    PluginMessages.EDIT.FAILED_SOUND.playTo(clicker);
                    ResidenceMarketUI.open(clicker, previousGUI);
                }
            }
        };
    }

    protected ItemStack buildBackItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&c返回"));
            meta.setLore(List.of(ColorParser.parse("&e▶ 左键 &8| &f返回领地列表")));
            item.setItemMeta(meta);
        }
        return item;
    }
}
