package com.artformgames.plugin.residencelist.bedrock;

import com.artformgames.plugin.residencelist.api.residence.ResidenceData;
import com.artformgames.plugin.residencelist.conf.PluginConfig;
import com.artformgames.plugin.residencelist.conf.PluginMessages;
import com.artformgames.plugin.residencelist.utils.ResidenceUtils;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基岩版领地交易市场界面。
 * <p>
 * 提供转让领地、出售/购买领地、出租/租用领地功能，以及交易市场列表。
 */
public class BedrockMarketUI {

    private BedrockMarketUI() {
    }

    // ======================== Transfer ========================

    /**
     * 打开转让领地表单。
     *
     * @param player        目标玩家
     * @param residenceData 领地数据
     * @param ownerFilter   领地列表的筛选主人（用于返回）
     */
    public static void openTransfer(Player player, ResidenceData residenceData, String ownerFilter) {
        sendTransferMenu(player, residenceData, ownerFilter);
    }

    private static void sendTransferMenu(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-转让领地】");

        form.content("§f领地：" + name + "\n"
                + "§f当前主人：" + residenceData.getOwner() + "\n"
                + "§7转让后保留原有权限设置。");

        form.button("§a确认转让", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked == 0) {
                    sendTransferForm(player, residenceData, ownerFilter);
                } else {
                    BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendTransferForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();

        BedrockPlayerSelectUI.open(player, "领地系统-转让领地", "目标玩家名称",
                (p, name) -> {
                    if (name == null || name.isBlank()) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                        BedrockResidenceManageUI.open(p, residenceData, ownerFilter);
                        return;
                    }
                    boolean success = ResidenceUtils.transferResidence(p, residence, name.trim());
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(p);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(p);
                    }
                    BedrockResidenceManageUI.open(p, residenceData, ownerFilter);
                },
                () -> sendTransferMenu(player, residenceData, ownerFilter)
        );
    }

    // ======================== Sell / Buy ========================

    /**
     * 打开出售/购买领地表单。
     *
     * @param player        目标玩家
     * @param residenceData 领地数据
     * @param ownerFilter   领地列表的筛选主人（用于返回）
     */
    public static void openSell(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();

        if (!ResidenceUtils.isEconomyEnabled()) {
            sendEconomyDisabled(player, residenceData, ownerFilter);
            return;
        }

        boolean isOwner = residence.isOwner(player);

        if (isOwner && !ResidenceUtils.isForSale(residence)) {
            sendSellForm(player, residenceData, ownerFilter);
        } else if (isOwner) {
            sendOwnerSellingForm(player, residenceData, ownerFilter);
        } else if (ResidenceUtils.isForSale(residence)) {
            sendBuyForm(player, residenceData, ownerFilter);
        } else {
            BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
        }
    }

    private static void sendEconomyDisabled(Player player, ResidenceData residenceData, String ownerFilter) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-出售领地】")
                .content("§c经济系统未启用，无法出售领地。")
                .button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response ->
                BedrockFormUtil.runSync(() ->
                        BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendSellForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());

        CustomForm.Builder form = CustomForm.builder()
                .title("§e【领地系统-出售领地】");

        form.label("§f领地：" + name + "\n§7出售后领地权限设置保留不变。");
        form.input("输入出售价格（金币）", "价格...", "");
        form.stepSlider("操作", 0, "确认上架", "取消");

        form.validResultHandler(response -> {
            int action = response.asStepSlider(2);
            String priceStr = response.asInput(1);
            BedrockFormUtil.runSync(() -> {
                if (action == 0) {
                    if (priceStr == null || priceStr.isBlank()) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                        BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                        return;
                    }
                    int amount;
                    try {
                        amount = Integer.parseInt(priceStr.trim());
                    } catch (NumberFormatException e) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                        BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                        return;
                    }
                    boolean success = ResidenceUtils.sellResidence(player, residence, amount);
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    }
                    BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                } else {
                    BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendOwnerSellingForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());
        int price = ResidenceUtils.getSalePrice(residence);

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-出售领地】");

        form.content("§f领地：" + name + "\n"
                + "§f出售价格：" + price + " 金币\n"
                + "§f状态：正在出售中");

        form.button("§c取消出售", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked == 0) {
                    boolean success = ResidenceUtils.removeFromSale(player, residence);
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    }
                }
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendBuyForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());
        int price = ResidenceUtils.getSalePrice(residence);

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-购买领地】");

        form.content("§f领地：" + name + "\n"
                + "§f出售价格：" + price + " 金币");

        form.button("§a确认购买", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked == 0) {
                    boolean success = ResidenceUtils.buyResidence(player, residence);
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    }
                    BedrockResidenceListUI.openList(player, ownerFilter);
                } else {
                    BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    // ======================== Rent ========================

    /**
     * 打开出租/租用领地表单。
     *
     * @param player        目标玩家
     * @param residenceData 领地数据
     * @param ownerFilter   领地列表的筛选主人（用于返回）
     */
    public static void openRent(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();

        if (!ResidenceUtils.isRentSystemEnabled()) {
            sendRentDisabled(player, residenceData, ownerFilter);
            return;
        }

        boolean isOwner = residence.isOwner(player);
        boolean forRent = ResidenceUtils.isForRent(residence);
        boolean rented = ResidenceUtils.isRented(residence);

        if (isOwner && !forRent) {
            sendRentForm(player, residenceData, ownerFilter);
        } else if (isOwner && forRent && !rented) {
            sendOwnerRentingForm(player, residenceData, ownerFilter);
        } else if (isOwner) {
            sendOwnerRentedForm(player, residenceData, ownerFilter);
        } else if (forRent && !rented) {
            sendRentConfirmForm(player, residenceData, ownerFilter);
        } else {
            BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
        }
    }

    private static void sendRentDisabled(Player player, ResidenceData residenceData, String ownerFilter) {
        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-出租领地】")
                .content("§c租借系统未启用，无法出租领地。")
                .button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response ->
                BedrockFormUtil.runSync(() ->
                        BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendRentForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());

        CustomForm.Builder form = CustomForm.builder()
                .title("§e【领地系统-设置出租】");

        form.label("§f领地：" + name + "\n§7出租后领地权限设置保留不变。");
        form.input("输入每日租金（金币）", "租金...", "");
        form.input("输入租期天数", "天数...", "");
        form.toggle("允许续租", true);
        form.toggle("出租后保留在市场", true);
        form.toggle("允许自动支付", false);
        form.stepSlider("操作", 0, "确认出租", "取消");

        form.validResultHandler(response -> {
            int action = response.asStepSlider(6);
            String costStr = response.asInput(1);
            String daysStr = response.asInput(2);
            boolean allowRenewing = response.asToggle(3);
            boolean stayInMarket = response.asToggle(4);
            boolean allowAutoPay = response.asToggle(5);
            BedrockFormUtil.runSync(() -> {
                if (action == 0) {
                    if (costStr == null || costStr.isBlank() || daysStr == null || daysStr.isBlank()) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                        BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                        return;
                    }
                    int cost;
                    int days;
                    try {
                        cost = Integer.parseInt(costStr.trim());
                        days = Integer.parseInt(daysStr.trim());
                    } catch (NumberFormatException e) {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                        BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                        return;
                    }
                    boolean success = ResidenceUtils.setForRent(player, residence, cost, days,
                            allowRenewing, stayInMarket, allowAutoPay);
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    }
                    BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                } else {
                    BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendOwnerRentingForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());
        int cost = ResidenceUtils.getRentCost(residence);
        int days = ResidenceUtils.getRentDays(residence);

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-出租领地】");

        form.content("§f领地：" + name + "\n"
                + "§f租金：" + cost + "/天\n"
                + "§f租期：" + days + "天\n"
                + "§f状态：等待租客");

        form.button("§c取消出租", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked == 0) {
                    boolean success = ResidenceUtils.removeFromRentMarket(player, residence);
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    }
                }
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendOwnerRentedForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());
        String rentingPlayer = ResidenceUtils.getRentingPlayer(residence);
        if (rentingPlayer == null) rentingPlayer = "未知";
        int cost = ResidenceUtils.getRentCost(residence);

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-出租领地】");

        form.content("§f领地：" + name + "\n"
                + "§f租客：" + rentingPlayer + "\n"
                + "§f租金：" + cost + "/天");

        form.button("§c强制退租", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked == 0) {
                    boolean success = ResidenceUtils.unrentResidence(player, residence);
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    }
                }
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendRentConfirmForm(Player player, ResidenceData residenceData, String ownerFilter) {
        ClaimedResidence residence = residenceData.getResidence();
        String name = BedrockFormUtil.stripColor(residenceData.getDisplayName());
        int cost = ResidenceUtils.getRentCost(residence);
        int days = ResidenceUtils.getRentDays(residence);

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-租用领地】");

        form.content("§f领地：" + name + "\n"
                + "§f租金：" + cost + "/天\n"
                + "§f租期：" + days + "天");

        form.button("§a确认租用", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked == 0) {
                    boolean success = ResidenceUtils.rentResidence(player, residence, false);
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    }
                    BedrockResidenceListUI.openList(player, ownerFilter);
                } else {
                    BedrockResidenceManageUI.open(player, residenceData, ownerFilter);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceManageUI.open(player, residenceData, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    // ======================== Market Listing ========================

    /**
     * 打开领地交易市场列表。
     *
     * @param player      目标玩家
     * @param ownerFilter 领地列表的筛选主人（用于返回）
     */
    public static void openMarket(Player player, String ownerFilter) {
        Map<String, Integer> forSale = Residence.getInstance().getTransactionManager().getBuyableResidences();
        Set<ClaimedResidence> forRent = Residence.getInstance().getRentManager().getRentableResidences();

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-领地交易市场】");

        form.content("§f以下是正在出售或出租的领地：");

        List<MarketEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : forSale.entrySet()) {
            String resName = entry.getKey();
            int price = entry.getValue();
            ClaimedResidence res = Residence.getInstance().getResidenceManager().getByName(resName);
            if (res == null) continue;
            entries.add(new MarketEntry(res, true, price));
            form.button("§a[出售] " + resName + "\n§7价格：" + price + "金币",
                    FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }

        for (ClaimedResidence res : forRent) {
            if (ResidenceUtils.isRented(res)) continue;
            int cost = ResidenceUtils.getRentCost(res);
            int days = ResidenceUtils.getRentDays(res);
            entries.add(new MarketEntry(res, false, cost));
            form.button("§b[出租] " + res.getName() + "\n§7租金：" + cost + "/天 × " + days + "天",
                    FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        }

        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        final int backIndex = entries.size();

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked >= 0 && clicked < entries.size()) {
                    MarketEntry entry = entries.get(clicked);
                    if (entry.forSale) {
                        sendMarketBuyForm(player, entry.residence, ownerFilter);
                    } else {
                        sendMarketRentForm(player, entry.residence, ownerFilter);
                    }
                } else if (clicked == backIndex) {
                    BedrockResidenceListUI.open(player);
                }
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                BedrockResidenceListUI.open(player)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendMarketBuyForm(Player player, ClaimedResidence residence, String ownerFilter) {
        String name = residence.getName();
        String owner = residence.getOwner();
        int price = ResidenceUtils.getSalePrice(residence);

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-购买领地】");

        form.content("§f领地：" + name + "\n"
                + "§f主人：" + owner + "\n"
                + "§f出售价格：" + price + " 金币");

        form.button("§a确认购买", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked == 0) {
                    boolean success = ResidenceUtils.buyResidence(player, residence);
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    }
                }
                openMarket(player, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                openMarket(player, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static void sendMarketRentForm(Player player, ClaimedResidence residence, String ownerFilter) {
        String name = residence.getName();
        String owner = residence.getOwner();
        int cost = ResidenceUtils.getRentCost(residence);
        int days = ResidenceUtils.getRentDays(residence);

        SimpleForm.Builder form = SimpleForm.builder()
                .title("§e【领地系统-租用领地】");

        form.content("§f领地：" + name + "\n"
                + "§f主人：" + owner + "\n"
                + "§f租金：" + cost + "/天\n"
                + "§f租期：" + days + "天");

        form.button("§a确认租用", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);
        form.button("§0返回", FormImage.Type.PATH, BedrockFormUtil.BUTTON_ICON);

        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            BedrockFormUtil.runSync(() -> {
                PluginConfig.GUI.CLICK_SOUND.playTo(player);
                if (clicked == 0) {
                    boolean success = ResidenceUtils.rentResidence(player, residence, false);
                    if (success) {
                        PluginMessages.EDIT.SUCCESS_SOUND.playTo(player);
                    } else {
                        PluginMessages.EDIT.FAILED_SOUND.playTo(player);
                    }
                }
                openMarket(player, ownerFilter);
            });
        });

        form.closedResultHandler(() -> BedrockFormUtil.runSync(() ->
                openMarket(player, ownerFilter)));

        BedrockFormUtil.sendForm(player, form);
    }

    private static class MarketEntry {
        final ClaimedResidence residence;
        final boolean forSale;
        final int price;

        MarketEntry(ClaimedResidence residence, boolean forSale, int price) {
            this.residence = residence;
            this.forSale = forSale;
            this.price = price;
        }
    }
}
