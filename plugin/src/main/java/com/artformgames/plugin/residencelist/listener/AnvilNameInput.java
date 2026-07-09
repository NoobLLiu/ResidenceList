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
 * 点击结果槽时直接读取名称、关闭铁砧并执行回调；
 * ESC 关闭则视为取消。
 */
public class AnvilNameInput implements Listener {

    private static final Map<UUID, InputSession> sessions = new HashMap<>();
    private static Method getRenameTextMethod = null;
    private static Method setRepairCostMethod = null;

    static {
        try {
            Class<?> anvilViewClass = Class.forName("org.bukkit.inventory.AnvilView");
            getRenameTextMethod = anvilViewClass.getMethod("getRenameText");
            setRepairCostMethod = anvilViewClass.getMethod("setRepairCost", int.class);
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

        // 通过反射设置修理费用为0，避免玩家因经验不足无法取出结果
        if (setRepairCostMethod != null) {
            try {
                setRepairCostMethod.invoke(player.getOpenInventory(), 0);
            } catch (Exception ignored) {
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof InputHolder)) return;

        int rawSlot = event.getRawSlot();

        // 点击结果槽(slot 2)：读取名称，关闭铁砧，执行回调
        if (rawSlot == 2) {
            InputSession session = sessions.remove(player.getUniqueId());
            if (session == null) {
                Main.severe("AnvilNameInput: session is null for player " + player.getName());
                return;
            }

            // 优先用反射读取 Paper AnvilView.getRenameText()
            String name = getRenameText(event.getView());
            Main.info("[AnvilNameInput] getRenameText() = '" + name + "'");

            // 回退1: 从事件当前点击物品(结果物品)读取
            if (name == null || name.isEmpty()) {
                name = extractName(event.getCurrentItem());
                Main.info("[AnvilNameInput] extractName(event.currentItem) = '" + name + "'");
            }

            // 回退2: 从铁砧库存 slot 2 读取
            if (name == null || name.isEmpty()) {
                name = extractName(topInventory.getItem(2));
                Main.info("[AnvilNameInput] extractName(slot2) = '" + name + "'");
            }

            final String finalName = name != null ? name : "";
            Main.info("[AnvilNameInput] finalName = '" + finalName + "', scheduling callback");

            // 取消事件，防止纸片进入玩家背包
            event.setCancelled(true);

            // 下一 tick 关闭铁砧并执行回调
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                try {
                    player.closeInventory();
                } catch (Exception e) {
                    Main.severe("[AnvilNameInput] Failed to close inventory: " + e.getMessage());
                }
                try {
                    session.callback().accept(player, finalName);
                    Main.info("[AnvilNameInput] callback executed successfully");
                } catch (Exception e) {
                    Main.severe("[AnvilNameInput] callback threw exception: " + e.getMessage());
                    e.printStackTrace();
                }
            });
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

        // session 已在 onClick 中移除（确认提交），此处仅处理 ESC 关闭（取消）
        InputSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        // ESC 关闭：不执行回调（视为取消）
        // 清除光标上的物品（防止玩家拿到重命名的纸）
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            if (player.getItemOnCursor() != null && player.getItemOnCursor().getType() == Material.PAPER) {
                player.setItemOnCursor(null);
            }
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

        InputSession(@NotNull BiConsumer<Player, String> callback) {
            this.callback = callback;
        }

        @NotNull BiConsumer<Player, String> callback() {
            return callback;
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
