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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 基岩版领地系统主菜单及领地列表界面（SimpleForm）。
 * <p>
 * 主菜单提供三个入口：领地传送、领地列表、创建领地。
 * 领地列表对应 Java 版 {@link com.artformgames.plugin.residencelist.ui.ResidenceListUI}。
 */
public class BedrockResidenceListUI {

    private BedrockResidenceListUI() {
    }

    /**
     * 打开主菜单。
     *
     * @param player 目标玩家
     */
    public static void open(Player player) {
        sendMainMenu(player, null);
    }

    /**
     * 打开领地列表（带筛选）。
     *
     * @param player 目标玩家
     * @param owner  筛选领地主人，null 表示显示全部
     */
    public static void openList(Player player, String owner) {
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

    // ======================== 主菜单 ========================

    private static void sendMainMenu(Player player, String ownerFilter) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a§l【领地系统-主菜单】");

        form.content("§f请选择您需要的功能:");

        form.button("§d§l领地传送");
        form.button("§e§l领地列表");
        form.button("§a§l创建领地");
        form.button("§c§l关闭");

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                switch (clicked) {
                    case 0 -> sendTeleportList(player, ownerFilter);
                    case 1 -> openList(player, ownerFilter);
                    case 2 -> BedrockCreateResidenceUI.open(player, ownerFilter);
                    // case 3 -> 关闭
                }
            });
        });

        BedrockFormUtil.sendForm(player, form);
    }

    // ======================== 领地传送 ========================

    private static void sendTeleportList(Player player, String ownerFilter) {
        UserListData data = ResidenceListAPI.getUserManager().getNullable(player.getUniqueId());
        if (data == null) {
            PluginMessages.LOAD_FAILED.sendTo(player);
            return;
        }

        List<ClaimedResidence> residences = collectResidences(player, data, null).stream()
                .filter(res -> {
                    ResidenceData d = Main.getInstance().getResidenceManager().getResidence(res);
                    return d.canTeleport(player);
                })
                .toList();

        if (residences.isEmpty()) {
            SimpleForm.Builder form = SimpleForm.builder()
                    .title("§d§l【领地系统-领地传送】")
                    .content("§f当前没有可传送的领地。")
                    .button("§e§l返回主菜单");
            form.validResultHandler(response ->
                    BedrockFormUtil.runSync(() -> sendMainMenu(player, ownerFilter)));
            BedrockFormUtil.sendForm(player, form);
            return;
        }

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§d§l【领地系统-领地传送】");

        form.content("§f点击领地即可传送:");

        for (ClaimedResidence res : residences) {
            ResidenceData resData = Main.getInstance().getResidenceManager().getResidence(res);
            String name = BedrockFormUtil.stripColor(resData.getDisplayName());
            form.button("§d" + name + "\n§f主人: §e" + resData.getOwner());
        }

        form.button("§e§l返回主菜单");

        final List<ClaimedResidence> finalResidences = residences;
        final int backBtnIndex = residences.size();

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked < finalResidences.size()) {
                    ClaimedResidence res = finalResidences.get(clicked);
                    ResidenceData resData = Main.getInstance().getResidenceManager().getResidence(res);
                    res.tpToResidence(player, player, player.hasPermission("residence.admin"));
                    PluginMessages.TELEPORT.SOUND.playTo(player);
                } else if (clicked == backBtnIndex) {
                    sendMainMenu(player, ownerFilter);
                }
            });
        });

        BedrockFormUtil.sendForm(player, form);
    }

    // ======================== 领地列表 ========================

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
        return display.stream().filter(r -> {
            ResidenceData d = Main.getInstance().getResidenceManager().getResidence(r);
            return d.isPublicDisplayed() || d.isOwner(player);
        }).toList();
    }

    private static void sendListForm(Player player, UserListData data, String owner, List<ClaimedResidence> residences) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e§l【领地系统-领地列表】");

        StringBuilder content = new StringBuilder();
        if (owner != null) {
            content.append("§f正在查看 §e").append(owner).append(" §f的领地\n");
        } else {
            content.append("§f所有公开领地均已展示\n");
        }
        content.append("§f排序: §e").append(data.getSortFunction().name())
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
            if (data.isPinned(res.getName())) btnText.append("§e★ ");
            btnText.append("§a").append(name);
            btnText.append("\n§f主人: §e").append(resData.getOwner());
            btnText.append(" §f| 规模: §e").append(size);
            btnText.append(" §f| 成员: §e").append(members);
            btnText.append("\n§f赞: §a").append(likes).append(" §f踩: §c").append(dislikes);

            if (!resData.getDescription().isEmpty()) {
                String desc = BedrockFormUtil.stripColor(String.join(" ", resData.getDescription()));
                if (desc.length() > 50) desc = desc.substring(0, 50) + "...";
                btnText.append("\n§f").append(desc);
            }

            form.button(btnText.toString());
        }

        // 功能按钮
        form.button("§e§l切换排序方式");
        form.button(owner == null ? "§e§l查看我的领地" : "§e§l查看所有领地");
        form.button("§a§l创建领地");
        form.button("§e§l返回主菜单");
        form.button("§c§l关闭");

        final List<ClaimedResidence> finalResidences = residences;
        final int sortBtnIndex = finalResidences.size();
        final int filterBtnIndex = finalResidences.size() + 1;
        final int createBtnIndex = finalResidences.size() + 2;
        final int backBtnIndex = finalResidences.size() + 3;
        // closeBtnIndex = finalResidences.size() + 4

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                if (clicked < finalResidences.size()) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    showResidenceActionMenu(player, data, finalResidences.get(clicked), owner);
                } else if (clicked == sortBtnIndex) {
                    data.setSortFunction(data.getSortFunction().next());
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    openList(player, owner);
                } else if (clicked == filterBtnIndex) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    openList(player, owner == null ? player.getName() : null);
                } else if (clicked == createBtnIndex) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    BedrockCreateResidenceUI.open(player, owner);
                } else if (clicked == backBtnIndex) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    sendMainMenu(player, owner);
                }
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
                .title("§a§l【领地系统-" + name + "】");

        StringBuilder content = new StringBuilder();
        content.append("§f━━━━━━━━━━━━━━━\n");
        content.append("§f主人: §e").append(resData.getOwner()).append("\n");
        content.append("§f规模: §e").append(residence.getMainArea().getSize()).append(" 方块\n");
        content.append("§f成员: §e").append(residence.getTrustedPlayers().size() + 1).append("\n");
        content.append("§f评价: §a赞 ").append(resData.countRate(ResidenceRate::recommend));
        content.append(" §c踩 ").append(resData.countRate(r -> !r.recommend())).append("\n");
        content.append("§f状态: ").append(resData.isPublicDisplayed() ? "§a公开" : "§c私有").append("\n");

        if (!resData.getDescription().isEmpty()) {
            content.append("\n§f描述: §e").append(BedrockFormUtil.stripColor(String.join(" ", resData.getDescription())));
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
        form.button("§e§l返回列表");

        final boolean canTeleport = resData.canTeleport(player);
        final boolean isPinned = data.isPinned(residence.getName());

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                switch (clicked) {
                    case 0 -> {
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
                        if (isPinned) {
                            data.removePin(residence.getName());
                            PluginMessages.UNPIN.SOUND.playTo(player);
                            PluginMessages.UNPIN.MESSAGE.sendTo(player, resData.getDisplayName());
                        } else {
                            data.setPin(residence.getName(), 0);
                            PluginMessages.PIN.SOUND.playTo(player);
                            PluginMessages.PIN.MESSAGE.sendTo(player, resData.getDisplayName());
                        }
                        openList(player, owner);
                    }
                    case 3 -> openList(player, owner);
                }
            });
        });

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendEmptyForm(Player player, String owner) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e§l【领地系统-领地列表】")
                .content("§f目前没有可显示的领地。")
                .button("§a§l创建领地")
                .button("§e§l返回主菜单")
                .button("§c§l关闭");

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                if (clicked == 0) {
                    BedrockCreateResidenceUI.open(player, owner);
                } else if (clicked == 1) {
                    sendMainMenu(player, owner);
                }
            });
        });

        BedrockFormUtil.sendForm(player, form);
    }
}
