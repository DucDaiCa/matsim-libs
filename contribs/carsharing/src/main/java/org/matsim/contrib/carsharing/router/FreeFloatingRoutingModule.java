package org.matsim.contrib.carsharing.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class FreeFloatingRoutingModule implements RoutingModule {
	
	public FreeFloatingRoutingModule() {		
		
	}
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person, Attributes tripAttributes) {
		
		final List<PlanElement> trip = new ArrayList<>();
						
		final Leg leg1 = PopulationUtils.createLeg("freefloating");
		CarsharingRoute route1 = new CarsharingRoute(fromFacility.getLinkId(), toFacility.getLinkId());
		leg1.setRoute(route1);
		trip.add( leg1 );	
						
		return trip;
	}	
	
}
