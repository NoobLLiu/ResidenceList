package com.artformgames.plugin.residencelist.bedrock;

import com.artformgames.plugin.residencelist.ResidenceListAPI;
import com.artformgames.plugin.residencelist.api.residence.ResidenceData;
import com.artformgames.plugin.residencelist.api.residence.ResidenceRate;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

import java.util.List;

/**
 * 基岩版领地管理界面。
 * <p>
 * 对应 Java 版 {@link com.artformgames.plugin.residencelist.ui.ResidenceManageUI}。
 * 只有领地主人或管理员可以使用。
 * 提供编辑昵称、描述、图标、公开/私有切换、设置传送点、管理评价功能。
 */
public class BedrockResidenceManageUI {

    private BedrockResidenceManageUI() {
    }

    /**
     * 打开领地管理表单。
     *
     * @param player        目标玩家
     * @param residenceData 领地数据
     * @param ownerFilter   领地列表的筛选主人（用于返回）
     */
    public static void open(Player player, ResidenceData residenceData, String ownerFilter) {
        sendManageMenu(player, residenceData, ownerFilter);
    }

    /**
     * 管理主菜单。
     */
    private static void sendManageMenu(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a【领地系统-管理领地】" + name);

        StringBuilder content = new StringBuilder();
        content.append("§f━━━━━━━━━━━━━━━\n");
        content.append("§f主人: §e").append(residenceData.getOwner()).append("\n");
        content.append("§f别名: §e").append(residenceData.getAliasName() != null ? residenceData.getAliasName() : "§f未设置").append("\n");
        content.append("§f状态: ").append(residenceData.isPublicDisplayed() ? "§a公开" : "§c私有").append("\n");
        content.append("§f规模: §e").append(residence.getMainArea().getSize()).append(" 方块\n");
        content.append("§f成员: §e").append(residence.getTrustedPlayers().size() + 1).append("\n");
        content.append("§f评价: §a赞 ").append(residenceData.countRate(ResidenceRate::recommend));
        content.append(" §c踩 ").append(residenceData.countRate(r -> !r.recommend())).append("\n");

        if (!residenceData.getDescription().isEmpty()) {
            content.append("\n§f描述:\n§e").append(BedrockFormUtil.stripColor(String.join("\n", residenceData.getDescription()))).append("\n");
        }

        // 传送点信息
        Location tpLoc = residenceData.getTeleportLocation(player, player.getLocation());
        if (tpLoc != null) {
            content.append("\n§f传送点: §e").append(tpLoc.getWorld().getName())
                    .append(" @ (").append(tpLoc.getBlockX()).append(", ")
                    .append(tpLoc.getBlockY()).append(", ").append(tpLoc.getBlockZ()).append(")");
        }

        form.content(content.toString());

        // 功能按钮
        form.button("§0编辑昵称", FormImage.Type.PATH, BedrockFormUtil.menuIcon("liuyanban"));
        form.button("§0编辑描述", FormImage.Type.PATH, BedrockFormUtil.menuIcon("guanligonggao"));
        form.button("§0编辑图标", FormImage.Type.PATH, BedrockFormUtil.menuIcon("tubiao"));
        form.button(residenceData.isPublicDisplayed() ? "§0切换为私有" : "§0切换为公开", FormImage.Type.PATH, BedrockFormUtil.menuIcon("lock"));
        form.button("§0传送到领地", FormImage.Type.PATH, BedrockFormUtil.menuIcon("gotohome"));
        form.button("§0设置传送点", FormImage.Type.PATH, BedrockFormUtil.menuIcon("addtmhome"));
        form.button("§0管理评价 §f(" + residenceData.getRates().size() + ")", FormImage.Type.PATH, BedrockFormUtil.menuIcon("guanligonggao"));
        form.button("§0转让领地", FormImage.Type.PATH, BedrockFormUtil.menuIcon("tmtp"));
        form.button("§0出售领地", FormImage.Type.PATH, BedrockFormUtil.menuIcon("tmmoney"));
        form.button("§0租借领地", FormImage.Type.PATH, BedrockFormUtil.menuIcon("cleanpack"));
        form.button("§0权限管理", FormImage.Type.PATH, BedrockFormUtil.menuIcon("guanliplayer"));
        form.button("§0返回领地列表", FormImage.Type.PATH, BedrockFormUtil.menuIcon("lastpage"));

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                switch (clicked) {
                    case 0 -> sendEditNameForm(player, residenceData, ownerFilter);
                    case 1 -> sendEditDescriptionForm(player, residenceData, ownerFilter);
                    case 2 -> sendEditIconForm(player, residenceData, ownerFilter);
                    case 3 -> {
                        boolean newStatus = !residenceData.isPublicDisplayed();
                        residenceData.modify(d -> d.setPublicDisplayed(newStatus));
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                        sendManageMenu(player, residenceData, ownerFilter);
                    }
                    case 4 -> {
                        residence.tpToResidence(player, player, player.hasPermission("residence.admin"));
                        PluginMessages.TELEPORT.SOUND.playTo(player);
                    }
                    case 5 -> {
                        residence.setTpLoc(player, player.hasPermission("residence.admin"));
                        PluginConfig.GUI.CLICK_SOUND.playTo(player);
                        sendManageMenu(player, residenceData, ownerFilter);
                    }
                    case 6 -> sendManageRatesForm(player, residenceData, ownerFilter);
                    case 7 -> BedrockMarketUI.openTransfer(player, residenceData, ownerFilter);
                    case 8 -> BedrockMarketUI.openSell(player, residenceData, ownerFilter);
                    case 9 -> BedrockMarketUI.openRent(player, residenceData, ownerFilter);
                    case 10 -> BedrockPermissionUI.open(player, residenceData, ownerFilter);
                    case 11 -> BedrockResidenceListUI.openList(player, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceListUI.openList(player, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 编辑昵称表单。
     */
    private static void sendEditNameForm(Player player, ResidenceData residenceData, String ownerFilter) {
        CustomForm.Builder form = CustomForm.builder()
                .title("§e【领地系统-编辑昵称】");

        String displayName = BedrockFormUtil.stripColor(residenceData.getDisplayName());
        form.input("为领地 " + displayName + " 设置别名（不超过16字符）",
                "输入新的别名...",
                residenceData.getAliasName() != null ? residenceData.getAliasName() : "");

        form.validResultHandler(response -> {
            String newName = response.asInput(0);
            BedrockFormUtil.runSync(() -> {
                if (newName == null || newName.isBlank()) {
                    PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    sendManageMenu(player, residenceData, ownerFilter);
                    return;
                }
                if (newName.length() > 16) {
                    PluginMessages.EDIT.NAME_TOO_LONG.sendTo(player, residenceData.getDisplayName());
                    PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    sendManageMenu(player, residenceData, ownerFilter);
                    return;
                }
                residenceData.modify(d -> d.setNickname(newName));
                PluginMessages.EDIT.NAME_UPDATED.sendTo(player, residenceData.getDisplayName());
                PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                sendManageMenu(player, residenceData, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendManageMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 编辑描述表单。
     */
    private static void sendEditDescriptionForm(Player player, ResidenceData residenceData, String ownerFilter) {
        CustomForm.Builder form = CustomForm.builder()
                .title("§e【领地系统-编辑描述】");

        String displayName = BedrockFormUtil.stripColor(residenceData.getDisplayName());
        String currentDesc = residenceData.getDescription().isEmpty() ? "" : String.join("\\n", residenceData.getDescription());
        form.input("为领地 " + displayName + " 编辑描述（可用\\n换行）",
                "输入描述内容...", currentDesc);

        form.validResultHandler(response -> {
            String desc = response.asInput(0);
            BedrockFormUtil.runSync(() -> {
                if (desc == null || desc.isBlank()) {
                    residenceData.modify(d -> d.setDescription(List.of()));
                } else {
                    String[] lines = desc.split("\\\\n");
                    residenceData.modify(d -> d.setDescription(lines));
                }
                PluginMessages.EDIT.DESCRIPTION_UPDATED.sendTo(player, residenceData.getDisplayName());
                PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                sendManageMenu(player, residenceData, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendManageMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 编辑图标表单（用 Dropdown 选择材质）。
     */
    private static void sendEditIconForm(Player player, ResidenceData residenceData, String ownerFilter) {
        CustomForm.Builder form = CustomForm.builder()
                .title("§e【领地系统-编辑图标】");

        String displayName = BedrockFormUtil.stripColor(residenceData.getDisplayName());

        // 提供常用的领地图标材质
        String[] iconOptions = {
                "草方块", "石头", "木板", "砖块", "书架",
                "金块", "铁块", "钻石块", "绿宝石块", "石英块",
                "工作台", "熔炉", "箱子", "末影箱", "信标",
                "花盆", "灯笼", "火把", "红石灯", "海晶灯"
        };

        int defaultIndex = 0;
        if (residenceData.getIconMaterial() != null) {
            String matName = residenceData.getIconMaterial().name();
            for (int i = 0; i < iconOptions.length; i++) {
                if (mapIconNameToMaterial(iconOptions[i]).name().equalsIgnoreCase(matName)) {
                    defaultIndex = i;
                    break;
                }
            }
        }

        form.dropdown("为领地 " + displayName + " 选择图标材质", defaultIndex, iconOptions);

        form.validResultHandler(response -> {
            int selectedIndex = response.asDropdown(0);
            String selectedIcon = iconOptions[selectedIndex];
            Material material = mapIconNameToMaterial(selectedIcon);

            BedrockFormUtil.runSync(() -> {
                if (PluginConfig.SETTINGS.BLOCKED_ICON_TYPES.contains(material)) {
                    PluginMessages.EDIT.ICON_BLOCKED.sendTo(player, residenceData.getDisplayName());
                    PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                } else {
                    residenceData.modify(d -> d.setIconMaterial(material, -1));
                    PluginMessages.EDIT.ICON_UPDATED.sendTo(player, residenceData.getDisplayName());
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                }
                sendManageMenu(player, residenceData, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendManageMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 将中文图标名映射为 Material。
     */
    private static Material mapIconNameToMaterial(String name) {
        return switch (name) {
            case "草方块" -> Material.GRASS_BLOCK;
            case "石头" -> Material.STONE;
            case "木板" -> Material.OAK_PLANKS;
            case "砖块" -> Material.BRICKS;
            case "书架" -> Material.BOOKSHELF;
            case "金块" -> Material.GOLD_BLOCK;
            case "铁块" -> Material.IRON_BLOCK;
            case "钻石块" -> Material.DIAMOND_BLOCK;
            case "绿宝石块" -> Material.EMERALD_BLOCK;
            case "石英块" -> Material.QUARTZ_BLOCK;
            case "工作台" -> Material.CRAFTING_TABLE;
            case "熔炉" -> Material.FURNACE;
            case "箱子" -> Material.CHEST;
            case "末影箱" -> Material.ENDER_CHEST;
            case "信标" -> Material.BEACON;
            case "花盆" -> Material.FLOWER_POT;
            case "灯笼" -> Material.LANTERN;
            case "火把" -> Material.TORCH;
            case "红石灯" -> Material.REDSTONE_LAMP;
            case "海晶灯" -> Material.SEA_LANTERN;
            default -> Material.GRASS_BLOCK;
        };
    }

    /**
     * 管理评价列表（管理员/领地主人可删除评价）。
     */
    private static void sendManageRatesForm(Player player, ResidenceData residenceData, String ownerFilter) {
        boolean allowDelete = player.hasPermission("residence.admin") ||
                (PluginConfig.SETTINGS.ALLOW_OWNER_DELETE_RATE.resolve() && residenceData.isOwner(player));

        if (residenceData.getRates().isEmpty()) {
            SimpleForm.Builder form = SimpleForm.builder()
                    .title("§e【领地系统-管理评价】")
                    .content("§f目前暂无评价。")
                    .button("§0返回", FormImage.Type.PATH, BedrockFormUtil.menuIcon("lastpage"));

            form.validResultHandler(response ->
                    BedrockFormUtil.runSync(() -> sendManageMenu(player, residenceData, ownerFilter)));

            form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                    sendManageMenu(player, residenceData, ownerFilter)));

            BedrockFormUtil.sendForm(player, form);
            return;
        }

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-管理评价】§f(" + residenceData.getRates().size() + ")");

        StringBuilder content = new StringBuilder();
        content.append("§f点击评价可").append(allowDelete ? "§c删除评价" : "§e查看详情").append("\n\n");

        for (ResidenceRate rate : residenceData.getRates().values()) {
            String author = rate.getAuthorName() != null ? rate.getAuthorName() : "?";
            content.append(rate.recommend() ? "§a赞 " : "§c踩 ")
                    .append("§f").append(author)
                    .append(" §e(").append(ResidenceListAPI.format(rate.time())).append(")\n");
        }

        form.content(content.toString());

        // 每条评价一个按钮
        for (ResidenceRate rate : residenceData.getRates().values()) {
            String author = rate.getAuthorName() != null ? rate.getAuthorName() : "?";
            String btnText = (rate.recommend() ? "§a赞 " : "§c踩 ") + "§f" + author;
            if (allowDelete) btnText += " §c[点击删除]";
            form.button(btnText, FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.menuIcon("lastpage"));

        final boolean finalAllowDelete = allowDelete;
        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            int rateCount = residenceData.getRates().size();
            BedrockFormUtil.runSync(() -> {
                if (clicked < rateCount) {
                    // 点击了某条评价
                    ResidenceRate[] rates = residenceData.getRates().values().toArray(new ResidenceRate[0]);
                    ResidenceRate target = rates[clicked];
                    if (finalAllowDelete) {
                        // 确认删除
                        sendDeleteRateConfirm(player, residenceData, ownerFilter, target);
                    } else {
                        sendManageMenu(player, residenceData, ownerFilter);
                    }
                } else {
                    sendManageMenu(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendManageMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 确认删除评价（ModalForm）。
     */
    private static void sendDeleteRateConfirm(Player player, ResidenceData residenceData,
                                               String ownerFilter, ResidenceRate rate) {
        String author = rate.getAuthorName() != null ? rate.getAuthorName() : "?";

        ModalForm.Builder form = ModalForm.builder()
                .title("§c【领地系统-确认删除评价】")
                .content("§f确定要删除 §e" + author + " §f的评价吗?\n\n"
                        + (rate.recommend() ? "§a赞 " : "§c踩 ")
                        + "§f" + BedrockFormUtil.stripColor(rate.content()))
                .button1("§0确认删除")
                .button2("§0取消");

        form.validResultHandler(response -> {
            BedrockFormUtil.runSync(() -> {
                if (response.clickedButtonId() == 0) {
                    residenceData.removeRate(rate.author());
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                }
                sendManageRatesForm(player, residenceData, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendManageRatesForm(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }
}
