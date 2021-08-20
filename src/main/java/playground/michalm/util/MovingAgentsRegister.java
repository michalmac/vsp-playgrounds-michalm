/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.michalm.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.collections.Tuple;

public class MovingAgentsRegister
		implements EventHandler, PersonDepartureEventHandler, PersonStuckEventHandler, PersonArrivalEventHandler {
	private final Map<Id<Person>, PersonDepartureEvent> movingAgentsMap = new LinkedHashMap<>();
	private final Map<Id<Person>, Tuple<PersonDepartureEvent, PersonStuckEvent>> stuckAgentsMap = new LinkedHashMap<>();

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getPersonId().toString().equals("0052514")) {
			System.err.println(event);
		}
		movingAgentsMap.put(event.getPersonId(), event);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getPersonId().toString().equals("0052514")) {
			System.err.println(event);
		}
		movingAgentsMap.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		if (event.getPersonId().toString().equals("0052514")) {
			System.err.println(event);
		}
		var departureEvent = movingAgentsMap.remove(event.getPersonId());
		stuckAgentsMap.put(event.getPersonId(), new Tuple<>(departureEvent, event));
	}

	public Map<Id<Person>, PersonDepartureEvent> getMovingAgents() {
		return movingAgentsMap;
	}

	public Map<Id<Person>, Tuple<PersonDepartureEvent, PersonStuckEvent>> getStuckAgents() {
		return stuckAgentsMap;
	}

	@Override
	public void reset(int iteration) {
		movingAgentsMap.clear();
	}

	public static final MovingAgentsRegister MOVING_AGENTS_REGISTER = new MovingAgentsRegister();

	public static AbstractModule createModule() {
		return new AbstractModule() {
			public void install() {
				addEventHandlerBinding().toInstance(MOVING_AGENTS_REGISTER);
			}
		};
	}
}
