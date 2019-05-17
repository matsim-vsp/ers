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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.*;
import org.matsim.contrib.ev.data.Charger;
import org.matsim.contrib.ev.data.file.LTHConsumptionModelReader;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.discharging.VehicleTypeSpecificDriveEnergyConsumptionFactory;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.routing.EVNetworkRoutingProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vsp.ers.consumption.ElectricRoadEnergyConsumption;
import org.matsim.vsp.ers.scoring.AgentSpecificASCScoring;
import org.matsim.vsp.ers.stats.ERSMobsimListener;

import java.util.function.Function;

public class RunERSTestscenario {

    public static void main(String[] args) {


        Config config = ConfigUtils.loadConfig(args[0], new EvConfigGroup());
        config.transit().setUseTransit(false);
        config.transit().setUsingTransitInMobsim(false);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Function<Charger, ChargingStrategy> chargingStrategyFactory = charger -> new FastThenSlowCharging(charger.getPower());

        VehicleTypeSpecificDriveEnergyConsumptionFactory driveEnergyConsumptionFactory = new VehicleTypeSpecificDriveEnergyConsumptionFactory();
        driveEnergyConsumptionFactory.addEnergyConsumptionModelFactory("smallCar", new ElectricRoadEnergyConsumption.Factory(b -> true, new LTHConsumptionModelReader(Id.create("smallCar", VehicleType.class)).readFile(ConfigGroup.getInputFileURL(config.getContext(), "CityCarMap.csv").getFile())));
        driveEnergyConsumptionFactory.addEnergyConsumptionModelFactory("mediumCar", new ElectricRoadEnergyConsumption.Factory(b -> true, new LTHConsumptionModelReader(Id.create("mediumCar", VehicleType.class)).readFile(ConfigGroup.getInputFileURL(config.getContext(), "MidCarMap.csv").getFile())));
        driveEnergyConsumptionFactory.addEnergyConsumptionModelFactory("SUV", new ElectricRoadEnergyConsumption.Factory(b -> true, new LTHConsumptionModelReader(Id.create("SUV", VehicleType.class)).readFile(ConfigGroup.getInputFileURL(config.getContext(), "SUVMap.csv").getFile())));
        driveEnergyConsumptionFactory.addEnergyConsumptionModelFactory("truck", new ElectricRoadEnergyConsumption.Factory(b -> true, new LTHConsumptionModelReader(Id.create("truck", VehicleType.class)).readFile(ConfigGroup.getInputFileURL(config.getContext(), "HGV40Map.csv").getFile())));

        AuxEnergyConsumption.Factory dummy = electricVehicle -> (period, timeOfDay) -> 0;
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new EvModule());

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(ElectricFleetSpecification.class).toProvider(VehiclesAsEVFleet.class).asEagerSingleton();
                bind(DriveEnergyConsumption.Factory.class).toInstance(driveEnergyConsumptionFactory);
                bind(AuxEnergyConsumption.Factory.class).toInstance(dummy);
                addRoutingModuleBinding(TransportMode.car).toProvider(new EVNetworkRoutingProvider(TransportMode.car));
                bind(ChargingLogic.Factory.class).toInstance(
                        charger -> new ChargingWithQueueingAndAssignmentLogic(charger, chargingStrategyFactory.apply(charger)));
                addRoutingModuleBinding(TransportMode.truck).toProvider(new EVNetworkRoutingProvider(TransportMode.truck));
                bindScoringFunctionFactory().to(AgentSpecificASCScoring.class);
                bind(TransitSchedule.class).toInstance(scenario.getTransitSchedule());
                installQSimModule(new AbstractQSimModule() {
                    @Override
                    protected void configureQSim() {
                        bind(VehicleChargingHandler.class).asEagerSingleton();
                        addQSimComponentBinding(EvModule.EV_COMPONENT).to(ERSMobsimListener.class);

                    }
                });


            }
        });

        controler.configureQSimComponents(components -> components.addNamedComponent(EvModule.EV_COMPONENT));

        controler.run();
    }

}
