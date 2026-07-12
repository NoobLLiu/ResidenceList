package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ResidenceBatchSelectUI extends GUI {

    public static void open(@NotNull Player player, @NotNull ClaimedResidence residence, @Nullable GUI previousGUI) {
        List<ResidencePlayer> trusted = new ArrayList<>(residence.getTrustedPlayers());
        new ResidenceBatchSelectUI(player, residence, trusted, new HashSet<>(), previousGUI).openGUI(player);
    }

    protected final @NotNull Player viewer;
    protected final @NotNull ClaimedResidence residence;
    protected final @NotNull List<ResidencePlayer> trustedPlayers;
    protected final @NotNull Set<UUID> selected;
    protected final @Nullable GUI previousGUI;

    private static final int[] SELECTED_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
    private static final int[] UNSELECTED_SLOTS = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};

    public ResidenceBatchSelectUI(@NotNull Player viewer, @NotNull ClaimedResidence residence,
                                  @NotNull List<ResidencePlayer> trustedPlayers,
                                  @NotNull Set<UUID> selected, @Nullable GUI previousGUI) {
        super(GUIType.SIX_BY_NINE, ColorParser.parse("&e&l批量编辑 - 选择玩家"));
        this.viewer = viewer;
        this.residence = residence;
        this.trustedPlayers = trustedPlayers;
        this.selected = selected;
        this.previousGUI = previousGUI;

        setEmptyItem(PluginConfig.ICON.EMPTY.get(viewer));
        loadSeparator();
        initButtons();
        loadPlayers();
    }

    private void loadSeparator() {
        ItemStack glass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse(" "));
            glass.setItemMeta(meta);
        }
        for (int slot = 27; slot <= 35; slot++) {
            setItem(slot, new GUIItem(glass));
        }
    }

    private void initButtons() {
        setItem(0, new GUIItem(buildBackItem()) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (previousGUI != null) {
                    previousGUI.openGUI(player);
                }
            }
        });

        boolean allSelected = !trustedPlayers.isEmpty() && selected.size() == trustedPlayers.size();
        ItemStack toggleItem = new ItemStack(allSelected ? Material.GRAY_WOOL : Material.LIME_WOOL);
        ItemMeta toggleMeta = toggleItem.getItemMeta();
        if (toggleMeta != null) {
            toggleMeta.setDisplayName(ColorParser.parse(allSelected ? "&c取消全选" : "&a全选"));
            toggleItem.setItemMeta(toggleMeta);
        }
        setItem(3, new GUIItem(toggleItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (allSelected) {
                    selected.clear();
                } else {
                    for (ResidencePlayer rp : trustedPlayers) {
                        selected.add(rp.getUniqueId());
                    }
                }
                new ResidenceBatchSelectUI(player, residence, trustedPlayers, selected, previousGUI).openGUI(player);
            }
        });

        int count = selected.size();
        ItemStack nextItem = new ItemStack(count > 0 ? Material.EMERALD_BLOCK : Material.BARRIER);
        ItemMeta nextMeta = nextItem.getItemMeta();
        if (nextMeta != null) {
            if (count > 0) {
                nextMeta.setDisplayName(ColorParser.parse("&a&l下一步（" + count + "）"));
                List<String> lore = new ArrayList<>();
                lore.add(ColorParser.parse("&7"));
                lore.add(ColorParser.parse("&7已选择 &f" + count + " &7名玩家"));
                lore.add(ColorParser.parse("&7"));
                lore.add(ColorParser.parse("&e▶ 左键点击 &8| &f进入批量编辑权限页面"));
                nextMeta.setLore(lore);
            } else {
                nextMeta.setDisplayName(ColorParser.parse("&c&l未选择玩家"));
                List<String> lore = new ArrayList<>();
                lore.add(ColorParser.parse("&7请先选择至少一名玩家"));
                nextMeta.setLore(lore);
            }
            nextItem.setItemMeta(nextMeta);
        }
        setItem(8, new GUIItem(nextItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                if (selected.isEmpty()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                List<UUID> selectedList = new ArrayList<>(selected);
                new ResidencePlayerPermUI(player, residence, previousGUI, selectedList, 1).openGUI(player);
            }
        });
    }

    private void loadPlayers() {
        List<ResidencePlayer> selectedList = trustedPlayers.stream()
                .filter(rp -> selected.contains(rp.getUniqueId()))
                .collect(Collectors.toList());
        List<ResidencePlayer> unselectedList = trustedPlayers.stream()
                .filter(rp -> !selected.contains(rp.getUniqueId()))
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(selectedList.size(), SELECTED_SLOTS.length); i++) {
            setItem(SELECTED_SLOTS[i], createPlayerItem(selectedList.get(i)));
        }
        for (int i = 0; i < Math.min(unselectedList.size(), UNSELECTED_SLOTS.length); i++) {
            setItem(UNSELECTED_SLOTS[i], createPlayerItem(unselectedList.get(i)));
        }
    }

    protected GUIItem createPlayerItem(@NotNull ResidencePlayer rp) {
        UUID uuid = rp.getUniqueId();
        String playerName = rp.getName();
        String displayName = playerName != null ? playerName : "Unknown";
        boolean isSelected = selected.contains(uuid);

        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) headItem.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            if (isSelected) {
                headMeta.setDisplayName(ColorParser.parse("&a[ ✓ ] &f" + displayName));
            } else {
                headMeta.setDisplayName(ColorParser.parse("&7[   ] &f" + displayName));
            }

            List<String> lore = new ArrayList<>();
            if (isSelected) {
                lore.add(ColorParser.parse("&a已选择"));
                lore.add(ColorParser.parse("&7"));
                lore.add(ColorParser.parse("&e▶ 左键 &8| &f取消选择"));
            } else {
                lore.add(ColorParser.parse("&7未选择"));
                lore.add(ColorParser.parse("&7"));
                lore.add(ColorParser.parse("&e▶ 左键 &8| &f选择该玩家"));
            }
            headMeta.setLore(lore);
            headMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            headItem.setItemMeta(headMeta);

            if (isSelected) {
                headItem.addUnsafeEnchantment(Enchantment.LURE, 1);
            }
        }

        return new GUIItem(headItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                if (!clickType.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (isSelected) {
                    selected.remove(uuid);
                } else {
                    selected.add(uuid);
                }
                new ResidenceBatchSelectUI(player, residence, trustedPlayers, selected, previousGUI).openGUI(player);
            }
        };
    }

    private ItemStack buildBackItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&c返回"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
