package com.artformgames.plugin.residencelist.listener;

import com.artformgames.plugin.residencelist.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 基于铁砧 GUI 的文本输入实现。
 * <p>
 * 直接使用 Paper 的 MenuType.ANVIL 创建真正的铁砧界面。
 * 参考: https://docs.papermc.io/paper/dev/menu-type-api/
 * 参考实现: cn.gmzc.mgteam.gui.AnvilInputGUI
 */
public class AnvilNameInput implements Listener {

    private static final Map<UUID, BiConsumer<Player, String>> callbacks = new HashMap<>();

    /**
     * 打开铁砧输入界面。
     *
     * @param player      目标玩家
     * @param title       铁砧界面的标题
     * @param placeholder 输入框的占位提示文本
     * @param callback    输入完成后的回调，参数为玩家和输入的文本；若玩家取消（ESC 关闭），文本为 null
     */
    public static void open(@NotNull Player player, @NotNull String title,
                            @NotNull String placeholder,
                            @NotNull BiConsumer<Player, String> callback) {
        callbacks.put(player.getUniqueId(), callback);

        ItemStack inputItem = ItemStack.of(Material.PAPER);
        ItemMeta meta = inputItem.getItemMeta();
        meta.displayName(Component.text(placeholder, NamedTextColor.GRAY));
        inputItem.setItemMeta(meta);

        var view = MenuType.ANVIL.builder()
                .title(Component.text(title))
                .build(player);
        view.getTopInventory().setItem(0, inputItem);
        view.open();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) return;
        if (!callbacks.containsKey(player.getUniqueId())) return;

        int slot = event.getRawSlot();
        if (slot >= 0 && slot <= 2) {
            if (slot != 2) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getSlot() != 2) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;

        BiConsumer<Player, String> callback = callbacks.remove(player.getUniqueId());
        if (callback == null) return;

        event.setCancelled(true);
        event.getView().getTopInventory().clear();

        final String text;
        if (result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
            text = result.getItemMeta().getDisplayName();
        } else {
            text = null;
        }
        Main.info("[AnvilNameInput] Read name from result: '" + text + "'");

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            player.closeInventory();
            callback.accept(player, text);
            Main.info("[AnvilNameInput] Callback executed with text: '" + text + "'");
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) return;

        BiConsumer<Player, String> callback = callbacks.remove(player.getUniqueId());
        if (callback != null) {
            event.getView().getTopInventory().clear();
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                callback.accept(player, null);
                Main.info("[AnvilNameInput] Input cancelled by player.");
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        callbacks.remove(event.getPlayer().getUniqueId());
    }
}
