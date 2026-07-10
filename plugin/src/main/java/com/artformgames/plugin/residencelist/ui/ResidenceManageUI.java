package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.configuration.Configuration;
import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.easyplugin.gui.paged.AutoPagedGUI;
import cc.carm.lib.mineconfiguration.bukkit.value.ConfiguredMessage;
import cc.carm.lib.mineconfiguration.bukkit.value.item.ConfiguredItem;
import cc.carm.lib.mineconfiguration.bukkit.value.item.PreparedItem;
import com.artformgames.plugin.residencelist.Main;
import com.artformgames.plugin.residencelist.ResidenceListAPI;
import com.artformgames.plugin.residencelist.api.residence.ResidenceData;
import com.artformgames.plugin.residencelist.api.residence.ResidenceRate;
import com.artformgames.plugin.residencelist.api.user.UserListData;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.listener.AnvilNameInput;
import com.artformgames.plugin.residencelist.utils.GUIUtils;
import com.artformgames.plugin.residencelist.utils.ResidenceUtils;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ResidenceManageUI extends AutoPagedGUI {

    public static void open(@NotNull Player player, @NotNull ResidenceData data, @Nullable GUI previousGUI) {
        UserListData user = ResidenceListAPI.getUserManager().getNullable(player.getUniqueId());
        if (user == null) {
            PluginMessages.LOAD_FAILED.sendTo(player);
            return;
        }
        new ResidenceManageUI(player, user, data, previousGUI).openGUI(player);
    }

    protected @NotNull Player viewer;
    protected final @NotNull UserListData userData;
    protected final @NotNull ResidenceData residenceData;
    protected @Nullable GUI previousGUI;

    public ResidenceManageUI(@NotNull Player viewer,
                             @NotNull UserListData userData, @NotNull ResidenceData residenceData,
                             @Nullable GUI previousGUI) {
        super(GUIType.SIX_BY_NINE, CONFIG.TITLE.parseLine(viewer, residenceData.getDisplayName()), 28, 52);
        this.viewer = viewer;
        this.userData = userData;
        this.residenceData = residenceData;
        this.previousGUI = previousGUI;

        setPreviousPageSlot(36);
        setNextPageSlot(44);
        setPreviousPageUI(PluginConfig.ICON.PAGE.PREVIOUS_PAGE.get(viewer));
        setNextPageUI(PluginConfig.ICON.PAGE.NEXT_PAGE.get(viewer));
        setNoPreviousPageUI(PluginConfig.ICON.PAGE.NO_PREVIOUS_PAGE.get(viewer));
        setNoNextPageUI(PluginConfig.ICON.PAGE.NO_NEXT_PAGE.get(viewer));
        setEmptyItem(PluginConfig.ICON.EMPTY.get(viewer));

        initItems();
        loadIcon();
        loadStatus();
        loadRates();
        loadPermissions();
        loadAdvanced();
    }

    public @NotNull Player getViewer() {
        return viewer;
    }

    public UserListData getPlayerData() {
        return userData;
    }

    public @NotNull ResidenceData getResidenceData() {
        return residenceData;
    }

    public void loadIcon() {
        setItem(11, generateIcon(getPlayerData(), getResidenceData().getResidence()));
    }

    public void initItems() {

        if (this.previousGUI != null) {
            setItem(0, new GUIItem(CONFIG.ITEMS.BACK.get(viewer)) {
                @Override
                public void onClick(Player player, ClickType clickType) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    previousGUI.openGUI(player);
                }
            });
        }
        Location teleportLocation = getResidenceData().getTeleportLocation(getViewer(), getViewer().getLocation());
        setItem(13, new GUIItem(CONFIG.ITEMS.TELEPORT.prepare(
                getResidenceData().getResidence().getMainArea().getWorldName(),
                teleportLocation.getBlockX(), teleportLocation.getBlockY(), teleportLocation.getBlockZ()
        ).get(getViewer())) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (type.isLeftClick()) {
                    getResidenceData().getResidence().tpToResidence(clicker, clicker, clicker.hasPermission("residence.admin"));
                    PluginMessages.TELEPORT.SOUND.playTo(clicker);
                } else if (type.isRightClick()) {
                    getResidenceData().getResidence().setTpLoc(clicker, clicker.hasPermission("residence.admin"));
                    PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                }
            }
        });

        setItem(14, new GUIItem(CONFIG.ITEMS.INFORMATION.get(getViewer())) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (type.isShiftClick()) {
                    clicker.closeInventory();
                    PluginMessages.EDIT.EDIT_SOUND.playTo(getViewer());
                    SelectIconGUI.open(clicker, ((player, itemStack) -> {
                        Material material = itemStack.getType();
                        if (PluginConfig.SETTINGS.BLOCKED_ICON_TYPES.contains(material)) {
                            PluginMessages.EDIT.ICON_BLOCKED.sendTo(player, getResidenceData().getDisplayName());
                            PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                        } else {
                            getResidenceData().modify(d -> d.setIconMaterial(itemStack));
                            PluginMessages.EDIT.ICON_UPDATED.sendTo(player, getResidenceData().getDisplayName());
                            PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                        }

                        loadIcon();
                        openGUI(player);
                    }));
                } else if (type.isLeftClick()) {
                    PluginMessages.EDIT.EDIT_SOUND.playTo(getViewer());
                } else if (type.isRightClick()) {
                    PluginMessages.EDIT.EDIT_SOUND.playTo(getViewer());
                }
            }
        });
    }

    public void loadStatus() {
        if (getResidenceData().isPublicDisplayed()) {
            setItem(15, new GUIItem(CONFIG.ITEMS.PUBLIC.get(getViewer())) {
                @Override
                public void onClick(Player clicker, ClickType type) {
                    getResidenceData().modify(d -> d.setPublicDisplayed(false));
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(clicker);
                    loadStatus();
                    updateView();
                }
            });
        } else {
            setItem(15, new GUIItem(CONFIG.ITEMS.PRIVATE.get(getViewer())) {
                @Override
                public void onClick(Player clicker, ClickType type) {
                    getResidenceData().modify(d -> d.setPublicDisplayed(true));
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(clicker);
                    loadStatus();
                    updateView();
                }
            });
        }
    }

    public void loadPermissions() {
        ClaimedResidence residence = getResidenceData().getResidence();

        setItem(12, new GUIItem(CONFIG.ITEMS.PERMISSIONS.get(getViewer())) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                PermissionChoiceGUI.open(clicker, residence, ResidenceManageUI.this);
            }
        });
    }

    public static class PermissionChoiceGUI extends GUI {

        public static void open(@NotNull Player player, @NotNull ClaimedResidence residence, @Nullable GUI previousGUI) {
            new PermissionChoiceGUI(player, residence, previousGUI).openGUI(player);
        }

        protected final @NotNull Player viewer;
        protected final @NotNull ClaimedResidence residence;
        protected final @Nullable GUI previousGUI;

        public PermissionChoiceGUI(@NotNull Player viewer, @NotNull ClaimedResidence residence, @Nullable GUI previousGUI) {
            super(GUIType.THREE_BY_NINE, CONFIG.PERMISSIONS_TITLE.parseLine(viewer));
            this.viewer = viewer;
            this.residence = residence;
            this.previousGUI = previousGUI;

            setEmptyItem(PluginConfig.ICON.EMPTY.get(viewer));

            if (previousGUI != null) {
                setItem(0, new GUIItem(CONFIG.ITEMS.BACK.get(viewer)) {
                    @Override
                    public void onClick(Player player, ClickType clickType) {
                        PluginConfig.GUI.CLICK_SOUND.playTo(player);
                        previousGUI.openGUI(player);
                    }
                });
            }

            setItem(11, new GUIItem(CONFIG.ITEMS.GLOBAL_PERMISSIONS.get(viewer)) {
                @Override
                public void onClick(Player clicker, ClickType type) {
                    if (!type.isLeftClick()) return;
                    PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                    ResidenceFlagUI.open(clicker, residence, PermissionChoiceGUI.this);
                }
            });

            setItem(15, new GUIItem(CONFIG.ITEMS.PLAYER_PERMISSIONS.get(viewer)) {
                @Override
                public void onClick(Player clicker, ClickType type) {
                    if (!type.isLeftClick()) return;
                    PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                    ResidencePlayerPermUI.open(clicker, residence, PermissionChoiceGUI.this);
                }
            });
        }
    }

    public void loadAdvanced() {
        ClaimedResidence residence = getResidenceData().getResidence();

        setItem(21, new GUIItem(CONFIG.ITEMS.RENAME.get(getViewer())) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                clicker.closeInventory();
                AnvilNameInput.open(clicker, "输入新领地名称", residence.getName(), (p, text) -> {
                    if (text == null || text.trim().isEmpty()) {
                        open(p, getResidenceData(), previousGUI);
                        return;
                    }
                    if (!ResidenceUtils.canManage(p, residence)) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                        open(p, getResidenceData(), previousGUI);
                        return;
                    }
                    boolean success = ResidenceUtils.renameResidence(p, residence, text.trim());
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(p);
                        open(p, ResidenceListAPI.getResidenceData(residence), previousGUI);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                        open(p, getResidenceData(), previousGUI);
                    }
                });
            }
        });

        setItem(22, new GUIItem(CONFIG.ITEMS.RESET_PERMISSIONS.get(getViewer())) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isShiftClick() || !type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                if (!ResidenceUtils.canManage(clicker, residence)) {
                    PluginMessages.EDIT.FAILED_SOUND.playTo(clicker);
                    return;
                }
                ResidenceUtils.resetPermissions(residence);
                PluginMessages.EDIT.SUCCESS_SOUND.playTo(clicker);
                open(clicker, getResidenceData(), previousGUI);
            }
        });

        setItem(23, new GUIItem(CONFIG.ITEMS.MIRROR_PERMISSIONS.get(getViewer())) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                clicker.closeInventory();
                AnvilNameInput.open(clicker, "输入源领地名称", "源领地名称", (p, text) -> {
                    if (text == null || text.trim().isEmpty()) {
                        open(p, getResidenceData(), previousGUI);
                        return;
                    }
                    ClaimedResidence source = ResidenceListAPI.getResidence(text.trim());
                    if (source == null) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                        open(p, getResidenceData(), previousGUI);
                        return;
                    }
                    if (!ResidenceUtils.canManage(p, residence) || !source.isOwner(p)) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                        open(p, getResidenceData(), previousGUI);
                        return;
                    }
                    ResidenceUtils.mirrorPermissions(p, residence, source);
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(p);
                    open(p, getResidenceData(), previousGUI);
                });
            }
        });

        setItem(24, new GUIItem(CONFIG.ITEMS.ENTER_MESSAGE.get(getViewer())) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                clicker.closeInventory();
                String current = ResidenceUtils.getEnterMessage(residence);
                AnvilNameInput.open(clicker, "设置进入提示", current != null ? current : "", (p, text) -> {
                    if (!ResidenceUtils.canManage(p, residence)) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                        open(p, getResidenceData(), previousGUI);
                        return;
                    }
                    ResidenceUtils.setEnterMessage(residence, text != null && !text.trim().isEmpty() ? text : null);
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(p);
                    open(p, getResidenceData(), previousGUI);
                });
            }
        });

        setItem(25, new GUIItem(CONFIG.ITEMS.LEAVE_MESSAGE.get(getViewer())) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                clicker.closeInventory();
                String current = ResidenceUtils.getLeaveMessage(residence);
                AnvilNameInput.open(clicker, "设置离开提示", current != null ? current : "", (p, text) -> {
                    if (!ResidenceUtils.canManage(p, residence)) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                        open(p, getResidenceData(), previousGUI);
                        return;
                    }
                    ResidenceUtils.setLeaveMessage(residence, text != null && !text.trim().isEmpty() ? text : null);
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(p);
                    open(p, getResidenceData(), previousGUI);
                });
            }
        });
    }

    public void loadRates() {
        if (getResidenceData().getRates().isEmpty()) {
            setItem(40, new GUIItem(CONFIG.ITEMS.EMPTY.get(getViewer())));
            return;
        }

        for (ResidenceRate value : getResidenceData().getRates().values()) {
            ConfiguredItem item = value.recommend() ? PluginConfig.ICON.RATE.LIKE : PluginConfig.ICON.RATE.DISLIKE;
            PreparedItem preparedItem = item.prepare(
                    Optional.ofNullable(value.getAuthorName()).orElse("?"),
                    PluginConfig.DATETIME_FORMATTER.format(value.time())
            );
            preparedItem.setSkullOwner(value.author());
            preparedItem.insert("comment", GUIUtils.sortContent(value.content()));
            if (allowDeletion(getViewer())) {
                preparedItem.insert("click-lore", CONFIG.ADDITIONAL_LORE.REMOVE);
            }
            addItem(new GUIItem(preparedItem.get(getViewer())) {
                @Override
                public void onClick(Player clicker, ClickType type) {
                    if (allowDeletion(getViewer())) {
                        getResidenceData().removeRate(value.author());
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(clicker);
                        open(getViewer(), residenceData, previousGUI);
                    }
                }
            });
        }
    }

    public boolean allowDeletion(@NotNull Player player) {
        if (getViewer().hasPermission("residence.admin")) return true;

        if (PluginConfig.SETTINGS.ALLOW_OWNER_DELETE_RATE.resolve()) {
            return residenceData.isOwner(player);
        }

        return false;
    }

    protected GUIItem generateIcon(UserListData userData, ClaimedResidence residence) {
        ResidenceData residenceData = Main.getInstance().getResidenceManager().getResidence(residence);
        PreparedItem icon = PluginConfig.ICON.INFO.prepare(
                this.residenceData.getDisplayName(), this.residenceData.getOwner(),
                residence.getTrustedPlayers().size() + 1, residence.getMainArea().getSize(),
                this.residenceData.countRate(ResidenceRate::recommend), this.residenceData.countRate(r -> !r.recommend())
        );
        icon.insert("click-lore", CONFIG.ADDITIONAL_LORE.CLICK);
        if (!getResidenceData().getDescription().isEmpty())
            icon.insert("description", getResidenceData().getDescription());
        if (userData.isPinned(residence.getName())) icon.glow();
        if (this.residenceData.getIconMaterial() != null) {
            icon.handleItem((i, p) -> i.setType(this.residenceData.getIconMaterial()));
            if (this.residenceData.getCustomModelData() > 0) {
                icon.handleMeta((itemMeta, player) -> itemMeta.setCustomModelData(this.residenceData.getCustomModelData()));
            }
        }
        return new GUIItem(icon.get(viewer)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (type.isLeftClick()) {      // Pin/Unpin
                    if (userData.isPinned(residence.getName())) {
                        userData.removePin(residence.getName());
                        PluginMessages.UNPIN.SOUND.playTo(clicker);
                        PluginMessages.UNPIN.MESSAGE.sendTo(clicker, residenceData.getDisplayName());
                    } else {
                        userData.setPin(residence.getName(), 0);
                        PluginMessages.PIN.SOUND.playTo(clicker);
                        PluginMessages.PIN.MESSAGE.sendTo(clicker, residenceData.getDisplayName());
                    }
                    loadIcon();
                    updateView();
                }
            }
        };
    }

    public interface CONFIG extends Configuration {

        ConfiguredMessage<String> TITLE = ConfiguredMessage.asString()
                .defaults("&a&l详细信息 &7#&f%(name)")
                .params("name").build();

        ConfiguredMessage<String> PERMISSIONS_TITLE = ConfiguredMessage.asString()
                .defaults("&e&l权限管理")
                .build();

        interface ITEMS extends Configuration {

            ConfiguredItem BACK = ConfiguredItem.create()
                    .defaultType(Material.REDSTONE_TORCH)
                    .defaultName("&c返回").build();

            ConfiguredItem TELEPORT = ConfiguredItem.create()
                    .defaultType(Material.ENDER_EYE)
                    .defaultName("&d&l传送")
                    .defaultLore(
                            "&7",
                            "&7领地坐标:",
                            "&f%(world)&7@&f%(x)&7,&f%(y),&f%(z)",
                            "",
                            "&e&l ▶ &l左键点击 &8|&f 传送到领地传送点",
                            "&e&l ▶ &l右键点击 &8|&f 设置当前所处位置为此领地传送点"
                    ).params("world", "x", "y", "z").build();

            ConfiguredItem INFORMATION = ConfiguredItem.create()
                    .defaultType(Material.OAK_SIGN)
                    .defaultName("&e&l编辑领地信息")
                    .defaultLore(
                            "",
                            "&e&l ▶ &l左键点击 &8|&f 设置领地昵称(别名)",
                            "&e&l ▶ &l右键点击 &8|&f 设置领地描述",
                            "&e&l ▶ &l中键点击 &8|&f 设置领地图标"
                    ).build();

            ConfiguredItem PUBLIC = ConfiguredItem.create()
                    .defaultType(Material.LIME_DYE)
                    .defaultName("&7当前状态: &a&l公开")
                    .defaultLore(
                            " ",
                            "&7现在所有的玩家都可以在公开列表内看到该领地.",
                            " ",
                            "&e&l ▶ &l左键点击 &8|&f 切换至 &c&l私有"
                    ).build();

            ConfiguredItem PRIVATE = ConfiguredItem.create()
                    .defaultType(Material.GRAY_DYE)
                    .defaultName("&7当前状态: &c&l私有")
                    .defaultLore(
                            " ",
                            "&7现在只有你能够看到此领地",
                            "&7其他玩家无法在公开列表看到该领地",
                            " ",
                            "&e&l ▶ &l左键点击 &8|&f 切换至 &a&l公开"
                    ).build();


            ConfiguredItem EMPTY = ConfiguredItem.create()
                    .defaultType(Material.BARRIER)
                    .defaultName("&7无评论")
                    .defaultLore(
                            "&7目前暂无评论"
                    ).build();

            ConfiguredItem PERMISSIONS = ConfiguredItem.create()
                    .defaultType(Material.BOOK)
                    .defaultName("&e&l权限管理")
                    .defaultLore(
                            "&7",
                            "&7管理领地的全局权限与玩家权限",
                            "&7",
                            "&e▶ 左键点击 &8| &f打开权限管理菜单"
                    ).build();

            ConfiguredItem GLOBAL_PERMISSIONS = ConfiguredItem.create()
                    .defaultType(Material.BOOK)
                    .defaultName("&e&l全局权限")
                    .defaultLore(
                            "&7",
                            "&7管理领地的全局权限设置",
                            "&7会影响到所有未单独设置权限的玩家",
                            "&7",
                            "&e▶ 左键点击 &8| &f打开全局权限编辑"
                    ).build();

            ConfiguredItem PLAYER_PERMISSIONS = ConfiguredItem.create()
                    .defaultType(Material.WRITABLE_BOOK)
                    .defaultName("&e&l玩家权限")
                    .defaultLore(
                            "&7",
                            "&7管理指定玩家的领地权限",
                            "&7可添加、移除信任玩家及其权限",
                            "&7",
                            "&e▶ 左键点击 &8| &f打开玩家权限编辑"
                    ).build();

            ConfiguredItem RENAME = ConfiguredItem.create()
                    .defaultType(Material.NAME_TAG)
                    .defaultName("&e&l重命名领地")
                    .defaultLore(
                            "&7",
                            "&7修改领地的实际名称",
                            "&7",
                            "&e▶ 左键点击 &8| &f输入新名称"
                    ).build();

            ConfiguredItem RESET_PERMISSIONS = ConfiguredItem.create()
                    .defaultType(Material.TNT)
                    .defaultName("&c&l重置权限")
                    .defaultLore(
                            "&7",
                            "&7将所有权限恢复为默认值",
                            "&c▶ Shift+左键 &8| &f确认重置"
                    ).build();

            ConfiguredItem MIRROR_PERMISSIONS = ConfiguredItem.create()
                    .defaultType(Material.HOPPER)
                    .defaultName("&e&l镜像权限")
                    .defaultLore(
                            "&7",
                            "&7从另一块领地复制权限设置到当前领地",
                            "&7",
                            "&e▶ 左键点击 &8| &f输入源领地名称"
                    ).build();

            ConfiguredItem ENTER_MESSAGE = ConfiguredItem.create()
                    .defaultType(Material.OAK_SIGN)
                    .defaultName("&e&l进入提示")
                    .defaultLore(
                            "&7",
                            "&7设置玩家进入领地时显示的提示",
                            "&7留空即可清除当前提示",
                            "&7",
                            "&e▶ 左键点击 &8| &f编辑进入提示"
                    ).build();

            ConfiguredItem LEAVE_MESSAGE = ConfiguredItem.create()
                    .defaultType(Material.BIRCH_SIGN)
                    .defaultName("&e&l离开提示")
                    .defaultLore(
                            "&7",
                            "&7设置玩家离开领地时显示的提示",
                            "&7留空即可清除当前提示",
                            "&7",
                            "&e▶ 左键点击 &8| &f编辑离开提示"
                    ).build();

        }

        interface ADDITIONAL_LORE extends Configuration {

            ConfiguredMessage<String> CLICK = ConfiguredMessage.asString().defaults(
                    "&e&l ▶ &l左键点击 &8|&f 置顶/取消置顶"
            ).build();

            ConfiguredMessage<String> REMOVE = ConfiguredMessage.asString().defaults(
                    "&e&l ▶ &l左键点击 &8|&f 删除该条评论 &c(管理员权限)"
            ).build();

        }

    }
}
