/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.vsp.ers.bev.testscenario;/*
 * created by jbischoff, 12.10.2018
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class GenerateChargersFromGasStations {

    public static void main(String[] args) throws IOException, ParseException {
        String folder = "D:/ers/ev-test/";

        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3006");
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(folder + "network-osm.xml.gz");
        NetworkFilterManager nfm = new NetworkFilterManager(network);
        nfm.addLinkFilter(l -> {
            if (l.getAllowedModes().contains(TransportMode.car)) return true;
            else return false;
        });
        Network filteredNet = nfm.applyFilters();
        BufferedReader in = new BufferedReader(new FileReader(folder + "gas-stations-sweden.json"));
        Map<Id<Charger>, ChargerSpecification> chargers = new HashMap<>();


        JSONParser jp = new JSONParser();

        JSONObject jsonObject = (JSONObject) jp.parse(in);
        JSONArray elements = ((JSONArray) (jsonObject.get("elements")));
        for (Object o : elements) {
            JSONObject jo = (JSONObject) o;
            double y = Double.parseDouble(jo.get("lat").toString());
            double x = Double.parseDouble(jo.get("lon").toString());
            Coord c = ct.transform(new Coord(x, y));
            Link l = NetworkUtils.getNearestLink(filteredNet, c);
            Id<Charger> carCharger = Id.create(l.getId().toString() + "fast", Charger.class);
			ChargerSpecification fastCharger = ImmutableChargerSpecification.newBuilder()
                    .id(carCharger)
					.plugPower(120 * EvUnits.W_PER_kW)
					.plugCount(10)
					.linkId(l.getId())
					.chargerType("fast")
					.build();
            chargers.put(carCharger, fastCharger);
            Id<Charger> truckChargerId = Id.create(l.getId().toString() + "truck", Charger.class);

            ChargerSpecification truckCharger = ImmutableChargerSpecification.newBuilder()
                    .id(truckChargerId)
					.plugPower(400 * EvUnits.W_PER_kW)
					.plugCount(2)
					.linkId(l.getId())
					.chargerType("truck")
					.build();
            chargers.put(truckChargerId, truckCharger);
        }
        new ChargerWriter(chargers.values().stream()).write(folder + "chargers_gasstations.xml");

    }


}
