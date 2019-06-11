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
 * created by jbischoff, 05.06.2019
 */

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

public class ERSLinkStats {
    public static String ERSLINKSTATS = "ersStats";
    private final double startTime;
    private final double binsize;
    private final Id<Link> linkId;
    DecimalFormat df = new DecimalFormat("0.00");
    private double[] emmitedEnergy;
    private int bins;
    private Map<Id<ElectricVehicle>, Double> ersEnergyPerVehicle;

    public ERSLinkStats(double simStart, double simEnd, double binsize, Id<Link> linkId) {
        bins = (int) ((simEnd - simStart) / binsize);
        this.startTime = simStart;
        this.binsize = binsize;
        this.emmitedEnergy = new double[bins];
        this.linkId = linkId;
        this.ersEnergyPerVehicle = new HashMap<>();

    }

    public void addEmmitedEnergyForVehicle(double time, double energy, ElectricVehicle electricVehicle) {
        double ersEnergy = ersEnergyPerVehicle.getOrDefault(electricVehicle.getId(), 0.0) + energy;
        ersEnergyPerVehicle.put(electricVehicle.getId(), ersEnergy);
        emmitedEnergy[getBin(time)] += energy;
    }

    private int getBin(double time) {
        return (int) ((time - startTime) / binsize);
    }

    public List<String> getRecords() {
        List<String> record = new ArrayList<>();
        record.add(linkId.toString());
        for (int i = 0; i < emmitedEnergy.length; i++) {
            record.add(df.format(EvUnits.J_to_kWh(emmitedEnergy[i])));
        }
        record.add(df.format(EvUnits.J_to_kWh(DoubleStream.of(emmitedEnergy).sum())));
        return record;
    }

    public Map<Id<ElectricVehicle>, Double> getErsEnergyPerVehicle() {
        return ersEnergyPerVehicle;
    }
}
