package com.artformgames.plugin.residencelist.bedrock;

import com.artformgames.plugin.residencelist.api.residence.ResidenceData;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.utils.ResidenceFlagCategory;
import com.artformgames.plugin.residencelist.utils.ResidenceUtils;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * 基岩版领地权限管理界面。
 * <p>
 * 提供全局权限、玩家权限、重命名、镜像权限、进出消息和重置权限功能。
 */
public class BedrockPermissionUI {

    private BedrockPermissionUI() {
    }

    /**
     * 打开权限管理主菜单。
     *
     * @param player        目标玩家
     * @param residenceData 领地数据
     * @param ownerFilter   领地列表的筛选主人（用于返回）
     */
    public static void open(Player player, ResidenceData residenceData, String ownerFilter) {
        sendPermissionMenu(player, residenceData, ownerFilter);
    }

    /**
     * 权限管理主菜单。
     */
    private static void sendPermissionMenu(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§a【领地系统-权限管理】" + name);

        StringBuilder content = new StringBuilder();
        content.append("§f━━━━━━━━━━━━━━━\n");
        content.append("§f管理领地: §e").append(name).append("\n");
        content.append("§f请选择要管理的权限项目:");
        form.content(content.toString());

        form.button("§0全局权限设置", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0玩家权限管理", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0重命名领地", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0镜像权限", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0进出提示消息", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0重置全部权限", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回管理页", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                switch (clicked) {
                    case 0 -> sendGlobalFlagCategoryList(player, residenceData, ownerFilter);
                    case 1 -> sendPlayerPermissionMenu(player, residenceData, ownerFilter);
                    case 2 -> sendRenameForm(player, residenceData, ownerFilter);
                    case 3 -> sendMirrorForm(player, residenceData, ownerFilter);
                    case 4 -> sendMessageForm(player, residenceData, ownerFilter);
                    case 5 -> sendResetConfirm(player, residenceData, ownerFilter);
                    case 6 -> BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    // ======================== 全局权限 ========================

    /**
     * 全局权限分类列表。
     */
    private static void sendGlobalFlagCategoryList(Player player, ResidenceData residenceData, String ownerFilter) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-全局权限分类】");

        form.content("§f点击分类以编辑对应的全局权限开关。");

        for (ResidenceFlagCategory category : ResidenceFlagCategory.all()) {
            form.button("§0" + category.getDisplayName(), FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        final int backIndex = ResidenceFlagCategory.all().size();

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked >= 0 && clicked < backIndex) {
                    sendGlobalFlagCategoryForm(player, residenceData,
                            ResidenceFlagCategory.all().get(clicked), ownerFilter);
                } else if (clicked == backIndex) {
                    sendPermissionMenu(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendPermissionMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 某一分类下的全局权限开关表单。
     */
    private static void sendGlobalFlagCategoryForm(Player player, ResidenceData residenceData,
                                                   ResidenceFlagCategory category, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        List<Flags> flags = category.getGlobalFlags();

        CustomForm.Builder form = CustomForm.builder()
                .title("§e【全局权限-" + category.getDisplayName() + "】");

        form.stepSlider("操作", 0,
                "保存并继续编辑",
                "保存并返回上一级",
                "不保存，直接返回");

        for (Flags flag : flags) {
            boolean current = residence.getPermissions().has(flag, flag.isEnabled());
            form.toggle(flag.getName() + " - " + BedrockFormUtil.stripColor(flag.getDesc()), current);
        }

        form.validResultHandler(response -> BedrockFormUtil.runSync(() -> {
            int action = response.asStepSlider(0);
            if (action == 2) {
                // 不保存，直接返回
                sendGlobalFlagCategoryList(player, residenceData, ownerFilter);
                return;
            }
            // Save flags
            for (int i = 0; i < flags.size(); i++) {
                Flags flag = flags.get(i);
                boolean value = response.asToggle(i + 1);
                ResidenceUtils.setGlobalFlag(player, residence, flag.name(), value ? "true" : "false");
            }
            PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
            if (action == 0) {
                // 保存并继续编辑
                sendGlobalFlagCategoryForm(player, residenceData, category, ownerFilter);
            } else {
                // 保存并返回上一级 (action == 1)
                sendGlobalFlagCategoryList(player, residenceData, ownerFilter);
            }
        }));

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendGlobalFlagCategoryList(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    // ======================== 玩家权限 ========================

    /**
     * 玩家权限管理主菜单。
     */
    private static void sendPlayerPermissionMenu(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        List<ResidencePlayer> trusted = new ArrayList<>(residence.getTrustedPlayers());

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-玩家权限管理】");

        StringBuilder content = new StringBuilder();
        content.append("§f当前信任玩家列表（").append(trusted.size()).append("）\n");
        content.append("§f请选择操作：");
        form.content(content.toString());

        form.button("§a添加信任玩家", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§b编辑信任玩家", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§c移除信任玩家", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                switch (clicked) {
                    case 0 -> sendAddTrustedPlayerForm(player, residenceData, ownerFilter);
                    case 1 -> sendEditTrustedPlayerSelector(player, residenceData, ownerFilter);
                    case 2 -> sendRemoveTrustedPlayerSelector(player, residenceData, ownerFilter);
                    case 3 -> sendPermissionMenu(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendPermissionMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 添加信任玩家表单。
     */
    private static void sendAddTrustedPlayerForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();

        BedrockPlayerSelectUI.open(player, "领地系统-添加信任玩家", "玩家ID...",
                (p, name) -> {
                    if (name == null || name.isBlank()) {
                        sendPlayerPermissionMenu(p, residenceData, ownerFilter);
                        return;
                    }
                    OfflinePlayer target = Bukkit.getOfflinePlayer(name.trim());
                    boolean success = ResidenceUtils.setPlayerFlag(p, residence, target.getUniqueId(), "trusted", "true");
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(p);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                    }
                    sendPlayerPermissionMenu(p, residenceData, ownerFilter);
                },
                () -> sendPlayerPermissionMenu(player, residenceData, ownerFilter)
        );
    }

    /**
     * 为已选玩家选择权限分类。
     */
    private static void sendPlayerCategoryListForm(Player player, ResidenceData residenceData,
                                                   List<UUID> targetUUIDs, String ownerFilter) {
        // Build display names
        StringBuilder namesBuilder = new StringBuilder();
        for (UUID uuid : targetUUIDs) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName() != null ? op.getName() : "?";
            namesBuilder.append(name).append(", ");
        }
        if (namesBuilder.length() > 2) namesBuilder.setLength(namesBuilder.length() - 2);
        String names = namesBuilder.toString();

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【已选玩家权限分类】");

        StringBuilder content = new StringBuilder();
        content.append("§f已选玩家：§e").append(names).append("\n");
        if (targetUUIDs.size() > 1) {
            OfflinePlayer first = Bukkit.getOfflinePlayer(targetUUIDs.get(0));
            String firstName = first.getName() != null ? first.getName() : "?";
            content.append("§7以 §e").append(firstName).append(" §7的权限为底板进行编辑，保存后将应用到所有已选玩家。");
        }
        form.content(content.toString());

        for (ResidenceFlagCategory category : ResidenceFlagCategory.all()) {
            form.button("§0" + category.getDisplayName(), FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        final int backIndex = ResidenceFlagCategory.all().size();

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked >= 0 && clicked < backIndex) {
                    sendPlayerFlagForm(player, residenceData, targetUUIDs,
                            ResidenceFlagCategory.all().get(clicked), ownerFilter);
                } else if (clicked == backIndex) {
                    sendPlayerPermissionMenu(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendPlayerPermissionMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 为已选玩家编辑某一分类下的权限开关（支持批量）。
     */
    private static void sendPlayerFlagForm(Player player, ResidenceData residenceData,
                                           List<UUID> targetUUIDs, ResidenceFlagCategory category,
                                           String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();

        // Use first player for default values
        UUID firstUUID = targetUUIDs.get(0);
        OfflinePlayer firstOp = Bukkit.getOfflinePlayer(firstUUID);
        String firstName = firstOp.getName();
        if (firstName == null) {
            sendPlayerPermissionMenu(player, residenceData, ownerFilter);
            return;
        }

        // Build all names for display
        StringBuilder namesBuilder = new StringBuilder();
        for (UUID uuid : targetUUIDs) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName() != null ? op.getName() : "?";
            namesBuilder.append(name).append(", ");
        }
        if (namesBuilder.length() > 2) namesBuilder.setLength(namesBuilder.length() - 2);

        List<Flags> flags = category.getPlayerFlags();

        CustomForm.Builder form = CustomForm.builder()
                .title("§e【玩家权限-" + category.getDisplayName() + "】");

        // Label: selected players info
        StringBuilder label = new StringBuilder();
        label.append("§f已选玩家：§e").append(namesBuilder).append("\n");
        if (targetUUIDs.size() > 1) {
            label.append("§7以 §e").append(firstName).append(" §7的权限为底板进行编辑，保存后将应用到所有已选玩家。");
        } else {
            label.append("§7正在编辑：§f").append(firstName);
        }
        form.label(label.toString());

        // StepSlider for action
        form.stepSlider("操作", 0,
                "保存并继续编辑",
                "保存并返回分类",
                "不保存，直接返回分类");

        // Flag toggles - read from first player
        for (Flags flag : flags) {
            boolean current = residence.getPermissions().playerHas(firstName, flag.name(), flag.isEnabled());
            form.toggle(flag.getName() + " - " + BedrockFormUtil.stripColor(flag.getDesc()), current);
        }

        // label=index 0, stepSlider=index 1, toggles start at index 2
        form.validResultHandler(response -> BedrockFormUtil.runSync(() -> {
            int action = response.asStepSlider(1);
            if (action == 2) {
                // 不保存，直接返回
                sendPlayerCategoryListForm(player, residenceData, targetUUIDs, ownerFilter);
                return;
            }
            // Save: apply to all selected players
            for (int i = 0; i < flags.size(); i++) {
                Flags flag = flags.get(i);
                boolean value = response.asToggle(i + 2);
                String stateStr = value ? "true" : "false";
                for (UUID uuid : targetUUIDs) {
                    ResidenceUtils.setPlayerFlag(player, residence, uuid, flag.name(), stateStr);
                }
            }
            PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
            if (action == 0) {
                // 保存并继续编辑
                sendPlayerFlagForm(player, residenceData, targetUUIDs, category, ownerFilter);
            } else {
                // 保存并返回分类 (action == 1)
                sendPlayerCategoryListForm(player, residenceData, targetUUIDs, ownerFilter);
            }
        }));

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendPlayerCategoryListForm(player, residenceData, targetUUIDs, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 编辑信任玩家：打开通用选择器选择已信任玩家。
     */
    private static void sendEditTrustedPlayerSelector(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        List<ResidencePlayer> trusted = new ArrayList<>(residence.getTrustedPlayers());

        if (trusted.isEmpty()) {
            SimpleForm.Builder form = SimpleForm.builder()
                    .title("§e【领地系统-编辑信任玩家】")
                    .content("§f当前没有信任玩家。")
                    .button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
            form.validResultHandler(response ->
                    BedrockFormUtil.runSync(() -> sendPlayerPermissionMenu(player, residenceData, ownerFilter)));
            form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                    sendPlayerPermissionMenu(player, residenceData, ownerFilter)));
            BedrockFormUtil.sendForm(player, form);
            return;
        }

        List<UUID> candidates = new ArrayList<>();
        for (ResidencePlayer rp : trusted) {
            candidates.add(rp.getUniqueId());
        }

        BedrockPlayerSelector.open(
                player,
                "§e【编辑信任玩家-选择玩家】",
                candidates,
                new java.util.LinkedHashSet<>(),
                1,
                selected -> {
                    if (selected.isEmpty()) {
                        sendPlayerPermissionMenu(player, residenceData, ownerFilter);
                    } else {
                        List<UUID> selectedList = new ArrayList<>(selected);
                        sendPlayerCategoryListForm(player, residenceData, selectedList, ownerFilter);
                    }
                },
                () -> sendPlayerPermissionMenu(player, residenceData, ownerFilter)
        );
    }

    /**
     * 移除信任玩家：打开通用选择器选择要移除的玩家。
     */
    private static void sendRemoveTrustedPlayerSelector(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        List<ResidencePlayer> trusted = new ArrayList<>(residence.getTrustedPlayers());

        if (trusted.isEmpty()) {
            SimpleForm.Builder form = SimpleForm.builder()
                    .title("§e【领地系统-移除信任玩家】")
                    .content("§f当前没有信任玩家可以移除。")
                    .button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
            form.validResultHandler(response ->
                    BedrockFormUtil.runSync(() -> sendPlayerPermissionMenu(player, residenceData, ownerFilter)));
            form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                    sendPlayerPermissionMenu(player, residenceData, ownerFilter)));
            BedrockFormUtil.sendForm(player, form);
            return;
        }

        List<UUID> candidates = new ArrayList<>();
        for (ResidencePlayer rp : trusted) {
            candidates.add(rp.getUniqueId());
        }

        BedrockPlayerSelector.open(
                player,
                "§c【移除信任玩家-选择玩家】",
                candidates,
                new java.util.LinkedHashSet<>(),
                1,
                selected -> {
                    if (selected.isEmpty()) {
                        sendPlayerPermissionMenu(player, residenceData, ownerFilter);
                    } else {
                        List<UUID> selectedList = new ArrayList<>(selected);
                        sendBatchRemoveConfirm(player, residenceData, selectedList, ownerFilter);
                    }
                },
                () -> sendPlayerPermissionMenu(player, residenceData, ownerFilter)
        );
    }

    /**
     * 批量移除确认。
     */
    private static void sendBatchRemoveConfirm(Player player, ResidenceData residenceData,
                                               List<UUID> targets, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();

        StringBuilder names = new StringBuilder();
        for (UUID uuid : targets) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName() != null ? op.getName() : "?";
            names.append(name).append(", ");
        }
        if (names.length() > 2) names.setLength(names.length() - 2);

        ModalForm.Builder form = ModalForm.builder()
                .title("§c【领地系统-确认移除玩家】")
                .content("§f确定要移除以下 §e" + targets.size() + " §f名玩家的所有权限吗？\n\n"
                        + "§e" + names + "\n\n"
                        + "§c此操作不可撤销。")
                .button1("§0确认移除")
                .button2("§0取消");

        form.validResultHandler(response -> BedrockFormUtil.runSync(() -> {
            if (response.clickedButtonId() == 0) {
                boolean allSuccess = true;
                for (UUID uuid : targets) {
                    boolean success = ResidenceUtils.removePlayerAllFlags(player, residence, uuid);
                    if (!success) allSuccess = false;
                }
                if (allSuccess) {
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                } else {
                    PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                }
            }
            sendPlayerPermissionMenu(player, residenceData, ownerFilter);
        }));

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendPlayerPermissionMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    // ======================== 重命名 / 镜像 / 消息 / 重置 ========================

    /**
     * 重命名领地表单。
     */
    private static void sendRenameForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();

        CustomForm.Builder form = CustomForm.builder()
                .title("§e【领地系统-重命名领地】");

        form.input("为领地设置新的名称（英文/数字/下划线）", "输入新名称...", residence.getName());

        form.validResultHandler(response -> {
            String newName = response.asInput(0);
            BedrockFormUtil.runSync(() -> {
                if (newName == null || newName.isBlank()) {
                    sendPermissionMenu(player, residenceData, ownerFilter);
                    return;
                }
                boolean success = ResidenceUtils.renameResidence(player, residence, newName.trim());
                if (success) {
                    PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                } else {
                    PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                }
                sendPermissionMenu(player, residenceData, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendPermissionMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 镜像权限：选择源领地列表。
     */
    private static void sendMirrorForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String ownerName = residence.getOwner();

        List<ClaimedResidence> sources = new ArrayList<>();
        for (ClaimedResidence other : Residence.getInstance().getResidenceManager().getResidences().values()) {
            if (other.getName().equals(residence.getName())) continue;
            if (ownerName != null && ownerName.equals(other.getOwner())) {
                sources.add(other);
            }
        }

        if (sources.isEmpty()) {
            SimpleForm.Builder form = SimpleForm.builder()
                    .title("§e【领地系统-镜像权限】")
                    .content("§f主人名下没有其他可用于镜像权限的领地。")
                    .button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

            form.validResultHandler(response ->
                    BedrockFormUtil.runSync(() -> sendPermissionMenu(player, residenceData, ownerFilter)));

            form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                    sendPermissionMenu(player, residenceData, ownerFilter)));

            BedrockFormUtil.sendForm(player, form);
            return;
        }

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-镜像权限】");

        form.content("§f选择要从中复制权限的源领地。");

        for (ClaimedResidence source : sources) {
            ResidenceData sourceData = com.artformgames.plugin.residencelist.Main.getInstance()
                    .getResidenceManager().getResidence(source);
            String name = BedrockFormUtil.stripColor(sourceData.getDisplayName());
            form.button("§0" + name + "\n§1" + source.getName(), FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        final int backIndex = sources.size();

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked >= 0 && clicked < backIndex) {
                    sendMirrorConfirm(player, residenceData, sources.get(clicked), ownerFilter);
                } else if (clicked == backIndex) {
                    sendPermissionMenu(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendPermissionMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 确认镜像权限。
     */
    private static void sendMirrorConfirm(Player player, ResidenceData residenceData,
                                          ClaimedResidence source, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        ResidenceData sourceData = com.artformgames.plugin.residencelist.Main.getInstance()
                .getResidenceManager().getResidence(source);
        String sourceName = BedrockFormUtil.stripColor(sourceData.getDisplayName());

        ModalForm.Builder form = ModalForm.builder()
                .title("§c【领地系统-确认镜像权限】")
                .content("§f确定要从 §e" + sourceName + " §f镜像权限到本领地吗？\n\n"
                        + "§c此操作会覆盖当前领地的所有权限设置。")
                .button1("§0确认镜像")
                .button2("§0取消");

        form.validResultHandler(response -> BedrockFormUtil.runSync(() -> {
            if (response.clickedButtonId() == 0) {
                ResidenceUtils.mirrorPermissions(player, residence, source);
                PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
            }
            sendPermissionMenu(player, residenceData, ownerFilter);
        }));

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendMirrorForm(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 进出提示消息表单。
     */
    private static void sendMessageForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();

        CustomForm.Builder form = CustomForm.builder()
                .title("§e【领地系统-进出提示消息】");

        String enter = ResidenceUtils.getEnterMessage(residence);
        String leave = ResidenceUtils.getLeaveMessage(residence);

        form.input("进入领地提示消息（留空则清除）", "输入进入提示...", enter != null ? enter : "");
        form.input("离开领地提示消息（留空则清除）", "输入离开提示...", leave != null ? leave : "");

        form.validResultHandler(response -> BedrockFormUtil.runSync(() -> {
            String newEnter = response.asInput(0);
            String newLeave = response.asInput(1);

            ResidenceUtils.setEnterMessage(residence, newEnter != null && !newEnter.isBlank() ? newEnter : null);
            ResidenceUtils.setLeaveMessage(residence, newLeave != null && !newLeave.isBlank() ? newLeave : null);

            PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
            sendPermissionMenu(player, residenceData, ownerFilter);
        }));

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendPermissionMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 重置全部权限确认。
     */
    private static void sendResetConfirm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();

        ModalForm.Builder form = ModalForm.builder()
                .title("§c【领地系统-确认重置权限】")
                .content("§f确定要重置 §e" + BedrockFormUtil.stripColor(residenceData.getDisplayName())
                        + " §f的全部权限吗？\n\n"
                        + "§c此操作会恢复默认权限，不可撤销。")
                .button1("§0确认重置")
                .button2("§0取消");

        form.validResultHandler(response -> BedrockFormUtil.runSync(() -> {
            if (response.clickedButtonId() == 0) {
                ResidenceUtils.resetPermissions(residence);
                PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
            }
            sendPermissionMenu(player, residenceData, ownerFilter);
        }));

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                sendPermissionMenu(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }
}
