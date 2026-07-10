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
import org.geysermc.cumulus.util.FormImage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 基岩版领地系统主菜单及领地列表界面（SimpleForm）。
 * <p>
 * 按钮配色规则：第一行 §0（加粗黑色），第二行 §1（正常深蓝色）。
 */
public class BedrockResidenceListUI {

    private static final int PAGE_SIZE = 10;

    private BedrockResidenceListUI() {
    }

    /**
     * 打开主菜单。
     */
    public static void open(Player player) {
        sendMainMenu(player);
    }

    /**
     * 打开领地列表（带筛选）。
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

        sendListForm(player, data, owner, residences, 1);
    }

    // ======================== 主菜单 ========================

    private static void sendMainMenu(Player player) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a【领地系统-主菜单】");

        form.content("§f请选择您需要的功能:");

        form.button("§0个人领地传送", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0个人领地列表", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0公开领地列表", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0创建个人领地", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0关闭", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                switch (clicked) {
                    case 0 -> sendTeleportList(player);
                    case 1 -> openList(player, player.getName());
                    case 2 -> openList(player, null);
                    case 3 -> BedrockCreateResidenceUI.open(player, null);
                }
            });
        });

        BedrockFormUtil.sendForm(player, form);
    }

    // ======================== 个人领地传送 ========================

    private static void sendTeleportList(Player player) {
        UserListData data = ResidenceListAPI.getUserManager().getNullable(player.getUniqueId());
        if (data == null) {
            PluginMessages.LOAD_FAILED.sendTo(player);
            return;
        }

        List<ClaimedResidence> residences = collectResidences(player, data, player.getName()).stream()
                .filter(res -> {
                    ResidenceData d = Main.getInstance().getResidenceManager().getResidence(res);
                    return d.canTeleport(player);
                })
                .toList();

        if (residences.isEmpty()) {
            SimpleForm.Builder form = SimpleForm.builder()
                    .title("§d【领地系统-个人领地传送】")
                    .content("§f当前没有可传送的领地。")
                    .button("§0返回主菜单", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
            form.validResultHandler(response ->
                    BedrockFormUtil.runSync(() -> sendMainMenu(player)));
            BedrockFormUtil.sendForm(player, form);
            return;
        }

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§d【领地系统-个人领地传送】");

        form.content("§f点击领地即可传送:");

        for (ClaimedResidence res : residences) {
            ResidenceData resData = Main.getInstance().getResidenceManager().getResidence(res);
            String name = BedrockFormUtil.stripColor(resData.getDisplayName());
            form.button("§0" + name + "\n§1" + resData.getOwner(), FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }

        form.button("§0返回主菜单", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        final List<ClaimedResidence> finalResidences = residences;
        final int backBtnIndex = residences.size();

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked < finalResidences.size()) {
                    ClaimedResidence res = finalResidences.get(clicked);
                    res.tpToResidence(player, player, player.hasPermission("residence.admin"));
                    PluginMessages.TELEPORT.SOUND.playTo(player);
                } else if (clicked == backBtnIndex) {
                    sendMainMenu(player);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() -> sendMainMenu(player)));
        BedrockFormUtil.sendForm(player, form);
    }

    // ======================== 领地列表 ========================

    private static List<ClaimedResidence> collectResidences(Player player, UserListData data, String owner) {
        List<ClaimedResidence> display = new ArrayList<>();
        Comparator<ClaimedResidence> comparator = data.getSortFunction().residenceComparator(data.isSortReversed());

        data.getPinned().stream()
                .map(ResidenceListAPI::getResidence)
                .filter(res -> res != null && (owner == null || res.isOwner(owner)))
                .filter(res -> ResidenceUtils.viewable(res, player))
                .sorted(comparator).forEach(display::add);

        ResidenceListAPI.listResidences().stream()
                .filter(res -> !display.contains(res))
                .filter(res -> owner == null || res.isOwner(owner))
                .filter(res -> ResidenceUtils.viewable(res, player))
                .sorted(comparator).forEach(display::add);

        return display.stream().filter(r -> {
            ResidenceData d = Main.getInstance().getResidenceManager().getResidence(r);
            return d.isPublicDisplayed() || d.isOwner(player);
        }).toList();
    }

    private static void sendListForm(Player player, UserListData data, String owner,
                                     List<ClaimedResidence> residences, int page) {
        boolean isPersonal = owner != null && owner.equals(player.getName());
        String titleName = isPersonal ? "个人领地列表" : "公开领地列表";

        int totalPages = (int) Math.ceil((double) residences.size() / PAGE_SIZE);
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, residences.size());
        List<ClaimedResidence> pageItems = residences.subList(fromIndex, toIndex);

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-" + titleName + "】§f(第" + page + "页/共" + totalPages + "页)");

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
        for (ClaimedResidence res : pageItems) {
            ResidenceData resData = Main.getInstance().getResidenceManager().getResidence(res);
            String name = BedrockFormUtil.stripColor(resData.getDisplayName());
            int likes = resData.countRate(ResidenceRate::recommend);
            int dislikes = resData.countRate(r -> !r.recommend());
            StringBuilder btnText = new StringBuilder();
            if (data.isPinned(res.getName())) btnText.append("§e★ ");
            btnText.append("§0").append(name);
            // 第二行精简：去掉"主人:"，去掉竖线前后空格
            btnText.append("\n§1").append(resData.getOwner());
            btnText.append("|§a赞").append(likes).append("|§c踩").append(dislikes);

            if (!resData.getDescription().isEmpty()) {
                String desc = BedrockFormUtil.stripColor(String.join(" ", resData.getDescription()));
                if (desc.length() > 40) desc = desc.substring(0, 40) + "...";
                btnText.append("\n§1").append(desc);
            }

            form.button(btnText.toString(), FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }

        // 分页和功能按钮
        List<ClaimedResidence> finalResidences = residences;
        int[] btnIndices = new int[4]; // prev, next, sort, back
        int btnIndex = pageItems.size();

        if (page > 1) {
            form.button("§0上一页 §f(第" + (page - 1) + "页)", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
            btnIndices[0] = btnIndex++;
        } else {
            btnIndices[0] = -1;
        }

        if (page < totalPages) {
            form.button("§0下一页 §f(第" + (page + 1) + "页)", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
            btnIndices[1] = btnIndex++;
        } else {
            btnIndices[1] = -1;
        }

        form.button("§0切换排序方式", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        btnIndices[2] = btnIndex++;
        form.button("§0返回主菜单", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        btnIndices[3] = btnIndex;

        final int currentPage = page;
        final int prevBtnIndex = btnIndices[0];
        final int nextBtnIndex = btnIndices[1];
        final int sortBtnIndex = btnIndices[2];
        final int backBtnIndex = btnIndices[3];

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                if (clicked < pageItems.size()) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    showResidenceActionMenu(player, data, pageItems.get(clicked), owner);
                } else if (prevBtnIndex >= 0 && clicked == prevBtnIndex) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    sendListForm(player, data, owner, finalResidences, currentPage - 1);
                } else if (nextBtnIndex >= 0 && clicked == nextBtnIndex) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    sendListForm(player, data, owner, finalResidences, currentPage + 1);
                } else if (clicked == sortBtnIndex) {
                    data.setSortFunction(data.getSortFunction().next());
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    sendListForm(player, data, owner, finalResidences, 1);
                } else if (clicked == backBtnIndex) {
                    PluginConfig.GUI.CLICK_SOUND.playTo(player);
                    sendMainMenu(player);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() -> sendMainMenu(player)));
        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 操作子菜单。
     */
    private static void showResidenceActionMenu(Player player, UserListData data, ClaimedResidence residence, String owner) {
        ResidenceData resData = Main.getInstance().getResidenceManager().getResidence(residence);
        String name = BedrockFormUtil.stripColor(resData.getDisplayName());

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a【领地系统-" + name + "】");

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

        form.button("§0查看详细信息", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        if (resData.canTeleport(player)) {
            form.button("§0传送到领地", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }
        if (data.isPinned(residence.getName())) {
            form.button("§0取消置顶", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        } else {
            form.button("§0置顶领地", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }
        form.button("§0返回列表", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

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

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() -> openList(player, owner)));
        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendEmptyForm(Player player, String owner) {
        boolean isPersonal = owner != null && owner.equals(player.getName());
        String titleName = isPersonal ? "个人领地列表" : "公开领地列表";

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-" + titleName + "】")
                .content("§f目前没有可显示的领地。")
                .button("§0返回主菜单", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON)
                .button("§0关闭", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                if (clicked == 0) {
                    sendMainMenu(player);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() -> sendMainMenu(player)));
        BedrockFormUtil.sendForm(player, form);
    }
}
