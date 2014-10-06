package com.instancedev.addresses;

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
import java.util.ArrayList;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

	String wrong_syntax_0 = "&cWrong syntax, must be: &6/atp <street> <number> <postcode> &c.";
	String wrong_syntax_1 = "&cExample: &6/atp Vesterbrogade 6C 1620";
	String teleported_to = "&2Teleporteret til &c<address> <number>";
	String you_can_now_tp_here = "&2Du kan frit teleportere til denne adresse igen! Kontakt en admin hvis dette ikke er din �nskede lokation. Husk at din lokation er synlig p� kortet for alle spillere.";
	String if_not_wanted = "&7If this is not your wanted location, please contact an admin.";
	String no_points_left = "&cIngen point tilbage. Skriv &a/buy &cfor at k�be flere. &7No points left. Type &f/buy &7to get more.";
	String no_points_buy_more = "&2Skriv &a/buy &2for at k�be flere. &7Type &f/buy &7for more points.";
	String cmd_syntax = "&7Syntax: /atp <vejnavn> <husnr> <postnr>";
	String atp = "Address Teleportation Points: ";
	String could_not_find_address = "&cKunne ikke finde adressen: &6<address> <number> (<postcode>)&c. &7Wrong address or syntax.";

	public void onEnable() {
		plugin = this;
		setupEconomy();
		getConfig().addDefault("mysql.pw", "password");
		getConfig().addDefault("messages.wrong_syntax_0", wrong_syntax_0);
		getConfig().addDefault("messages.wrong_syntax_1", wrong_syntax_1);
		getConfig().addDefault("messages.teleported_to", teleported_to);
		getConfig().addDefault("messages.you_can_now_tp_here", you_can_now_tp_here);
		getConfig().addDefault("messages.if_not_wanted", if_not_wanted);
		getConfig().addDefault("messages.no_points_left", no_points_left);
		getConfig().addDefault("messages.no_points_buy_more", no_points_buy_more);
		getConfig().addDefault("messages.cmd_syntax", cmd_syntax);
		getConfig().addDefault("messages.atp", atp);
		getConfig().addDefault("messages.could_not_find_address", could_not_find_address);
		getConfig().options().copyDefaults(true);
		this.saveConfig();

		Bukkit.getPluginManager().registerEvents(this, this);

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

		wrong_syntax_0 = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.wrong_syntax_0"));
		wrong_syntax_1 = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.wrong_syntax_1"));
		teleported_to = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.teleported_to"));
		you_can_now_tp_here = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.you_can_now_tp_here"));
		if_not_wanted = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.if_not_wanted"));
		no_points_left = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no_points_left"));
		no_points_buy_more = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no_points_buy_more"));
		cmd_syntax = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.cmd_syntax"));
		atp = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.atp"));
		could_not_find_address = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.could_not_find_address"));
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
				for (int i = 0; i < args.length; i++) {
					if (!Character.isDigit(args[i].charAt(0))) {
						address += toUppercase(args[i]) + " ";
					} else {
						c = i;
						break;
					}
				}
				if (address.length() < 1) {
					p.sendMessage(wrong_syntax_0);
					p.sendMessage(wrong_syntax_1);
					return true;
				}
				address = address.substring(0, address.length() - 1);

				if (c + 1 < args.length) {
					number = args[c].toUpperCase();
					postcode = args[c + 1];
				} else {
					p.sendMessage(wrong_syntax_0);
					p.sendMessage(wrong_syntax_1);
					return true;
				}

				if (mysqlUsedAddress(p.getName(), address, number, postcode)) {
					if (tryTP(p, address, number, postcode, true)) {
						p.sendMessage(teleported_to.replaceAll("<address>", address).replaceAll("<number>", number));
					}
					return true;
				}

				int currentpoints = (int) econ.getBalance(p.getName());
				if (currentpoints > 0) {
					if (tryTP(p, address, number, postcode, true)) {
						econ.withdrawPlayer(p.getName(), 1.0D);
						p.sendMessage(teleported_to.replaceAll("<address>", address).replaceAll("<number>", number) + you_can_now_tp_here);
						p.sendMessage(if_not_wanted);
					}
				} else {
					p.sendMessage(no_points_left);
				}
				return true;
			} else if (args.length > 0 && args.length < 3) {
				p.sendMessage(wrong_syntax_0);
				p.sendMessage(wrong_syntax_1);
				return true;
			} else {
				int points = (int) econ.getBalance(p.getName());
				String color = ChatColor.GREEN.toString();
				if (points < 1) {
					color = ChatColor.GOLD.toString();
				}
				p.sendMessage(atp + color + "" + ChatColor.BOLD + Integer.toString((int) econ.getBalance(p.getName())));
				if (points < 1) {
					p.sendMessage(no_points_buy_more);
				} else {
					p.sendMessage(cmd_syntax);
				}
				ArrayList<String> addresses = new ArrayList<String>(mysqlAllAddresses(p.getName()));
				if (addresses.size() > 0) {
					p.sendMessage(ChatColor.YELLOW + "=== " + ChatColor.WHITE + "Adresser " + ChatColor.YELLOW + "===");
					for (String address : mysqlAllAddresses(p.getName())) {
						p.sendMessage(ChatColor.GRAY + address);
					}
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets the first character of a string uppercase and the rest lowercase and returns the new string
	 * 
	 * @param str
	 * @return
	 */
	public String toUppercase(String str) {
		String ret = str.toLowerCase();
		ret = Character.toUpperCase(ret.charAt(0)) + ret.substring(1);
		return ret;
	}

	public boolean tryTP(Player p, String address, String number, String postcode, boolean needsServerCheck) {
		try {
			String req = "http://dawa.aws.dk/adresser?vejnavn=" + address + "&husnr=" + number + "&postnr=" + postcode + "&srid=25832";
			if (p.isOp()) {
				p.sendMessage(req);
			}
			if (!getLL(p, req, needsServerCheck, address, number, postcode)) {
				return false;
			}
			mysqlUpdateAddressUses(p.getName(), address, number, postcode);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Gets UTM coordinates from official denmark geo agency
	 * 
	 * @param p
	 * @param url
	 * @throws Exception
	 */
	public boolean getLL(Player p, String url, boolean needsServerCheck, String address, String number, String postcode) throws Exception {
		try {
			URL obj = new URL(url.replaceAll(" ", "%20"));
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");

			con.setRequestProperty("User-Agent", "Mozilla");

			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'GET' request to URL : " + url);
			// System.out.println("Response Code : " + responseCode);

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

			if (cindex < 1 || commaindex < 1 || endstuffsindex < 1) {
				p.sendMessage(could_not_find_address.replaceAll("<address>", address).replaceAll("<number>", number).replaceAll("<postcode>", postcode));
				return false;
			}
			try {
				x = r.substring(cindex + 24, commaindex);
				y = r.substring(commaindex + 9, (endstuffsindex - 6));
			} catch (Exception e) {
				p.sendMessage(could_not_find_address.replaceAll("<address>", address).replaceAll("<number>", number).replaceAll("<postcode>", postcode));
				return false;
			}

			System.out.println(x);
			System.out.println(y);

			int tpx = (int) Double.parseDouble(x) - 600000;
			int tpz = 6200000 - (int) Double.parseDouble(y);

			int tpy = 0;

			for (int i = 0; i < 220; i++) {
				if (p.getWorld().getBlockAt(new Location(p.getWorld(), tpx, i, tpz)).getType() == Material.AIR) {
					tpy = i;
					break;
				}
			}

			if (p.getWorld().getBlockAt(tpx, 3, tpz).getType() == Material.AIR) {
				// most likely wrong server
				if (needsServerCheck) {
					getLogger().info("Wrong server, fixing it.");
					this.handleCorrectServer(p.getName(), address, number, postcode);
					return true;
				}
				return true;
			}

			tpy++;

			// try to find a grass/stone block in a radius of 10 blocks (20*20)
			boolean done = false;
			for (int i_ = 0; i_ < 20; i_++) {
				if (done) {
					break;
				}
				for (int j_ = 0; j_ < 20; j_++) {
					Block b = p.getWorld().getBlockAt(tpx - 10 + i_, tpy - 2, tpz - 10 + j_);
					if (b.getType() == Material.STONE || b.getType() == Material.GRASS || b.getType() == Material.WOOL) {
						if (b.getLocation().clone().add(0D, 1D, 0D).getBlock().getType() == Material.AIR) {
							tpx = tpx - 5 + i_;
							tpz = tpz - 5 + j_;
							done = true;
							break;
						}
					}
				}
			}

			p.teleport(new Location(p.getWorld(), tpx, tpy, tpz));
			// p.teleport(new Location(p.getWorld(), Double.parseDouble(x) -
			// 600000,
			// tpy, 6200000 - Double.parseDouble(y)));
			return true;
		} catch (Exception e) {
			p.sendMessage(could_not_find_address.replaceAll("<address>", address).replaceAll("<number>", number).replaceAll("<postcode>", postcode));
			return false;
		}
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
	 * Returns whether address is free to teleport to (as player paid for it) or not
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
	 * Returns all paid addresses of a player
	 * 
	 * @param p
	 * @param address
	 * @param number
	 * @param postcode
	 * @return
	 */
	public ArrayList<String> mysqlAllAddresses(String p) {
		ArrayList<String> ret = new ArrayList<String>();
		MySQL MySQL = new MySQL("localhost", "3306", "addresses", "root", getConfig().getString("mysql.pw"));
		Connection c = null;
		c = MySQL.open();

		try {
			ResultSet res3 = c.createStatement().executeQuery("SELECT * FROM address WHERE player='" + p + "'");
			if (!res3.isBeforeFirst()) {
				System.out.println("No addresses found");
				return ret;
			}
			while (res3.next()) {
				ret.add(res3.getString("street") + " " + res3.getString("number") + " " + res3.getString("postcode"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ret;
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
	 * Will save the address into mysql and connect the player to the correct server
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
	public void onPlayerJoin(final PlayerJoinEvent event) {
		Bukkit.getScheduler().runTaskLater(this, new Runnable() {
			public void run() {
				getCurrentServername(event.getPlayer().getName());
			}
		}, 30L);

		// check if need to be teleported somewhere
		this.mysqlHandleTempAddress(event.getPlayer().getName());
	}

}
