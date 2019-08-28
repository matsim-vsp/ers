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
 * created by jbischoff, 09.10.2018
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricFleetWriter;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.fleet.ImmutableElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import com.google.common.collect.ImmutableList;

public class GenerateChargers {

	public static void main(String[] args) {
		String folder = "D:/ers/ev-test/";

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(folder + "network-osm.xml.gz");

		ChargerSpecification charger = ImmutableChargerSpecification.newBuilder()
				.id(Id.create(113273 + "charger", Charger.class))
				.plugPower(EvUnits.W_PER_kW * 50)
				.plugCount(2)
				.linkId(Id.createLinkId(113273))
				.chargerType("fast")
				.build();
		ChargerSpecification charger2 = ImmutableChargerSpecification.newBuilder()
				.id(Id.create(74836 + "charger", Charger.class))
				.plugPower(EvUnits.W_PER_kW * 50)
				.plugCount(2)
				.linkId(Id.createLinkId(74836))
				.chargerType("fast")
				.build();

		ChargerSpecification chargert = ImmutableChargerSpecification.newBuilder()
				.id(Id.create(113273 + "truckcharger", Charger.class))
				.plugPower(EvUnits.W_PER_kW * 200)
				.plugCount(2)
				.linkId(Id.createLinkId(113273))
				.chargerType("truck")
				.build();
		ChargerSpecification chargert2 = ImmutableChargerSpecification.newBuilder()
				.id(Id.create(74836 + "truckcharger", Charger.class))
				.plugPower(EvUnits.W_PER_kW * 200)
				.plugCount(2)
				.linkId(Id.createLinkId(74836))
				.chargerType("truck")
				.build();

		List<ChargerSpecification> chargers = new ArrayList<>();
		chargers.add(charger);
		chargers.add(charger2);
		chargers.add(chargert);
		chargers.add(chargert2);
		new ChargerWriter(chargers.stream()).write(folder + "test-chargers.xml");
		ImmutableList<String> chargingTypes = ImmutableList.<String>builder().add("fast").add("slow").build();
		ElectricVehicleSpecification ev = ImmutableElectricVehicleSpecification.newBuilder()
				.id(Id.create("testEV1", ElectricVehicle.class))
				.batteryCapacity(30 * EvUnits.J_PER_kWh)
				.initialSoc(30 * EvUnits.J_PER_kWh)
				.chargerTypes(chargingTypes)
				.vehicleType("car")
				.build();
		new ElectricFleetWriter(Collections.singletonList(ev).stream()).write(folder + "test_evs.xml");

	}
}
