package com.artformgames.plugin.residencelist.listener;

import com.artformgames.plugin.residencelist.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 使用 Bukkit 原生铁砧 GUI 实现文本输入。
 * 玩家在铁砧中输入文字后点击结果槽位即可提交。
 */
public class AnvilNameInput implements Listener {

    private static final Map<UUID, InputSession> sessions = new HashMap<>();

    public static void open(@NotNull Player player, @NotNull String title,
                            @NotNull String defaultText,
                            @NotNull BiConsumer<Player, String> callback) {
        InputHolder holder = new InputHolder();
        Inventory anvil = Bukkit.createInventory(holder, InventoryType.ANVIL, title);
        holder.setInventory(anvil);

        // 在输入槽放置一张纸作为可重命名物品
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
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof InputHolder)) return;

        // 取消所有点击，防止玩家拿走物品
        event.setCancelled(true);

        // 只有点击结果槽（slot 2）才提交
        if (event.getRawSlot() == 2) {
            ItemStack result = topInventory.getItem(2);
            String name = extractName(result);

            InputSession session = sessions.remove(player.getUniqueId());
            if (session == null) return;

            player.closeInventory();
            final String finalName = name;
            Main.getInstance().getScheduler().run(() -> session.callback().accept(player, finalName));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory topInventory = event.getInventory();
        if (!(topInventory.getHolder() instanceof InputHolder)) return;

        // 延迟检查，避免与点击提交冲突
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            InputSession session = sessions.get(player.getUniqueId());
            if (session == null) return;

            // 如果玩家关闭了铁砧但没有提交，清除会话
            sessions.remove(player.getUniqueId());
            Main.getInstance().getScheduler().run(() -> session.callback().accept(player, null));
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private static String extractName(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "";
        if (meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return "";
    }

    private record InputSession(@NotNull BiConsumer<Player, String> callback) {}

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
