package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.configuration.Configuration;
import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.mineconfiguration.bukkit.value.ConfiguredMessage;
import cc.carm.lib.mineconfiguration.bukkit.value.item.ConfiguredItem;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.listener.EditHandler;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.permissions.PermissionGroup;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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
        // ===== 三个整合按钮（说明+功能合一）=====
        // 按钮1: 第一步 - 选取圈地范围（点击切换自动选取工具）
        boolean autoEnabled = Residence.getInstance().getAutoSelectionManager()
                .getList().containsKey(viewer.getUniqueId());
        setItem(20, new GUIItem(buildStep1Item(autoEnabled)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                clicker.closeInventory();
                Residence.getInstance().getAutoSelectionManager().switchAutoSelection(clicker);
                CreateResidenceUI.open(clicker, owner);
            }
        });

        // 按钮2: 第二步 - 查看选取信息（指上去查看，点击刷新）
        setItem(22, new GUIItem(buildStep2Item()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                CreateResidenceUI.open(clicker, owner);
            }
        });

        // 按钮3: 第三步 - 输入名称并确认购买（点击进入铁砧页面）
        boolean hasSelection = Residence.getInstance().getSelectionManager().hasPlacedBoth(viewer);
        setItem(24, new GUIItem(hasSelection
                ? CONFIG.ITEMS.STEP_3_READY.get(viewer)
                : CONFIG.ITEMS.STEP_3_NOT_READY.get(viewer)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                if (!Residence.getInstance().getSelectionManager().hasPlacedBoth(clicker)) {
                    PluginMessages.CREATE.NO_SELECTION.sendTo(clicker);
                    PluginMessages.CREATE.FAILED_SOUND.playTo(clicker);
                    return;
                }

                clicker.closeInventory();
                PluginMessages.CREATE.NOTIFY.sendTo(clicker);
                PluginMessages.CREATE.ASK_SOUND.playTo(clicker);
                EditHandler.start(clicker, (player, content) -> {
                    if (content == null || content.isBlank()) {
                        PluginMessages.CREATE.FAILED_SOUND.playTo(player);
                        return;
                    }

                    // 等效 /res create <name>：通过Bukkit命令分发触发所有Residence原生检查
                    Bukkit.dispatchCommand(player, "res create " + content);
                    ResidenceListUI.open(player, owner);
                });
            }
        });

        // 返回列表按钮 (slot 49) — 退出时自动关闭自动选取模式
        setItem(49, new GUIItem(CONFIG.ITEMS.BACK.get(viewer)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                // 如果处于自动选取模式，自动关闭
                if (Residence.getInstance().getAutoSelectionManager()
                        .getList().containsKey(clicker.getUniqueId())) {
                    Residence.getInstance().getAutoSelectionManager().switchAutoSelection(clicker);
                }
                clicker.closeInventory();
                ResidenceListUI.open(clicker, owner);
            }
        });
    }

    /**
     * 构建按钮1：第一步 - 选取圈地范围 + 自动选取开关
     * 物品类型根据自动选取状态变化（绿宝石=开，红石=关）
     */
    private ItemStack buildStep1Item(boolean autoEnabled) {
        ItemStack item = new ItemStack(autoEnabled ? Material.EMERALD : Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(autoEnabled
                    ? "§e§l第一步 §7| §f选取圈地范围 §2§l[自动选取已开启]"
                    : "§e§l第一步 §7| §f选取圈地范围 §4§l[自动选取已关闭]");

            List<String> lore = new ArrayList<>();
            lore.add("§7");
            lore.add("§7使用领地选取工具（铲子/木斧）");
            lore.add("§7左键和右键分别选取两个对角点");
            lore.add("§7或使用 &a自动选取工具 &7自动选区");
            lore.add("§7");
            if (autoEnabled) {
                lore.add("§a▶ 点击 §8| §f关闭自动选取工具");
            } else {
                lore.add("§a▶ 点击 §8| §f开启自动选取工具");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 构建按钮2：第二步 - 查看选取信息
     * 鼠标指上去显示当前选区尺寸和价格，点击刷新
     */
    private ItemStack buildStep2Item() {
        boolean hasSelection = Residence.getInstance().getSelectionManager().hasPlacedBoth(viewer);

        if (!hasSelection) {
            return CONFIG.ITEMS.STEP_2_EMPTY.get(viewer);
        }

        CuboidArea area = Residence.getInstance().getSelectionManager().getSelectionCuboid(viewer);
        if (area == null) {
            return CONFIG.ITEMS.STEP_2_EMPTY.get(viewer);
        }

        ResidencePlayer rPlayer = Residence.getInstance().getPlayerManager().getResidencePlayer(viewer);
        PermissionGroup group = rPlayer.getGroup();
        double cost = area.getCost(group);
        boolean economyEnabled = Residence.getInstance().getConfigManager().enableEconomy();
        boolean chargeOnCreation = Residence.getInstance().getConfigManager().isChargeOnCreation();

        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§l第二步 §7| §f查看选取信息");

            List<String> lore = new ArrayList<>();
            lore.add("§7");
            lore.add("§f当前选区: §e" + area.getXSize() + "×" + area.getYSize() + "×" + area.getZSize());
            lore.add("§f总面积: §e" + area.getSize() + " §7方块");
            lore.add("§f世界: §e" + area.getWorldName());

            Location low = area.getLowLocation();
            Location high = area.getHighLocation();
            if (low != null && high != null) {
                lore.add("§f坐标: §7(" + low.getBlockX() + "," + low.getBlockY() + "," + low.getBlockZ()
                        + ") → (" + high.getBlockX() + "," + high.getBlockY() + "," + high.getBlockZ() + ")");
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
            lore.add("§a▶ 点击 §8| §f刷新选区信息");
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

            // 按钮2未选取时的显示
            ConfiguredItem STEP_2_EMPTY = ConfiguredItem.create()
                    .defaultType(Material.BARRIER)
                    .defaultName("&e&l第二步 &7| &f查看选取信息 &8(无选区)")
                    .defaultLore(
                            "&7",
                            "&7鼠标悬停可查看当前选区信息",
                            "&c请先选取区域后再查看信息",
                            "&7",
                            "&a▶ 点击 §8| §f刷新选区信息"
                    ).build();

            // 按钮3已选取时的显示
            ConfiguredItem STEP_3_READY = ConfiguredItem.create()
                    .defaultType(Material.EMERALD)
                    .defaultName("&e&l第三步 &7| &f输入名称并确认购买")
                    .defaultLore(
                            "&7",
                            "&7选区已就绪！",
                            "&7点击后在聊天栏输入领地名称",
                            "&7输入 '#cancel' 可取消操作",
                            "&7",
                            "&c注意: 创建后将扣除相应费用",
                            "&7",
                            "&a▶ 点击 §8| §f开始创建"
                    ).build();

            // 按钮3未选取时的显示
            ConfiguredItem STEP_3_NOT_READY = ConfiguredItem.create()
                    .defaultType(Material.BARRIER)
                    .defaultName("&e&l第三步 &7| &f输入名称并确认购买 &8(未选取)")
                    .defaultLore(
                            "&7",
                            "&c请先完成第一步和第二步",
                            "&c选取区域后再创建领地"
                    ).build();

            // 返回按钮
            ConfiguredItem BACK = ConfiguredItem.create()
                    .defaultType(Material.ARROW)
                    .defaultName("&7&l返回领地列表")
                    .defaultLore(
                            "&7",
                            "&a▶ 点击 §8| §f返回"
                    ).build();
        }
    }
}
