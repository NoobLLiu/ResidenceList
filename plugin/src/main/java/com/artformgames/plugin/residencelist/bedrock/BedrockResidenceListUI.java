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
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.util.FormImage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 基岩版领地列表界面（SimpleForm）。
 * <p>
 * 对应 Java 版 {@link com.artformgames.plugin.residencelist.ui.ResidenceListUI}。
 * 由于基岩版按钮没有右键/Q键，点击领地后会弹出操作子菜单。
 */
public class BedrockResidenceListUI {

    // 按钮索引常量（固定按钮区，排在领地按钮之后）
    private static final int BTN_SORT = -1;
    private static final int BTN_FILTER = -2;
    private static final int BTN_CREATE = -3;
    private static final int BTN_BACK = -4;

    private BedrockResidenceListUI() {
    }

    /**
     * 打开领地列表表单。
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
            sendEmptyForm(player, owner);
            return;
        }

        sendListForm(player, data, owner, residences);
    }

    private static List<ClaimedResidence> collectResidences(Player player, UserListData data, String owner) {
        List<ClaimedResidence> display = new ArrayList<>();
        Comparator<ClaimedResidence> comparator = data.getSortFunction().residenceComparator(data.isSortReversed());

        // 置顶领地优先
        data.getPinned().stream()
                .map(ResidenceListAPI::getResidence)
                .filter(res -> res != null && (owner == null || res.isOwner(owner)))
                .filter(res -> ResidenceUtils.viewable(res, player))
                .sorted(comparator).forEach(display::add);

        // 其余领地
        ResidenceListAPI.listResidences().stream()
                .filter(res -> !display.contains(res))
                .filter(res -> owner == null || res.isOwner(owner))
                .filter(res -> ResidenceUtils.viewable(res, player))
                .sorted(comparator).forEach(display::add);

        // 过滤可见性（公开或本人拥有）
        display.stream().filter(r -> {
            ResidenceData d = Main.getInstance().getResidenceManager().getResidence(r);
            return d.isPublicDisplayed() || d.isOwner(player);
        });

        return display.stream().filter(r -> {
            ResidenceData d = Main.getInstance().getResidenceManager().getResidence(r);
            return d.isPublicDisplayed() || d.isOwner(player);
        }).toList();
    }

    private static void sendListForm(Player player, UserListData data, String owner, List<ClaimedResidence> residences) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a§l领地列表");

        StringBuilder content = new StringBuilder();
        if (owner != null) {
            content.append("§7正在查看 §f").append(owner).append(" §7的领地\n");
        } else {
            content.append("§7所有公开领地均已展示\n");
        }
        content.append("§7排序: §f").append(data.getSortFunction().name())
                .append(data.isSortReversed() ? " §c(降序)" : " §a(升序)")
                .append("\n");
        form.content(content.toString());

        // 领地按钮
        for (ClaimedResidence res : residences) {
            ResidenceData resData = Main.getInstance().getResidenceManager().getResidence(res);
            String name = BedrockFormUtil.stripColor(resData.getDisplayName());
            int likes = resData.countRate(ResidenceRate::recommend);
            int dislikes = resData.countRate(r -> !r.recommend());
            int members = res.getTrustedPlayers().size() + 1;
            long size = res.getMainArea().getSize();

            StringBuilder btnText = new StringBuilder();
            if (data.isPinned(res.getName())) btnText.append("★ ");
            btnText.append("§a").append(name);
            btnText.append("\n§7主人: §f").append(resData.getOwner());
            btnText.append(" §7| 规模: §f").append(size);
            btnText.append(" §7| 成员: §f").append(members);
            btnText.append("\n§7赞: §a").append(likes).append(" §7踩: §c").append(dislikes);

            if (!resData.getDescription().isEmpty()) {
                String desc = BedrockFormUtil.stripColor(String.join(" ", resData.getDescription()));
                if (desc.length() > 50) desc = desc.substring(0, 50) + "...";
                btnText.append("\n§7").append(desc);
            }

            form.button(btnText.toString());
        }

        // 功能按钮
        form.button("§e§l切换排序方式");
        form.button(owner == null ? "§e§l查看我的领地" : "§e§l查看所有领地");
        form.button("§a§l创建领地");
        form.button("§c§l关闭");

        final List<ClaimedResidence> finalResidences = residences;
        final int sortBtnIndex = finalResidences.size();
        final int filterBtnIndex = finalResidences.size() + 1;
        final int createBtnIndex = finalResidences.size() + 2;
        final int closeBtnIndex = finalResidences.size() + 3;

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                if (clicked < finalResidences.size()) {
                    // 点击了某个领地
                    showResidenceActionMenu(player, data, finalResidences.get(clicked), owner);
                } else if (clicked == sortBtnIndex) {
                    data.setSortFunction(data.getSortFunction().next());
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    open(player, owner);
                } else if (clicked == filterBtnIndex) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    open(player, owner == null ? player.getName() : null);
                } else if (clicked == createBtnIndex) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    BedrockCreateResidenceUI.open(player, owner);
                }
                // closeBtnIndex -> 不做任何事，表单自动关闭
            });
        });

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 显示某个领地的操作子菜单（查看详情/传送/置顶）。
     */
    private static void showResidenceActionMenu(Player player, UserListData data, ClaimedResidence residence, String owner) {
        ResidenceData resData = Main.getInstance().getResidenceManager().getResidence(residence);
        String name = BedrockFormUtil.stripColor(resData.getDisplayName());

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a§l" + name);

        StringBuilder content = new StringBuilder();
        content.append("§7主人: §f").append(resData.getOwner()).append("\n");
        content.append("§7规模: §f").append(residence.getMainArea().getSize()).append(" 方块\n");
        content.append("§7成员: §f").append(residence.getTrustedPlayers().size() + 1).append("\n");
        content.append("§7赞: §a").append(resData.countRate(ResidenceRate::recommend));
        content.append(" §7踩: §c").append(resData.countRate(r -> !r.recommend())).append("\n");
        content.append("§7状态: ").append(resData.isPublicDisplayed() ? "§a公开" : "§c私有").append("\n");

        if (!resData.getDescription().isEmpty()) {
            content.append("\n§7描述: §f").append(BedrockFormUtil.stripColor(String.join(" ", resData.getDescription())));
        }

        form.content(content.toString());

        form.button("§e§l查看详细信息");
        if (resData.canTeleport(player)) {
            form.button("§d§l传送到领地");
        }
        if (data.isPinned(residence.getName())) {
            form.button("§c§l取消置顶");
        } else {
            form.button("§a§l置顶领地");
        }
        form.button("§7§l返回列表");

        final boolean canTeleport = resData.canTeleport(player);
        final boolean isPinned = data.isPinned(residence.getName());

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                switch (clicked) {
                    case 0 -> {
                        PluginConfig.GUI.CLICK_SOUND.playTo(player);
                        if (resData.isOwner(player)) {
                            BedrockResidenceManageUI.open(player, resData, owner);
                        } else {
                            BedrockResidenceInfoUI.open(player, resData, owner);
                        }
                    }
                    case 1 -> {
                        if (canTeleport) {
                            residence.tpToResidence(player, player, player.hasPermission("residence.admin"));
                            PluginMessages.TELEPORT.SOUND.playTo(player);
                        }
                    }
                    case 2 -> {
                        int targetIndex = canTeleport ? 2 : 1;
                        if (clicked == targetIndex) {
                            if (isPinned) {
                                data.removePin(residence.getName());
                                PluginMessages.UNPIN.SOUND.playTo(player);
                                PluginMessages.UNPIN.MESSAGE.sendTo(player, resData.getDisplayName());
                            } else {
                                data.setPin(residence.getName(), 0);
                                PluginMessages.PIN.SOUND.playTo(player);
                                PluginMessages.PIN.MESSAGE.sendTo(player, resData.getDisplayName());
                            }
                            open(player, owner);
                        } else {
                            open(player, owner);
                        }
                    }
                    case 3 -> open(player, owner);
                }
            });
        });

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendEmptyForm(Player player, String owner) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a§l领地列表")
                .content("§7目前没有可显示的领地。")
                .button("§a§l创建领地")
                .button("§c§l关闭");

        form.validResultHandler(response -> {
            if (response.clickedButtonId() == 0) {
                BedrockFormUtil.runSync(() -> BedrockCreateResidenceUI.open(player, owner));
            }
        });

        BedrockFormUtil.sendForm(player, form);
    }
}
