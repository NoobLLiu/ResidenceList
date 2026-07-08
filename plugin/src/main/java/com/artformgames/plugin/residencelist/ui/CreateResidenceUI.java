package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.configuration.Configuration;
import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.mineconfiguration.bukkit.value.ConfiguredItem;
import cc.carm.lib.mineconfiguration.bukkit.value.ConfiguredMessage;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.listener.AnvilNameInput;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.permissions.PermissionGroup;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CreateResidenceUI extends GUI {

    public static void open(@NotNull Player player, @Nullable String owner) {
        new CreateResidenceUI(player, owner).openGUI(player);
    }

    protected final @NotNull Player viewer;
    protected final @Nullable String owner;

    public CreateResidenceUI(@NotNull Player viewer, @Nullable String owner) {
        super(GUIType.SIX_BY_NINE, CONFIG.TITLE.parseLine(viewer));
        this.viewer = viewer;
        this.owner = owner;
        setEmptyItem(PluginConfig.ICON.EMPTY.get(viewer));
        initItems();
    }

    public void initItems() {
        // 步骤指引（左侧列）
        setItem(11, new GUIItem(CONFIG.ITEMS.STEP_1.get(viewer)));
        setItem(20, new GUIItem(CONFIG.ITEMS.STEP_2.get(viewer)));
        setItem(29, new GUIItem(CONFIG.ITEMS.STEP_3.get(viewer)));

        // 选区信息展示（中间）
        setItem(22, new GUIItem(buildSelectionInfoItem()));

        // 底部按钮栏
        // 按钮1: 自动选取开关 (slot 45)
        boolean autoEnabled = Residence.getInstance().getAutoSelectionManager()
                .getList().containsKey(viewer.getUniqueId());
        ConfiguredItem autoItem = autoEnabled
                ? CONFIG.ITEMS.AUTO_SELECT_ENABLED : CONFIG.ITEMS.AUTO_SELECT_DISABLED;
        setItem(45, new GUIItem(autoItem.get(viewer)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                clicker.closeInventory();
                Residence.getInstance().getAutoSelectionManager().switchAutoSelection(clicker);
                CreateResidenceUI.open(clicker, owner);
            }
        });

        // 按钮2: 确认选取范围和价格 (slot 49)
        setItem(49, new GUIItem(buildConfirmPriceItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                CreateResidenceUI.open(clicker, owner);
            }
        });

        // 返回列表按钮 (slot 48)
        setItem(48, new GUIItem(CONFIG.ITEMS.BACK.get(viewer)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                clicker.closeInventory();
                ResidenceListUI.open(clicker, owner);
            }
        });

        // 按钮3: 确认购买 (slot 53)
        boolean hasSelection = Residence.getInstance().getSelectionManager().hasPlacedBoth(viewer);
        setItem(53, new GUIItem(hasSelection
                ? CONFIG.ITEMS.PURCHASE_ENABLED.get(viewer)
                : CONFIG.ITEMS.PURCHASE_DISABLED.get(viewer)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                if (!Residence.getInstance().getSelectionManager().hasPlacedBoth(clicker)) {
                    PluginMessages.CREATE.NO_SELECTION.sendTo(clicker);
                    PluginMessages.CREATE.FAILED_SOUND.playTo(clicker);
                    return;
                }

                clicker.closeInventory();
                PluginMessages.CREATE.ANVIL_OPEN.sendTo(clicker);
                AnvilNameInput.open(clicker, PluginMessages.CREATE.ANVIL_TITLE.parseLine(clicker),
                        PluginMessages.CREATE.ANVIL_DEFAULT.parseLine(clicker),
                        (player, name) -> {
                    if (name == null || name.isBlank()) {
                        PluginMessages.CREATE.FAILED_SOUND.playTo(player);
                        CreateResidenceUI.open(player, owner);
                        return;
                    }

                    // 等效 /res create：resadmin=false, deductMoney=true
                    boolean success = Residence.getInstance().getResidenceManager()
                            .addResidence(player, name, false, true);

                    if (success) {
                        PluginMessages.CREATE.SUCCESS.sendTo(player, name);
                        PluginMessages.CREATE.SUCCESS_SOUND.playTo(player);
                        ResidenceListUI.open(player, owner);
                    } else {
                        PluginMessages.CREATE.FAILED_SOUND.playTo(player);
                        CreateResidenceUI.open(player, owner);
                    }
                });
            }
        });
    }

    /**
     * 构建选区信息展示物品（动态显示当前选区状态、大小、价格）
     */
    private ItemStack buildSelectionInfoItem() {
        boolean hasSelection = Residence.getInstance().getSelectionManager().hasPlacedBoth(viewer);

        if (!hasSelection) {
            return CONFIG.ITEMS.SELECTION_EMPTY.get(viewer);
        }

        CuboidArea area = Residence.getInstance().getSelectionManager().getSelectionCuboid(viewer);
        if (area == null) {
            return CONFIG.ITEMS.SELECTION_EMPTY.get(viewer);
        }

        ResidencePlayer rPlayer = Residence.getInstance().getPlayerManager().getResidencePlayer(viewer);
        PermissionGroup group = rPlayer.getGroup();
        double cost = area.getCost(group);
        boolean economyEnabled = Residence.getInstance().getConfigManager().enableEconomy();
        boolean chargeOnCreation = Residence.getInstance().getConfigManager().isChargeOnCreation();

        // 构建动态信息物品
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§l选区信息");

            List<String> lore = new ArrayList<>();
            lore.add("§7");
            lore.add("§f尺寸: §e" + area.getXSize() + "×" + area.getYSize() + "×" + area.getZSize());
            lore.add("§f总面积: §e" + area.getSize() + " §7方块");
            lore.add("§f世界: §e" + area.getWorldName());

            Location low = area.getLowLocation();
            Location high = area.getHighLocation();
            if (low != null && high != null) {
                lore.add("§f坐标: §7(" + low.getBlockX() + "," + low.getBlockY() + "," + low.getBlockZ()
                        + ") §7→ §7(" + high.getBlockX() + "," + high.getBlockY() + "," + high.getBlockZ() + ")");
            }

            lore.add("§7");
            if (economyEnabled && chargeOnCreation && cost > 0) {
                String formatted = Residence.getInstance().getEconomyManager() != null
                        ? Residence.getInstance().getEconomyManager().format(cost)
                        : String.format("%.2f", cost);
                lore.add("§f创建费用: §e" + formatted);
            } else {
                lore.add("§f创建费用: §a免费");
            }

            int currentCount = rPlayer.getResAmount();
            int maxCount = rPlayer.getMaxRes();
            lore.add("§f领地数量: §e" + currentCount + "§7/" + maxCount);

            lore.add("§7");
            lore.add("§a ▶ 点击 §8|§f 刷新信息");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 构建确认价格按钮物品（物品名称显示价格概要）
     */
    private ItemStack buildConfirmPriceItem() {
        boolean hasSelection = Residence.getInstance().getSelectionManager().hasPlacedBoth(viewer);

        if (!hasSelection) {
            return CONFIG.ITEMS.CONFIRM_PRICE_EMPTY.get(viewer);
        }

        CuboidArea area = Residence.getInstance().getSelectionManager().getSelectionCuboid(viewer);
        if (area == null) {
            return CONFIG.ITEMS.CONFIRM_PRICE_EMPTY.get(viewer);
        }

        ResidencePlayer rPlayer = Residence.getInstance().getPlayerManager().getResidencePlayer(viewer);
        PermissionGroup group = rPlayer.getGroup();
        double cost = area.getCost(group);
        boolean economyEnabled = Residence.getInstance().getConfigManager().enableEconomy();
        boolean chargeOnCreation = Residence.getInstance().getConfigManager().isChargeOnCreation();

        // 物品名称直接显示价格信息
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String priceText;
            if (economyEnabled && chargeOnCreation && cost > 0) {
                String formatted = Residence.getInstance().getEconomyManager() != null
                        ? Residence.getInstance().getEconomyManager().format(cost)
                        : String.format("%.2f", cost);
                priceText = "§a§l确认范围和价格 §7(" + formatted + ")";
            } else {
                priceText = "§a§l确认范围和价格 §7(免费)";
            }
            meta.setDisplayName(priceText);

            List<String> lore = new ArrayList<>();
            lore.add("§7");
            lore.add("§f当前选区: §e" + area.getXSize() + "×" + area.getYSize() + "×" + area.getZSize());
            lore.add("§f总面积: §e" + area.getSize() + " §7方块");
            if (economyEnabled && chargeOnCreation && cost > 0) {
                String formatted = Residence.getInstance().getEconomyManager() != null
                        ? Residence.getInstance().getEconomyManager().format(cost)
                        : String.format("%.2f", cost);
                lore.add("§f预计费用: §e" + formatted);
            }
            lore.add("§7");
            lore.add("§a ▶ 点击 §8|§f 刷新选区信息");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public interface CONFIG extends Configuration {

        ConfiguredMessage<String> TITLE = ConfiguredMessage.asString()
                .defaults("&a&l创建领地")
                .build();

        interface ITEMS extends Configuration {

            // 步骤指引
            ConfiguredItem STEP_1 = ConfiguredItem.create()
                    .defaultType(Material.WOODEN_AXE)
                    .defaultName("&e&l第一步 &7|&f 选取区域")
                    .defaultLore(
                            "&7",
                            "&7使用领地选取工具（铲子/木斧）",
                            "&7左键和右键分别选取两个对角点",
                            "&7或者使用下方的 &a自动选取工具 &7开关",
                            "&7走动时自动扩展选区范围"
                    ).build();

            ConfiguredItem STEP_2 = ConfiguredItem.create()
                    .defaultType(Material.GOLD_INGOT)
                    .defaultName("&e&l第二步 &7|&f 确认范围和价格")
                    .defaultLore(
                            "&7",
                            "&7查看中间的信息面板确认选区",
                            "&7或鼠标悬停 &a确认范围和价格 &7按钮",
                            "&7查看选区尺寸和创建费用",
                            "&7",
                            "&7如果费用不足，请重新选取区域"
                    ).build();

            ConfiguredItem STEP_3 = ConfiguredItem.create()
                    .defaultType(Material.EMERALD)
                    .defaultName("&e&l第三步 &7|&f 命名并购买")
                    .defaultLore(
                            "&7",
                            "&7确认选区无误后，点击 &a确认购买 &7按钮",
                            "&7在弹出的铁砧界面中输入领地名称",
                            "&7取出结果物品即可完成创建",
                            "&7",
                            "&c注意: 创建后将扣除相应费用"
                    ).build();

            // 选区信息面板
            ConfiguredItem SELECTION_EMPTY = ConfiguredItem.create()
                    .defaultType(Material.BARRIER)
                    .defaultName("&c&l尚未选取区域")
                    .defaultLore(
                            "&7",
                            "&7请先使用选取工具选择两个对角点",
                            "&7或开启自动选取工具"
                    ).build();

            // 自动选取开关
            ConfiguredItem AUTO_SELECT_ENABLED = ConfiguredItem.create()
                    .defaultType(Material.EMERALD)
                    .defaultName("&a&l自动选取 &2&l[已开启]")
                    .defaultLore(
                            "&7",
                            "&7自动选取工具已 &a开启&7。",
                            "&7走动时会自动扩展选区范围。",
                            "&7",
                            "&a ▶ 点击 §8|§f 关闭自动选取"
                    ).build();

            ConfiguredItem AUTO_SELECT_DISABLED = ConfiguredItem.create()
                    .defaultType(Material.REDSTONE)
                    .defaultName("&c&l自动选取 &4&l[已关闭]")
                    .defaultLore(
                            "&7",
                            "&7自动选取工具已 &c关闭&7。",
                            "&7请使用铲子/木斧手动选取区域。",
                            "&7",
                            "&a ▶ 点击 §8|§f 开启自动选取"
                    ).build();

            // 确认价格按钮（无选区时）
            ConfiguredItem CONFIRM_PRICE_EMPTY = ConfiguredItem.create()
                    .defaultType(Material.GOLD_NUGGET)
                    .defaultName("&7&l确认范围和价格 &8(无选区)")
                    .defaultLore(
                            "&7",
                            "&c请先选取区域后再确认价格"
                    ).build();

            // 确认购买按钮
            ConfiguredItem PURCHASE_ENABLED = ConfiguredItem.create()
                    .defaultType(Material.EMERALD)
                    .defaultName("&a&l确认购买")
                    .defaultLore(
                            "&7",
                            "&7选区已就绪！",
                            "&7点击后将在铁砧界面输入领地名称",
                            "&7并扣除相应费用完成创建",
                            "&7",
                            "&a ▶ 点击 §8|§f 开始创建"
                    ).build();

            ConfiguredItem PURCHASE_DISABLED = ConfiguredItem.create()
                    .defaultType(Material.BARRIER)
                    .defaultName("&c&l确认购买 &7(未选取)")
                    .defaultLore(
                            "&7",
                            "&c请先选取区域后再购买"
                    ).build();

            // 返回按钮
            ConfiguredItem BACK = ConfiguredItem.create()
                    .defaultType(Material.ARROW)
                    .defaultName("&7&l返回领地列表")
                    .defaultLore(
                            "&7",
                            "&a ▶ 点击 §8|§f 返回"
                    ).build();
        }
    }
}
