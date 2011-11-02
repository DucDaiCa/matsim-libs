/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.tnicolai.matsim4opus.matsim4urbansim;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.org.matsim.config.MatsimConfigType;
import playground.tnicolai.matsim4opus.utils.DateUtil;
import playground.tnicolai.matsim4opus.utils.InitMATSimScenario;
import playground.tnicolai.matsim4opus.utils.JAXBUnmaschal;
import playground.tnicolai.matsim4opus.utils.UtilityCollection;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.WorkplaceObject;
import playground.tnicolai.matsim4opus.utils.io.FileCopy;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbansimParcelModel;


/**
 * @author thomas
 *
 */
public class MATSim4UrbanSim {

	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSim.class);

	// MATSim scenario
	ScenarioImpl scenario = null;
	// Benchmarking computation times and hard disc space ... 
	Benchmark benchmark = null;
	// indicates if MATSim run was successful
	static boolean isSuccessfulMATSimRun = Boolean.FALSE;
	
	/**
	 * constructor
	 * 
	 * @param args contains at least a reference to 
	 * 		  MATSim4UrbanSim configuration generated by UrbanSim
	 * 
	 */
	MATSim4UrbanSim(String args[]){
		
		// Stores location of MATSim configuration file
		String matsimConfiFile = (args!= null && args.length>0) ? args[0].trim():null;
		// checks if args parameter contains a valid path
		isValidPath(matsimConfiFile);
		// loading and initializing MATSim config
		MatsimConfigType matsimConfig = unmarschal(matsimConfiFile);
		
		// 
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		if( !(new InitMATSimScenario(scenario, matsimConfig)).init() ){
			log.error("An error occured while initializing MATSim scenario ...");
			System.exit(-1);
		}			
		// init Benchmark as default
		benchmark = new Benchmark();
		// init loader
		ScenarioUtils.loadScenario(scenario);
	}
	
	/**
	 * prepare MATSim for traffic flow simulation ...
	 */
	void runMATSim(){
		log.info("Starting MATSim from Urbansim");	

		// checking for if this is only a test run.MATSim
		// a test run only validates the xml config file by initializing the xml config via the xsd.
		if( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.IS_TEST_RUN).equalsIgnoreCase(Constants.TRUE)){
			log.info("TestRun was successful...");
			return;
		}

		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		NetworkImpl network = scenario.getNetwork();
		modifyNetwork(network);
		cleanNetwork(network);
		
		// get the data from urbansim (parcels and persons)
		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( Integer.parseInt( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.YEAR) ) );
		// read urbansim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
		
		readUrbansimParcelModel(readFromUrbansim, parcels, zones);
		int pc = benchmark.addMeasure("Population construction");
		Population newPopulation = readUrbansimPersons(readFromUrbansim, parcels, network);
		benchmark.stoppMeasurement(pc);
		System.out.println("Population construction took: " + benchmark.getDurationInSeconds( pc ) + " seconds.");
		Map<Id,WorkplaceObject> numberOfWorkplacesPerZone = ReadUrbansimJobs(readFromUrbansim);

		log.info("### DONE with demand generation from urbansim ###") ;

		// set population in scenario
		scenario.setPopulation(newPopulation);
		// scenario.setFacilities(facilities); // tnicolai: suggest to implement method

		runControler(zones, numberOfWorkplacesPerZone, parcels, readFromUrbansim);
		
		if( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BACKUP_RUN_DATA_PARAM).equalsIgnoreCase("TRUE") ){ // tnicolai: Experimental, comment out for MATSim4UrbanSim release
			// saving results from current run
			saveRunOutputs();			
			cleanUrbanSimOutput();
		}
		
	}
	
	/**
	 * read urbansim parcel table and build facilities and zones in MATSim
	 * 
	 * @param readFromUrbansim
	 * @param parcels
	 * @param zones
	 */
	void readUrbansimParcelModel(ReadFromUrbansimParcelModel readFromUrbansim, ActivityFacilitiesImpl parcels, ActivityFacilitiesImpl zones){

		readFromUrbansim.readFacilities(parcels, zones);
		// write the facilities from the UrbanSim parcel model as a compressed locations.xml file into the temporary directory if they are need in following runs
		// new FacilitiesWriter(parcels).write( Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + Constants.OUTPUT_PARCEL_FILE_GZ ); 	// disabled, since output file will not be re-used
		// new FacilitiesWriter(zones).write( Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + Constants.OUTPUT_ZONES_FILE_GZ );		// disabled, since output file will not be re-used
	}
	
	/**
	 * read person table from urbansim and build MATSim population
	 * 
	 * @param readFromUrbansim
	 * @param parcels
	 * @param network
	 * @return
	 */
	Population readUrbansimPersons(ReadFromUrbansimParcelModel readFromUrbansim, ActivityFacilitiesImpl parcels, NetworkImpl network){
		// read urbansim population (these are simply those entities that have the person, home and work ID)
		Population oldPopulation = null;
		if ( scenario.getConfig().plans().getInputFile() != null ) {
			
			String mode = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.MATSIM_MODE);
			if(mode.equals(Constants.HOT_START))
				log.info("MATSim is running in HOT start mode, i.e. MATSim starts with pop file from previous run: " + scenario.getConfig().plans().getInputFile());
			else if(mode.equals(Constants.WARM_START))
				log.info("MATSim is running in WARM start mode, i.e. MATSim starts with pre-existing pop file:" + scenario.getConfig().plans().getInputFile());
			
			log.info("Persons not found in pop file are added; persons no longer in urbansim persons file are removed." ) ;
			
			oldPopulation = scenario.getPopulation() ;
			// log.info("Note that the `continuation of iterations' will only work if you set this up via different config files for") ;
			// log.info("every year and know what you are doing.") ;
		}
		else {
			log.warn("No population specified in matsim config file; assuming COLD start.");
			log.info("(I.e. generate new pop from urbansim files.)" );
			oldPopulation = null;
		}

		// read urbansim persons.  Generates hwh acts as side effect
		Population newPopulation = readFromUrbansim.readPersons( oldPopulation, parcels, network, Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.SAMPLING_RATE)) ) ;
		oldPopulation=null;
		System.gc();
		
		return newPopulation;
	}
	
	/**
	 * Reads in the job table from urbansim that contains for every "job_id" the corresponded "parcel_id_work" and "zone_id_work"
	 * and returns an HashMap with the number of job for each zone.
	 * 
	 * @return HashMap
	 */
	Map<Id,WorkplaceObject> ReadUrbansimJobs(ReadFromUrbansimParcelModel readFromUrbansim){

		return readFromUrbansim.readZoneBasedWorkplaces();
	}
	
	/**
	 * run simulation
	 * @param zones
	 */
	void runControler( ActivityFacilitiesImpl zones, Map<Id,WorkplaceObject> numberOfWorkplacesPerZone, ActivityFacilitiesImpl parcels, 
			ReadFromUrbansimParcelModel readFromUrbansim){
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
		controler.setCreateGraphs(false);	// sets, whether output Graphs are created
		
		// The following lines register what should be done _after_ the iterations were run:
//		controler.addControlerListener( new MATSim4UrbanSimControlerListenerV2( zones, numberOfWorkplacesPerZone, parcels, scenario ) );
		controler.addControlerListener( new MATSim4UrbanSimControlerListenerV3( zones, numberOfWorkplacesPerZone, parcels, scenario ) );
		
		// tnicolai todo?: count number of cars per h on a link
		// write ControlerListener that implements AfterMobsimListener (notifyAfterMobsim)
		// get VolumeLinkAnalyzer by "event.getControler.getVolume... and run getVolumesForLink. that returns an int array with the number of cars per hour on an specific link 
		// see also http://matsim.org/docs/controler
		
		// run the iterations, including the post-processing:
		controler.run() ;
	}
	
	/**
	 * verifying if args argument contains a valid path. 
	 * @param args
	 */
	void isValidPath(String matsimConfiFile){
		// test the path to matsim config xml
		if( matsimConfiFile==null || matsimConfiFile.length() <= 0 || !pathExsits( matsimConfiFile ) ){
			log.error(matsimConfiFile + " is not a valid path to a MATSim configuration file. SHUTDOWN MATSim!");
			System.exit(Constants.NOT_VALID_PATH);
		}
	}
	
	/**
	 * Checks a given path if it exists
	 * @param arg path
	 * @return true if the given file exists
	 */
	boolean pathExsits(String matsimConfigFile){

		if( (new File(matsimConfigFile)).exists() )
			return true;
		return false;
	}
	
	/**
	 * cleaning matsim network
	 * @param network
	 */
	void cleanNetwork(NetworkImpl network){
		log.info("") ;
		log.info("Cleaning network ...");
		( new NetworkCleaner() ).run(network);
		log.info("... finished cleaning network.");
		log.info(""); 
	}
	
	/**
	 * This method allows to modify the MATSim network
	 * This needs to be implemented by another class
	 * 
	 * @param network
	 */
	void modifyNetwork(NetworkImpl network){
		// this is just a stub and does nothing. 
		// This needs to be implemented/overwritten by another class
	}
	
	/**
	 * loading, validating and initializing MATSim config.
	 */
	MatsimConfigType unmarschal(String matsimConfigFile){
		
		// JAXBUnmaschal reads the UrbanSim generated MATSim config, validates it against
		// the current xsd (checks e.g. the presents and data type of parameter) and generates
		// an Java object representing the config file.
		JAXBUnmaschal unmarschal = new JAXBUnmaschal( matsimConfigFile );
		
		MatsimConfigType matsimConfig = null;
		
		// binding the parameter from the MATSim Config into the JaxB data structure
		if( (matsimConfig = unmarschal.unmaschalMATSimConfig()) == null){
			
			log.error("Unmarschalling failed. SHUTDOWN MATSim!");
			System.exit(Constants.UNMARSCHALLING_FAILED);
		}
		return matsimConfig;
	}
	
	/**
	 * Saving UrbanSim and MATSim results for current run in a backup directory ...
	 */
	void saveRunOutputs() {
		log.info("Saving UrbanSim and MATSim outputs ...");
		
		String saveDirectory = "run" + scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.YEAR) + "-" + DateUtil.now();
		String savePath = UtilityCollection.checkPathEnding( Constants.MATSIM_4_OPUS_BACKUP + saveDirectory );
		FileCopy.copyTree(Constants.MATSIM_4_OPUS_TEMP, savePath);
		
		String newPlansFile = Constants.MATSIM_4_OPUS_OUTPUT + Constants.GENERATED_PLANS_FILE_NAME;
		
		// get population / plans file
		try {
			FileCopy.fileCopy( new File(newPlansFile) , new File(savePath + Constants.GENERATED_PLANS_FILE_NAME) );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		log.info("Saving UrbanSim and MATSim outputs done!");
		
		String targetLocationHotStartFile = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.TARGET_LOCATION_HOT_START_PLANS_FILE);
		if(!targetLocationHotStartFile.equals("")){
			
			log.info("Preparing hot start for next MATSim run ...");
			boolean success = FileCopy.moveFileOrDirectory(newPlansFile, targetLocationHotStartFile);
			if(success)
				log.info("Hot start preparation successful!");
			else
				log.error("Error while moving plans file. This is not fatal but hot start will not work!");
		}
	}
	
	/**
	 * This is experimental
	 * Removes UrbanSim output files for MATSim, since they are 
	 * saved by performing saveRunOutputs() in a previous step.
	 */
	void cleanUrbanSimOutput(){
		
		log.info("Cleaning MATSim4Opus temp directory (" + Constants.MATSIM_4_OPUS_TEMP + ") from UrbanSim output." );
		
		ArrayList<File> fileNames = FileCopy.listAllFiles(new File(Constants.MATSIM_4_OPUS_TEMP), Boolean.FALSE);
		Iterator<File> fileNameIterator = fileNames.iterator();
		while(fileNameIterator.hasNext()){
			File f = fileNameIterator.next();
			try {
				if(f.getCanonicalPath().endsWith(".tab") || f.getCanonicalPath().endsWith(".meta")){
					log.info("Removing " + f.getCanonicalPath());
					f.delete();
				}
			} catch (IOException e) {
				e.printStackTrace();
				log.info("While removing UrbanSim output an IO error occured. This is not critical.");
			}
		}
		log.info("... done!");
	}
	
	/**
	 * Entry point
	 * @param args urbansim command prompt
	 */
	public static void main(String args[]){
		MATSim4UrbanSim m4u = new MATSim4UrbanSim(args);
		m4u.runMATSim();
		MATSim4UrbanSim.isSuccessfulMATSimRun = Boolean.TRUE;
	}
	
	/**
	 * this method is only called/needed by "matsim4opus.matsim.MATSim4UrbanSimTest"
	 * @return true if run was successful
	 */
	public static boolean getRunStatus(){
		return MATSim4UrbanSim.isSuccessfulMATSimRun;
	}
}



