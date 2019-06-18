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
 * created by jbischoff, 15.10.2018
 */

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.fleet.*;
import org.matsim.core.gbl.MatsimRandom;

import java.util.Random;


public class VehiclesAsEVFleet implements Provider<ElectricFleetSpecification> {

    @Inject
    Population population;

    private ElectricFleetSpecification electricFleet;

    private final String truckType = "truck";
    private final double truckCapacity;
    private final ImmutableList<String> truckChargers = ImmutableList.<String>builder().add("truck").build();

    private final String smallcarType = "smallCar";
    private final double smallCarCapacity;

    private final String mediumcarType = "mediumCar";
    private final double mediumCarCapacity;

    private final String suvcarType = "SUV";
    private final double suvCarCapacity;

    private final ImmutableList<String> carChargers = ImmutableList.<String>builder().add("fast").build();

    private final Random random = MatsimRandom.getRandom();

    public VehiclesAsEVFleet(double truckCapacity, double smallCarCapacity, double mediumCarCapacity, double suvCarCapacity) {
        this.truckCapacity = truckCapacity;
        this.smallCarCapacity = smallCarCapacity;
        this.mediumCarCapacity = mediumCarCapacity;
        this.suvCarCapacity = suvCarCapacity;
    }

    @Override
    public ElectricFleetSpecification get() {
        electricFleet = new ElectricFleetSpecificationImpl();
        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            plan.getPlanElements().stream().filter(Leg.class::isInstance).forEach(pl -> {
                if (((Leg) pl).getMode().equals(TransportMode.car))
                    generateCar(person.getId());
                else if (((Leg) pl).getMode().equals(TransportMode.truck))
                    generateTruck(person.getId());
            });
        }

        return electricFleet;
    }

    private void generateTruck(Id<Person> id) {
        generateAndAddVehicle(id, TransportMode.truck, truckCapacity, truckCapacity, truckType, truckChargers);
    }

    private void generateCar(Id<Person> id) {
        double r = random.nextDouble();
        double capacity;
        String type;
        if (r < 0.5) {
            type = mediumcarType;
            capacity = mediumCarCapacity;
        } else if (r < 0.85) {
            type = suvcarType;
            capacity = suvCarCapacity;
        } else {
            type = smallcarType;
            capacity = smallCarCapacity;
        }
        generateAndAddVehicle(id, TransportMode.car, capacity, capacity, type, carChargers);
    }

    private void generateAndAddVehicle(Id<Person> id, String mode, double batteryCapa, double soc, String vehicleType, ImmutableList<String> chargerTypes) {
        Id<ElectricVehicle> evId = mode.equals(TransportMode.car) ? Id.create(id, ElectricVehicle.class) : Id.create(id.toString() + "_" + mode, ElectricVehicle.class);
        if (!electricFleet.getVehicleSpecifications().containsKey(evId)) {
            ElectricVehicleSpecification ev = ImmutableElectricVehicleSpecification.newBuilder().id(evId).batteryCapacity(batteryCapa).initialSoc(soc).chargerTypes(chargerTypes).vehicleType(vehicleType).build();
            electricFleet.addVehicleSpecification(ev);
        }
    }


}
