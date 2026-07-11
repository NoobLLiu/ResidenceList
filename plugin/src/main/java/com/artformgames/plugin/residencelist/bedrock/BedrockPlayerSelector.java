package com.artformgames.plugin.residencelist.bedrock;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 基岩版玩家多选表单。
 * <p>
 * 提供分页、多选的玩家选择界面，支持继续选择、翻页与结束选择操作。
 * 适用于领地信任玩家批量添加等场景。
 */
public class BedrockPlayerSelector {

    private static final int PAGE_SIZE = 10;

    private BedrockPlayerSelector() {
    }

    /**
     * 打开玩家选择表单。
     *
     * @param player     目标基岩版玩家
     * @param title      表单标题（支持 § 颜色代码）
     * @param candidates 候选玩家 UUID 列表（将按名称排序后展示）
     * @param selected   当前已选择的玩家集合（会被原地修改）
     * @param page       当前页码（1-based）
     * @param onDone     用户选择"结束选择"时的回调
     * @param onBack     用户关闭表单（X 按钮）时的回调
     */
    public static void open(Player player,
                            String title,
                            List<UUID> candidates,
                            Set<UUID> selected,
                            int page,
                            Consumer<Set<UUID>> onDone,
                            Runnable onBack) {
        if (candidates == null || candidates.isEmpty()) {
            sendEmptyForm(player, title, onBack);
            return;
        }

        List<UUID> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparing(BedrockPlayerSelector::getName));

        int totalPages = (sorted.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, sorted.size());
        List<UUID> pageCandidates = new ArrayList<>(sorted.subList(start, end));

        CustomForm.Builder form = CustomForm.builder().title(title);

        form.label(buildSelectedLabel(selected));

        for (UUID uuid : pageCandidates) {
            form.toggle(getName(uuid), selected.contains(uuid));
        }

        form.stepSlider("操作（第 " + page + "/" + totalPages + " 页）", List.of(
                "继续选择（更新已选）",
                "下一页",
                "上一页",
                "结束选择"
        ));

        final int currentPage = page;
        final List<UUID> pageCandidatesFinal = pageCandidates;

        form.validResultHandler(response -> BedrockFormUtil.runSync(() -> {
            for (int i = 0; i < pageCandidatesFinal.size(); i++) {
                UUID uuid = pageCandidatesFinal.get(i);
                if (response.asToggle(i)) {
                    selected.add(uuid);
                } else {
                    selected.remove(uuid);
                }
            }

            int action = response.asStepSlider(pageCandidatesFinal.size());
            switch (action) {
                case 0 -> open(player, title, candidates, selected, currentPage, onDone, onBack);
                case 1 -> open(player, title, candidates, selected, currentPage + 1, onDone, onBack);
                case 2 -> open(player, title, candidates, selected, currentPage - 1, onDone, onBack);
                case 3 -> onDone.accept(selected);
                default -> open(player, title, candidates, selected, currentPage, onDone, onBack);
            }
        }));

        form.closedResultHandler(() -> BedrockFormUtil.runSync(onBack));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 候选玩家为空时显示的提示表单。
     */
    private static void sendEmptyForm(Player player, String title, Runnable onBack) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title(title)
                .content("§f没有可选玩家。")
                .button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> BedrockFormUtil.runSync(onBack));
        form.closedResultHandler(() -> BedrockFormUtil.runSync(onBack));

        BedrockFormUtil.sendForm(player, form);
    }

    /**
     * 构建已选玩家列表的 Label 文本。
     */
    private static String buildSelectedLabel(Set<UUID> selected) {
        if (selected == null || selected.isEmpty()) {
            return "§f当前已选择玩家列表（0）：无";
        }

        List<String> names = new ArrayList<>(selected.size());
        for (UUID uuid : selected) {
            names.add(getName(uuid));
        }
        names.sort(Comparator.naturalOrder());

        return "§f当前已选择玩家列表（" + selected.size() + "）：" + String.join(", ", names);
    }

    /**
     * 获取指定 UUID 对应的玩家名称，无法获取时回退为 "?"。
     */
    private static String getName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = op.getName();
        return name != null ? name : "?";
    }
}
