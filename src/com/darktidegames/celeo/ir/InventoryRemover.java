package com.darktidegames.celeo.ir;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * <b>InventoryRemover</b> for /u/craftcraft
 * 
 * @author Celeo
 */
public class InventoryRemover extends JavaPlugin implements Listener
{

	/** The list of items that are banned */
	private List<Integer> bannedInventoryItems = new ArrayList<Integer>();
	/** The list of items that are banned from the world */
	private List<Integer> bannedWorldItems = new ArrayList<Integer>();
	/** Separate Logger file for tracking end portal uses */
	private Logger endLogger = Logger.getLogger("Minecraft.InventoryRemover");

	@Override
	public void onLoad()
	{
		getDataFolder().mkdirs();
		if (!new File(getDataFolder(), "config.yml").exists())
			saveDefaultConfig();
	}

	@Override
	public void onEnable()
	{
		load();
		try
		{
			endLogger.setUseParentHandlers(false);
			File temp = new File(getDataFolder().getAbsolutePath() + "/end.log");
			if (!temp.exists())
				temp.createNewFile();
			FileHandler handler = new FileHandler(temp.getAbsolutePath(), true);
			handler.setFormatter(new Formatter()
			{
				@Override
				public String format(LogRecord logRecord)
				{
					return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss:S z").format(new Date(logRecord.getMillis()))
							+ " " + logRecord.getMessage() + "\n";
				}
			});
			endLogger.addHandler(handler);
			getLogger().info("End logger setup");
		}
		catch (Exception e)
		{
			endLogger.setUseParentHandlers(true);
			getLogger().warning("Could not properly setup end logger, so end teleports will be logged to the main server.log file");
		}
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info("Enabled");
	}

	/**
	 * Load the banned item list from the configuration file
	 */
	private void load()
	{
		reloadConfig();
		bannedInventoryItems = getConfig().getIntegerList("bannedItems.inventory");
		if (bannedInventoryItems == null)
			bannedInventoryItems = new ArrayList<Integer>();
		bannedWorldItems = getConfig().getIntegerList("bannedItems.world");
		if (bannedWorldItems == null)
			bannedWorldItems = new ArrayList<Integer>();
		getLogger().info("Settings loaded from the configuration file");
		getLogger().info("Inventory items: " + bannedInventoryItems.toString());
		getLogger().info("World items: " + bannedWorldItems.toString());
	}

	@Override
	public void onDisable()
	{
		getLogger().info("Disabled");
	}

	/**
	 * Allow players to refresh the banned item list while the server is running
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			if (player.isOp() || player.hasPermission("ir.reload"))
			{
				load();
				player.sendMessage("§aSettings loaded from the configuration file");
			}
			else
				player.sendMessage("§cYou cannot use that command");
			return true;
		}
		load();
		return true;
	}

	/**
	 * Check player inventories when they join the server
	 * 
	 * @param event
	 *            PlayerJoinEvent
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		if (!player.isOp())
			checkInventory(player.getInventory());
	}

	/**
	 * Check inventories of chests, furnaces, and droppers
	 * 
	 * @param event
	 *            InventoryOpenEvent
	 */
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event)
	{
		checkInventory(event.getInventory());
	}

	/**
	 * Check placed banned blocks
	 * 
	 * @param event
	 *            PlayerInteractEvent
	 */
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if (player.isOp())
			return;
		if (event.getClickedBlock() == null)
			return;
		Block block = event.getClickedBlock();
		if (bannedWorldItems.contains(Integer.valueOf(block.getTypeId())))
			block.setTypeId(0);
	}

	/**
	 * Check breaking item frames with banned items inside
	 * 
	 * @param event
	 *            HangingBreakByEntityEvent
	 */
	@EventHandler
	public void onHangingItemChange(HangingBreakByEntityEvent event)
	{
		if (event.getEntity() instanceof ItemFrame)
		{
			ItemFrame f = (ItemFrame) event.getEntity();
			if (bannedWorldItems.contains(Integer.valueOf(f.getItem().getTypeId())))
				f.setItem(new ItemStack(0));
		}
	}

	/**
	 * Remove all banned items from the inventory
	 * 
	 * @param inventory
	 *            Inventory
	 */
	private void checkInventory(Inventory inventory)
	{
		ItemStack[] ret = inventory.getContents();
		for (ItemStack i : ret)
		{
			if (i == null)
				continue;
			if (bannedInventoryItems.contains(Integer.valueOf(i.getTypeId())))
				i.setTypeId(0);
		}
		inventory.setContents(ret);
	}

	/**
	 * Track players traveling to any end worlds
	 * 
	 * @param event
	 *            PlayerPortalEvent
	 */
	@EventHandler
	public void onPlayerPortal(PlayerPortalEvent event)
	{
		Player player = event.getPlayer();
		if (event.getFrom().getWorld().getEnvironment().equals(Environment.THE_END))
			return;
		if (!event.getTo().getWorld().getEnvironment().equals(Environment.THE_END))
			return;
		Location from = event.getFrom();
		endLogger.info(String.format("2 %s traveled to the end from world %s at %d, %d, %d", player.getName(), from.getWorld().getName(), from.getBlockX(), from.getBlockY(), from.getBlockZ()));
	}

	/**
	 * Disable pigs from spawning from spawners
	 * 
	 * @param event
	 *            CreatureSpawnEvent
	 */
	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event)
	{
		LivingEntity entity = event.getEntity();
		if (entity instanceof Pig)
			if (event.getSpawnReason().equals(SpawnReason.SPAWNER))
				event.setCancelled(true);
	}

}