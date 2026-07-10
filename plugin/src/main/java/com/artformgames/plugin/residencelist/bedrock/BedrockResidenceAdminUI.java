package com.artformgames.plugin.residencelist.bedrock;

import com.artformgames.plugin.residencelist.Main;
import com.artformgames.plugin.residencelist.ResidenceListAPI;
import com.artformgames.plugin.residencelist.api.residence.ResidenceData;
import com.artformgames.plugin.residencelist.api.residence.ResidenceRate;
import com.artformgames.plugin.residencelist.api.user.UserListData;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.utils.ResidenceUtils;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 基岩版管理员领地列表界面（SimpleForm）。
 * <p>
 * 对应 Java 版 {@link com.artformgames.plugin.residencelist.ui.admin.ResidenceAdminUI}。
 * 管理员可以看到所有领地（包括私有），并直接进入管理界面。
 */
public class BedrockResidenceAdminUI {

    private BedrockResidenceAdminUI() {
    }

    /**
     * 打开管理员领地列表表单。
     *
     * @param player 目标玩家
     * @param owner  筛选领地主人，null 表示显示全部
     */
    public static void open(Player player, String owner) {
        UserListData data = ResidenceListAPI.getUserManager().getNullable(player.getUniqueId());
        if (data == null) {
            PluginMessages.LOAD_FAILED.sendTo(player);
            return;
        }

        List<ClaimedResidence> residences = collectResidences(player, data, owner);
        if (residences.isEmpty()) {
            SimpleForm.Builder form = SimpleForm.builder()
                    .title("§a【领地系统-管理员领地列表】")
                    .content("§f目前没有领地。")
                    .button("§0关闭");
            BedrockFormUtil.sendForm(player, form);
            return;
        }

        sendAdminListForm(player, data, owner, residences);
    }

    private static List<ClaimedResidence> collectResidences(Player player, UserListData data, String owner) {
        Comparator<ClaimedResidence> comparator = data.getSortFunction().residenceComparator(data.isSortReversed());
        return ResidenceListAPI.listResidences().stream()
                .filter(res -> owner == null || res.isOwner(owner))
                .sorted(comparator)
                .toList();
    }

    private static void sendAdminListForm(Player player, UserListData data, String owner, List<ClaimedResidence> residences) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a【领地系统-管理员领地列表】(" + residences.size() + ")");

        StringBuilder content = new StringBuilder();
        if (owner != null) {
            content.append("§f正在查看 §f").append(owner).append(" §f的领地\n");
        }
        content.append("§f排序: §f").append(data.getSortFunction().name())
                .append(data.isSortReversed() ? " §c(降序)" : " §a(升序)");
        form.content(content.toString());

        // 领地按钮
        for (ClaimedResidence res : residences) {
            ResidenceData resData = Main.getInstance().getResidenceManager().getResidence(res);
            String name = BedrockFormUtil.stripColor(resData.getDisplayName());
            int likes = resData.countRate(ResidenceRate::recommend);
            int dislikes = resData.countRate(r -> !r.recommend());

            StringBuilder btnText = new StringBuilder();
            btnText.append("§a").append(name);
            btnText.append("\n§f主人: §f").append(resData.getOwner());
            btnText.append(" §f| 状态: ").append(resData.isPublicDisplayed() ? "§a公开" : "§c私有");
            btnText.append("\n§f规模: §f").append(res.getMainArea().getSize());
            btnText.append(" §f| §a赞: ").append(likes).append(" §f| §c踩: ").append(dislikes);

            form.button(btnText.toString());
        }

        // 功能按钮
        form.button("§0切换排序方式");
        form.button(owner == null ? "§0按主人筛选" : "§0查看所有领地");
        form.button("§0关闭");

        final List<ClaimedResidence> finalResidences = residences;
        final int sortBtnIndex = finalResidences.size();
        final int filterBtnIndex = finalResidences.size() + 1;
        final int closeBtnIndex = finalResidences.size() + 2;

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                if (clicked < finalResidences.size()) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    // 管理员直接进入管理界面
                    ClaimedResidence res = finalResidences.get(clicked);
                    ResidenceData resData = Main.getInstance().getResidenceManager().getResidence(res);
                    BedrockResidenceManageUI.open(player, resData, owner);
                } else if (clicked == sortBtnIndex) {
                    data.setSortFunction(data.getSortFunction().next());
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    open(player, owner);
                } else if (clicked == filterBtnIndex) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    open(player, owner == null ? player.getName() : null);
                }
            });
        });

        BedrockFormUtil.sendForm(player, form);
    }
}
