package com.artformgames.plugin.residencelist.bedrock;

import com.artformgames.plugin.residencelist.Main;
import com.artformgames.plugin.residencelist.ResidenceListAPI;
import com.artformgames.plugin.residencelist.api.residence.ResidenceData;
import com.artformgames.plugin.residencelist.api.residence.ResidenceRate;
import com.artformgames.plugin.residencelist.api.user.UserListData;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.utils.ResidenceUtils;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.Map;

/**
 * 基岩版领地详情界面。
 * <p>
 * 对应 Java 版 {@link com.artformgames.plugin.residencelist.ui.ResidenceInfoUI}。
 * 提供查看信息、传送、评价、查看评价列表、查看成员列表功能。
 */
public class BedrockResidenceInfoUI {

    private BedrockResidenceInfoUI() {
    }

    /**
     * 打开领地详情表单。
     *
     * @param player      目标玩家
     * @param residenceData 领地数据
     * @param ownerFilter  领地列表的筛选主人（用于返回）
     */
    public static void open(Player player, ResidenceData residenceData, String ownerFilter) {
        sendMainMenu(player, residenceData, ownerFilter);
    }

    /**
     * 主菜单：显示领地信息和操作按钮。
     */
    private static void sendMainMenu(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a§l【领地系统-详细信息】");

        StringBuilder content = new StringBuilder();
        content.append("§f━━━━━━━━━━━━━━━\n");
        content.append("§f主人: §f").append(residenceData.getOwner()).append("\n");
        content.append("§f规模: §f").append(residence.getMainArea().getSize()).append(" 方块\n");
        content.append("§f成员: §f").append(residence.getTrustedPlayers().size() + 1).append("\n");

        int likes = residenceData.countRate(ResidenceRate::recommend);
        int dislikes = residenceData.countRate(r -> !r.recommend());
        content.append("§f评价: §a赞 ").append(likes).append(" §c踩 ").append(dislikes).append("\n");
        content.append("§f状态: ").append(residenceData.isPublicDisplayed() ? "§a公开" : "§c私有").append("\n");

        if (!residenceData.getDescription().isEmpty()) {
            content.append("\n§f描述:\n§f").append(BedrockFormUtil.stripColor(String.join("\n", residenceData.getDescription()))).append("\n");
        }

        // 传送坐标信息
        Location tpLoc = residenceData.getTeleportLocation(player);
        if (tpLoc != null && residenceData.canTeleport(player)) {
            content.append("\n§f传送点: §f").append(tpLoc.getWorld().getName())
                    .append(" @ (").append(tpLoc.getBlockX()).append(", ")
                    .append(tpLoc.getBlockY()).append(", ")
                    .append(tpLoc.getBlockZ()).append(")");
        }

        form.content(content.toString());

        // 功能按钮
        if (tpLoc != null && residenceData.canTeleport(player)) {
            form.button("§0§l传送到领地");
        }
        form.button("§0§l评分评价");
        form.button("§0§l查看所有评价 §f(" + residenceData.getRates().size() + ")");
        form.button("§0§l查看成员列表");
        form.button("§0§l返回领地列表");

        final boolean hasTeleport = tpLoc != null && residenceData.canTeleport(player);
        final int btnRate = hasTeleport ? 1 : 0;
        final int btnRates = hasTeleport ? 2 : 1;
        final int btnMembers = hasTeleport ? 3 : 2;
        final int btnBack = hasTeleport ? 4 : 3;

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (hasTeleport && clicked == 0) {
                    // 传送
                    residence.tpToResidence(player, player, player.hasPermission("residence.admin"));
                    PluginMessages.TELEPORT.SOUND.playTo(player);
                } else if (clicked == btnRate) {
                    sendRateForm(player, residenceData, ownerFilter);
                } else if (clicked == btnRates) {
                    sendRatesList(player, residenceData, ownerFilter);
                } else if (clicked == btnMembers) {
                    sendMembersList(player, residenceData, ownerFilter);
                } else if (clicked == btnBack) {
                    BedrockResidenceListUI.openList(player, ownerFilter);
                }
            });
        });

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 评分评价表单（CustomForm）。
     */
    private static void sendRateForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ResidenceRate existing = residenceData.getRates().get(player.getUniqueId());

        CustomForm.Builder form = CustomForm.builder()
                .title("§e§l【领地系统-评分评价】");

        form.label("§e请对该领地进行评价:");

        // 评价类型下拉
        String[] rateOptions = {"§a赞 - 推荐", "§c踩 - 不推荐"};
        int defaultOption = (existing != null && !existing.recommend()) ? 1 : 0;
        form.dropdown("评价类型", rateOptions);

        // 评价内容输入
        String defaultComment = existing != null ? existing.content() : "";
        form.input("评价内容", "输入你的评价...", defaultComment);

        if (existing != null) {
            form.label("§e你已评价过此领地，再次提交将覆盖旧评价。\n§e提交空内容将删除评价。");
        }

        form.validResultHandler(response -> {
            int rateType = response.asDropdown(0);
            String comment = response.asInput(1);

            BedrockFormUtil.runSync(() -> {
                boolean recommend = (rateType == 0);

                if (comment == null || comment.isBlank()) {
                    // 删除评价
                    if (existing != null) {
                        residenceData.removeRate(player.getUniqueId());
                        PluginMessages.COMMENT.REMOVED.sendTo(player, residenceData.getDisplayName());
                    } else {
                        PluginMessages.COMMENT.NOT_RATED.sendTo(player, residenceData.getDisplayName());
                    }
                } else {
                    residenceData.addRate(comment, recommend, player.getUniqueId());
                    PluginMessages.COMMENT.YES_SOUND.playTo(player);
                }
                sendMainMenu(player, residenceData, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendMainMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 评价列表（SimpleForm）。
     */
    private static void sendRatesList(Player player, ResidenceData residenceData, String ownerFilter) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e§l【领地系统-评价列表】(" + residenceData.getRates().size() + ")");

        StringBuilder content = new StringBuilder();
        if (residenceData.getRates().isEmpty()) {
            content.append("§f目前暂无评价。");
        } else {
            for (ResidenceRate rate : residenceData.getRates().values()) {
                String author = rate.getAuthorName() != null ? rate.getAuthorName() : "?";
                content.append(rate.recommend() ? "§a赞 " : "§c踩 ")
                        .append("§f").append(author)
                        .append(" §e(").append(ResidenceListAPI.format(rate.time())).append(")\n");
                if (!rate.content().isBlank()) {
                    content.append("§f  \"").append(BedrockFormUtil.stripColor(rate.content())).append("\"\n");
                }
                content.append("\n");
            }
        }
        form.content(content.toString());
        form.button("§0§l返回");

        form.validResultHandler(response ->
                BedrockFormUtil.runSync(() -> sendMainMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 成员列表（SimpleForm）。
     */
    private static void sendMembersList(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e§l【领地系统-成员列表】(" + (residence.getTrustedPlayers().size() + 1) + ")");

        StringBuilder content = new StringBuilder();

        // 领地主人
        if (!ResidenceUtils.isServerLand(residence)) {
            content.append("§a§l[主人] §f").append(residenceData.getOwner()).append("\n");
        } else {
            content.append("§e§l[服务器领地]\n");
        }

        // 信任成员
        for (ResidencePlayer trusted : residence.getTrustedPlayers()) {
            content.append("§f[成员] §f").append(trusted.getName()).append("\n");
        }

        form.content(content.toString());
        form.button("§0§l返回");

        form.validResultHandler(response ->
                BedrockFormUtil.runSync(() -> sendMainMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }
}
