/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestEmission.java                                                       *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.contrib.emissions;

import static org.matsim.contrib.emissions.HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
import static org.matsim.contrib.emissions.HbefaVehicleCategory.PASSENGER_CAR;
import static org.matsim.contrib.emissions.Pollutant.CO;
import static org.matsim.contrib.emissions.Pollutant.CO2_TOTAL;
import static org.matsim.contrib.emissions.Pollutant.FC;
import static org.matsim.contrib.emissions.Pollutant.HC;
import static org.matsim.contrib.emissions.Pollutant.NMHC;
import static org.matsim.contrib.emissions.Pollutant.NO2;
import static org.matsim.contrib.emissions.Pollutant.NOx;
import static org.matsim.contrib.emissions.Pollutant.PM;
import static org.matsim.contrib.emissions.Pollutant.SO2;

import java.util.*;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;


/**
 * @author julia
 * <p>
 * test for playground.vsp.emissions.ColdEmissionAnalysisModule
 * <p>
 * ColdEmissionAnalysisModule (ceam)
 * public methods and corresponding tests:
 * ceam module parameter - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 * ceam - constructor, nothing to test
 * reset - nothing to test
 * calculate cold emissions and throw event - calculateColdEmissionsAndThrowEventTest
 * <p>
 * private methods and corresponding tests:
 * rescale cold emissions - rescaleColdEmissionsTest
 * calculate cold emissions - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 * convert string to tuple - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 * <p>
 * in all cases the needed tables are created manually by the setUp() method
 * see test methods for details on the particular test cases
 */

public class TestColdEmissionAnalysisModule {
	private static final Logger logger = Logger.getLogger(TestColdEmissionAnalysisModule.class);
	
	private ColdEmissionAnalysisModule coldEmissionAnalysisModule;
	
	private final String passengercar = "PASSENGER_CAR";
	private final Double startTime = 0.0;
	private static final Double parkingDuration = 1.;
	// same values as int for table
	private static final int tableParkingDuration = (int) Math.round( parkingDuration );
	private static final int tableAccDistance = 1;
	private static final Set<Pollutant> pollutants = new HashSet<>(Arrays.asList(CO, CO2_TOTAL, FC, HC, NMHC, NOx, NO2,PM, SO2));
	private final int numberOfColdEmissions = pollutants.size();
	// strings for test cases
	
	// The material below was confused in the way that strings like "petrol" or "diesel" were given for the
	// size classes, and "<1,4L" or ">=2L" for the emissions concept.  Tried to make it consistent,
	// but I don't know if it is still testing the original functionality.  kai, jul'18
	
	// first case: complete data - corresponding entry in average table
	private static final String petrol_technology = "petrol";
	private static final String none_sizeClass = "average";
	private static final String none_emConcept = "average";
	// second case: complete data - corresponding entry in detailed table
	private static final String petrol_technology2 = "petrol";  // maybe use same as above? kai, jul'18
	private static final String leq14l_sizeClass = "<1,4L";
	private static final String PC_P_Euro_1_emConcept = "PC-P-Euro-1";
	// third case: complete data corresponding entries in average and detailed table - use detailed
	private static final String diesel_technology = "diesel";
	private static final String geq2l_sizeClass = ">=2L";
	private static final String PC_D_Euro_3_emConcept = "PC-D-Euro-3";

	// emission factors for tables - no dublicates!
	private static final Double detailedPetrolFactor = 100.;
	private static final Double detailedDieselFactor = 10.;
	private static final Double averageAverageFactor = .1;
	private static final Double averagePetrolFactor = .01;

	private static final double fakeFactor = -1.;
	
	private boolean excep = false;

	// This used to be one large test class, which had separate table entries for each test, but put them all into the same table.  The result was
	// difficult if not impossible to debug, and the resulting detailed table was inconsistent in the sense that it did not contain all combinations of
	// entries. -- I have now pulled this apart into 6 different test classes, this one here plus "Case1" to "Case4" and "Case6".
	// The other tests are remaining in this class.  Things look ok, but given that the
	// single class before was so large that I could not fully comprehend it, there may now be errors in the ripped-apart classes.  Hopefully, over time,
	// this will help to sort things out.  kai (for warm emissions) / kmt, apr'20


	//@KMT is this to be delete or does it need to be repaired? --> "repaired" it
	@Test
	public void calculateColdEmissionsAndThrowEventTest_completeData() {

		/*
		 * six test cases with complete input data
		 * or input that should be assigned to average/default cases
		 */

		ColdEmissionAnalysisModule coldEmissionAnalysisModule  = setUp();
		List<ArrayList> testCases = new ArrayList<>();

		ArrayList<Object> testCase2 = new ArrayList<>();
		ArrayList<Object> testCase1 = new ArrayList<>();
		ArrayList<Object> testCase3 = new ArrayList<>();
		ArrayList<Object> testCase4 = new ArrayList<>();
		ArrayList<Object> testCase6 = new ArrayList<>();

//		// first case: complete data
//		// corresponding entry in average table
		Collections.addAll( testCase1, passengercar, petrol_technology, none_sizeClass, none_emConcept, averagePetrolFactor );

		// second case: complete data
		// corresponding entry in detailed table
		Collections.addAll( testCase2, passengercar, petrol_technology2, leq14l_sizeClass, PC_P_Euro_1_emConcept, detailedPetrolFactor );
//
		// third case: complete data
		// corresponding entries in average and detailed table; should use the detailed entry; thus
		// error when using the average entry.
		Collections.addAll( testCase3, passengercar, diesel_technology, geq2l_sizeClass, PC_D_Euro_3_emConcept, detailedDieselFactor );
		

		// sixth case is moved out to own test class.
		// sixth case: heavy goods vehicle
		// -> throw warning -> use detailed or average table for passenger cars
		String heavygoodsvehicle = "HEAVY_GOODS_VEHICLE";
		Collections.addAll( testCase6, heavygoodsvehicle, petrol_technology, none_sizeClass, none_emConcept, averagePetrolFactor );

		testCases.add( testCase1 );
		testCases.add( testCase2 );
		testCases.add( testCase3 );
		testCases.add( testCase4 );
//		testCases.add( testCase5 );
		testCases.add( testCase6 );

		for ( List<Object> tc : testCases ) {
			logger.info("Running testcase: " + testCases.indexOf( tc ) + " " + tc.toString());
			HandlerToTestEmissionAnalysisModules.reset();
			Id<Link> linkId = Id.create( "linkId" + testCases.indexOf( tc ), Link.class );
			Id<Vehicle> vehicleId = Id.create( "vehicleId" + testCases.indexOf( tc ), Vehicle.class );
			Id<VehicleType> vehicleTypeId = Id.create( tc.get( 0 ) + ";" + tc.get( 1 ) + ";" + tc.get( 2 ) + ";" + tc.get( 3 ), VehicleType.class );

			Vehicle vehicle = VehicleUtils.getFactory().createVehicle( vehicleId, VehicleUtils.getFactory().createVehicleType( vehicleTypeId ) );
			logger.info("VehicleId: " + vehicle.getId().toString());
			logger.info("VehicleTypeId: " + vehicle.getType().getId());


			//@KMT Method changed
			//coldEmissionAnalysisModule.checkVehicleInfoAndCalculateWColdEmissions( linkId, vehicle, startTime, parkingDuration, tableAccDistance );
			coldEmissionAnalysisModule.checkVehicleInfoAndCalculateWColdEmissions(vehicle.getType(), vehicleId,linkId, startTime, parkingDuration, tableAccDistance);

			String message = "The expected emissions for " + tc.toString() + " are " + numberOfColdEmissions * (Double) tc.get( 4 ) + " but were " + HandlerToTestEmissionAnalysisModules.getSum();
			Assert.assertEquals( message, numberOfColdEmissions * (Double) tc.get( 4 ), HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON );
		}
	}
	
	@Test
	public void calculateColdEmissionsAndThrowEventTest_Exceptions() {
		
		/*
		 * four test cases
		 * all of them should throw exceptions
		 */

		ColdEmissionAnalysisModule coldEmissionAnalysisModule  = setUp();
		List<Id<VehicleType>> testCasesExceptions = new ArrayList<>();
		excep = false;
		
		// seventh case: no corresponding entry either in the detailed nor the average table
		Id<VehicleType> vehicleInfoForNoCase = Id.create( "PASSENGER_CAR;PC diesel;;>=2L", VehicleType.class );
//		Id<VehicleType> vehicleInfoForNoCase = Id.create( "PASSENGER_CAR;"+diesel_technology+";" + geq2l_sizeClass + ";", VehicleType.class );
//		testCasesExceptions.add( vehicleInfoForNoCase ); //this will return the average passenger car value
		// eighth case: vehicle category not specified
		testCasesExceptions.add( Id.create( ";;;", VehicleType.class ) );
		// ninth case: empty string as id
		testCasesExceptions.add( Id.create( "", VehicleType.class ) );
		// tenth case: null id
		testCasesExceptions.add( null );
		
		for ( Id<VehicleType> vehicleTypeId : testCasesExceptions ) {
			String message = "'" + vehicleTypeId + "'" + " was used to calculate cold emissions and generate an emissions event."
							     + "It should instead throw an exception because it is not a valid vehicle information string.";
			try {
				Id<Link> linkId = Id.create( "linkId" + testCasesExceptions.indexOf( vehicleTypeId ), Link.class );
				Id<Vehicle> vehicleId = Id.create( "vehicleId" + testCasesExceptions.indexOf( vehicleTypeId ), Vehicle.class );
				Vehicle vehicle = VehicleUtils.getFactory().createVehicle( vehicleId, VehicleUtils.getFactory().createVehicleType( vehicleTypeId ) );
				coldEmissionAnalysisModule.checkVehicleInfoAndCalculateWColdEmissions(vehicle.getType(), vehicle.getId(), linkId, startTime, parkingDuration, tableAccDistance);
			} catch ( Exception e ) {
				excep = true;
			}
			Assert.assertTrue( message, excep );
			excep = false;
		}
		
	}
	
	@Test
	public void calculateColdEmissionsAndThrowEventTest_minimalVehicleInformation() {

		ColdEmissionAnalysisModule coldEmissionAnalysisModule  = setUp();
		excep = false;
		
		// eleventh case: no specifications for technology, size, class, em concept
		// string has no semicolons as seperators - use average values
		Id<VehicleType> vehInfo11 = Id.create( passengercar, VehicleType.class );
		Id<Link> linkId11 = Id.create( "link id 11", Link.class );
		Id<Vehicle> vehicleId7 = Id.create( "vehicle 11", Vehicle.class );
		
		Vehicle vehicle = VehicleUtils.getFactory().createVehicle( vehicleId7, VehicleUtils.getFactory().createVehicleType( vehInfo11 ) );
		
		HandlerToTestEmissionAnalysisModules.reset();
		coldEmissionAnalysisModule.checkVehicleInfoAndCalculateWColdEmissions(vehicle.getType(), vehicle.getId(), linkId11, startTime, parkingDuration, tableAccDistance);
		String message = "The expected emissions for an emissions event with vehicle information string '" + vehInfo11 + "' are " +
						     numberOfColdEmissions * averageAverageFactor + " but were " + HandlerToTestEmissionAnalysisModules.getSum();
		Assert.assertEquals( message, numberOfColdEmissions * averageAverageFactor, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON );
		
	}
	
	private ColdEmissionAnalysisModule setUp() {
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<>();
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<>();
		
		fillAveragesTable( avgHbefaColdTable );
		fillDetailedTable( detailedHbefaColdTable );
		
		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		if ( (Boolean) true ==null ) {
			ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.asEngineInformationAttributes );
		} else if ( true ) {
			ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		} else {
			ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
		}
		//This represents the previous behavior, which fallbacks to the average table, if values are not found in the detailed table, kmt apr'20
		ecg.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
		return coldEmissionAnalysisModule = new ColdEmissionAnalysisModule( avgHbefaColdTable, detailedHbefaColdTable, ecg, pollutants, emissionEventManager );
		
	}
	
	private static void fillDetailedTable( Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable ) {
		// create all needed and one unneeded entry for the detailed table
		{
			// add passenger car entry "petrol;<=1.4L;PC-P-Euro-1":
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology2, leq14l_sizeClass, PC_P_Euro_1_emConcept );
			putIntoHbefaColdTable( detailedHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( detailedPetrolFactor ), PASSENGER_CAR );
		}
		{
			// add passenger car entry "diesel;>=2L;PC-D-Euro-3":
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( diesel_technology, geq2l_sizeClass, PC_D_Euro_3_emConcept );
			putIntoHbefaColdTable( detailedHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( detailedDieselFactor ), PASSENGER_CAR );
		}
		{
			// add heavy goods vehicle entry "petrol;none;none":
			//(pre-existing comment: HEAVY_GOODS_VEHICLE;PC petrol;petrol;none should not be used --???)
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, none_emConcept );
			putIntoHbefaColdTable( detailedHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( fakeFactor ), HEAVY_GOODS_VEHICLE );
		}
//		{
//			// add passenger car entry "petrol;none;nullCase":
//			// (pre-existing comment: "PASSENGER_CAR;PC petrol;petrol;nullCase" --???)
//			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, nullcase_emConcept );
//
//
//			putIntoHbefaColdTable( detailedHbefaColdTable, vehAtt, detColdFactor, PASSENGER_CAR );
//		}
	}
	
	private static void fillAveragesTable( Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable ) {

		// create all needed and one unneeded entry for the average table

		{
			// add passenger car entry "average;average;average":
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( "average", "average", "average" ) ;
			
			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor(averageAverageFactor), PASSENGER_CAR );
		}
		{
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, none_emConcept );
			
			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( averagePetrolFactor ), PASSENGER_CAR );
		}
		{
			// duplicate from detailed table, but with different emission factor.
			// this should not be used but is needed to assure that the detailed table is tried before the average table
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( diesel_technology, geq2l_sizeClass, PC_D_Euro_3_emConcept );
			
			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( fakeFactor ), PASSENGER_CAR );
		}
		{
			// add HGV entry "petrol;none;none".
			// (pre-existing comment: HEAVY_GOODS_VEHICLE;PC petrol;petrol;none should not be used --???)
			final HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, none_emConcept );
			
			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( fakeFactor ), HEAVY_GOODS_VEHICLE );
		}
	}
	
	private static void putIntoHbefaColdTable( final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable, final HbefaVehicleAttributes vehAtt, final HbefaColdEmissionFactor detColdFactor, final HbefaVehicleCategory hbefaVehicleCategory ) {
		for ( Pollutant cp : pollutants ) {
			HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();
			detColdKey.setHbefaDistance( tableAccDistance );
			detColdKey.setHbefaParkingTime( tableParkingDuration );
			detColdKey.setHbefaVehicleAttributes( vehAtt );
			detColdKey.setHbefaVehicleCategory( hbefaVehicleCategory );
			detColdKey.setHbefaComponent( cp );
			detailedHbefaColdTable.put( detColdKey, detColdFactor );
		}
	}
	
}
