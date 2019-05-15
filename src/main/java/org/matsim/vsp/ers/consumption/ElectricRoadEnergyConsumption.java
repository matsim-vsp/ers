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

package org.matsim.vsp.ers.consumption;/*
 * created by jbischoff, 15.05.2019
 */

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.discharging.LTHDriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

public class ElectricRoadEnergyConsumption implements DriveEnergyConsumption {

    private LTHDriveEnergyConsumption delegate;
    private ElectricVehicle ev;

    public ElectricRoadEnergyConsumption(LTHDriveEnergyConsumption delegate) {
        this.delegate = delegate;
    }

    @Override
    public double calcEnergyConsumption(Link link, double travelTime) {
        return 0;
    }

}
