package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.listener.AnvilNameInput;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class PlayerSelectModeUI extends GUI {

    protected final @NotNull Player viewer;
    protected final @NotNull String anvilTitle;
    protected final @NotNull String anvilPlaceholder;
    protected final @NotNull BiConsumer<Player, String> callback;
    protected final @Nullable GUI previousGUI;

    public PlayerSelectModeUI(@NotNull Player viewer,
                              @NotNull String anvilTitle,
                              @NotNull String anvilPlaceholder,
                              @NotNull BiConsumer<Player, String> callback,
                              @Nullable GUI previousGUI) {
        super(GUIType.THREE_BY_NINE, ColorParser.parse("&e&l选择玩家来源"));
        this.viewer = viewer;
        this.anvilTitle = anvilTitle;
        this.anvilPlaceholder = anvilPlaceholder;
        this.callback = callback;
        this.previousGUI = previousGUI;

        setEmptyItem(PluginConfig.ICON.EMPTY.get(viewer));
        initItems();
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

        ItemStack onlineItem = new ItemStack(Material.EMERALD);
        ItemMeta onlineMeta = onlineItem.getItemMeta();
        if (onlineMeta != null) {
            onlineMeta.setDisplayName(ColorParser.parse("&a&l从在线玩家中选择"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&7从当前在线的玩家中选择"));
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f查看在线玩家列表"));
            onlineMeta.setLore(lore);
            onlineItem.setItemMeta(onlineMeta);
        }
        setItem(11, new GUIItem(onlineItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                if (!clickType.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                new OnlinePlayerSelectUI(player, anvilTitle, anvilPlaceholder, callback, PlayerSelectModeUI.this).openGUI(player);
            }
        });

        ItemStack allItem = new ItemStack(Material.NAME_TAG);
        ItemMeta allMeta = allItem.getItemMeta();
        if (allMeta != null) {
            allMeta.setDisplayName(ColorParser.parse("&e&l从全部玩家中选择"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&7手动输入玩家名称"));
            lore.add(ColorParser.parse("&7可选择离线玩家"));
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&e▶ 左键 &8| &f输入玩家名称"));
            allMeta.setLore(lore);
            allItem.setItemMeta(allMeta);
        }
        setItem(15, new GUIItem(allItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                if (!clickType.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                player.closeInventory();
                AnvilNameInput.open(player, anvilTitle, anvilPlaceholder, (p, text) -> {
                    callback.accept(p, text);
                });
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
        setItem(22, new GUIItem(cancelItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                if (!clickType.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (previousGUI != null) {
                    previousGUI.openGUI(player);
                }
            }
        });
    }
}
