package com.artformgames.plugin.residencelist.ui;

import cc.carm.lib.easyplugin.gui.GUI;
import cc.carm.lib.easyplugin.gui.GUIItem;
import cc.carm.lib.easyplugin.gui.GUIType;
import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.listener.AnvilNameInput;
import com.artformgames.plugin.residencelist.utils.ResidenceUtils;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CreateResidenceUI extends GUI {

    public static void open(@NotNull Player player, @Nullable String owner) {
        new CreateResidenceUI(player, owner).openGUI(player);
    }

    protected final @NotNull Player viewer;
    protected final @Nullable String owner;

    public CreateResidenceUI(@NotNull Player viewer, @Nullable String owner) {
        super(GUIType.SIX_BY_NINE, PluginMessages.CREATE_GUI.TITLE.parseLine(viewer));
        this.viewer = viewer;
        this.owner = owner;
        setEmptyItem(PluginConfig.ICON.EMPTY.get(viewer));
        initItems();
    }

    /** 反射获取Residence选取工具名称，避免CMIMaterial编译时依赖 */
    private static String getSelectionToolName() {
        try {
            Object configManager = Residence.getInstance().getConfigManager();
            Method getTool = configManager.getClass().getMethod("getSelectionTool");
            Object tool = getTool.invoke(configManager);
            Method getName = tool.getClass().getMethod("getName");
            Object result = getName.invoke(tool);
            return result != null ? result.toString() : "Wooden Hoe";
        } catch (Exception e) {
            return "Wooden Hoe";
        }
    }

    public void initItems() {
        String toolName = getSelectionToolName();
        boolean autoEnabled = Residence.getInstance().getAutoSelectionManager()
                .getList().containsKey(viewer.getUniqueId());
        boolean hasSelection = Residence.getInstance().getSelectionManager().hasPlacedBoth(viewer);

        // ===== 按钮1: 第一步 - 选取圈地范围（说明+切换自动选取）=====
        setItem(20, new GUIItem(buildStep1Item(autoEnabled, toolName)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                clicker.closeInventory();
                ResidenceUtils.toggleAutoSelection(clicker);
                CreateResidenceUI.open(clicker, owner);
            }
        });

        // ===== 按钮2: 第二步 - 查看选取信息（指上去查看，点击刷新）=====
        setItem(22, new GUIItem(buildStep2Item()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                CreateResidenceUI.open(clicker, owner);
            }
        });

        // ===== 按钮3: 第三步 - 输入名称并确认购买（点击进入铁砧输入）=====
        setItem(24, new GUIItem(hasSelection
                ? buildStep3ReadyItem()
                : buildStep3NotReadyItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                if (!type.isLeftClick()) return;
                if (!Residence.getInstance().getSelectionManager().hasPlacedBoth(clicker)) {
                    PluginMessages.CREATE.NO_SELECTION.sendTo(clicker);
                    PluginMessages.CREATE.FAILED_SOUND.playTo(clicker);
                    return;
                }

                PluginMessages.CREATE.ASK_SOUND.playTo(clicker);
                clicker.closeInventory();
                AnvilNameInput.open(clicker,
                        PluginMessages.CREATE_GUI.ANVIL_TITLE.parseLine(clicker),
                        PluginMessages.CREATE_GUI.ANVIL_PLACEHOLDER.parseLine(clicker),
                        (player, content) -> {
                    if (content == null || content.isBlank()) {
                        if (content != null) {
                            PluginMessages.CREATE.FAILED_SOUND.playTo(player);
                        }
                        return;
                    }

                    // 等效 /res create <name>
                    Bukkit.dispatchCommand(player, "res create " + content);
                    ResidenceListUI.open(player, owner);
                });
            }
        });

        // ===== Y轴扩展按钮 (slots 30, 31, 32) 仅在有选区时可用 =====
        setItem(30, new GUIItem(buildSkyItem(hasSelection)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                if (!Residence.getInstance().getSelectionManager().hasPlacedBoth(clicker)) {
                    PluginMessages.CREATE.NO_SELECTION.sendTo(clicker);
                    PluginMessages.CREATE.FAILED_SOUND.playTo(clicker);
                    return;
                }
                ResidenceUtils.expandSky(clicker);
                CreateResidenceUI.open(clicker, owner);
            }
        });

        setItem(31, new GUIItem(buildVertItem(hasSelection)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                if (!Residence.getInstance().getSelectionManager().hasPlacedBoth(clicker)) {
                    PluginMessages.CREATE.NO_SELECTION.sendTo(clicker);
                    PluginMessages.CREATE.FAILED_SOUND.playTo(clicker);
                    return;
                }
                ResidenceUtils.expandVert(clicker);
                CreateResidenceUI.open(clicker, owner);
            }
        });

        setItem(32, new GUIItem(buildBedrockItem(hasSelection)) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                if (!Residence.getInstance().getSelectionManager().hasPlacedBoth(clicker)) {
                    PluginMessages.CREATE.NO_SELECTION.sendTo(clicker);
                    PluginMessages.CREATE.FAILED_SOUND.playTo(clicker);
                    return;
                }
                ResidenceUtils.expandBedrock(clicker);
                CreateResidenceUI.open(clicker, owner);
            }
        });

        // ===== 返回按钮 (slot 49) 退出时自动关闭自动选取 =====
        setItem(49, new GUIItem(buildBackItem()) {
            @Override
            public void onClick(Player clicker, ClickType type) {
                PluginConfig.GUI.CLICK_SOUND.playTo(clicker);
                if (Residence.getInstance().getAutoSelectionManager()
                        .getList().containsKey(clicker.getUniqueId())) {
                    ResidenceUtils.toggleAutoSelection(clicker);
                }
                clicker.closeInventory();
                ResidenceListUI.open(clicker, owner);
            }
        });
    }

    private ItemStack buildStep1Item(boolean autoEnabled, String toolName) {
        ItemStack item = new ItemStack(autoEnabled ? Material.EMERALD : Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String statusText = autoEnabled ? "&2&l[自动选取已开启]" : "&4&l[自动选取已关闭]";
        meta.setDisplayName(ColorParser.parse(
                PluginMessages.CREATE_GUI.ITEMS.STEP_1_NAME.parseLine(viewer) + " " + statusText));

        String loreRaw = PluginMessages.CREATE_GUI.ITEMS.STEP_1_LORE.parseLine(viewer)
                .replace("%tool%", toolName);
        List<String> lore = splitLore(loreRaw);
        lore.add("");
        lore.add(ColorParser.parse(autoEnabled
                ? PluginMessages.CREATE_GUI.ITEMS.STEP_1_STATUS_ON.parseLine(viewer)
                : PluginMessages.CREATE_GUI.ITEMS.STEP_1_STATUS_OFF.parseLine(viewer)));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildStep2Item() {
        boolean hasSelection = Residence.getInstance().getSelectionManager().hasPlacedBoth(viewer);

        if (!hasSelection) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorParser.parse(
                        PluginMessages.CREATE_GUI.ITEMS.STEP_2_EMPTY_NAME.parseLine(viewer)));
                meta.setLore(splitLore(
                        PluginMessages.CREATE_GUI.ITEMS.STEP_2_EMPTY_LORE.parseLine(viewer)));
                item.setItemMeta(meta);
            }
            return item;
        }

        CuboidArea area = Residence.getInstance().getSelectionManager().getSelectionCuboid(viewer);
        if (area == null) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorParser.parse(
                        PluginMessages.CREATE_GUI.ITEMS.STEP_2_EMPTY_NAME.parseLine(viewer)));
                meta.setLore(splitLore(
                        PluginMessages.CREATE_GUI.ITEMS.STEP_2_EMPTY_LORE.parseLine(viewer)));
                item.setItemMeta(meta);
            }
            return item;
        }

        ResidencePlayer rPlayer = Residence.getInstance().getPlayerManager().getResidencePlayer(viewer);
        PermissionGroup group = rPlayer.getGroup();
        double cost = area.getCost(group);
        boolean economyEnabled = Residence.getInstance().getConfigManager().enableEconomy();
        boolean chargeOnCreation = Residence.getInstance().getConfigManager().isChargeOnCreation();

        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ColorParser.parse(
                PluginMessages.CREATE_GUI.ITEMS.STEP_2_NAME.parseLine(viewer)));

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
        lore.add(ColorParser.parse(
                PluginMessages.CREATE_GUI.ITEMS.STEP_2_REFRESH_TIP.parseLine(viewer)));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildStep3ReadyItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse(
                    PluginMessages.CREATE_GUI.ITEMS.STEP_3_READY_NAME.parseLine(viewer)));
            meta.setLore(splitLore(
                    PluginMessages.CREATE_GUI.ITEMS.STEP_3_READY_LORE.parseLine(viewer)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildStep3NotReadyItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse(
                    PluginMessages.CREATE_GUI.ITEMS.STEP_3_NOT_READY_NAME.parseLine(viewer)));
            meta.setLore(splitLore(
                    PluginMessages.CREATE_GUI.ITEMS.STEP_3_NOT_READY_LORE.parseLine(viewer)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse(
                    PluginMessages.CREATE_GUI.ITEMS.BACK_NAME.parseLine(viewer)));
            meta.setLore(splitLore(
                    PluginMessages.CREATE_GUI.ITEMS.BACK_LORE.parseLine(viewer)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildSkyItem(boolean hasSelection) {
        ItemStack item = new ItemStack(hasSelection ? Material.WHITE_WOOL : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&f&l扩展选区至天空 (Y=320)"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7将当前选区顶部扩展到世界高度上限"));
            if (!hasSelection) {
                lore.add(ColorParser.parse("&c需要先选取两个对角点"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildVertItem(boolean hasSelection) {
        ItemStack item = new ItemStack(hasSelection ? Material.NETHER_STAR : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&e&l扩展选区至全高 (2D领地)"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7将当前选区从基岩扩展到天空"));
            lore.add(ColorParser.parse("&7适用于平面领地(忽略Y轴)"));
            if (!hasSelection) {
                lore.add(ColorParser.parse("&c需要先选取两个对角点"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildBedrockItem(boolean hasSelection) {
        ItemStack item = new ItemStack(hasSelection ? Material.BEDROCK : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorParser.parse("&8&l扩展选区至基岩 (Y=-64)"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorParser.parse("&7将当前选区底部扩展到世界最低处"));
            if (!hasSelection) {
                lore.add(ColorParser.parse("&c需要先选取两个对角点"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static List<String> splitLore(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(ColorParser.parse(line));
        }
        return lines;
    }

}
