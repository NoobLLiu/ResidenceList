package com.artformgames.plugin.residencelist.command.admin;

import cc.carm.lib.easyplugin.command.SubCommand;
import com.artformgames.plugin.residencelist.Main;
import com.artformgames.plugin.residencelist.command.AdminCommands;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand extends SubCommand<AdminCommands> {

    public ReloadCommand(@NotNull AdminCommands parent, String identifier, String... aliases) {
        super(parent, identifier, aliases);
    }

    @Override
    public Void execute(JavaPlugin plugin, CommandSender sender, String[] args) throws Exception {

        PluginMessages.RELOAD.START.sendTo(sender);

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            try {
                long s1 = System.currentTimeMillis();
                Bukkit.getPluginManager().disablePlugin(Main.getInstance());
                Bukkit.getPluginManager().enablePlugin(Main.getInstance());
                long elapsed = System.currentTimeMillis() - s1;
                PluginMessages.RELOAD.SUCCESS.sendTo(sender, elapsed);
            } catch (Exception e) {
                PluginMessages.RELOAD.FAILED.sendTo(sender);
                e.printStackTrace();
            }
        });

        return null;
    }

}
