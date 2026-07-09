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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 使用 Paper 的 MenuType.ANVIL 创建真正的铁砧 GUI 实现文本输入。
 * 通过反射调用 Paper API（MenuType / Component），因为项目编译基于 Spigot API。
 * 参考实现: cn.gmzc.unifiedmenu.AnvilInputGUI
 */
public class AnvilNameInput implements Listener {

    private static final Map<UUID, BiConsumer<Player, String>> callbacks = new HashMap<>();

    // Paper API 反射缓存
    private static Method menuTypeBuilderMethod;
    private static Method builderTitleMethod;
    private static Method builderBuildMethod;
    private static Method viewOpenMethod;
    private static Method componentTextMethod;
    private static Method metaDisplayNameMethod;
    private static boolean paperApiAvailable = false;

    static {
        try {
            // MenuType.ANVIL
            Class<?> menuTypeClass = Class.forName("org.bukkit.inventory.MenuType");
            Field anvilField = menuTypeClass.getField("ANVIL");
            Object anvilType = anvilField.get(null);

            // MenuType.builder()
            menuTypeBuilderMethod = menuTypeClass.getMethod("builder");

            // Component.text(String)
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            componentTextMethod = componentClass.getMethod("text", String.class);

            // builder.title(Component) — 在运行时获取 builder 的实际类型
            // 暂时不缓存，因为 builder 类型未知，在 open() 中动态获取

            paperApiAvailable = true;
            Main.info("[AnvilNameInput] Paper MenuType API loaded successfully.");
        } catch (Exception e) {
            Main.info("[AnvilNameInput] Paper MenuType API not available: " + e.getMessage());
        }
    }

    public static void open(@NotNull Player player, @NotNull String title,
                            @NotNull String defaultText,
                            @NotNull BiConsumer<Player, String> callback) {
        callbacks.put(player.getUniqueId(), callback);

        if (paperApiAvailable) {
            openPaperAnvil(player, title, defaultText);
        } else {
            // 回退到 Bukkit 方式（可能无法正确读取名称）
            openBukkitAnvil(player, title, defaultText);
        }
    }

    /**
     * 使用 Paper MenuType.ANVIL 创建真正的铁砧（推荐方式）。
     */
    private static void openPaperAnvil(@NotNull Player player, @NotNull String title, @NotNull String defaultText) {
        try {
            // MenuType.ANVIL
            Class<?> menuTypeClass = Class.forName("org.bukkit.inventory.MenuType");
            Field anvilField = menuTypeClass.getField("ANVIL");
            Object anvilType = anvilField.get(null);

            // MenuType.builder()
            Object builder = menuTypeClass.getMethod("builder").invoke(anvilType);

            // Component.text(title)
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Object titleComponent = componentClass.getMethod("text", String.class).invoke(null, title);

            // builder.title(Component)
            builder.getClass().getMethod("title", componentClass).invoke(builder, titleComponent);

            // builder.build(player) -> InventoryView
            Object view = builder.getClass().getMethod("build", Player.class).invoke(builder, player);

            // 准备输入物品
            ItemStack inputItem = new ItemStack(Material.PAPER);
            ItemMeta meta = inputItem.getItemMeta();
            if (meta != null) {
                // 优先用 Component 设置 display name（Paper 方式）
                try {
                    Object nameComponent = componentClass.getMethod("text", String.class).invoke(null, defaultText);
                    meta.getClass().getMethod("displayName", componentClass).invoke(meta, nameComponent);
                } catch (Exception e) {
                    // 回退到 legacy 方式
                    meta.setDisplayName(defaultText);
                }
                inputItem.setItemMeta(meta);
            }

            // view.getTopInventory().setItem(0, inputItem)
            Object topInventory = view.getClass().getMethod("getTopInventory").invoke(view);
            topInventory.getClass().getMethod("setItem", int.class, ItemStack.class).invoke(topInventory, 0, inputItem);

            // view.open()
            view.getClass().getMethod("open").invoke(view);
        } catch (Exception e) {
            Main.severe("[AnvilNameInput] Failed to open Paper anvil: " + e.getMessage());
            e.printStackTrace();
            // 回退
            openBukkitAnvil(player, title, defaultText);
        }
    }

    /**
     * 回退方式：使用 Bukkit createInventory（可能无法正确处理重命名）。
     */
    private static void openBukkitAnvil(@NotNull Player player, @NotNull String title, @NotNull String defaultText) {
        Main.info("[AnvilNameInput] Falling back to Bukkit anvil.");
        var holder = new Object(); // 简单 holder
        var anvil = Bukkit.createInventory(null, InventoryType.ANVIL, title);
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(defaultText);
            paper.setItemMeta(meta);
        }
        anvil.setItem(0, paper);
        player.openInventory(anvil);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) return;
        if (event.getSlot() != 2) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) {
            Main.info("[AnvilNameInput] result is null or air, ignoring click.");
            return;
        }

        BiConsumer<Player, String> callback = callbacks.remove(player.getUniqueId());
        if (callback == null) return;

        event.setCancelled(true);
        event.getView().getTopInventory().clear();

        // 从结果物品读取名称（legacy API，Spigot 可用）
        final String text;
        if (result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
            text = result.getItemMeta().getDisplayName();
        } else {
            text = null;
        }
        Main.info("[AnvilNameInput] read name from result item: '" + text + "'");

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            player.closeInventory();
            callback.accept(player, text);
            Main.info("[AnvilNameInput] callback executed with text: '" + text + "'");
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) return;

        BiConsumer<Player, String> callback = callbacks.remove(player.getUniqueId());
        if (callback != null) {
            // ESC 关闭：传 null 表示取消
            event.getView().getTopInventory().clear();
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                callback.accept(player, null);
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        callbacks.remove(event.getPlayer().getUniqueId());
    }
}
