package com.artformgames.plugin.residencelist.bedrock;

import com.artformgames.plugin.residencelist.ResidenceListAPI;
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
 * 基岩版无法使用铁砧输入，改为 CustomForm 的 Input 组件。
 */
public class BedrockCreateResidenceUI {

    private BedrockCreateResidenceUI() {
    }

    /**
     * 打开创建领地表单。
     *
     * @param player     目标玩家
     * @param ownerFilter 领地列表的筛选主人（用于返回）
     */
    public static void open(Player player, String ownerFilter) {
        sendCreateForm(player, ownerFilter);
    }

    /**
     * 创建领地表单，显示选区信息并输入名称。
     */
    private static void sendCreateForm(Player player, String ownerFilter) {
        boolean hasSelection = Residence.getInstance().getSelectionManager().hasPlacedBoth(player);
        boolean autoEnabled = Residence.getInstance().getAutoSelectionManager()
                .getList().containsKey(player.getUniqueId());
        String toolName = getSelectionToolName();

        CustomForm.Builder form = CustomForm.builder()
                .title("§a§l创建领地");

        // 第一步：选区状态
        form.label("§e§l第一步 | 选取圈地范围");
        form.label("§7使用领地选取工具 §f" + toolName + " §7选取两个对角点");
        form.label(autoEnabled ? "§2[自动选取已开启]" : "§4[自动选取已关闭]");

        // 第二步：选区信息
        if (hasSelection) {
            CuboidArea area = Residence.getInstance().getSelectionManager().getSelectionCuboid(player);
            if (area != null) {
                form.label("§e§l第二步 | 选区信息");
                form.label("§f选区大小: §e" + area.getXSize() + "×" + area.getYSize() + "×" + area.getZSize());
                form.label("§f总面积: §e" + area.getSize() + " §7方块");
                form.label("§f世界: §e" + area.getWorldName());

                Location low = area.getLowLocation();
                Location high = area.getHighLocation();
                if (low != null && high != null) {
                    form.label("§f坐标: §7(" + low.getBlockX() + "," + low.getBlockY() + "," + low.getBlockZ()
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
                form.label("§f领地数量: §e" + rPlayer.getResAmount() + "§7/" + rPlayer.getMaxRes());
            }
        } else {
            form.label("§e§l第二步 | 选区信息 §8(无选区)");
            form.label("§c请先选取区域后再创建领地");
        }

        // 第三步：输入名称
        if (hasSelection) {
            form.label("§e§l第三步 | 输入领地名称");
            form.input("领地名称", "输入领地名称...", "");
            form.label("§c注意: 创建后可能扣除相应费用");
        }

        form.validResultHandler(response -> {
            if (!hasSelection) {
                PluginMessages.CREATE.NO_SELECTION.sendTo(player);
                PluginMessages.CREATE.FAILED_SOUND.playTo(player);
                return;
            }

            // 最后一个组件是 input（index 需要计算 label 数量后的位置）
            // 由于 label 不产生响应值，input 是唯一的响应组件
            String name = response.asInput(0);
            if (name == null || name.isBlank()) {
                PluginMessages.CREATE.FAILED_SOUND.playTo(player);
                sendCreateForm(player, ownerFilter);
                return;
            }

            BedrockFormUtil.runSync(() -> {
                PluginMessages.CREATE.ASK_SOUND.playTo(player);
                // 等效 /res create <name>
                Bukkit.dispatchCommand(player, "res create " + name);
                PluginMessages.CREATE.SUCCESS.sendTo(player, name);
                BedrockResidenceListUI.open(player, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceListUI.open(player, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
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
