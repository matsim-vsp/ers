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
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargeUpToMaxSocStrategy;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.charging.FastThenSlowCharging;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.discharging.VehicleTypeSpecificDriveEnergyConsumptionFactory;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.infrastructure.LTHConsumptionModelReader;
import org.matsim.contrib.ev.routing.EvNetworkRoutingProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vsp.ers.scoring.AgentSpecificASCScoring;
import org.matsim.vsp.ers.stats.ERSMobsimListener;

/**
 * No ERS is used by cars (same code is used for running as in other ERS scenarios in order to receive the same output dataC)
 */
public class RunBEVScenario {

    public static void main(String[] args) {

        Config config = ConfigUtils.loadConfig(args[0], new EvConfigGroup());

        double truckCapacitykWh = Double.parseDouble(args[1]);
        double smallCarCapacitykWh = Double.parseDouble(args[2]);
        double mediumCarCapacitykWh = Double.parseDouble(args[3]);
        double suvCarCapacitykWh = Double.parseDouble(args[4]);

        config.transit().setUseTransit(false);
        config.transit().setUsingTransitInMobsim(false);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        VehicleTypeSpecificDriveEnergyConsumptionFactory driveEnergyConsumptionFactory = new VehicleTypeSpecificDriveEnergyConsumptionFactory();
        driveEnergyConsumptionFactory.addEnergyConsumptionModelFactory("smallCar",
                new LTHConsumptionModelReader(Id.create("smallCar", VehicleType.class)).readURL(
                        ConfigGroup.getInputFileURL(config.getContext(), "CityCarMap.csv")));
        driveEnergyConsumptionFactory.addEnergyConsumptionModelFactory("mediumCar",
                new LTHConsumptionModelReader(Id.create("mediumCar", VehicleType.class)).readURL(
                        ConfigGroup.getInputFileURL(config.getContext(), "MidCarMap.csv")));
        driveEnergyConsumptionFactory.addEnergyConsumptionModelFactory("SUV",
                new LTHConsumptionModelReader(Id.create("SUV", VehicleType.class)).readURL(
                        ConfigGroup.getInputFileURL(config.getContext(), "SUVMap.csv")));
        driveEnergyConsumptionFactory.addEnergyConsumptionModelFactory("truck",
                new LTHConsumptionModelReader(Id.create("truck", VehicleType.class)).readURL(
                        ConfigGroup.getInputFileURL(config.getContext(), "HGV16Map.csv")));

        AuxEnergyConsumption.Factory dummy = electricVehicle -> (timeOfDay, period, linkId) -> 0;

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new EvModule());

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(ElectricFleetSpecification.class).toProvider(new VehiclesAsEVFleet(EvUnits.kWh_to_J(truckCapacitykWh), EvUnits.kWh_to_J(smallCarCapacitykWh), EvUnits.kWh_to_J(mediumCarCapacitykWh), EvUnits.kWh_to_J(suvCarCapacitykWh))).asEagerSingleton();
                bind(DriveEnergyConsumption.Factory.class).toInstance(driveEnergyConsumptionFactory);
                bind(AuxEnergyConsumption.Factory.class).toInstance(dummy);
                addRoutingModuleBinding(TransportMode.car).toProvider(new EvNetworkRoutingProvider(TransportMode.car));
                bind(ChargingLogic.Factory.class).toProvider(new ChargingWithQueueingAndAssignmentLogic.FactoryProvider(
                        charger -> new ChargeUpToMaxSocStrategy(charger, 1)));
                bind(ChargingPower.Factory.class).toInstance(FastThenSlowCharging::new);
                addRoutingModuleBinding(TransportMode.truck).toProvider(
                        new EvNetworkRoutingProvider(TransportMode.truck));
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
