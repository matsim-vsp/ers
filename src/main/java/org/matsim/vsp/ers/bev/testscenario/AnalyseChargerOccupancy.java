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
 * created by jbischoff, 09.04.2019
 */


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.matsim.core.utils.misc.Time;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AnalyseChargerOccupancy {

    private static final int TIMEBINLENGTH = 300;
    private static final String inputfile = "D:/runs-svn/ers_sweden/17-bev/ITERS/it.50/534628fast.txt";
    private static final String outputfile = "D:/runs-svn/ers_sweden/17-bev/ITERS/it.50/534628fast_occ.csv";

    public static void main(String[] args) throws IOException {
        double[] powerOverTime = new double[(30 * 3600 / TIMEBINLENGTH)];
        Reader reader = Files.newBufferedReader(Paths.get(inputfile));
        CSVParser parser = new CSVParser(reader, CSVFormat.newFormat('\t'));
        for (CSVRecord csvRecord : parser) {
            double startTime = Time.parseTime(csvRecord.get(1));
            double endTime = Time.parseTime(csvRecord.get(2));
            double energy = Double.parseDouble(csvRecord.get(7));
            int startBin = getBin(startTime);
            int endBin = getBin(endTime);
            double powerPerBin = (3600 / TIMEBINLENGTH) * energy / (endBin - startBin);


            for (int i = startBin; i <= endBin; i++) {
                powerOverTime[i] += 1;
//            powerOverTime[i] += powerPerBin;
            }
        }
        CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Paths.get(outputfile)), CSVFormat.DEFAULT.withDelimiter(';').withHeader("Time", "Power"));
        for (int i = 0; i < powerOverTime.length; i++) {
            printer.printRecord(Time.writeTime(i * TIMEBINLENGTH), powerOverTime[i]);
        }
        printer.flush();
        printer.close();
    }

    private static int getBin(double time) {
        int bin = (int) (time / TIMEBINLENGTH);
        return bin;
    }


}
