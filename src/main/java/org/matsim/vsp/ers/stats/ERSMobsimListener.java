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

package org.matsim.vsp.ers.stats;/*
 * created by jbischoff, 17.05.2019
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.stats.ChargerPowerCollector;
import org.matsim.core.config.Config;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.utils.misc.Time;
import org.matsim.vsp.ers.consumption.ElectricRoadEnergyConsumption;

import com.google.inject.Inject;

public class ERSMobsimListener implements MobsimBeforeCleanupListener, MobsimInitializedListener {


    @Inject
    public ERSMobsimListener(Config config) {
        this.config = config;
        simStart = Math.max(0, config.qsim().getStartTime().seconds());
        simEnd = Math.max(config.qsim().getEndTime().seconds(), 36 * 3600);
    }

    private static double BINSIZE = 3600;
    @Inject
    OutputDirectoryHierarchy controlerIO;
    @Inject
    IterationCounter iterationCounter;
    @Inject
    Network network;
    @Inject
    Config config;

    @Inject
    ElectricFleet electricFleet;

    @Inject
    ChargerPowerCollector chargerPowerCollector;

    private int bins;

    final double simStart;
    final double simEnd;

    @Override
    public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {

        writeERSUsage();
        analyseChargingEnergy();

    }

    private void writeERSUsage() {
        List<String> header = new ArrayList<>();
        header.add("Link");
        for (int i = 0; i < bins; i++) {
            header.add(Time.writeTime(i * BINSIZE));
        }
        header.add("Total");
        try (CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "ERS_usage.csv"))), CSVFormat.DEFAULT.withDelimiter(';'))) {
            csvPrinter.printRecord(header);
            network.getLinks().values().stream().
                    filter(l -> l.getAttributes().getAsMap().containsKey(ERSLinkStats.ERSLINKSTATS)).
                    forEach(l -> {
                        try {
                            csvPrinter.printRecord(((ERSLinkStats) l.getAttributes().getAttribute(ERSLinkStats.ERSLINKSTATS)).getRecords());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void analyseChargingEnergy() {
        Map<Id<ElectricVehicle>, Double> aggregatedErsEnergy = aggregateErsEnergy();
        Map<Id<ElectricVehicle>, Double> aggregatedFastChargerEnergy = chargerPowerCollector.getLogList().stream().collect(Collectors.toMap(k -> k.getVehicleId(), k -> k.getTransmitted_Energy(), (o, o2) -> o + o2));
        try (
                CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "energy_sources_split.csv"))), CSVFormat.DEFAULT.withDelimiter(';')
                        .withHeader("VehicleId", "energyAtSimStart", "energyfromERS", "energyFromFastCharging", "energyAtSimEnd"))
        ) {
            for (ElectricVehicle ev : electricFleet.getElectricVehicles().values()) {
                csvPrinter.printRecord(ev.getId().toString(),
                        EvUnits.J_to_kWh(ev.getBattery().getCapacity()),
                        EvUnits.J_to_kWh(aggregatedErsEnergy.getOrDefault(ev.getId(), 0.0)),
                        EvUnits.J_to_kWh(aggregatedFastChargerEnergy.getOrDefault(ev.getId(), 0.0)),
                        EvUnits.J_to_kWh(ev.getBattery().getSoc()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent e) {
        bins = (int) ((simEnd - simStart) / BINSIZE);

        network.getLinks().
                values().
                stream().
                filter(l -> l.getAttributes().getAsMap().containsKey(ElectricRoadEnergyConsumption.ER_LINK_POWER)).
                forEach(l -> l.getAttributes().putAttribute(ERSLinkStats.ERSLINKSTATS, new ERSLinkStats(simStart, simEnd, BINSIZE, ((Link) l).getId())));
    }

    private Map<Id<ElectricVehicle>, Double> aggregateErsEnergy() {
        return network.getLinks().values().stream()
                .filter(l -> l.getAttributes().getAsMap().containsKey(ERSLinkStats.ERSLINKSTATS))
                .flatMap(l -> (((ERSLinkStats) l.getAttributes().getAttribute(ERSLinkStats.ERSLINKSTATS))).getErsEnergyPerVehicle().entrySet().stream())
                .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue(), (v1, v2) -> v1 + v2));


    }
}



