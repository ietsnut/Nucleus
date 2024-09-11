package com.ietsnut.nucleus;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.ReloadCommand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.CachedServerIcon;

import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class Nucleus extends JavaPlugin implements Listener {

    private static final String PREFIX = ChatColor.DARK_RED + "§ Nucleus: " + ChatColor.WHITE;

    private static final Color COLOR = new Color(170, 0, 0);

    private static Scoreboard board;
    private static Objective deaths;

    private static CachedServerIcon icon;
    private static BukkitTask task;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        ScoreboardManager manager = getServer().getScoreboardManager();
        assert manager != null;
        board = manager.getMainScoreboard();
        deaths = board.getObjective("Deaths");
        if (deaths == null) {
            deaths = board.registerNewObjective("Deaths", Criteria.DEATH_COUNT, ChatColor.DARK_RED + "☠", RenderType.INTEGER);
        }
        deaths.setDisplayName(ChatColor.DARK_RED + "☠");
        deaths.setRenderType(RenderType.INTEGER);
        for (Player player : getServer().getOnlinePlayers()) {
            player.setScoreboard(board);
            player.setDisplayName(skulls(player) + player.getName());
        }
        getServer().setDefaultGameMode(GameMode.SURVIVAL);
        getServer().setSpawnRadius(0);
        for (World world : getServer().getWorlds()) {
            world.setDifficulty(Difficulty.HARD);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        }
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(COLOR);
        g.setFont(new Font("Liberation Sans", Font.PLAIN, 32));
        g.drawString("§", 24, 40);
        g.dispose();
        try {
            icon = getServer().loadServerIcon(image);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(skulls(player)));
                }
            }
        }.runTaskTimerAsynchronously(this, 20, 20);
        print("enabled", "version: " + getServer().getVersion(), "bukkit: " + getServer().getBukkitVersion(), "name: " + getServer().getName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = update(e.getPlayer());
        e.setJoinMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + ">>> " + ChatColor.WHITE + ChatColor.ITALIC + player.getName());
        if (deaths.getScore(player.getName()).getScore() >= 3) {
            player.setGameMode(GameMode.SPECTATOR);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(ChatColor.RED + "" + ChatColor.ITALIC + "<<< " + ChatColor.WHITE + ChatColor.ITALIC + update(e.getPlayer()).getName());
    }

    @EventHandler
    public void onMessage(AsyncPlayerChatEvent e) {
        e.setFormat(update(e.getPlayer()).getDisplayName() + ": " + ChatColor.WHITE + e.getMessage());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        update(e.getEntity());
        e.setDeathMessage(ChatColor.WHITE + "" + ChatColor.ITALIC + e.getDeathMessage() + ChatColor.DARK_RED + ChatColor.ITALIC + " -☠");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = update(e.getPlayer());
        new BukkitRunnable(){
            @Override
            public void run() {
                if (deaths.getScore(player.getName()).getScore() >= 3) {
                    player.setGameMode(GameMode.SPECTATOR);
                    for (Player player1 : getServer().getOnlinePlayers()) {
                        player1.sendTitle(ChatColor.DARK_RED + "⚔ " + player.getName() + " ⚔", ChatColor.DARK_RED + "is game over", 20, 140, 20);
                        player1.playSound(player1, Sound.ITEM_GOAT_HORN_SOUND_5, 1.0F, 1.0F);
                        player1.playSound(player1, Sound.ITEM_GOAT_HORN_SOUND_6, 1.0F, 1.0F);
                        player1.playSound(player1, Sound.ITEM_GOAT_HORN_SOUND_3, 1.0F, 1.0F);
                    }
                } else {
                    player.setGameMode(GameMode.SURVIVAL);
                    for (Player player1 : getServer().getOnlinePlayers()) {
                        player1.sendTitle(ChatColor.DARK_RED + player.getName(), skulls(player), 20, 140, 20);
                        player1.playSound(player1, Sound.ITEM_GOAT_HORN_SOUND_7, 1.0F, 1.0F);
                    }
                }
            }
        }.runTaskLater(this, 20);
    }

    @Override
    public void onDisable() {
        task.cancel();
        print("disabled", "made by ietsnut");
    }

    @EventHandler
    public void onDrop(ItemSpawnEvent e) {
        Item drop = e.getEntity();
        ItemMeta meta = drop.getItemStack().getItemMeta();
        assert meta != null;
        if (meta.hasDisplayName()) {
            drop.setCustomName(meta.getDisplayName());
        } else {
            drop.setCustomName(Arrays.stream(drop.getItemStack().getType().name().replace("_", " ").split(" ")).map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase()).collect(Collectors.joining(" ")));
        }
        drop.setCustomNameVisible(true);
    }

    @EventHandler
    public void onList(ServerListPingEvent e) {
        e.setMotd(ChatColor.DARK_RED + "Ｎｕｃｌｅｕｓ");
        e.setServerIcon(icon);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        if (!e.getPlayer().isOp()) {
            e.setCancelled(true);
        }
    }

    private static Player update(Player player) {
        player.setScoreboard(board);
        player.setDisplayName(skulls(player) + player.getName());
        player.setPlayerListName(skulls(player) + player.getName());
        return player;
    }

    private static String skulls(Player player) {
        int d = 3 - deaths.getScore(player.getName()).getScore();
        return ChatColor.DARK_RED +  "☠".repeat(Math.max(0, d)) + (d == 0 ? "" : " ") + ChatColor.WHITE;
    }

    private static void print(String... strings) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(PREFIX + strings[0]);
        for (int i = 1; i < strings.length; i++) {
            console.sendMessage(ChatColor.WHITE + "\t~ " + strings[i]);
        }
    }

}
