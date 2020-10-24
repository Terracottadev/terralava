package org.terradev.terralava;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Terralava extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new LavaDrinkingManager(), this);
    }
}
/**
 * The Class that technically handles everything within the plugin. It is responsible of keeping track of when and how much
 *  the player have slurped lava as well as killing the player when they have slurped too much lava.
 *  
 *  @since 0.0.1
 */
class LavaDrinkingManager implements Listener {

    /**
     * Keeps track when a player slurped lava for the last time
     * @since 0.0.1
     */
    private HashMap<UUID, Long> lastInteraction = new HashMap<UUID, Long>();

    /**
     * Keeps track of how much the player has slurped lava. Usually the player will be killed when the value is equal to 8.
     * @since 0.0.1
     */
    private HashMap<UUID, Byte> drinkingProgress = new HashMap<UUID, Byte>();

    /**
     * The Event handle that is the heart of the plugin. Processes player deaths as well as increments the drinkingProgress.
     * @param evt The event that needs to be processed
     * @since 0.0.1
     */
    @EventHandler
    public void onPlayerLavaDrink(@NotNull PlayerInteractEvent evt) {
        if (evt.getAction() == Action.RIGHT_CLICK_AIR && evt.getPlayer().getInventory().getItemInMainHand().getType() == Material.LAVA_BUCKET) {
            Player player = evt.getPlayer();
            UUID playerid = player.getUniqueId();
            Long old = lastInteraction.put(playerid, System.currentTimeMillis());
            if (old == null || (System.currentTimeMillis()-old) > 250) {
                // more than 5 ticks skipped, reset progress
                drinkingProgress.put(playerid, (byte) 0x00);
            }
            if (drinkingProgress.computeIfPresent(playerid, (id, dur) -> (byte) (dur + 1)) == 8) {
                player.getInventory().getItemInMainHand().setType(Material.BUCKET);
                player.getWorld().spawnParticle(Particle.FALLING_LAVA, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.5);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 10, 1);
                player.setHealth(0.0);
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 10, 1);
            }
        }
    }
    
    /**
     * The Event handle for when a player quits, which removes the player in internal Data structures.
     * This Event handle purely exists to perform GC as otherwise with time player instances may accumulate which creates a memory leak.
     * @param evt The Event that needs to be processed
     * @since 0.0.1
     */
    @EventHandler
    public void simpleGC(@NotNull PlayerQuitEvent evt) {
        lastInteraction.remove(evt.getPlayer().getUniqueId());
        drinkingProgress.remove(evt.getPlayer().getUniqueId());
    }
    
    /**
     * The event handle for when a player dies, used for rudimentary GC and to change the death message where appropriate
     * @param evt The event that needs to be processed
     * @since 0.0.1
     */
    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent evt) {
        if (drinkingProgress.remove(evt.getEntity().getUniqueId()) == 100) {
            lastInteraction.remove(evt.getEntity().getUniqueId()); // GC
            evt.setDeathMessage(evt.getEntity().getCustomName() + ChatColor.RESET + " decided to drink lava.");
        }
    }
}
