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

import java.util.function.Predicate;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.FastThenSlowCharging;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.utils.misc.Time;
import org.matsim.vsp.ers.stats.ERSMobsimListener;

public class ElectricRoadEnergyConsumption implements DriveEnergyConsumption {

    public static String ER_LINK_POWER = "ersPower_kW";
    private final Predicate<ElectricVehicle> wantsToCharge;

    private final DriveEnergyConsumption delegate;

    private final ElectricVehicle ev;

    public static class Factory implements DriveEnergyConsumption.Factory {

        private final Predicate<ElectricVehicle> wantsToCharge;
        private final DriveEnergyConsumption.Factory delegateFactory;

        public Factory(Predicate<ElectricVehicle> wantsToCharge, DriveEnergyConsumption.Factory delegateFactory) {
            this.wantsToCharge = wantsToCharge;
            this.delegateFactory = delegateFactory;
        }

        @Override
        public DriveEnergyConsumption create(ElectricVehicle electricVehicle) {
            return new ElectricRoadEnergyConsumption(delegateFactory.create(electricVehicle), wantsToCharge, electricVehicle);
        }
    }


    private ElectricRoadEnergyConsumption(DriveEnergyConsumption delegate, Predicate<ElectricVehicle> wantsToCharge, ElectricVehicle electricVehicle) {
        this.delegate = delegate;
        this.wantsToCharge = wantsToCharge;
        this.ev = electricVehicle;
    }

    @Override
	public double calcEnergyConsumption(Link link, double travelTime, double linkLeaveTime) {
		double consumption = delegate.calcEnergyConsumption(link, travelTime, linkLeaveTime);
        double maxChargingPower = getElectricRoadChargingPower(link);
        if (maxChargingPower > 0 && wantsToCharge.test(ev)) {
            double charge = calculateCharge(consumption, link, travelTime, maxChargingPower);
            charge = Math.max(charge, ev.getBattery().getCapacity() - ev.getBattery().getSoc());
            ev.getBattery().charge(charge);

			if (!Time.isUndefinedTime(linkLeaveTime)) {
				((ERSMobsimListener.ERSLinkStats)link.getAttributes()
						.getAttribute(ERSMobsimListener.ERSLinkStats.ERSLINKSTATS)).addEmmitedEnergy(linkLeaveTime,
						charge);

            }
        }
        return consumption;
    }

    private double calculateCharge(double consumption, Link link, double travelTime, double maxChargingPower) {
        double charge = new FastThenSlowCharging(maxChargingPower).calcEnergyCharge(ev, travelTime);
        //TODO: Consider a also the consumed energy to calculate re-charge
        return charge;
    }

    private double getElectricRoadChargingPower(Link link) {
        if (link.getAttributes().getAsMap().containsKey(ER_LINK_POWER)) {
            return EvUnits.kW_to_W((double) link.getAttributes().getAttribute(ER_LINK_POWER));
        } else
            return 0.0;
    }


}

