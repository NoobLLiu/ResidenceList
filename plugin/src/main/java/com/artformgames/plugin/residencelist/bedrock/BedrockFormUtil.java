package com.artformgames.plugin.residencelist.bedrock;

import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * 基岩版表单通用工具类。
 * <p>
 * 负责检测基岩版玩家、安全发送 Cumulus 表单。
 * 当 Floodgate 未安装时，所有检测均返回 false，插件优雅降级至 Java 版 GUI。
 */
public final class BedrockFormUtil {

    private BedrockFormUtil() {
    }

    private static Boolean floodgateAvailable = null;

    /**
     * 检查 Floodgate API 是否可用（即服务器是否安装了 Floodgate）。
     */
    public static boolean isFloodgateAvailable() {
        if (floodgateAvailable == null) {
            try {
                Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgateAvailable = true;
            } catch (ClassNotFoundException e) {
                floodgateAvailable = false;
            }
        }
        return floodgateAvailable;
    }

    /**
     * 判断指定玩家是否为基岩版玩家。
     *
     * @param player 目标玩家
     * @return 如果是基岩版玩家返回 true；如果 Floodgate 未安装或玩家不是基岩版返回 false
     */
    public static boolean isBedrockPlayer(Player player) {
        if (!isFloodgateAvailable()) return false;
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 向基岩版玩家发送表单。
     * <p>
     * 注意：表单响应回调可能在 Netty 线程执行，如需调用 Bukkit API 请使用
     * {@link #runSync(Runnable)} 切回主线程。
     *
     * @param player     目标玩家
     * @param formBuilder 表单构建器
     * @return 是否成功发送
     */
    public static boolean sendForm(Player player, FormBuilder<?, ?, ?> formBuilder) {
        if (!isBedrockPlayer(player)) return false;
        try {
            return FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder);
        } catch (Exception e) {
            Main.severe("Failed to send bedrock form to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 向基岩版玩家发送已构建的表单。
     */
    public static boolean sendForm(Player player, Form form) {
        if (!isBedrockPlayer(player)) return false;
        try {
            return FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
        } catch (Exception e) {
            Main.severe("Failed to send bedrock form to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 在主线程上执行任务（表单回调通常在 Netty 线程）。
     */
    public static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(Main.getInstance(), runnable);
    }

    /**
     * 清理颜色代码，用于在表单中显示纯文本。
     * Bedrock 表单不支持 § 颜色代码，需要移除。
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        return ColorParser.clear(text);
    }
}
