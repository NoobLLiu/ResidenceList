package com.artformgames.plugin.residencelist.bedrock;

import com.artformgames.plugin.residencelist.conf.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class BedrockPlayerSelectUI {

    private static final int PER_PAGE = 20;

    private BedrockPlayerSelectUI() {
    }

    public static void open(Player player, String title, String inputPlaceholder,
                            BiConsumer<Player, String> callback, Runnable backAction) {
        sendSelectMode(player, title, inputPlaceholder, callback, backAction);
    }

    private static void sendSelectMode(Player player, String title, String inputPlaceholder,
                                       BiConsumer<Player, String> callback, Runnable backAction) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【" + title + "】");

        form.content("§f请选择玩家来源：");
        form.button("§a从在线玩家中选择", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§e从全部玩家中选择", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.menuIcon("lastpage"));

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                switch (clicked) {
                    case 0 -> sendOnlinePlayerList(player, title, inputPlaceholder, callback,
                            () -> sendSelectMode(player, title, inputPlaceholder, callback, backAction), 0);
                    case 1 -> sendInputForm(player, title, inputPlaceholder, callback,
                            () -> sendSelectMode(player, title, inputPlaceholder, callback, backAction));
                    case 2 -> backAction.run();
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(backAction));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendOnlinePlayerList(Player player, String title, String inputPlaceholder,
                                             BiConsumer<Player, String> callback, Runnable backAction,
                                             int page) {
        List<Player> online = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) online.add(p);
        }

        int totalPages = Math.max(1, (online.size() + PER_PAGE - 1) / PER_PAGE);

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【" + title + "】 §7第" + (page + 1) + "/" + totalPages + "页");

        StringBuilder content = new StringBuilder();
        content.append("§f在线玩家列表（共").append(online.size()).append("人）\n");
        content.append("§f点击玩家名称进行选择。");
        form.content(content.toString());

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, online.size());
        int playerCount = end - start;

        for (int i = start; i < end; i++) {
            form.button("§0" + online.get(i).getName(), FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }

        boolean hasPrev = page > 0;
        boolean hasNext = page < totalPages - 1;

        if (hasPrev) {
            form.button("§0上一页", FormImage.Type.PATH, BedrockFormUtil.menuIcon("lastpage"));
        }
        if (hasNext) {
            form.button("§0下一页", FormImage.Type.PATH, BedrockFormUtil.menuIcon("lastpage"));
        }
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.menuIcon("lastpage"));

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked < playerCount) {
                    Player selected = online.get(start + clicked);
                    callback.accept(player, selected.getName());
                } else {
                    int navIndex = clicked - playerCount;
                    if (hasPrev && navIndex == 0) {
                        sendOnlinePlayerList(player, title, inputPlaceholder, callback, backAction, page - 1);
                    } else if (hasNext && navIndex == (hasPrev ? 1 : 0)) {
                        sendOnlinePlayerList(player, title, inputPlaceholder, callback, backAction, page + 1);
                    } else {
                        backAction.run();
                    }
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(backAction));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendInputForm(Player player, String title, String placeholder,
                                      BiConsumer<Player, String> callback, Runnable backAction) {
        CustomForm.Builder form = CustomForm.builder()
                .title("§e【" + title + "】");

        form.input("输入玩家名称", placeholder, "");

        form.validResultHandler(response -> {
            String name = response.asInput(0);
            BedrockFormUtil.runSync(() -> callback.accept(player, name));
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(backAction));

        BedrockFormUtil.sendForm(player, form);
    }
}
