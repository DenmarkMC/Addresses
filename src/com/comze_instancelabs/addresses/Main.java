package com.comze_instancelabs.addresses;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class Main extends JavaPlugin implements PluginMessageListener, Listener {

	Economy econ = null;

	JavaPlugin plugin = null;

	public void onEnable() {
		plugin = this;
		setupEconomy();
		getConfig().addDefault("mysql.pw", "password");
		getConfig().options().copyDefaults(true);
		this.saveConfig();

		Bukkit.getPluginManager().registerEvents(this, this);

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

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
				String address = "";
				String number = "";
				String postcode = "";
				int c = 0;
				for(int i = 0; i < args.length; i++){
					if(!Character.isDigit(args[i].charAt(0))){
						address += args[i] + " ";
					}else{
						c = i;
						break;
					}
				}
				address = address.substring(0, address.length() - 1);
				
				if(c < args.length + 1){
					number = args[c];
					postcode = args[c + 1];
				}else{
					p.sendMessage(ChatColor.RED + "Wrong syntax, must be: " + ChatColor.GOLD + "/atp <street> <number> <postcode>" + ChatColor.RED + ".");
					return true;
				}
				

				if (mysqlUsedAddress(p.getName(), address, number, postcode)) {
					tryTP(p, address, number, postcode, true);
					p.sendMessage(ChatColor.GREEN + "Teleported to " + address + " " + number + ".");
					p.sendMessage("If this is not your wanted location please contact an admin. " + ChatColor.GRAY + "Kontakt en administrator hvis dette ikke er din oenskede lokation.");
					return true;
				}

				int currentpoints = (int) econ.getBalance(p.getName());
				if (currentpoints > 0) {
					econ.withdrawPlayer(p.getName(), 1.0D);
					tryTP(p, address, number, postcode, true);
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
				p.sendMessage("Address teleportation points left: " + ChatColor.DARK_AQUA + "" + ChatColor.BOLD + Integer.toString((int) econ.getBalance(p.getName())));
				if (points < 1) {
					p.sendMessage("No Address Teleportation Points left. Type " + ChatColor.AQUA + "/buy " + ChatColor.WHITE + "to get more. §7Ingen point tilbage. Skriv §f/buy §7for at koebe flere.");
				}
				return true;
			}
		}
		return false;
	}

	public void tryTP(Player p, String address, String number, String postcode, boolean needsServerCheck) {
		try {
			String req = "http://dawa.aws.dk/adresser?vejnavn=" + address + "&husnr=" + number + "&postnr=" + postcode + "&srid=25832";
			if (p.isOp()) {
				p.sendMessage(req);
			}
			getLL(p, req, needsServerCheck, address, number, postcode);
			mysqlUpdateAddressUses(p.getName(), address, number, postcode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets UTM coordinates from official denmark geo agency
	 * 
	 * @param p
	 * @param url
	 * @throws Exception
	 */
	public void getLL(Player p, String url, boolean needsServerCheck, String address, String number, String postcode) throws Exception {
		URL obj = new URL(url.replaceAll(" ", "%20"));
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");

		con.setRequestProperty("User-Agent", "Mozilla");

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		//System.out.println("Response Code : " + responseCode);

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

		if (p.getWorld().getBlockAt(tpx, 3, tpy).getType() == Material.AIR) {
			// most likely wrong server
			if (needsServerCheck) {
				getLogger().info("Wrong server, fixing it.");
				this.handleCorrectServer(p.getName(), address, number, postcode);
				return;
			}
			return;
		}

		tpy++;

		p.teleport(new Location(p.getWorld(), Double.parseDouble(x) - 600000, tpy, 6200000 - Double.parseDouble(y)));
	}

	/**
	 * Saves address for later free teleportation for the player
	 * 
	 * @param p_
	 * @param address
	 * @param number
	 * @param postcode
	 */
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

	/**
	 * Returns whether address is free to teleport to (as player paid for it) or
	 * not
	 * 
	 * @param p
	 * @param address
	 * @param number
	 * @param postcode
	 * @return
	 */
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

	/**
	 * Temporary saves address when player appears to be on wrong server
	 * 
	 * @param p_
	 * @param address
	 * @param number
	 * @param postcode
	 */
	public void mysqlUpdateAddressTemp(String p_, String address, String number, String postcode) {
		MySQL MySQL = new MySQL("localhost", "3306", "addresses_temp", "root", getConfig().getString("mysql.pw"));
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

	/**
	 * Will teleport the player if an address is found in the temp database
	 * 
	 * @param p_
	 */
	public void mysqlHandleTempAddress(String p_) {
		MySQL MySQL = new MySQL("localhost", "3306", "addresses_temp", "root", getConfig().getString("mysql.pw"));
		Connection c = null;
		c = MySQL.open();

		try {
			ResultSet res3 = c.createStatement().executeQuery("SELECT * FROM address WHERE player='" + p_ + "'");
			if (!res3.isBeforeFirst()) {
				return;
			}
			res3.next();
			String address = res3.getString("street");
			String number = res3.getString("number");
			String postcode = res3.getString("postcode");
			tryTP(Bukkit.getPlayer(p_), address, number, postcode, false);
			mysqlRemoveTempAddress(p_);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Removes a temp address from the database
	 * 
	 * @param p_
	 */
	public void mysqlRemoveTempAddress(String p_) {
		MySQL MySQL = new MySQL("localhost", "3306", "addresses_temp", "root", getConfig().getString("mysql.pw"));
		Connection c = null;
		c = MySQL.open();

		try {
			c.createStatement().executeUpdate("DELETE FROM address WHERE player='" + p_ + "'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Will save the address into mysql and connect the player to the correct
	 * server
	 * 
	 * @param player
	 * @param address
	 * @param number
	 * @param postcode
	 */
	public void handleCorrectServer(final String player, String address, String number, String postcode) {
		getLogger().info(serverName);
		if (serverName.equalsIgnoreCase("east")) {
			this.mysqlUpdateAddressTemp(player, address, number, postcode);
			Bukkit.getScheduler().runTaskLater(this, new Runnable() {
				public void run() {
					connectToServer(player, "west");
				}
			}, 30L); // 1.5 secs
		} else if (serverName.equalsIgnoreCase("west")) {
			this.mysqlUpdateAddressTemp(player, address, number, postcode);
			Bukkit.getScheduler().runTaskLater(this, new Runnable() {
				public void run() {
					connectToServer(player, "east");
				}
			}, 30L); // 1.5 secs
		}
	}

	// BUNGEE FUNCTIONS START

	public void connectToServer(String player, String server) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try {
			out.writeUTF("Connect");
			out.writeUTF(server);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Bukkit.getPlayer(player).sendPluginMessage(this, "BungeeCord", stream.toByteArray());
	}

	public void getCurrentServername(String player) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try {
			out.writeUTF("GetServer");
		} catch (IOException e) {
			e.printStackTrace();
		}
		getServer().sendPluginMessage(this, "BungeeCord", stream.toByteArray());
	}

	public String serverName = "";

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		try {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
			String subchannel = in.readUTF();
			if (subchannel.equals("GetServer")) {
				serverName = in.readUTF();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// BUNGEE FUNCTIONS STOP

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		getCurrentServername(event.getPlayer().getName());

		// check if need to be teleported somewhere
		this.mysqlHandleTempAddress(event.getPlayer().getName());
	}

}
