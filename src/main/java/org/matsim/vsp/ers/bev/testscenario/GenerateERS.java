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
 * created by jbischoff, 17.05.2019
 */

import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.vsp.ers.consumption.ElectricRoadEnergyConsumption;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

public class GenerateERS {

    public static final String SWEDENSHAPE = "D:/ers/sweden-model/commuters/sweden_oneshape.shp";
    public static final String SWEDENNET = "D:/ers/scenario/network-osm.xml.gz";
    public static final String SWEDENNET_ERS = "D:/ers/scenario/network-osm-ers.xml.gz";
    public static final String SWEDENNET_ERS_only = "D:/ers/scenario/network-osm-ers-only.xml.gz";

    public static void main(String[] args) {
        Feature cover = ShapeFileReader.getAllFeatures(SWEDENSHAPE).iterator().next();
        MultiPolygon polygon = (MultiPolygon) ((SimpleFeature) cover).getDefaultGeometry();
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(SWEDENNET);
        network.getLinks().values().stream().
                filter(l -> polygon.contains(MGC.coord2Point(((Link) l).getCoord()))).
//                filter(l->String.valueOf(l.getAttributes().getAttribute("type")).equals("motorway")).
        filter(l -> l.getFreespeed() > 110 / 3.6 && l.getNumberOfLanes() > 1).
                forEach(l -> l.getAttributes().putAttribute(ElectricRoadEnergyConsumption.ER_LINK_POWER, 100.0));

        NetworkFilterManager networkFilterManager = new NetworkFilterManager(network);
        networkFilterManager.addLinkFilter(new NetworkLinkFilter() {
            @Override
            public boolean judgeLink(Link l) {
                return (l.getAttributes().getAsMap().containsKey(ElectricRoadEnergyConsumption.ER_LINK_POWER));
            }
        });

        new NetworkWriter(network).write(SWEDENNET_ERS);
        new NetworkWriter(networkFilterManager.applyFilters()).write(SWEDENNET_ERS_only);
    }
}
