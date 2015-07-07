package eu.carrade.amaury.Camelia.listeners;

import eu.carrade.amaury.Camelia.*;
import eu.carrade.amaury.Camelia.drawing.drawTools.core.*;
import eu.carrade.amaury.Camelia.drawing.whiteboard.*;
import eu.carrade.amaury.Camelia.game.*;
import eu.carrade.amaury.Camelia.game.turns.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.*;

import java.util.*;
import java.util.concurrent.*;


public class DrawListener implements Listener {


	Map<UUID, BukkitTask> watchRightClickTasks = new ConcurrentHashMap<>();


	/**
	 * This event is called every 200ms (4 ticks) when a player is right-clicking continuously. (Experimental, the value
	 * is noticeably stable).
	 */
	@EventHandler
	public void onPlayerRightClicks(PlayerInteractEvent ev) {

		if (ev.getAction() != Action.RIGHT_CLICK_BLOCK && ev.getAction() != Action.RIGHT_CLICK_AIR && ev.getAction() != Action.PHYSICAL || ev.getItem() == null) {
			return;
		}

		if (!ev.getPlayer().isOp()) ev.setCancelled(true);


		final UUID id = ev.getPlayer().getUniqueId();

		if (watchRightClickTasks.containsKey(id)) {
			watchRightClickTasks.get(id).cancel();
		}

		Camelia.getInstance().getDrawingManager().setRightClicking(id, true);

		watchRightClickTasks.put(id, Bukkit.getScheduler().runTaskLaterAsynchronously(Camelia.getInstance(), new Runnable() {
			@Override
			public void run() {
				Camelia.getInstance().getDrawingManager().setRightClicking(id, false);
			}
		}, 4l));
	}

	/**
	 * Handles the tools used on click
	 */
	@EventHandler
	public void onPlayerInteracts(PlayerInteractEvent ev) {
		if (!ev.getPlayer().isOp()) ev.setCancelled(true);

		Drawer drawer = Camelia.getInstance().getGameManager().getDrawer(ev.getPlayer().getUniqueId());

		if (drawer == null || ev.getItem() == null) return; // Moderator maybe

		if (drawer.isDrawing()) {
			ev.setCancelled(true);

			DrawTool tool = drawer.getActiveTool();

			if (tool == null) return;

			WhiteboardLocation target = WhiteboardLocation.fromBukkitLocation(Camelia.getInstance().getWhiteboard().getTargetBlock(ev.getPlayer()));

			switch (ev.getAction()) {

				case LEFT_CLICK_BLOCK:
				case LEFT_CLICK_AIR:

					tool.onLeftClick(target, drawer);

					break;


				case RIGHT_CLICK_BLOCK:
				case RIGHT_CLICK_AIR:

					if (tool instanceof ClicDrawTool) {
						tool.onRightClick(target, drawer);
					}

					break;


				default:
					break;
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent ev) {
		if (!ev.getWhoClicked().isOp() && ev.getWhoClicked() instanceof Player) {
			ev.setCancelled(true);
			((Player) ev.getWhoClicked()).updateInventory();
		}
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent ev) {
		if (!ev.getWhoClicked().isOp() && ev.getWhoClicked() instanceof Player) {
			ev.setCancelled(true);
			((Player) ev.getWhoClicked()).updateInventory();
		}
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent ev) {
		Drawer drawer = Camelia.getInstance().getGameManager().getDrawer(ev.getPlayer().getUniqueId());
		if (drawer == null) return;

		Turn currentTurn = Camelia.getInstance().getDrawTurnsManager().getCurrentTurn();
		if (currentTurn == null || !currentTurn.isActive()) {
			return;
		}

		if (currentTurn.getDrawer() != null && drawer.equals(currentTurn.getDrawer())) {
			ev.setCancelled(true);
			drawer.getPlayer().sendMessage(ChatColor.RED + "Vous ne pouvez pas vous exprimer pendant que vous dessinez !");
			return;
		}

		// TODO Allow players to talk if they say something totally unrelated to the word (comments about the draw...)
		if (drawer.hasFoundCurrentWord()) {
			ev.setCancelled(true);
			drawer.getPlayer().sendMessage(ChatColor.RED + "Vous avez déjà trouvé, laissez les autres chercher !");
			return;
		}


		String word = ev.getMessage().trim();
		Word.FoundState found = currentTurn.checkIfFound(ev.getPlayer(), word);

		switch (found) {
			case NEAR:
				ev.setCancelled(true);
				ev.getPlayer().sendMessage(ChatColor.GREEN + "« " + word + " » n'est pas très loin...");
				break;

			case FOUND:
				ev.setCancelled(true);
				break;
		}
	}
}
