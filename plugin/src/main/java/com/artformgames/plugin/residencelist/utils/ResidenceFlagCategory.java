package com.artformgames.plugin.residencelist.utils;

import com.bekvon.bukkit.residence.containers.Flags;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Residence 权限分类定义。
 * <p>
 * 使用 flag 名称字符串存储，在运行时通过 {@link Flags#getFlag(String)} 解析。
 * 这样当 Residence JAR 版本不同时，不存在的 flag 会被自动跳过，避免编译错误。
 */
public enum ResidenceFlagCategory {

    BUILD_DESTROY("建造与破坏",
            "build", "place", "destroy", "container"),

    INTERACT_USE("交互与使用",
            "use", "door", "button", "lever", "pressure",
            "diode", "note", "table", "enchant", "brew",
            "anvil", "beacon", "bed", "cake", "flowerpot",
            "egg", "honey", "honeycomb", "copper", "brush",
            "goathorn", "anchor", "commandblock", "command"),

    ITEMS_DROPS("物品与掉落",
            "itemdrop", "itempickup", "nodurability"),

    MOVEMENT_TELEPORT("移动与传送",
            "move", "tp", "enderpearl", "chorustp",
            "fly", "nofly", "elytra",
            "wspeed1", "wspeed2", "jump2", "jump3"),

    ENTITIES_MOBS("生物与实体",
            "mobkilling", "animalkilling", "vehicledestroy", "vehicleplacing",
            "riding", "leash", "shear", "dye", "animalfeeding",
            "nametag", "harvest", "trade", "hook",
            "animals", "monsters", "nomobs", "canimals", "cmonsters",
            "nanimals", "nmonsters", "sanimals", "smonsters",
            "creeper", "dragongrief", "witherspawn", "phantomspawn",
            "witherdamage", "witherdestruction", "mobexpdrop", "mobitemdrop",
            "boarding", "raid"),

    ENVIRONMENT_PHYSICS("环境与物理",
            "ignite",
            "flow", "waterflow", "lavaflow", "explode", "tnt",
            "piston", "pistonprotection", "decay", "grow", "spread",
            "skulk", "iceform", "icemelt", "dryup", "coraldryup",
            "copperoxidation", "fallinprotection", "flowinprotection",
            "snowtrail", "trample", "golemopenchest",
            "burn", "fireball", "firespread", "anvilbreak"),

    COMBAT_PROTECTION("战斗与保护",
            "friendlyfire",
            "pvp", "damage", "falldamage", "safezone",
            "shoot", "snowball", "hotfloor",
            "keepinv", "keepexp", "respawn", "healing", "feed"),

    VISUAL_EFFECTS("视觉效果",
            "day", "night", "rain", "sun", "glow",
            "title", "visualizer", "coords", "hidden"),

    ECONOMY_RESIDENCE("经济与领地",
            "admin", "bank", "subzone", "chat",
            "shop", "backup", "craft");

    private final @NotNull String displayName;
    private final @NotNull List<String> flagNames;

    ResidenceFlagCategory(@NotNull String displayName, @NotNull String... flagNames) {
        this.displayName = displayName;
        this.flagNames = Collections.unmodifiableList(Arrays.asList(flagNames));
    }

    public @NotNull String getDisplayName() {
        return displayName;
    }

    public @NotNull List<String> getFlagNames() {
        return flagNames;
    }

    /**
     * 获取本分类下实际可用的 flag 对象（自动跳过当前 Residence 版本中不存在的 flag）。
     */
    public @NotNull List<Flags> getFlags() {
        List<Flags> result = new ArrayList<>();
        for (String name : flagNames) {
            Flags flag = Flags.getFlag(name);
            if (flag != null) {
                result.add(flag);
            }
        }
        return result;
    }

    /**
     * 获取可用于全局设置（/res set）的 flag：FlagMode 为 Residence 或 Both。
     */
    public @NotNull List<Flags> getGlobalFlags() {
        List<Flags> result = new ArrayList<>();
        for (Flags flag : getFlags()) {
            if (flag.getFlagMode() != Flags.FlagMode.Player) {
                result.add(flag);
            }
        }
        return result;
    }

    /**
     * 获取可用于个人设置（/res pset）的 flag：FlagMode 为 Player 或 Both。
     */
    public @NotNull List<Flags> getPlayerFlags() {
        List<Flags> result = new ArrayList<>();
        for (Flags flag : getFlags()) {
            if (flag.getFlagMode() != Flags.FlagMode.Residence) {
                result.add(flag);
            }
        }
        return result;
    }

    /**
     * 获取所有分类。
     */
    public static @NotNull List<ResidenceFlagCategory> all() {
        return Collections.unmodifiableList(Arrays.asList(values()));
    }

}
