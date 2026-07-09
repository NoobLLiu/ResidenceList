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
 * 由于编译环境为 Spigot API，运行环境为 Paper，通过反射调用 Paper 的 AnvilView.getRenameText()。
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

        // 取消所有点击
        event.setCancelled(true);

        // 只处理结果槽（slot 2）
        if (event.getRawSlot() != 2) return;

        // 通过反射获取 Paper 的 AnvilView.getRenameText()
        String name = getRenameText(event.getView());

        // 如果反射失败，回退到读取结果物品名称
        if (name == null || name.isEmpty()) {
            name = extractName(topInventory.getItem(2));
        }

        InputSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        final String finalName = (name != null) ? name : "";

        // 延迟关闭和回调，避免事件嵌套
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            player.closeInventory();
            session.callback().accept(player, finalName);
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof InputHolder)) return;

        // 延迟1tick检查，避免与点击提交冲突
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            InputSession session = sessions.remove(player.getUniqueId());
            if (session == null) return; // 已被 onClick 处理
            session.callback().accept(player, null);
        }, 1L);
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

    private record InputSession(@NotNull BiConsumer<Player, String> callback) {
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
