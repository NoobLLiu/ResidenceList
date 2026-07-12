package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.easyplugin.gui.paged.AutoPagedGUI;
import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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

public class ResidenceBatchSelectUI extends AutoPagedGUI {

    public static void open(@NotNull Player player, @NotNull ClaimedResidence residence, @Nullable GUI previousGUI) {
        List<ResidencePlayer> trusted = new ArrayList<>(residence.getTrustedPlayers());
        new ResidenceBatchSelectUI(player, residence, trusted, new HashSet<>(), previousGUI).openGUI(player);
    }

    protected final @NotNull Player viewer;
    protected final @NotNull ClaimedResidence residence;
    protected final @NotNull List<ResidencePlayer> trustedPlayers;
    protected final @NotNull Set<UUID> selected;
    protected final @Nullable GUI previousGUI;

    public ResidenceBatchSelectUI(@NotNull Player viewer, @NotNull ClaimedResidence residence,
                                  @NotNull List<ResidencePlayer> trustedPlayers,
                                  @NotNull Set<UUID> selected, @Nullable GUI previousGUI) {
        super(GUIType.SIX_BY_NINE,
                ColorParser.parse("&e&l批量编辑 - 选择玩家"),
                10, 52);
        this.viewer = viewer;
        this.residence = residence;
        this.trustedPlayers = trustedPlayers;
        this.selected = selected;
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

    public void initItems() {
        setItem(0, new GUIItem(buildBackItem()) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (previousGUI != null) {
                    previousGUI.openGUI(player);
                }
            }
        });

        int count = selected.size();
        ItemStack batchItem = new ItemStack(count > 0 ? Material.EMERALD_BLOCK : Material.BARRIER);
        ItemMeta batchMeta = batchItem.getItemMeta();
        if (batchMeta != null) {
            if (count > 0) {
                batchMeta.setDisplayName(ColorParser.parse("&a&l已选玩家列表（" + count + "）"));
                List<String> lore = new ArrayList<>();
                lore.add(ColorParser.parse("&7"));
                lore.add(ColorParser.parse("&7已选择 &f" + count + " &7名玩家："));
                for (UUID uuid : selected) {
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    lore.add(ColorParser.parse(" &8- &f" + (name != null ? name : uuid.toString().substring(0, 8))));
                }
                lore.add(ColorParser.parse("&7"));
                lore.add(ColorParser.parse("&e▶ 左键点击 &8| &f进入批量编辑权限页面"));
                batchMeta.setLore(lore);
            } else {
                batchMeta.setDisplayName(ColorParser.parse("&c&l未选择玩家"));
                List<String> lore = new ArrayList<>();
                lore.add(ColorParser.parse("&7请先选择至少一名玩家"));
                batchMeta.setLore(lore);
            }
            batchItem.setItemMeta(batchMeta);
        }
        setItem(49, new GUIItem(batchItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                if (selected.isEmpty()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                List<UUID> selectedList = new ArrayList<>(selected);
                new ResidencePlayerPermUI(player, residence, previousGUI, selectedList, 1).openGUI(player);
            }
        });

        boolean allSelected = selected.size() == trustedPlayers.size() && !trustedPlayers.isEmpty();
        ItemStack toggleItem = new ItemStack(allSelected ? Material.GRAY_WOOL : Material.LIME_WOOL);
        ItemMeta toggleMeta = toggleItem.getItemMeta();
        if (toggleMeta != null) {
            if (allSelected) {
                toggleMeta.setDisplayName(ColorParser.parse("&c取消全选"));
            } else {
                toggleMeta.setDisplayName(ColorParser.parse("&a全选"));
            }
            toggleItem.setItemMeta(toggleMeta);
        }
        setItem(45, new GUIItem(toggleItem) {
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
    }

    private void loadPlayers() {
        for (ResidencePlayer rp : trustedPlayers) {
            UUID uuid = rp.getUniqueId();
            boolean isSelected = selected.contains(uuid);
            addItem(createPlayerItem(uuid, isSelected, rp.getName()));
        }
    }

    protected GUIItem createPlayerItem(UUID uuid, boolean isSelected, String playerName) {
        String displayName = playerName != null ? playerName : "Unknown";

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