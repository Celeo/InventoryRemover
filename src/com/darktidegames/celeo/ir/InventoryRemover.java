package com.darktidegames.celeo.ir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryRemover extends JavaPlugin implements Listener
{

	private List<Integer> bannedItems = new ArrayList<Integer>();

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
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info("Enabled");
	}

	private void load()
	{
		reloadConfig();
		bannedItems = getConfig().getIntegerList("bannedItems");
		if (bannedItems == null)
			bannedItems = new ArrayList<Integer>();
		getLogger().info("Settings loaded from the configuration file");
	}

	@Override
	public void onDisable()
	{
		getLogger().info("Disabled");
	}

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

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		checkInventory(player.getInventory());
	}

	@EventHandler
	public void onPlayerOpenInventory(InventoryOpenEvent event)
	{
		checkInventory(event.getInventory());
	}

	private void checkInventory(Inventory inventory)
	{
		ItemStack[] ret = inventory.getContents();
		for (ItemStack i : ret)
		{
			if (i == null)
				continue;
			if (bannedItems.contains(Integer.valueOf(i.getTypeId())))
				i.setTypeId(0);
		}
		inventory.setContents(ret);
	}
}