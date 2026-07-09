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
 * 关键：不取消结果槽(slot 2)的点击，让铁砧正常处理重命名。
 * 玩家取出结果物品后铁砧自动关闭，在 onClose 中读取名称并回调。
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

        // 允许点击结果槽(slot 2)，不取消——让铁砧正常处理重命名
        if (rawSlot == 2) {
            // 不取消事件，玩家取出结果后铁砧自动关闭，由 onClose 处理回调
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

        // 读取名称：优先用反射获取 Paper AnvilView.getRenameText()
        String name = getRenameText(event.getView());

        // 反射失败则从输入槽(slot 0)的物品名读取（铁砧会更新输入物品的显示名）
        if (name == null || name.isEmpty()) {
            name = extractName(event.getInventory().getItem(0));
        }

        final String finalName = (name != null) ? name : "";

        // 移除玩家可能拿到的结果物品（重命名的纸）
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            // 清除光标上的物品
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
