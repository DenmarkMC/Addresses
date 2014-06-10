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

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	JavaPlugin plugin = null;

	public void onEnable() {
		plugin = this;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("address")) {
			if (args.length > 1) {
				String address = args[0];
				String number = args[1];
				Player p = (Player) sender;
				try {
					// getLL(p, "http://dawa.aws.dk/adresser?vejnavn=" + address
					// + "&husnr=" + number + "&supplerendebynavn=Bjerge Str");
					p.sendMessage("http://dawa.aws.dk/adresser?vejnavn=" + address + "&husnr=" + number);
					getLL(p, "http://dawa.aws.dk/adresser?vejnavn=" + address + "&husnr=" + number + "&srid=25832");
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

		// getConvert(p, x, y);
		// t(p, x, y);
		// executeConvert(p, x, y);
		//doStuffz(p, x, y);
		p.teleport(new Location(p.getWorld(), Double.parseDouble(x) - 600000, 100, 6200000 - Double.parseDouble(y)));
	}

	double lat = 0D;
	double lngd = 0D;
	double lngdeg;

	public void doStuffz(Player p, String y, String x) {

		lat = Double.parseDouble(x);
		lngd = Double.parseDouble(y);
		lngdeg = Math.round(Double.parseDouble(x) * 100) / 100; // Double.parseDouble(x);
																// // dd.dd form
		convert(p);

	}

	/*
	 * public void convert(Player p) { // konverter fra Lat /Long til UTM (WGS84
	 * / NAD84) final double knu = 0.9996; // scale along central meridian of //
	 * zone final double a = 6378137; // Equatorial radius in meters final
	 * double b = 6356752.3142; // Polar radius in meters
	 * 
	 * double e = Math.sqrt(1 - (b * b) / (a * a)); // e = the eccenticity of
	 * the // earth's elliptical // cross-section double e2 = e * e / (1 - (e *
	 * e)); // The quantile e' only occurs in even // powers double n = (a - b)
	 * / (a + b); int zone = (int) (31 + (lngdeg / 6)); // Calculating UTM zone
	 * using // Longetude in dd.dd form as // supplied by the GPS double pi = 6
	 * * zone - 183; // Central meridian of zone double pii = (lngdeg - pi) *
	 * Math.PI / 180; // Differance between // Longitude and central // meridian
	 * of zone double rho = a * (1 - e * e) / Math.pow((1 - (e * e) *
	 * (Math.sin(lat) * (Math.sin(lat)))), (3.0 / 2.0)); double nu = a /
	 * (Math.pow((1 - (e * e * (Math.sin(lat)) * (Math.sin(lat)))), (1.0 /
	 * 2.0)));
	 * 
	 * double A0 = a * (1 - n + (5 / 4) * (Math.pow(n, 2) - Math.pow(n, 3)) +
	 * (81 / 64) * (Math.pow(n, 4) - Math.pow(n, 5))); double B0 = (3 * a * n /
	 * 2) * (1 - n - (7 * n * n / 8) * (1 - n) + (55 / 64) * (Math.pow(n, 4) -
	 * Math.pow(n, 5))); double C0 = (15 * a * n * n / 16) * (1 - n + (3 * n * n
	 * / 4) * (1 - n)); double D0 = (35 * a * Math.pow(n, 3) / 48) * (1 - n + 11
	 * * n * n / 16); double E0 = (315 * a * Math.pow(n, 4) / 51) * (1 - n); //
	 * Calculation of the Meridional Arc double S = A0 * lat - B0 * Math.sin(2 *
	 * lat) + C0 * Math.sin(4 * lat) - D0 * Math.sin(6 * lat) + E0 * Math.sin(8
	 * * lat);
	 * 
	 * double Ki = S * knu; double Kii = knu * nu * Math.sin(lat) *
	 * Math.cos(lat) / 2; double Kiii = (knu * nu * Math.sin(lat) *
	 * Math.pow(Math.cos(lat), 3) / 24) * (5 - Math.pow(Math.tan(lat), 2) + 9 *
	 * Math.pow(e2, 2) * Math.pow(Math.cos(lat), 2) + 4 * Math.pow(e2, 2) *
	 * Math.pow(Math.cos(lat), 4));
	 * 
	 * double Kiv = knu * nu * Math.cos(lat); double Kv = knu *
	 * Math.pow(Math.cos(lat), 3) * (nu / 6) * (1 - Math.pow(Math.tan(lat), 2) +
	 * e2 * Math.pow(Math.cos(lat), 2));
	 * 
	 * double UTMni = (Ki + Kii * Math.pow(pii, 2) + Kiii * Math.pow(pii, 4));//
	 * Northing double UTMei = 500000 + (Kiv * pii + Kv * Math.pow(pii, 3));
	 * double UTMn = (int) UTMni; // Northing, rounded to closest integer double
	 * UTMe = (int) UTMei; // Easting, rounded to closest integer
	 * 
	 * System.out.println(UTMn); System.out.println(UTMe);
	 * 
	 * //return [x-f*600000,6200000-y]; p.teleport(new Location(p.getWorld(),
	 * UTMe - 600000, 100, UTMn - 6200000)); }
	 */

	double a = 0; // equatorial radius in meters
	double f = 0; // polar flattening
	double b = 0; // polar radius in meters
	double e = 0; // eccentricity
	double e0 = 0; // e'

	public void setDatum() {
		this.a = 6378137.0D;
		this.f = 1 / 298.2572236;
		this.b = this.a * (1 - this.f); // polar radius
		this.e = Math.sqrt(1 - Math.pow(this.b, 2) / Math.pow(this.a, 2));
		this.e0 = this.e / Math.sqrt(1 - Math.pow(this.e, 1));
	}

	double drad = Math.PI / 180;
	double k = 1;
	double k0 = 0.9996;

	public void convert(Player p){
		setDatum();
        double phi = lat * this.drad;                              // convert latitude to radians
        double lng = lngd * this.drad;                             // convert longitude to radians
        double utmz = 1 + Math.floor((lngd + 180) / 6);            // longitude to utm zone
        double zcm = 3 + 6 * (utmz - 1) - 180;                     // central meridian of a zone
        double latz = 0;                                           // this gives us zone A-B for below 80S
        double esq = (1 - (this.b / this.a) * (this.b / this.a));
        double e0sq = this.e * this.e / (1 - Math.pow(this.e, 2));
        double M = 0;

        // convert latitude to latitude zone for nato
        if (lat > -80 && lat < 72) {
            latz = Math.floor((lat + 80) / 8) + 2;      // zones C-W in this range
        } if (lat > 72 && lat < 84) {
            latz = 21;                                  // zone X
        } else if (lat > 84) {
            latz = 23;                                  // zone Y-Z
        }

        double N = this.a / Math.sqrt(1 - Math.pow(this.e * Math.sin(phi), 2));
        double T = Math.pow(Math.tan(phi), 2);
        double C = e0sq * Math.pow(Math.cos(phi), 2);
        double A = (lngd - zcm) * this.drad * Math.cos(phi);

        // calculate M (USGS style)
        M = phi * (1 - esq * (1 / 4 + esq * (3 / 64 + 5 * esq / 256)));
        M = M - Math.sin(2 * phi) * (esq * (3 / 8 + esq * (3 / 32 + 45 * esq / 1024)));
        M = M + Math.sin(4 * phi) * (esq * esq * (15 / 256 + esq * 45 / 1024));
        M = M - Math.sin(6 * phi) * (esq * esq * esq * (35 / 3072));
        M = M * this.a;                                      //Arc length along standard meridian

        double M0 = 0;                                         // if another point of origin is used than the equator

        // now we are ready to calculate the UTM values...
        // first the easting
        double x = this.k0 * N * A * (1 + A * A * ((1 - T + C) / 6 + A * A * (5 - 18 * T + T * T + 72 * C - 58 * e0sq) / 120)); //Easting relative to CM
        x = x + 500000; // standard easting

        // now the northing
        double y = this.k0 * (M - M0 + N * Math.tan(phi) * (A * A * (1 / 2 + A * A * ((5 - T + 9 * C + 4 * C * C) / 24 + A * A * (61 - 58 * T + T * T + 600 * C - 330 * e0sq) / 720))));    // first from the equator
        double yg = y + 10000000;  //yg = y global, from S. Pole
        if (y < 0) {
            y = 10000000 + y;   // add in false northing if south of the equator
        }

        //double digraph = this.makeDigraph(x, y, utmz);
        System.out.println(x);
        System.out.println(y);
        
        p.teleport(new Location(p.getWorld(), x - 600000, 100, 6200000 - y));
        /*double rv = { 
            global: { 
                easting: Math.round(10*(x))/10, 
                northing: Math.round(10*y)/10, 
                zone: utmz, 
                southern: phi < 0 
            }, 
            nato: { 
                easting: Math.round(10*(x-100000*Math.floor(x/100000)))/10, 
                northing: Math.round(10*(y-100000*Math.floor(y/100000)))/10, 
                latZone: this.digraphLettersN[latz],
                lngZone: utmz,
                digraph: digraph
            }
        }*/

        //return rv;
	}
	/*
	 * public void executeConvert(Player p, String x, String y) throws
	 * ScriptException, FileNotFoundException{ ScriptEngineManager factory = new
	 * ScriptEngineManager(); ScriptEngine engine =
	 * factory.getEngineByName("JavaScript"); engine.put("x", x);
	 * engine.put("y", y); engine.eval(new java.io.FileReader("denmark.js")); }
	 * 
	 * public void getConvert(Player p, String x, String y) throws Exception {
	 * 
	 * String url = "http://localhost/t.php?x=" + x + "&y=" + y; URL obj = new
	 * URL(url); HttpURLConnection con = (HttpURLConnection)
	 * obj.openConnection(); con.setRequestMethod("GET");
	 * 
	 * con.setRequestProperty("User-Agent", "Mozilla");
	 * 
	 * int responseCode = con.getResponseCode();
	 * System.out.println("\nSending 'GET' request to URL : " + url);
	 * System.out.println("Response Code : " + responseCode);
	 * 
	 * //BufferedReader in = new BufferedReader( // new
	 * InputStreamReader(con.getInputStream())); //String inputLine;
	 * //StringBuffer response = new StringBuffer();
	 * 
	 * //while ((inputLine = in.readLine()) != null) { //
	 * response.append(inputLine); //} //in.close();
	 * 
	 * 
	 * System.out.println(new String(readData(con), "UTF-8")); //print result
	 * //String mcx = r.substring(r.indexOf("MC:"), r.indexOf(",",
	 * r.indexOf("MC:"))); //String mcz = r.substring(r.indexOf("," + 1,
	 * r.indexOf("MC:")), r.indexOf(".")) + "0";
	 * //getLogger().info(response.toString()); //p.sendMessage(mcx + " " +
	 * mcz); //p.teleport(new Location(p.getWorld(), (int)Float.parseFloat(mcx),
	 * 100, (int)Float.parseFloat(mcz))); }
	 * 
	 * public void t(Player p, String x, String y) throws IOException{ URL url =
	 * new URL("http://localhost/t.php?x=" + x + "&y=" + y); URLConnection con =
	 * url.openConnection(); InputStream in = con.getInputStream(); String
	 * encoding = con.getContentEncoding(); encoding = encoding == null ?
	 * "UTF-8" : encoding;
	 * 
	 * ByteArrayOutputStream baos = new ByteArrayOutputStream(); byte[] buf =
	 * new byte[8192]; int len = 0; while ((len = in.read(buf)) != -1) {
	 * baos.write(buf, 0, len); } String body = new String(baos.toByteArray(),
	 * encoding); System.out.println(body); }
	 * 
	 * 
	 * 
	 * public byte[] readData(HttpURLConnection conn) throws IOException,
	 * InterruptedException { String _connlen =
	 * conn.getHeaderField("Content-Length"); int connlen =
	 * Integer.parseInt(_connlen); InputStream isr = null; byte[] bytes = new
	 * byte[connlen];
	 * 
	 * try { isr = conn.getInputStream();
	 * 
	 * //security count that it doesn't begin to hang int maxcounter = 0; //wait
	 * till all data is available, max 5sec while((isr.available() != connlen)
	 * && (maxcounter < 5000)){ Thread.sleep(1); maxcounter++; } //Throw if not
	 * all data could be read if(maxcounter >= 5000) throw new
	 * IllegalAccessError();
	 * 
	 * //read the data if(isr.read(bytes, 0, connlen) < 0) throw new
	 * IllegalAccessError();
	 * 
	 * 
	 * } finally { if (isr != null) isr.close(); }
	 * 
	 * return bytes; }
	 */

}
