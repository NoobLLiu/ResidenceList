package com.artformgames.plugin.residencelist.listener;

import com.artformgames.plugin.residencelist.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 使用 Bukkit 原生铁砧 GUI 实现文本输入。
 * 关键：在 onClick 中点击结果槽时就读取名称（此时铁砧还开着），
 * 存入 session，onClose 时使用。不取消结果槽点击，让铁砧正常关闭。
 */
public class AnvilNameInput implements Listener {

    private static final Map<UUID, InputSession> sessions = new HashMap<>();
    private static Method getRenameTextMethod = null;

    static {
        try {
            getRenameTextMethod = Class.forName("org.bukkit.inventory.AnvilView")
                    .getMethod("getRenameText");
        } catch (Exception ignored) {
            // Paper API not available at compile time, use reflection at runtime
        }
    }

    public static void open(@NotNull Player player, @NotNull String title,
                            @NotNull String defaultText,
                            @NotNull BiConsumer<Player, String> callback) {
        InputHolder holder = new InputHolder();
        Inventory anvil = Bukkit.createInventory(holder, InventoryType.ANVIL, title);
        holder.setInventory(anvil);

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(defaultText);
            paper.setItemMeta(meta);
        }
        anvil.setItem(0, paper);

        sessions.put(player.getUniqueId(), new InputSession(callback));
        player.openInventory(anvil);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof InputHolder)) return;

        int rawSlot = event.getRawSlot();

        // 点击结果槽(slot 2)：在铁砧处理之前读取名称
        if (rawSlot == 2) {
            InputSession session = sessions.get(player.getUniqueId());
            if (session == null) return;

            // 此时铁砧还开着，结果物品还在 slot 2
            // 优先用反射读取 Paper AnvilView.getRenameText()
            String name = getRenameText(event.getView());

            // 反射失败则从结果物品(slot 2)的显示名读取
            if (name == null || name.isEmpty()) {
                name = extractName(topInventory.getItem(2));
            }

            // 存入 session，标记为已提交
            session.setName(name != null ? name : "");
            session.setSubmitted(true);

            // 不取消事件，让铁砧正常处理（玩家取出结果 → 铁砧关闭 → onClose 触发）
            return;
        }

        // 取消输入槽(slot 0, 1)的点击，防止玩家拿走输入物品
        if (rawSlot >= 0 && rawSlot < 3) {
            event.setCancelled(true);
            return;
        }

        // 取消从玩家背包 shift-click 到铁砧
        if (event.isShiftClick() && event.getClickedInventory() != topInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof InputHolder)) return;

        InputSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        String name;
        if (session.isSubmitted()) {
            // 玩家点击了结果槽，名称已在 onClick 中读取
            name = session.getName();
        } else {
            // 玩家直接关闭（ESC），尝试读取
            name = getRenameText(event.getView());
            if (name == null || name.isEmpty()) {
                name = extractName(event.getInventory().getItem(0));
            }
        }

        final String finalName = (name != null) ? name : "";

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            // 清除光标上的物品（防止玩家拿到重命名的纸）
            if (player.getItemOnCursor() != null && player.getItemOnCursor().getType() == Material.PAPER) {
                player.setItemOnCursor(null);
            }
            session.callback().accept(player, finalName);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 通过反射调用 Paper 的 AnvilView.getRenameText()
     */
    private static String getRenameText(@NotNull org.bukkit.inventory.InventoryView view) {
        if (getRenameTextMethod != null) {
            try {
                Object result = getRenameTextMethod.invoke(view);
                return result != null ? result.toString() : null;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String extractName(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "";
        return meta.hasDisplayName() ? meta.getDisplayName() : "";
    }

    private static class InputSession {
        private final @NotNull BiConsumer<Player, String> callback;
        private String name;
        private boolean submitted;

        InputSession(@NotNull BiConsumer<Player, String> callback) {
            this.callback = callback;
        }

        @NotNull BiConsumer<Player, String> callback() {
            return callback;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        boolean isSubmitted() {
            return submitted;
        }

        void setSubmitted(boolean submitted) {
            this.submitted = submitted;
        }
    }

    public static class InputHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        void setInventory(@NotNull Inventory inventory) {
            this.inventory = inventory;
        }
    }

}
