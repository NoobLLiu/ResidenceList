package com.artformgames.plugin.residencelist.utils;

import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResidenceUtils {
    private static final UUID TEMP_UUID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    public static boolean isServerLand(ClaimedResidence residence) {
        return residence.getOwnerUUID() == null
                || TEMP_UUID.equals(residence.getOwnerUUID())
                || Residence.getInstance().getServerUUID().equals(residence.getOwnerUUID());
    }

    public static boolean hiddenDefault() {
        return !PluginConfig.SETTINGS.DEFAULT_STATUS.resolve();
    }

    public static boolean viewable(@NotNull ClaimedResidence residence, @NotNull Player viewer) {
        if (isServerLand(residence)) return true;
        if (!residence.getPermissions().has(Flags.hidden, hiddenDefault())) return true;
        return residence.isOwner(viewer);
    }

    public static void toggleAutoSelection(@NotNull Player player) {
        boolean currentlyEnabled = Residence.getInstance().getAutoSelectionManager()
                .getList().containsKey(player.getUniqueId());
        if (!currentlyEnabled) {
            Residence.getInstance().getSelectionManager().selectBySize(player, 0, 0, 0);
        }
        Residence.getInstance().getAutoSelectionManager().switchAutoSelection(player);
    }

    public static void expandSky(@NotNull Player player) {
        Residence.getInstance().getSelectionManager().sky(player, player.hasPermission("residence.admin"));
    }

    public static void expandBedrock(@NotNull Player player) {
        Residence.getInstance().getSelectionManager().bedrock(player, player.hasPermission("residence.admin"));
    }

    public static void expandVert(@NotNull Player player) {
        Residence.getInstance().getSelectionManager().vert(player, player.hasPermission("residence.admin"));
    }

    // ======================== Permission helpers ========================

    public static boolean isResAdmin(@NotNull Player player) {
        return player.hasPermission("residence.admin");
    }

    public static boolean canManage(@NotNull Player player, @NotNull ClaimedResidence res) {
        return res.isOwner(player) || isResAdmin(player);
    }

    public static FlagPermissions.FlagState toFlagState(@Nullable String value) {
        if (value == null) return FlagPermissions.FlagState.NEITHER;
        return FlagPermissions.stringToFlagState(value);
    }

    public static boolean setGlobalFlag(@NotNull Player player, @NotNull ClaimedResidence res,
                                        @NotNull String flag, @Nullable String state) {
        return res.getPermissions().setFlag(player, flag, toFlagState(state), isResAdmin(player), true);
    }

    public static boolean setPlayerFlag(@NotNull Player player, @NotNull ClaimedResidence res,
                                        @NotNull UUID targetUUID, @NotNull String flag,
                                        @Nullable String state) {
        String playerName = Bukkit.getOfflinePlayer(targetUUID).getName();
        if (playerName == null) return false;
        return res.getPermissions().setPlayerFlag(player, playerName, flag,
                state == null ? "remove" : state, isResAdmin(player), false);
    }

    public static boolean removePlayerAllFlags(@NotNull Player player, @NotNull ClaimedResidence res,
                                               @NotNull UUID targetUUID) {
        String playerName = Bukkit.getOfflinePlayer(targetUUID).getName();
        if (playerName == null) return false;
        return res.getPermissions().removeAllPlayerFlags(player, playerName, isResAdmin(player));
    }

    public static void resetPermissions(@NotNull ClaimedResidence res) {
        res.getPermissions().applyDefaultFlags();
    }

    public static boolean renameResidence(@NotNull Player player, @NotNull ClaimedResidence res,
                                          @NotNull String newName) {
        return Residence.getInstance().getResidenceManager()
                .renameResidence(player, res.getName(), newName, isResAdmin(player));
    }

    public static void mirrorPermissions(@NotNull Player player,
                                         @NotNull ClaimedResidence target, @NotNull ClaimedResidence source) {
        Residence.getInstance().getResidenceManager().mirrorPerms(player, source.getName(), target.getName(), isResAdmin(player));
    }

    public static void setEnterMessage(@NotNull ClaimedResidence res, @Nullable String message) {
        res.setEnterMessage(message);
    }

    public static void setLeaveMessage(@NotNull ClaimedResidence res, @Nullable String message) {
        res.setLeaveMessage(message);
    }

    public static @Nullable String getEnterMessage(@NotNull ClaimedResidence res) {
        return res.getEnterMessage();
    }

    public static @Nullable String getLeaveMessage(@NotNull ClaimedResidence res) {
        return res.getLeaveMessage();
    }

    // ======================== Transfer / Market / Rent ========================

    public static boolean transferResidence(@NotNull Player player, @NotNull ClaimedResidence res,
                                            @NotNull String targetName) {
        Residence.getInstance().getResidenceManager()
                .giveResidence(player, targetName, res, isResAdmin(player), true);
        return true;
    }

    public static boolean sellResidence(@NotNull Player player, @NotNull ClaimedResidence res, int amount) {
        Residence.getInstance().getTransactionManager()
                .putForSale(res, player, amount, isResAdmin(player));
        return true;
    }

    public static boolean buyResidence(@NotNull Player player, @NotNull ClaimedResidence res) {
        Residence.getInstance().getTransactionManager()
                .buyPlot(res, player, isResAdmin(player));
        return true;
    }

    public static boolean removeFromSale(@NotNull Player player, @NotNull ClaimedResidence res) {
        Residence.getInstance().getTransactionManager()
                .removeFromSale(player, res, isResAdmin(player));
        return true;
    }

    public static boolean isForSale(@NotNull ClaimedResidence res) {
        return Residence.getInstance().getTransactionManager().isForSale(res);
    }

    public static int getSalePrice(@NotNull ClaimedResidence res) {
        return Residence.getInstance().getTransactionManager().getSaleAmount(res);
    }

    public static boolean setForRent(@NotNull Player player, @NotNull ClaimedResidence res,
                                     int cost, int days, boolean allowRenewing,
                                     boolean stayInMarket, boolean allowAutoPay) {
        Residence.getInstance().getRentManager()
                .setForRent(player, res, cost, days, allowRenewing, stayInMarket, allowAutoPay, isResAdmin(player));
        return true;
    }

    public static boolean rentResidence(@NotNull Player player, @NotNull ClaimedResidence res, boolean autoPay) {
        Residence.getInstance().getRentManager()
                .rent(player, res, autoPay, isResAdmin(player));
        return true;
    }

    public static boolean unrentResidence(@NotNull Player player, @NotNull ClaimedResidence res) {
        Residence.getInstance().getRentManager()
                .unrent(player, res, isResAdmin(player));
        return true;
    }

    public static boolean removeFromRentMarket(@NotNull Player player, @NotNull ClaimedResidence res) {
        Residence.getInstance().getRentManager()
                .removeFromForRent(player, res, isResAdmin(player));
        return true;
    }

    public static boolean payRent(@NotNull Player player, @NotNull ClaimedResidence res) {
        Residence.getInstance().getRentManager()
                .payRent(player, res, isResAdmin(player));
        return true;
    }

    public static boolean isForRent(@NotNull ClaimedResidence res) {
        return Residence.getInstance().getRentManager().isForRent(res);
    }

    public static boolean isRented(@NotNull ClaimedResidence res) {
        return Residence.getInstance().getRentManager().isRented(res);
    }

    public static String getRentingPlayer(@NotNull ClaimedResidence res) {
        return Residence.getInstance().getRentManager().getRentingPlayer(res);
    }

    public static int getRentCost(@NotNull ClaimedResidence res) {
        return Residence.getInstance().getRentManager().getCostOfRent(res);
    }

    public static int getRentDays(@NotNull ClaimedResidence res) {
        return Residence.getInstance().getRentManager().getRentDays(res);
    }

    public static boolean isRentRepeatable(@NotNull ClaimedResidence res) {
        return Residence.getInstance().getRentManager().getRentableRepeatable(res);
    }

    public static boolean isEconomyEnabled() {
        return Residence.getInstance().getConfigManager().enableEconomy();
    }

    public static boolean isRentSystemEnabled() {
        return Residence.getInstance().getConfigManager().enabledRentSystem();
    }

    public static @NotNull List<Flags> getGlobalFlags() {
        List<Flags> list = new ArrayList<>();
        for (Flags f : Flags.values()) {
            if (f.getFlagMode() == Flags.FlagMode.Residence || f.getFlagMode() == Flags.FlagMode.Both) {
                list.add(f);
            }
        }
        return list;
    }

    public static @NotNull List<Flags> getPlayerFlags() {
        List<Flags> list = new ArrayList<>();
        for (Flags f : Flags.values()) {
            if (f.getFlagMode() == Flags.FlagMode.Player || f.getFlagMode() == Flags.FlagMode.Both) {
                list.add(f);
            }
        }
        return list;
    }

}
