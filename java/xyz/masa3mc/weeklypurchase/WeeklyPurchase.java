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
            //???????????????????????????
            URL fetchWebsite = null;
            try {
                fetchWebsite = new URL(config.getString("url"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            //??????????????????
            File file = new File(this.getDataFolder(), "weeklyPurchase.xlsx");
            try {
                FileUtils.copyURLToFile(fetchWebsite, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //xlsx???????????????????????????
            Workbook excel = null;
            try {
                excel = WorkbookFactory.create(new File(this.getDataFolder(), "weeklyPurchase.xlsx"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // ?????????????????????
            Sheet sheet = excel.getSheet("??????");
            //????????????????????????????????????ArrayList?????????
            ArrayList<Integer> index = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int getRandom = (int) (Math.random() * (sheet.getRow(0).getCell(4).getNumericCellValue() - 1)) + 1;
                index.add(getRandom);
            }
            //???????????????????????????????????????????????????????????????
            List<String> material = new ArrayList<>();
            List<String> materialName = new ArrayList<>();
            List<Integer> price = new ArrayList<>();
            for (int i : index) {
                material.add(sheet.getRow(i).getCell(3).getStringCellValue());
                materialName.add(sheet.getRow(i).getCell(1).getStringCellValue());
                price.add((int) sheet.getRow(i).getCell(2).getNumericCellValue());
            }
            //QuickShop???api????????????????????????????????????????????????????????????
            QuickShopAPI api = (QuickShopAPI) plugin;
            List<Location> location = new ArrayList<>(Arrays.asList(new Location(getServer().getWorld("lobby"), -421, 7, -1095),
                    new Location(getServer().getWorld("lobby"), -421, 5, -1095),
                    new Location(getServer().getWorld("lobby"), -419, 7, -1095),
                    new Location(getServer().getWorld("lobby"), -419, 5, -1095)));
            List<Location> frameLocation = new ArrayList<>(Arrays.asList(new Location(getServer().getWorld("lobby"), -421, 6, -1094, 90, 90),
                    new Location(getServer().getWorld("lobby"), -421, 4, -1094, 90, 90),
                    new Location(getServer().getWorld("lobby"), -419, 6, -1094, 90, 90),
                    new Location(getServer().getWorld("lobby"), -419, 4, -1094, 90, 90)));
            //???????????????
            for (Entity entity : Bukkit.getWorld("lobby").getNearbyEntities(new Location(getServer().getWorld("lobby"), -420, 5, -1095), 2, 2, 2))
                entity.remove();
            //???????????????
            Bukkit.broadcastMessage("??a????????????????????????????????????");
            for (int i = 0; i < 4; i++) {
                //??????????????????????????????
                api.getShopManager().getShop(location.get(i)).setItem(new ItemStack(Material.getMaterial(material.get(i))));
                api.getShopManager().getShop(location.get(i)).setPrice(price.get(i));
                //?????????????????????
                ItemFrame itemFrame = Bukkit.getWorld("lobby").spawn(frameLocation.get(i), ItemFrame.class);
                itemFrame.setFacingDirection(BlockFace.SOUTH);
                itemFrame.setItem(new ItemStack(Material.getMaterial(material.get(i))));
                //?????????????????????
                Bukkit.broadcastMessage("??a??l" + materialName.get(i) + " ??7| ??6" + price.get(i) + "??7???");
            }
            String webhookText = materialName.get(0) + ": " + price.get(0) + "???\\r" + materialName.get(1) + ": " + price.get(1) + "???\\r" + materialName.get(2) + ": " + price.get(2) + "???\\r" + materialName.get(3) + ": " + price.get(3) + "???";
            DiscordWebhook webhook = new DiscordWebhook(config.getString("webhook"));
            webhook.setContent("?????????????????????????????????");
            webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle("????????????").setDescription(webhookText).setColor(Color.GREEN));
            try {
                webhook.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
