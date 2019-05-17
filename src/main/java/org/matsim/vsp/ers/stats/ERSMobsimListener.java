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

import com.google.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.core.config.Config;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.utils.misc.Time;
import org.matsim.vsp.ers.consumption.ElectricRoadEnergyConsumption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

public class ERSMobsimListener implements MobsimBeforeCleanupListener, MobsimInitializedListener {


    @Inject
    public ERSMobsimListener(Config config) {
        this.config = config;
        simStart = Math.max(0, config.qsim().getStartTime());
        simEnd = Math.max(config.qsim().getEndTime(), 36 * 3600);
    }

    public static class ERSLinkStats {
        public static String ERSLINKSTATS = "ersStats";
        DecimalFormat df = new DecimalFormat("0.00");


        public ERSLinkStats(double simStart, double simEnd, double binsize, Id<Link> linkId) {
            bins = (int) ((simEnd - simStart) / binsize);
            this.startTime = simStart;
            this.binsize = binsize;
            this.emmitedEnergy = new double[bins];
            this.linkId = linkId;

        }

        public void addEmmitedEnergy(double time, double energy) {
            emmitedEnergy[getBin(time)] += energy;
        }

        private int getBin(double time) {
            return (int) ((time - startTime) / binsize);
        }

        private double[] emmitedEnergy;
        private int bins;
        private final double startTime;
        private final double binsize;
        private final Id<Link> linkId;

        public List<String> getRecords() {
            List<String> record = new ArrayList<>();
            record.add(linkId.toString());
            for (int i = 0; i < emmitedEnergy.length; i++) {
                record.add(df.format(EvUnits.J_to_kWh(emmitedEnergy[i])));
            }
            record.add(df.format(EvUnits.J_to_kWh(DoubleStream.of(emmitedEnergy).sum())));
            return record;
        }
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
    private int bins;

    final double simStart;
    final double simEnd;

    @Override
    public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
        try {
            List<String> header = new ArrayList<>();
            header.add("Link");
            for (int i = 0; i < bins; i++) {
                header.add(Time.writeTime(i * BINSIZE));
            }
            header.add("Total");
            CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "ERS_usage.csv"))), CSVFormat.DEFAULT.withDelimiter(';'));
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
            csvPrinter.close();


        } catch (IOException e1) {
            e1.printStackTrace();
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
}



