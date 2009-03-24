/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesProductionKTIYear1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.preprocess.facilitiescreationfrombz;

import org.apache.log4j.Logger;
import org.matsim.facilities.FacilitiesImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;

/**
 * Generates the facilities file for all of Switzerland from the Swiss
 * National Enterprise Census of the year 2000 (published 2001).
 */
public class FacilitiesProductionKTI {

	public enum KTIYear {KTI_YEAR_2007, KTI_YEAR_2008}

	// work
	public static final String ACT_TYPE_WORK = "work";
	public static final String WORK_SECTOR2 = "work_sector2";
	public static final String WORK_SECTOR3 = "work_sector3";

	// education
	public static final String ACT_TYPE_EDUCATION = "education";

	public static final String EDUCATION_KINDERGARTEN = ACT_TYPE_EDUCATION + "_kindergarten";
	public static final String EDUCATION_PRIMARY = ACT_TYPE_EDUCATION + "_primary";
	public static final String EDUCATION_SECONDARY = ACT_TYPE_EDUCATION + "_secondary";
	public static final String EDUCATION_HIGHER = ACT_TYPE_EDUCATION + "_higher";
	public static final String EDUCATION_OTHER = ACT_TYPE_EDUCATION + "_other";

	// shopping
	public static final String ACT_TYPE_SHOP = "shop";
	public static final String SHOP_RETAIL_GT2500 = ACT_TYPE_SHOP + "_retail_gt2500sqm";
	public static final String SHOP_RETAIL_GET1000 = ACT_TYPE_SHOP + "_retail_get1000sqm";
	public static final String SHOP_RETAIL_GET400 = ACT_TYPE_SHOP + "_retail_get400sqm";
	public static final String SHOP_RETAIL_GET100 = ACT_TYPE_SHOP + "_retail_get100sqm";
	public static final String SHOP_RETAIL_LT100 = ACT_TYPE_SHOP + "_retail_lt100sqm";
	public static final String SHOP_OTHER = ACT_TYPE_SHOP + "_other";

	// leisure
	public static final String ACT_TYPE_LEISURE = "leisure";
	public static final String LEISURE_SPORTS = ACT_TYPE_LEISURE + "_sports";
	public static final String LEISURE_CULTURE = ACT_TYPE_LEISURE + "_culture";
	public static final String LEISURE_GASTRO = ACT_TYPE_LEISURE + "_gastro";
	public static final String LEISURE_HOSPITALITY = ACT_TYPE_LEISURE + "_hospitality";


	private static Logger log = Logger.getLogger(FacilitiesProductionKTI.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FacilitiesProductionKTI.facilitiesProduction(KTIYear.KTI_YEAR_2008);
	}
	
	private static void facilitiesProduction(KTIYear ktiYear) {
		
		FacilitiesImpl facilities = (FacilitiesImpl)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE,null);
		
		facilities.setName(
				"Facilities based on the Swiss National Enterprise Census of the year 2000. Generated by org.matsim.demandmodeling.facilities.FacilitiesProductionKTIYear1"
		);
		
		log.info("Adding and running facilities algorithms...");
		new FacilitiesAllActivitiesFTE(ktiYear).run(facilities);
		new AddOpentimes().run(facilities);
//		new FacilitiesRandomizeHectareCoordinates().run(facilities);
		facilities.runAlgorithms();
		log.info("Adding and running facilities algorithms...done.");

		Gbl.createConfig(null);
		Gbl.getConfig().setParam("facilities", "outputFacilitiesFile", "output/facilities.xml.gz");
		System.out.println("  writing facilities file... ");
		new FacilitiesWriter(facilities).write();
		System.out.println("  done.");
		
	}
	
}
