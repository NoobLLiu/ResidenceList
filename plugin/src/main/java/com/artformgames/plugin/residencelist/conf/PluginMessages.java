package com.artformgames.plugin.residencelist.conf;

import cc.carm.lib.configuration.Configuration;
import cc.carm.lib.configuration.annotation.ConfigPath;
import cc.carm.lib.easyplugin.utils.ColorParser;
import cc.carm.lib.mineconfiguration.bukkit.value.ConfiguredMessage;
import cc.carm.lib.mineconfiguration.bukkit.value.ConfiguredSound;
import de.themoep.minedown.MineDown;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

@ConfigPath(root = true)
public interface PluginMessages extends Configuration {

    static @NotNull ConfiguredMessage.Builder<BaseComponent[]> value() {
        return ConfiguredMessage.create(getParser())
                .dispatcher((sender, message) -> {
                    for (BaseComponent[] baseComponents : message) {
                        sender.spigot().sendMessage(baseComponents);
                    }
                });
    }

    static @NotNull BiFunction<CommandSender, String, BaseComponent[]> getParser() {
        return PluginMessages::parse;
    }

    static @NotNull BaseComponent[] parse(@NotNull CommandSender sender, @NotNull String message) {
        if (sender instanceof Player player) message = PlaceholderAPI.setPlaceholders(player, message);
        return MineDown.parse(ColorParser.parse(message));
    }

    ConfiguredMessage<BaseComponent[]> LOAD_FAILED = value()
            .defaults("&c&l抱歉! &f由于你的领地数据加载失败，请尝试重新加入或联系管理员!")
            .build();

    interface COMMAND extends Configuration {

        ConfiguredMessage<BaseComponent[]> USER = value()
                .defaults(
                        "&e&lResidenceList &f指令提示 &7(/reslist)",
                        "&8#&f open &e[玩家]",
                        "&8-&7 打开主页面",
                        "&8#&f info &e<领地名称>",
                        "&8-&7 查看该领地的详细信息",
                        "&8#&f edit &e<领地名称>",
                        "&8-&7 打开领地信息编辑页面"
                ).build();

        ConfiguredMessage<BaseComponent[]> ADMIN = value()
                .defaults(
                        "&e&lResidenceList &f管理员指令提示 &7(/reslistadmin)",
                        "&8#&f open &e[玩家]",
                        "&8-&7 以管理员身份打开主界面",
                        "&8#&f edit &e<领地名称>",
                        "&8-&7 以管理员身份打开领地信息编辑页面",
                        "&8#&f reload",
                        "&8-&7 重载配置文件"
                ).build();

        ConfiguredMessage<BaseComponent[]> NO_PERMISSION = value()
                .defaults("&c&l抱歉! &f你没有权限这样做!")
                .build();

        ConfiguredMessage<BaseComponent[]> ONLY_PLAYER = value()
                .defaults("&c&l抱歉! &f该指令只能由玩家执行!")
                .build();


        ConfiguredMessage<BaseComponent[]> NOT_EXISTS = value()
                .defaults("&c&l抱歉! &f没有名称为 &e#%(residence) &f的领地!")
                .params("residence")
                .build();

        ConfiguredMessage<BaseComponent[]> UNKNOWN_PLAYER = value()
                .defaults("&c&l抱歉! &f没有名称为 &e#%(name) &f的玩家!")
                .params("name")
                .build();


        ConfiguredMessage<BaseComponent[]> NOT_OWNER = value()
                .defaults("&c&l抱歉! &f你不是领地 &e#%(residence) &f的主人!")
                .params("residence", "residence_nickname").build();


    }

    interface RELOAD extends Configuration {

        ConfiguredMessage<BaseComponent[]> START = value()
                .defaults("&f正在重新加载配置文件...")
                .build();

        ConfiguredMessage<BaseComponent[]> SUCCESS = value()
                .defaults("&a&l成功! &fResidenceList配置文件已重新加载, 耗时 &a%(time)&f 毫秒")
                .params("time")
                .build();

        ConfiguredMessage<BaseComponent[]> FAILED = value()
                .defaults("&c&l失败! &fResidenceList配置文件重载失败，请检查控制台")
                .build();

    }


    interface PIN extends Configuration {
        ConfiguredSound SOUND = ConfiguredSound.of("BLOCK_ANVIL_PLACE");
        ConfiguredMessage<BaseComponent[]> MESSAGE = value()
                .defaults("&f你成功置顶了领地 &e%(residence)&f!")
                .params("residence")
                .build();
    }


    interface UNPIN extends Configuration {
        ConfiguredSound SOUND = ConfiguredSound.of(Sound.BLOCK_ANVIL_BREAK, 0.5F);
        ConfiguredMessage<BaseComponent[]> MESSAGE = value()
                .defaults("&f你取消置顶了领地 &e%(residence)&f!")
                .params("residence")
                .build();
    }


    interface TELEPORT extends Configuration {
        ConfiguredSound SOUND = ConfiguredSound.of(Sound.ENTITY_ENDERMAN_TELEPORT, 0.5F);

        ConfiguredMessage<BaseComponent[]> NO_LOCATION = value()
                .defaults("&c&l抱歉! &f你暂时无法传送到领地 &e%(residence) &f!")
                .params("residence")
                .build();
    }


    interface COMMENT extends Configuration {
        ConfiguredSound ASK_SOUND = ConfiguredSound.of(Sound.ENTITY_CHICKEN_EGG, 0.5F);
        ConfiguredSound YES_SOUND = ConfiguredSound.of(Sound.ENTITY_VILLAGER_YES, 0.5F);
        ConfiguredSound NO_SOUND = ConfiguredSound.of(Sound.ENTITY_VILLAGER_NO, 0.5F);

        ConfiguredMessage<BaseComponent[]> NOTIFY = value()
                .defaults(
                        "&f你正在评价领地 &e%(residence)&f,请在聊天栏输入你的评价",
                        "&f你可以输入 '&e#cancel&f' 来取消评价"
                ).params("residence").build();

        ConfiguredMessage<BaseComponent[]> NOT_RATED = value()
                .defaults(
                        "&f你还没有评价领地 &e%(residence)&f, 请先评价!"
                ).params("residence").build();
        ConfiguredMessage<BaseComponent[]> REMOVED = value()
                .defaults(
                        "&f你成功删除了领地 &e%(residence)&f 的评价!"
                ).params("residence").build();

    }

    interface EDIT extends Configuration {
        ConfiguredSound EDIT_SOUND = ConfiguredSound.of(Sound.ENTITY_CHICKEN_EGG, 0.5F);
        ConfiguredSound SUCCESS_SOUND = ConfiguredSound.of(Sound.BLOCK_LEVER_CLICK, 0.5F);
        ConfiguredSound FAILED_SOUND = ConfiguredSound.of(Sound.ENTITY_VILLAGER_NO, 0.5F);

        ConfiguredMessage<BaseComponent[]> NAME = value()
                .defaults(
                        "&f你正在为领地 &e%(residence)&f设置别名, 请在聊天栏输入",
                        "&f别名长度 &e不能超过 16 个字符&f.",
                        "&f你可以输入 '&e#cancel&f' 来取消操作"
                ).params("residence").build();

        ConfiguredMessage<BaseComponent[]> DESCRIPTION = value()
                .defaults(
                        "&f你正在为领地 &e%(residence)&f编辑描述, 请在聊天栏输入",
                        "&f你可以使用 '&e\\\\n&f' 进行换行.",
                        "&f你也可以输入 '&e#cancel&f' 来取消操作"
                ).params("residence").build();

        ConfiguredMessage<BaseComponent[]> NAME_TOO_LONG = value()
                .defaults(
                        "&c&l抱歉! &f领地别名过长,",
                        "&f注意别名长度 &e不能超过 16 个字符&f."
                ).params("residence").build();

        ConfiguredMessage<BaseComponent[]> NAME_UPDATED = value()
                .defaults("&f成功为领地 &e%(residence)&f 更新了别名!")
                .params("residence")
                .build();

        ConfiguredMessage<BaseComponent[]> DESCRIPTION_UPDATED = value()
                .defaults("&f成功为领地 &e%(residence)&f 更新了描述!")
                .params("residence")
                .build();

        ConfiguredMessage<BaseComponent[]> ICON_UPDATED = value()
                .defaults("&f成功为领地 &e%(residence)&f 更新了图标!")
                .params("residence")
                .build();

        ConfiguredMessage<BaseComponent[]> ICON_BLOCKED = value()
                .defaults("&f你不能使用这个物品作为领地图标!")
                .build();
    }

    interface CREATE extends Configuration {
        ConfiguredSound ASK_SOUND = ConfiguredSound.of(Sound.ENTITY_CHICKEN_EGG, 0.5F);
        ConfiguredSound SUCCESS_SOUND = ConfiguredSound.of(Sound.BLOCK_LEVER_CLICK, 0.5F);
        ConfiguredSound FAILED_SOUND = ConfiguredSound.of(Sound.ENTITY_VILLAGER_NO, 0.5F);

        ConfiguredMessage<BaseComponent[]> NOTIFY = value()
                .defaults(
                        "&f你正在创建新的领地, 请在聊天栏输入领地名称.",
                        "&f请确保你已经用领地选取工具选好了区域.",
                        "&f你可以输入 '&e#cancel&f' 来取消操作."
                ).build();

        ConfiguredMessage<BaseComponent[]> NO_SELECTION = value()
                .defaults(
                        "&c&l抱歉! &f你需要先用领地选取工具选取区域!",
                        "&f请使用选取工具选择两个对角点后再创建领地."
                ).build();

        ConfiguredMessage<BaseComponent[]> SUCCESS = value()
                .defaults("&a&l成功! &f你成功创建了领地 &e%(residence)&f!")
                .params("residence")
                .build();
    }

    interface CREATE_GUI extends Configuration {

        ConfiguredMessage<String> TITLE = ConfiguredMessage.asString()
                .defaults("&a&l创建领地")
                .build();

        // 铁砧输入界面标题和占位符
        ConfiguredMessage<String> ANVIL_TITLE = ConfiguredMessage.asString()
                .defaults("&a&l创建领地 - 输入名称")
                .build();

        ConfiguredMessage<String> ANVIL_PLACEHOLDER = ConfiguredMessage.asString()
                .defaults("&7输入领地名称...")
                .build();

        interface ITEMS extends Configuration {

            ConfiguredMessage<String> STEP_1_NAME = ConfiguredMessage.asString()
                    .defaults("&e&l第一步 &7| &f选取圈地范围")
                    .build();
            ConfiguredMessage<String> STEP_1_LORE = ConfiguredMessage.asString()
                    .defaults("&7\n&7使用领地选取工具(%tool%)\n&7左键和右键分别选取两个对角点\n&7或使用 &a自动选取 &7自动选区")
                    .build();
            ConfiguredMessage<String> STEP_1_STATUS_ON = ConfiguredMessage.asString()
                    .defaults("&2&l[自动选取已开启]")
                    .build();
            ConfiguredMessage<String> STEP_1_STATUS_OFF = ConfiguredMessage.asString()
                    .defaults("&4&l[自动选取已关闭]")
                    .build();

            ConfiguredMessage<String> STEP_2_NAME = ConfiguredMessage.asString()
                    .defaults("&e&l第二步 &7| &f查看选取信息")
                    .build();
            ConfiguredMessage<String> STEP_2_EMPTY_NAME = ConfiguredMessage.asString()
                    .defaults("&e&l第二步 &7| &f查看选取信息 &8(无选区)")
                    .build();
            ConfiguredMessage<String> STEP_2_EMPTY_LORE = ConfiguredMessage.asString()
                    .defaults("&7\n&7鼠标悬停可查看当前选区信息\n&c请先选取区域后再查看信息\n&7\n&a▶ 点击 &8| &f刷新选区信息")
                    .build();
            ConfiguredMessage<String> STEP_2_REFRESH_TIP = ConfiguredMessage.asString()
                    .defaults("&a▶ 点击 &8| &f刷新选区信息")
                    .build();
            // 第二步选区详情（buildStep2Item 中动态显示的选区信息）
            ConfiguredMessage<String> STEP_2_DETAIL = ConfiguredMessage.asString()
                    .defaults("&7\n&f当前选区: &e%(size_x)&7×&e%(size_y)&7×&e%(size_z)\n&f总面积: &e%(total_size) &7方块\n&f世界: &e%(world)\n&f坐标: &7(%(pos_low)) → (%(pos_high))\n&7\n&f创建费用: %(cost)\n&f领地数量: %(count)&7/%(max)\n&7\n&a▶ 点击 &8| &f刷新选区信息")
                    .build();

            ConfiguredMessage<String> STEP_3_READY_NAME = ConfiguredMessage.asString()
                    .defaults("&e&l第三步 &7| &f输入名称并确认购买")
                    .build();
            ConfiguredMessage<String> STEP_3_READY_LORE = ConfiguredMessage.asString()
                    .defaults("&7\n&7选区已就绪!\n&7点击后在铁砧界面输入领地名称\n&7取出结果物品即可完成创建\n&7\n&c注意: 创建后将扣除相应费用\n&7\n&a▶ 点击 &8| &f开始创建")
                    .build();
            ConfiguredMessage<String> STEP_3_NOT_READY_NAME = ConfiguredMessage.asString()
                    .defaults("&e&l第三步 &7| &f输入名称并确认购买 &8(未选取)")
                    .build();
            ConfiguredMessage<String> STEP_3_NOT_READY_LORE = ConfiguredMessage.asString()
                    .defaults("&7\n&c请先完成第一步和第二步\n&c选取区域后再创建领地")
                    .build();

            ConfiguredMessage<String> BACK_NAME = ConfiguredMessage.asString()
                    .defaults("&7&l返回领地列表")
                    .build();
            ConfiguredMessage<String> BACK_LORE = ConfiguredMessage.asString()
                    .defaults("&7\n&a▶ 点击 &8| &f返回")
                    .build();
        }
    }

}
