# CPTS464: Distributed Systems

## Simplified public transist system using RTI-DDS middleware.

### Problem Description:

#### In this simplified public transport system, suppose we only have one kind of vehicle (bus) in one city. This system is composed of two independent routes. Each route has the following attributes: 
- **Route name**: This is unique across the transport system.
- **Number of stops**: This is the total number of stops along the route; stops are numbered starting form number 1. Every route is a closed path so that bus can move to the first stop after is reached the last stop. 
- **Time between stops**: This is the number of seconds it takes for a vehicle to move from one stop to the next; we assume that all the stops on a given route are evenly spaced out along that route.
- A route can accommodate a number of vehicles; each vehicle is identified by a name that is unique route-wide. All vehicles can contain up to 100 passengers.

Vehicles move along the route in a one-way fashion from the first to the last stop according to the “time between stops” attribute of the route. When a vehicle reaches the last stop, it then moves to the first stop and goes along the route again. It is removed from the system after it goes through the route 3 times. The only exceptions to this behavior are:
- The breakdown of a vehicle, which causes its temporary removal from the system, and a backup vehicle for the same route (if available) to be added to the route 15 seconds after the breakdown was detected, moving from the stop where breakdown happened. 

- An accident found along the route, which delays the vehicle by a fixed amount of time (10 seconds).

- Light traffic conditions, which dynamically decrease the time between stops by a 25% factor.

- Heavy traffic conditions, which dynamically increase the time between stops by a 50% factor.

When a vehicle reaches a stop, the following information is collected:
- Timestamp (hh:mm:ss)
- Vehicle fill-in ratio (0 to 100)
- Traffic conditions (normal, light, heavy)
- Detection of any accidents

Several attribute values in the simulated transport system are randomly generated according to the following distribution of probability:
- Accidents happen 10% of the time
- Traffic is heavy 10% of the time, light 25% of the time, and normal for the remaining 65% of the time
- The fill-in ratio is a completely random non-negative integer less than or equal to 100.

 
 ### Problem: 
  - For some reason the operator data is not read correctly
