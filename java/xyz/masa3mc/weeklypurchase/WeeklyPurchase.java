package xyz.masa3mc.weeklypurchase;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.maxgamer.quickshop.api.QuickShopAPI;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class WeeklyPurchase extends JavaPlugin {

    File configFile;
    FileConfiguration config;

    @Override
    public void onEnable() {
        getCommand("weeklypurchase").setExecutor(new WeeklyPurchaseCmd());
        createFiles();
        scheduler();
    }

    public void createFiles() {
        configFile = new File(this.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            this.saveResource("config.yml", false);
        }
        config = new YamlConfiguration();

        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    void scheduler() {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                Date now = Calendar.getInstance().getTime();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE HH:mm:ss");
                if (simpleDateFormat.format(now).equals(config.getString("time"))) {
                    setPurchase();
                }
            }
        };
        task.runTaskTimer(WeeklyPurchase.this, 0L, 20L);
    }

    void setPurchase() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("QuickShop");
        if (plugin != null) {
            //おとしてきますね♥
            URL fetchWebsite = null;
            try {
                fetchWebsite = new URL(config.getString("url"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            //保存します♥
            File file = new File(this.getDataFolder(), "weeklyPurchase.xlsx");
            try {
                FileUtils.copyURLToFile(fetchWebsite, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //xlsxとしてよみこみまつ
            Workbook excel = null;
            try {
                excel = WorkbookFactory.create(new File(this.getDataFolder(), "weeklyPurchase.xlsx"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // シート名を取得
            Sheet sheet = excel.getSheet("週替");
            //ランダムで出た４つの値をArrayListに代入
            ArrayList<Integer> index = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int getRandom = (int) (Math.random() * (sheet.getRow(0).getCell(4).getNumericCellValue() - 1)) + 1;
                index.add(getRandom);
            }
            //表から取得したアイテムと価格をリストに追加
            List<String> material = new ArrayList<>();
            List<String> materialName = new ArrayList<>();
            List<Integer> price = new ArrayList<>();
            for (int i : index) {
                material.add(sheet.getRow(i).getCell(3).getStringCellValue());
                materialName.add(sheet.getRow(i).getCell(1).getStringCellValue());
                price.add((int) sheet.getRow(i).getCell(2).getNumericCellValue());
            }
            //QuickShopのapiからショップを取得、アイテムと価格を設定
            QuickShopAPI api = (QuickShopAPI) plugin;
            List<Location> location = new ArrayList<>(Arrays.asList(new Location(getServer().getWorld("lobby"), -421, 7, -1095),
                    new Location(getServer().getWorld("lobby"), -421, 5, -1095),
                    new Location(getServer().getWorld("lobby"), -419, 7, -1095),
                    new Location(getServer().getWorld("lobby"), -419, 5, -1095)));
            List<Location> frameLocation = new ArrayList<>(Arrays.asList(new Location(getServer().getWorld("lobby"), -421, 6, -1094, 90, 90),
                    new Location(getServer().getWorld("lobby"), -421, 4, -1094, 90, 90),
                    new Location(getServer().getWorld("lobby"), -419, 6, -1094, 90, 90),
                    new Location(getServer().getWorld("lobby"), -419, 4, -1094, 90, 90)));
            //額縁の削除
            for (Entity entity : Bukkit.getWorld("lobby").getNearbyEntities(new Location(getServer().getWorld("lobby"), -420, 5, -1095), 2, 2, 2))
                entity.remove();
            //変更の通知
            Bukkit.broadcastMessage("§a週替買取を変更しました！");
            for (int i = 0; i < 4; i++) {
                //アイテムと価格の変更
                api.getShopManager().getShop(location.get(i)).setItem(new ItemStack(Material.getMaterial(material.get(i))));
                api.getShopManager().getShop(location.get(i)).setPrice(price.get(i));
                //額縁セットマン
                ItemFrame itemFrame = Bukkit.getWorld("lobby").spawn(frameLocation.get(i), ItemFrame.class);
                itemFrame.setFacingDirection(BlockFace.SOUTH);
                itemFrame.setItem(new ItemStack(Material.getMaterial(material.get(i))));
                //変更内容の通知
                Bukkit.broadcastMessage("§a§l" + materialName.get(i) + " §7| §6" + price.get(i) + "§7円");
            }
            String webhookText = materialName.get(0) + ": " + price.get(0) + "円\\r" + materialName.get(1) + ": " + price.get(1) + "円\\r" + materialName.get(2) + ": " + price.get(2) + "円\\r" + materialName.get(3) + ": " + price.get(3) + "円";
            DiscordWebhook webhook = new DiscordWebhook(config.getString("webhook"));
            webhook.setContent("今週の買取はこちらです");
            webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle("週替買取").setDescription(webhookText).setColor(Color.GREEN));
            try {
                webhook.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
