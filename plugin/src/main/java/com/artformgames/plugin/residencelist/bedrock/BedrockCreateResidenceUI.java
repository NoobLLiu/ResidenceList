package com.artformgames.plugin.residencelist.bedrock;

import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.permissions.PermissionGroup;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;

import java.lang.reflect.Method;

/**
 * 基岩版创建领地界面。
 * <p>
 * 对应 Java 版 {@link com.artformgames.plugin.residencelist.ui.CreateResidenceUI}。
 * 分为两步：第一步显示选区信息与操作按钮，第二步输入领地名称并确认创建。
 */
public class BedrockCreateResidenceUI {

    private BedrockCreateResidenceUI() {
    }

    /**
     * 打开创建领地页面。
     *
     * @param player     目标玩家
     * @param ownerFilter 领地列表的筛选主人（用于返回）
     */
    public static void open(Player player, String ownerFilter) {
        sendCreateForm(player, ownerFilter);
    }

    // ======================== 第一步：创建领地主页面 ========================

    private static void sendCreateForm(Player player, String ownerFilter) {
        boolean hasSelection = Residence.getInstance().getSelectionManager().hasPlacedBoth(player);
        boolean autoEnabled = Residence.getInstance().getAutoSelectionManager()
                .getList().containsKey(player.getUniqueId());
        String toolName = getSelectionToolName();

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a§l【领地系统-创建领地】");

        StringBuilder content = new StringBuilder();

        // 第一步说明
        content.append("§e§l第一步 | 选取圈地范围\n");
        content.append("§f使用领地选取工具 §e").append(toolName).append(" §f选取两个对角点\n");
        content.append("§f自动圈地模式: ").append(autoEnabled ? "§a已开启" : "§c已关闭").append("\n");

        // 第二步：选区信息
        content.append("\n§e§l第二步 | 选区信息\n");
        if (hasSelection) {
            CuboidArea area = Residence.getInstance().getSelectionManager().getSelectionCuboid(player);
            if (area != null) {
                content.append("§f选区大小: §e").append(area.getXSize())
                        .append("×").append(area.getYSize()).append("×").append(area.getZSize()).append("\n");
                content.append("§f总面积: §e").append(area.getSize()).append(" 方块\n");
                content.append("§f世界: §e").append(area.getWorldName()).append("\n");

                Location low = area.getLowLocation();
                Location high = area.getHighLocation();
                if (low != null && high != null) {
                    content.append("§f坐标: §e(").append(low.getBlockX()).append(",")
                            .append(low.getBlockY()).append(",").append(low.getBlockZ())
                            .append(") -> (").append(high.getBlockX()).append(",")
                            .append(high.getBlockY()).append(",").append(high.getBlockZ()).append(")\n");
                }

                ResidencePlayer rPlayer = Residence.getInstance().getPlayerManager().getResidencePlayer(player);
                PermissionGroup group = rPlayer.getGroup();
                double cost = area.getCost(group);
                boolean economyEnabled = Residence.getInstance().getConfigManager().enableEconomy();
                boolean chargeOnCreation = Residence.getInstance().getConfigManager().isChargeOnCreation();

                if (economyEnabled && chargeOnCreation && cost > 0) {
                    String formatted = Residence.getInstance().getEconomyManager() != null
                            ? Residence.getInstance().getEconomyManager().format(cost)
                            : String.format("%.2f", cost);
                    content.append("§f创建费用: §e").append(formatted).append("\n");
                } else {
                    content.append("§f创建费用: §a免费\n");
                }
                content.append("§f领地数量: §e").append(rPlayer.getResAmount())
                        .append("§f/§e").append(rPlayer.getMaxRes()).append("\n");
            }
        } else {
            content.append("§c请先选取区域后再创建领地\n");
        }

        form.content(content.toString());

        // 按钮（顺序固定，索引计算简单）
        // 0: 开/关自动选区模式（点击后直接关闭表单，让玩家走动操作）
        form.button(autoEnabled ? "§0§l关闭自动选区模式" : "§0§l开启自动选区模式");
        // 1: 返回上级菜单（退出自动圈地模式，返回主菜单）
        form.button("§0§l返回上级菜单");
        // 2: 关闭表单并开始圈地（保持自动圈地模式，关闭表单让玩家走动）
        form.button("§0§l关闭表单并开始圈地");
        // 3: 确认选区（进入第二步）
        if (hasSelection) {
            form.button("§0§l确认选区");
        }

        final boolean finalHasSelection = hasSelection;
        final int btnBack = 1;
        final int btnStartSelecting = 2;
        final int btnConfirm = finalHasSelection ? 3 : -1;

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked == 0) {
                    // 开/关自动选区模式，切换后直接关闭表单
                    Residence.getInstance().getAutoSelectionManager().switchAutoSelection(player);
                    // 不重新打开表单，让玩家走动操作
                } else if (clicked == btnBack) {
                    // 返回上级菜单：退出自动圈地模式
                    disableAutoSelection(player);
                    BedrockResidenceListUI.open(player);
                } else if (clicked == btnStartSelecting) {
                    // 关闭表单并开始圈地：保持自动圈地模式不变，直接关闭表单
                    // 不做任何事，表单自动关闭
                } else if (clicked == btnConfirm) {
                    sendConfirmForm(player, ownerFilter);
                }
            });
        });

        // 玩家直接 ESC 关闭表单：等同于"关闭表单并开始圈地"，保持自动圈地模式
        // 不设置 closedResultHandler，即不执行任何操作（保持自动圈地模式）

        BedrockFormUtil.sendForm(player, form);
    }

    // ======================== 第二步：确认选区创建领地 ========================

    private static void sendConfirmForm(Player player, String ownerFilter) {
        boolean hasSelection = Residence.getInstance().getSelectionManager().hasPlacedBoth(player);
        boolean autoEnabled = Residence.getInstance().getAutoSelectionManager()
                .getList().containsKey(player.getUniqueId());

        CustomForm.Builder form = CustomForm.builder()
                .title("§a§l【领地系统-创建领地-确认选区】");

        // 选区信息
        if (hasSelection) {
            CuboidArea area = Residence.getInstance().getSelectionManager().getSelectionCuboid(player);
            if (area != null) {
                form.label("§f选区大小: §e" + area.getXSize() + "×" + area.getYSize() + "×" + area.getZSize());
                form.label("§f总面积: §e" + area.getSize() + " 方块");
                form.label("§f世界: §e" + area.getWorldName());

                Location low = area.getLowLocation();
                Location high = area.getHighLocation();
                if (low != null && high != null) {
                    form.label("§f坐标: §e(" + low.getBlockX() + "," + low.getBlockY() + "," + low.getBlockZ()
                            + ") -> (" + high.getBlockX() + "," + high.getBlockY() + "," + high.getBlockZ() + ")");
                }

                ResidencePlayer rPlayer = Residence.getInstance().getPlayerManager().getResidencePlayer(player);
                PermissionGroup group = rPlayer.getGroup();
                double cost = area.getCost(group);
                boolean economyEnabled = Residence.getInstance().getConfigManager().enableEconomy();
                boolean chargeOnCreation = Residence.getInstance().getConfigManager().isChargeOnCreation();

                if (economyEnabled && chargeOnCreation && cost > 0) {
                    String formatted = Residence.getInstance().getEconomyManager() != null
                            ? Residence.getInstance().getEconomyManager().format(cost)
                            : String.format("%.2f", cost);
                    form.label("§f创建费用: §e" + formatted);
                } else {
                    form.label("§f创建费用: §a免费");
                }
                form.label("§f领地数量: §e" + rPlayer.getResAmount() + "§f/§e" + rPlayer.getMaxRes());
            }
        } else {
            form.label("§c当前没有选区，请先选取区域！");
            form.label("§f请返回上一级菜单选取区域。");
        }

        // 自动圈地模式状态
        form.label("§f自动圈地模式: " + (autoEnabled ? "§a已开启" : "§c已关闭"));

        // 输入框
        form.label("§e§l请输入领地名称");
        form.label("§f支持英文、数字、下划线");
        form.input("领地名称", "请输入领地名称...", "");

        form.validResultHandler(response -> {
            if (!hasSelection) {
                PluginMessages.CREATE.NO_SELECTION.sendTo(player);
                PluginMessages.CREATE.FAILED_SOUND.playTo(player);
                return;
            }

            // 使用 response.next() 而非 asInput(0)，因为 label 不产生响应值
            // next() 会自动跳过 label，返回第一个有响应值的组件（即 input）
            String name = response.next();
            if (name == null || name.isBlank()) {
                PluginMessages.CREATE.FAILED_SOUND.playTo(player);
                BedrockFormUtil.runSync(() -> sendCreateForm(player, ownerFilter));
                return;
            }

            BedrockFormUtil.runSync(() -> {
                PluginMessages.CREATE.ASK_SOUND.playTo(player);
                Bukkit.dispatchCommand(player, "res create " + name);
                PluginMessages.CREATE.SUCCESS.sendTo(player, name);
                BedrockResidenceListUI.open(player);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendCreateForm(player, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 关闭自动圈地模式（如果已开启）。
     */
    private static void disableAutoSelection(Player player) {
        if (Residence.getInstance().getAutoSelectionManager()
                .getList().containsKey(player.getUniqueId())) {
            Residence.getInstance().getAutoSelectionManager().switchAutoSelection(player);
        }
    }

    /**
     * 反射获取 Residence 选取工具名称。
     */
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
}
