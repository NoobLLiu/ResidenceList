package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.easyplugin.gui.paged.AutoPagedGUI;
import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.listener.AnvilNameInput;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class OnlinePlayerSelectUI extends AutoPagedGUI {

    protected final @NotNull Player viewer;
    protected final @NotNull BiConsumer<Player, String> callback;
    protected final @Nullable GUI previousGUI;
    protected final @NotNull String anvilTitle;
    protected final @NotNull String anvilPlaceholder;

    public OnlinePlayerSelectUI(@NotNull Player viewer,
                                 @NotNull String anvilTitle,
                                 @NotNull String anvilPlaceholder,
                                 @NotNull BiConsumer<Player, String> callback,
                                 @Nullable GUI previousGUI) {
        super(GUIType.SIX_BY_NINE,
                ColorParser.parse("&a&l在线玩家列表 &7(" + countOnline(viewer) + ")"),
                10, 52);
        this.viewer = viewer;
        this.anvilTitle = anvilTitle;
        this.anvilPlaceholder = anvilPlaceholder;
        this.callback = callback;
        this.previousGUI = previousGUI;

        setPreviousPageSlot(36);
        setNextPageSlot(44);
        setPreviousPageUI(PluginConfig.ICON.PAGE.PREVIOUS_PAGE.get(viewer));
        setNextPageUI(PluginConfig.ICON.PAGE.NEXT_PAGE.get(viewer));
        setNoPreviousPageUI(PluginConfig.ICON.PAGE.NO_PREVIOUS_PAGE.get(viewer));
        setNoNextPageUI(PluginConfig.ICON.PAGE.NO_NEXT_PAGE.get(viewer));
        setEmptyItem(PluginConfig.ICON.EMPTY.get(viewer));

        initItems();
        loadPlayers();
        goFirstPage();
    }

    private static int countOnline(Player viewer) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(viewer)) count++;
        }
        return count;
    }

    private void initItems() {
        ItemStack backItem = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ColorParser.parse("&c返回"));
            backItem.setItemMeta(backMeta);
        }
        setItem(0, new GUIItem(backItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (previousGUI != null) {
                    previousGUI.openGUI(player);
                }
            }
        });

        ItemStack cancelItem = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ColorParser.parse("&c&l取消选择"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&7取消本次操作并返回"));
            cancelItem.setItemMeta(cancelMeta);
        }
        setItem(8, new GUIItem(cancelItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                if (!clickType.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (previousGUI != null) {
                    previousGUI.openGUI(player);
                }
            }
        });

        ItemStack inputItem = new ItemStack(Material.NAME_TAG);
        ItemMeta inputMeta = inputItem.getItemMeta();
        if (inputMeta != null) {
            inputMeta.setDisplayName(ColorParser.parse("&e&l输入玩家ID"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&7手动输入玩家名称"));
            lore.add(ColorParser.parse("&7支持选择离线玩家"));
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f输入玩家名称"));
            inputMeta.setLore(lore);
            inputItem.setItemMeta(inputMeta);
        }
        setItem(45, new GUIItem(inputItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                if (!clickType.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                player.closeInventory();
                AnvilNameInput.open(player, anvilTitle, anvilPlaceholder, callback);
            }
        });
    }

    private void loadPlayers() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(viewer)) continue;

            ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta headMeta = (SkullMeta) headItem.getItemMeta();
            if (headMeta != null) {
                headMeta.setOwningPlayer(online);
                headMeta.setDisplayName(ColorParser.parse("&a" + online.getName()));
                List<String> lore = new ArrayList<>();
                lore.add(ColorParser.parse("&7"));
                lore.add(ColorParser.parse("&e▶ 左键 &8| &f选择该玩家"));
                headMeta.setLore(lore);
                headItem.setItemMeta(headMeta);
            }

            final String playerName = online.getName();
            addItem(new GUIItem(headItem) {
                @Override
                public void onClick(Player player, ClickType clickType) {
                    if (!clickType.isLeftClick()) return;
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    player.closeInventory();
                    callback.accept(player, playerName);
                }
            });
        }
    }
}
