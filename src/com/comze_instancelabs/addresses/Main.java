package com.comze_instancelabs.addresses;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	JavaPlugin plugin = null;

	public void onEnable(){
		plugin = this;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("address")){
			if(args.length > 1){
				String address = args[0];
				String number = args[1];
				Player p = (Player) sender;
				try {
					//getLL(p, "http://dawa.aws.dk/adresser?vejnavn=" + address + "&husnr=" + number + "&supplerendebynavn=Bjerge Str");
					p.sendMessage("http://dawa.aws.dk/adresser?vejnavn=" + address + "&husnr=" + number);
					getLL(p, "http://dawa.aws.dk/adresser?vejnavn=" + address + "&husnr=" + number);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return true;
		}
		return false;
	}
	


	public void getLL(Player p, String url) throws Exception {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");

		con.setRequestProperty("User-Agent", "Mozilla");
 
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
 
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
 
		//print result
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
		
		
		//getConvert(p, x, y);
		//t(p, x, y);
		executeConvert(p, x, y);
	}
	
	public void executeConvert(Player p, String x, String y) throws ScriptException, FileNotFoundException{
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("JavaScript");
		engine.put("x", x);
		engine.put("y", y);
		engine.eval(new java.io.FileReader("denmark.js"));
	}
	
	public void getConvert(Player p, String x, String y) throws Exception {
		
		String url = "http://localhost/t.php?x=" + x + "&y=" + y;
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");

		con.setRequestProperty("User-Agent", "Mozilla");
 
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
 
		//BufferedReader in = new BufferedReader(
		//        new InputStreamReader(con.getInputStream()));
		//String inputLine;
		//StringBuffer response = new StringBuffer();
 
		//while ((inputLine = in.readLine()) != null) {
		//	response.append(inputLine);
		//}
		//in.close();


		System.out.println(new String(readData(con), "UTF-8"));
		//print result
		//String mcx = r.substring(r.indexOf("MC:"), r.indexOf(",", r.indexOf("MC:")));
		//String mcz = r.substring(r.indexOf("," + 1, r.indexOf("MC:")), r.indexOf(".")) + "0";
		//getLogger().info(response.toString());
		//p.sendMessage(mcx + " " + mcz);
		//p.teleport(new Location(p.getWorld(), (int)Float.parseFloat(mcx), 100, (int)Float.parseFloat(mcz)));
	}

	public void t(Player p, String x, String y) throws IOException{
		URL url = new URL("http://localhost/t.php?x=" + x + "&y=" + y);
		URLConnection con = url.openConnection();
		InputStream in = con.getInputStream();
		String encoding = con.getContentEncoding();
		encoding = encoding == null ? "UTF-8" : encoding;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int len = 0;
		while ((len = in.read(buf)) != -1) {
		    baos.write(buf, 0, len);
		}
		String body = new String(baos.toByteArray(), encoding);
		System.out.println(body);
	}

	
	
	public byte[] readData(HttpURLConnection conn)
	        throws IOException, InterruptedException {
	    String _connlen = conn.getHeaderField("Content-Length");
	    int connlen = Integer.parseInt(_connlen);
	    InputStream isr = null;
	    byte[] bytes = new byte[connlen];

	    try {
	        isr = conn.getInputStream();

	        //security count that it doesn't begin to hang
	        int maxcounter = 0;
	        //wait till all data is available, max 5sec
	        while((isr.available() != connlen) && (maxcounter  < 5000)){
	            Thread.sleep(1);
	            maxcounter++;
	        }
	        //Throw if not all data could be read
	        if(maxcounter >= 5000)
	            throw new IllegalAccessError(); 

	        //read the data         
	        if(isr.read(bytes, 0, connlen) < 0)
	            throw new IllegalAccessError();     


	    } finally {
	        if (isr != null)
	            isr.close();
	    }

	    return bytes;
	}
	
}
