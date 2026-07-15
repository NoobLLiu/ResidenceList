package com.artformgames.plugin.residencelist.bedrock;

import cc.carm.lib.easyplugin.utils.ColorParser;
import com.artformgames.plugin.residencelist.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * 基岩版表单通用工具类。
 * <p>
 * 负责检测基岩版玩家、安全发送 Cumulus 表单。
 * 当 Floodgate 未安装时，所有检测均返回 false，插件优雅降级至 Java 版 GUI。
 */
public final class BedrockFormUtil {

    /** 占位图标路径（基岩版内置的草方块纹理） */
    public static final String BUTTON_ICON = "textures/blocks/grass_side_carried";
    private static final String MENU_ICON_PREFIX = "textures/menu_1/";

    private BedrockFormUtil() {
    }

    public static String menuIcon(String name) {
        return MENU_ICON_PREFIX + name + ".png";
    }

    /**
     * 将 Bukkit Material 映射为基岩版方块纹理路径，用于表单按钮图标。
     * 不在映射表中的材料会回退到默认草方块图标。
     */
    public static String getBlockIconPath(org.bukkit.Material material) {
        if (material == null) return BUTTON_ICON;
        return switch (material) {
            case GRASS_BLOCK -> "textures/blocks/grass_side_carried";
            case STONE -> "textures/blocks/stone";
            case COBBLESTONE -> "textures/blocks/cobblestone";
            case MOSSY_COBBLESTONE -> "textures/blocks/cobblestone_mossy";
            case DIRT -> "textures/blocks/dirt";
            case SAND -> "textures/blocks/sand";
            case GRAVEL -> "textures/blocks/gravel";
            case OAK_PLANKS -> "textures/blocks/planks_oak";
            case SPRUCE_PLANKS -> "textures/blocks/planks_spruce";
            case BIRCH_PLANKS -> "textures/blocks/planks_birch";
            case JUNGLE_PLANKS -> "textures/blocks/planks_jungle";
            case ACACIA_PLANKS -> "textures/blocks/planks_acacia";
            case DARK_OAK_PLANKS -> "textures/blocks/planks_big_oak";
            case BRICKS -> "textures/blocks/brick";
            case STONE_BRICKS -> "textures/blocks/stonebrick";
            case BOOKSHELF -> "textures/blocks/bookshelf";
            case OBSIDIAN -> "textures/blocks/obsidian";
            case GLOWSTONE -> "textures/blocks/glowstone";
            case GOLD_BLOCK -> "textures/blocks/gold_block";
            case IRON_BLOCK -> "textures/blocks/iron_block";
            case DIAMOND_BLOCK -> "textures/blocks/diamond_block";
            case EMERALD_BLOCK -> "textures/blocks/emerald_block";
            case REDSTONE_BLOCK -> "textures/blocks/redstone_block";
            case LAPIS_BLOCK -> "textures/blocks/lapis_block";
            case QUARTZ_BLOCK -> "textures/blocks/quartz_block_top";
            case NETHERITE_BLOCK -> "textures/blocks/netherite_block";
            case CRAFTING_TABLE -> "textures/blocks/crafting_table_front";
            case FURNACE -> "textures/blocks/furnace_front_off";
            case CHEST -> "textures/blocks/chest_front";
            case ENDER_CHEST -> "textures/blocks/ender_chest_front";
            case BEACON -> "textures/blocks/beacon";
            case FLOWER_POT -> "textures/blocks/flower_pot";
            case LANTERN -> "textures/blocks/lantern";
            case TORCH -> "textures/blocks/torch_on";
            case REDSTONE_LAMP -> "textures/blocks/redstone_lamp_on";
            case SEA_LANTERN -> "textures/blocks/sea_lantern";
            case PUMPKIN -> "textures/blocks/pumpkin_face_on";
            case MELON -> "textures/blocks/melon_side";
            case HAY_BLOCK -> "textures/blocks/hay_block_side";
            case TNT -> "textures/blocks/tnt_side";
            case NETHERRACK -> "textures/blocks/netherrack";
            case SOUL_SAND -> "textures/blocks/soul_sand";
            case END_STONE -> "textures/blocks/end_stone";
            case WHITE_WOOL -> "textures/blocks/wool_colored_white";
            case ORANGE_WOOL -> "textures/blocks/wool_colored_orange";
            case MAGENTA_WOOL -> "textures/blocks/wool_colored_magenta";
            case LIGHT_BLUE_WOOL -> "textures/blocks/wool_colored_light_blue";
            case YELLOW_WOOL -> "textures/blocks/wool_colored_yellow";
            case LIME_WOOL -> "textures/blocks/wool_colored_lime";
            case PINK_WOOL -> "textures/blocks/wool_colored_pink";
            case GRAY_WOOL -> "textures/blocks/wool_colored_gray";
            case LIGHT_GRAY_WOOL -> "textures/blocks/wool_colored_silver";
            case CYAN_WOOL -> "textures/blocks/wool_colored_cyan";
            case PURPLE_WOOL -> "textures/blocks/wool_colored_purple";
            case BLUE_WOOL -> "textures/blocks/wool_colored_blue";
            case BROWN_WOOL -> "textures/blocks/wool_colored_brown";
            case GREEN_WOOL -> "textures/blocks/wool_colored_green";
            case RED_WOOL -> "textures/blocks/wool_colored_red";
            case BLACK_WOOL -> "textures/blocks/wool_colored_black";
            default -> BUTTON_ICON;
        };
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
