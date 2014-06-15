package com.comze_instancelabs.addresses;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Oldbackup {
	

	// backup

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

	public void convert(Player p) {
		setDatum();
		double phi = lat * this.drad; // convert latitude to radians
		double lng = lngd * this.drad; // convert longitude to radians
		double utmz = 1 + Math.floor((lngd + 180) / 6); // longitude to utm zone
		double zcm = 3 + 6 * (utmz - 1) - 180; // central meridian of a zone
		double latz = 0; // this gives us zone A-B for below 80S
		double esq = (1 - (this.b / this.a) * (this.b / this.a));
		double e0sq = this.e * this.e / (1 - Math.pow(this.e, 2));
		double M = 0;

		// convert latitude to latitude zone for nato
		if (lat > -80 && lat < 72) {
			latz = Math.floor((lat + 80) / 8) + 2; // zones C-W in this range
		}
		if (lat > 72 && lat < 84) {
			latz = 21; // zone X
		} else if (lat > 84) {
			latz = 23; // zone Y-Z
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
		M = M * this.a; // Arc length along standard meridian

		double M0 = 0; // if another point of origin is used than the equator

		// now we are ready to calculate the UTM values...
		// first the easting
		double x = this.k0 * N * A * (1 + A * A * ((1 - T + C) / 6 + A * A * (5 - 18 * T + T * T + 72 * C - 58 * e0sq) / 120)); // Easting
																																// relative
																																// to
																																// CM
		x = x + 500000; // standard easting

		// now the northing
		double y = this.k0 * (M - M0 + N * Math.tan(phi) * (A * A * (1 / 2 + A * A * ((5 - T + 9 * C + 4 * C * C) / 24 + A * A * (61 - 58 * T + T * T + 600 * C - 330 * e0sq) / 720)))); // first
																																															// from
																																															// the
																																															// equator
		double yg = y + 10000000; // yg = y global, from S. Pole
		if (y < 0) {
			y = 10000000 + y; // add in false northing if south of the equator
		}

		// double digraph = this.makeDigraph(x, y, utmz);
		System.out.println(x);
		System.out.println(y);

		p.teleport(new Location(p.getWorld(), x - 600000, 100, 6200000 - y));
		/*
		 * double rv = { global: { easting: Math.round(10*(x))/10, northing:
		 * Math.round(10*y)/10, zone: utmz, southern: phi < 0 }, nato: {
		 * easting: Math.round(10*(x-100000*Math.floor(x/100000)))/10, northing:
		 * Math.round(10*(y-100000*Math.floor(y/100000)))/10, latZone:
		 * this.digraphLettersN[latz], lngZone: utmz, digraph: digraph } }
		 */

		// return rv;
	}
}
