import java.util.*;
public class Conditions {

	 enum TrafficCondition {
		 LIGHT, NORMAL, HEAVY
	 }

	 public TrafficCondition getTrafficCondition() {
		 int randomNum = new Random().nextInt(100);
		 if(randomNum >= 0 && randomNum <=10) {
			 return TrafficCondition.HEAVY;
		 } else if (randomNum >10 && randomNum <= 35) {
			 return TrafficCondition.LIGHT;
		 } else {
			 return TrafficCondition.NORMAL;
		 }	 
	 }
	
	 public boolean hasAccidentHappen() {
		 int randomNum = new Random().nextInt(100);
		 if(randomNum <= 10) {
			 return true;
		 }
		 return false;
	 }
}
