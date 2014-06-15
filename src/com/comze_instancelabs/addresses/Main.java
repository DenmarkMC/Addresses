package com.comze_instancelabs.addresses;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	Economy econ = null;

	JavaPlugin plugin = null;

	public void onEnable() {
		plugin = this;
		setupEconomy();
		getConfig().addDefault("mysql.pw", "password");
		getConfig().options().copyDefaults(true);
		this.saveConfig();
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("address")) {
			if (!(sender instanceof Player)) {
				return true;
			}
			Player p = (Player) sender;
			if (args.length > 2) {
				String address = args[0];
				String number = args[1];
				String postcode = args[2];

				if (mysqlUsedAddress(p.getName(), address, number, postcode)) {
					tryTP(p, address, number, postcode);
					p.sendMessage(ChatColor.GREEN + "Teleported to " + address + " " + number + ".");
					return true;
				}

				int currentpoints = (int) econ.getBalance(p.getName());
				if (currentpoints > 0) {
					econ.withdrawPlayer(p.getName(), 1.0D);
					tryTP(p, address, number, postcode);
					p.sendMessage(ChatColor.GREEN + "Teleported to " + address + " " + number + ". You can freely teleport to this address from now on!");
				} else {
					p.sendMessage(ChatColor.RED + "You have no address teleportation points left. Type /buy to get more.");
				}
				return true;
			} else if (args.length > 0 && args.length < 3) {
				p.sendMessage(ChatColor.RED + "Wrong syntax, must be: " + ChatColor.GOLD + "/atp <street> <number> <postcode>" + ChatColor.RED + ".");
				return true;
			} else {
				int points = (int) econ.getBalance(p.getName());
				p.sendMessage("Address teleportation points left: " + ChatColor.DARK_AQUA + "" + ChatColor.BOLD + Integer.toString((int)econ.getBalance(p.getName())));
				if (points < 1) {
					p.sendMessage("No Address Teleportation Points left. Type " + ChatColor.AQUA + "/buy " + ChatColor.WHITE + "to get more. §7Ingen point tilbage. Skriv §f/buy §7for at koebe flere.");
				}
				return true;
			}
		}
		return false;
	}

	public void tryTP(Player p, String address, String number, String postcode) {
		try {
			String req = "http://dawa.aws.dk/adresser?vejnavn=" + address + "&husnr=" + number + "&postnr=" + postcode + "&srid=25832";
			if (p.isOp()) {
				p.sendMessage(req);
			}
			getLL(p, req);
			mysqlUpdateAddressUses(p.getName(), address, number, postcode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getLL(Player p, String url) throws Exception {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");

		con.setRequestProperty("User-Agent", "Mozilla");

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// print result
		String r = response.toString();
		String x = "";
		String y = "";

		int cindex = r.indexOf("\"koordinater\"");
		int commaindex = r.indexOf(",", cindex);
		int endstuffsindex = r.indexOf("]", commaindex);
		x = r.substring(cindex + 24, commaindex);
		y = r.substring(commaindex + 9, (endstuffsindex - 6));

		System.out.println(x);
		System.out.println(y);

		int tpx = (int) Double.parseDouble(x) - 600000;
		int tpz = 6200000 - (int) Double.parseDouble(y);

		int tpy = 0;

		for (int i = 0; i < 100; i++) {
			if (p.getWorld().getBlockAt(new Location(p.getWorld(), tpx, i, tpz)).getType() == Material.AIR) {
				tpy = i;
				break;
			}
		}

		tpy++;

		p.teleport(new Location(p.getWorld(), Double.parseDouble(x) - 600000, tpy, 6200000 - Double.parseDouble(y)));
	}

	public void mysqlUpdateAddressUses(String p_, String address, String number, String postcode) {
		MySQL MySQL = new MySQL("localhost", "3306", "addresses", "root", getConfig().getString("mysql.pw"));
		Connection c = null;
		c = MySQL.open();

		try {
			ResultSet res3 = c.createStatement().executeQuery("SELECT * FROM address WHERE player='" + p_ + "' AND street='" + address + "' AND number='" + number + "' AND postcode='" + postcode + "'");
			if (!res3.isBeforeFirst()) {
				// there's no such user
				c.createStatement().executeUpdate("INSERT INTO address VALUES('0', '" + address + "', '" + p_ + "', '" + number + "', '" + postcode + "')");
				return;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean mysqlUsedAddress(String p, String address, String number, String postcode) {
		MySQL MySQL = new MySQL("localhost", "3306", "addresses", "root", getConfig().getString("mysql.pw"));
		Connection c = null;
		c = MySQL.open();

		try {
			ResultSet res3 = c.createStatement().executeQuery("SELECT * FROM address WHERE player='" + p + "' AND street='" + address + "' AND number='" + number + "' AND postcode='" + postcode + "'");
			if (!res3.isBeforeFirst()) {
				return false;
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	


}
