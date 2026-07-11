package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.easyplugin.gui.paged.AutoPagedGUI;
import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.listener.AnvilNameInput;
import com.artformgames.plugin.residencelist.utils.ResidenceFlagCategory;
import com.artformgames.plugin.residencelist.utils.ResidenceUtils;
import com.bekvon.bukkit.residence.containers.Flags;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResidencePlayerPermUI extends AutoPagedGUI {

    public static void open(@NotNull Player player, @NotNull ClaimedResidence residence, @Nullable GUI previousGUI) {
        new ResidencePlayerPermUI(player, residence, previousGUI, null).openGUI(player);
    }

    protected final @NotNull Player viewer;
    protected final @NotNull ClaimedResidence residence;
    protected final @Nullable GUI previousGUI;
    protected final @Nullable UUID selectedPlayer;

    public ResidencePlayerPermUI(@NotNull Player viewer, @NotNull ClaimedResidence residence,
                                 @Nullable GUI previousGUI, @Nullable UUID selectedPlayer) {
        this(viewer, residence, previousGUI, selectedPlayer, 1);
    }

    public ResidencePlayerPermUI(@NotNull Player viewer, @NotNull ClaimedResidence residence,
                                 @Nullable GUI previousGUI, @Nullable UUID selectedPlayer, int page) {
        super(GUIType.SIX_BY_NINE, computeTitle(residence, selectedPlayer), 10, 52);
        this.viewer = viewer;
        this.residence = residence;
        this.previousGUI = previousGUI;
        this.selectedPlayer = selectedPlayer;

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

    private static @NotNull String computeTitle(@NotNull ClaimedResidence residence, @Nullable UUID selectedPlayer) {
        if (selectedPlayer == null) {
            return ColorParser.parse("&a&l玩家权限管理 &7#&f" + residence.getName());
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(selectedPlayer);
        String playerName = op.getName() != null ? op.getName() : "Unknown";
        return ColorParser.parse("&a&l玩家权限 &7#&f" + playerName);
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
            backMeta.setDisplayName(ColorParser.parse(selectedPlayer == null ? "&c返回" : "&c返回玩家列表"));
            backItem.setItemMeta(backMeta);
        }
        setItem(0, new GUIItem(backItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (selectedPlayer == null) {
                    if (previousGUI != null) {
                        previousGUI.openGUI(player);
                    }
                } else {
                    new ResidencePlayerPermUI(player, residence, previousGUI, null).openGUI(player);
                }
            }
        });

        if (selectedPlayer != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(selectedPlayer);
            String playerName = op.getName() != null ? op.getName() : "Unknown";
            ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta headMeta = (SkullMeta) headItem.getItemMeta();
            if (headMeta != null) {
                headMeta.setOwningPlayer(op);
                headMeta.setDisplayName(ColorParser.parse("&a&l" + playerName));
                List<String> lore = new ArrayList<>();
                lore.add(ColorParser.parse("&7当前正在编辑该玩家的权限"));
                lore.add(ColorParser.parse("&7"));
                lore.add(ColorParser.parse("&c▶ Shift+左键 &8| &f移除该玩家所有权限"));
                headMeta.setLore(lore);
                headItem.setItemMeta(headMeta);
            }
            setItem(4, new GUIItem(headItem) {
                @Override
                public void onClick(Player player, ClickType clickType) {
                    if (clickType.isShiftClick() && clickType.isLeftClick()) {
                        PluginConfig.GUI.CLICK_SOUND.playTo(player);
                        if (!ResidenceUtils.canManage(player, residence)) {
                            PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                            return;
                        }
                        boolean success = ResidenceUtils.removePlayerAllFlags(player, residence, selectedPlayer);
                        if (success) {
                            PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                            new ResidencePlayerPermUI(player, residence, previousGUI, null).openGUI(player);
                        } else {
                            PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                        }
                    }
                }
            });
        }
    }

    public void loadContent() {
        this.container.clear();
        this.page = 1;

        if (selectedPlayer == null) {
            loadPlayerList();
        } else {
            loadFlagList();
        }
        goFirstPage();
    }

    private void loadPlayerList() {
        ItemStack addItem = new ItemStack(Material.EMERALD);
        ItemMeta addMeta = addItem.getItemMeta();
        if (addMeta != null) {
            addMeta.setDisplayName(ColorParser.parse("&a&l添加信任玩家"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&7点击输入玩家名称并为其添加"));
            lore.add(ColorParser.parse("&ftrusted &7权限以允许其编辑领地"));
            lore.add(ColorParser.parse("&7"));
            lore.add(ColorParser.parse("&e▶ 左键点击 &8| &f添加信任玩家"));
            addMeta.setLore(lore);
            addItem.setItemMeta(addMeta);
        }
        setItem(4, new GUIItem(addItem) {
            @Override
            public void onClick(Player player, ClickType clickType) {
                if (!clickType.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                player.closeInventory();
                AnvilNameInput.open(player, "输入玩家名称", "玩家ID", (p, text) -> {
                    if (text == null || text.trim().isEmpty()) {
                        new ResidencePlayerPermUI(p, residence, previousGUI, null).openGUI(p);
                        return;
                    }
                    if (!ResidenceUtils.canManage(p, residence)) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                        new ResidencePlayerPermUI(p, residence, previousGUI, null).openGUI(p);
                        return;
                    }
                    OfflinePlayer target = Bukkit.getOfflinePlayer(text.trim());
                    if (target.getUniqueId() == null) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                        new ResidencePlayerPermUI(p, residence, previousGUI, null).openGUI(p);
                        return;
                    }
                    boolean success = ResidenceUtils.setPlayerFlag(p, residence, target.getUniqueId(), "trusted", "true");
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(p);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                    }
                    new ResidencePlayerPermUI(p, residence, previousGUI, null).openGUI(p);
                });
            }
        });

        for (ResidencePlayer rp : residence.getTrustedPlayers()) {
            UUID uuid = rp.getUniqueId();
            String playerName = rp.getName() != null ? rp.getName() : "Unknown";

            ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta headMeta = (SkullMeta) headItem.getItemMeta();
            if (headMeta != null) {
                headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                headMeta.setDisplayName(ColorParser.parse("&a" + playerName));
                List<String> lore = new ArrayList<>();
                lore.add(ColorParser.parse("&7点击编辑该玩家权限"));
                lore.add(ColorParser.parse("&c▶ Shift+左键 &8| &f移除该玩家所有权限"));
                headMeta.setLore(lore);
                headItem.setItemMeta(headMeta);
            }

            addItem(new GUIItem(headItem) {
                @Override
                public void onClick(Player player, ClickType clickType) {
                    if (clickType.isShiftClick() && clickType.isLeftClick()) {
                        PluginConfig.GUI.CLICK_SOUND.playTo(player);
                        if (!ResidenceUtils.canManage(player, residence)) {
                            PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                            return;
                        }
                        boolean success = ResidenceUtils.removePlayerAllFlags(player, residence, uuid);
                        if (success) {
                            PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                        } else {
                            PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                        }
                        new ResidencePlayerPermUI(player, residence, previousGUI, null).openGUI(player);
                    } else if (clickType.isLeftClick()) {
                        PluginConfig.GUI.CLICK_SOUND.playTo(player);
                        new ResidencePlayerPermUI(player, residence, previousGUI, uuid).openGUI(player);
                    }
                }
            });
        }
    }

    private void loadFlagList() {
        for (ResidenceFlagCategory cat : ResidenceFlagCategory.all()) {
            List<Flags> flags = cat.getPlayerFlags();
            if (flags.isEmpty()) continue;
            for (Flags flag : flags) {
                addItem(createFlagItem(flag));
            }
        }
    }

    protected GUIItem createFlagItem(@NotNull Flags flag) {
        String flagName = flag.name();
        OfflinePlayer op = Bukkit.getOfflinePlayer(selectedPlayer);
        String playerName = op.getName();
        if (playerName == null) return new GUIItem(new ItemStack(Material.BARRIER));

        Map<String, Boolean> playerFlags = residence.getPermissions().getPlayerFlags(playerName);
        Boolean value = playerFlags != null ? playerFlags.get(flagName) : null;

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

                boolean success = ResidenceUtils.setPlayerFlag(player, residence, selectedPlayer, flagName, state);
                if (success) {
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    ResidencePlayerPermUI newUI = new ResidencePlayerPermUI(player, residence, previousGUI, selectedPlayer, getCurrentPage());
                    newUI.openGUI(player);
                } else {
                    PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                }
            }
        };
    }
}
