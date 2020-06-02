import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;

import com.rti.dds.domain.*;
import com.rti.dds.infrastructure.*;
import com.rti.dds.subscription.*;
import com.rti.dds.topic.*;
import com.rti.ndds.config.*;

public class PassengerSubscriber {

	public static String targetBus;
	public static String targetRoute;
	public static String targetStop;
	public static int currentStop;
	public static int destinationStop;
	public static String currentRoute;
	public static String currentBus = "";
	public static Boolean onBus = false;

	public static void main(String[] args) {

		int domainId = 0;
		int sampleCount = 0;

		if (args.length > 0) {
			System.out.println(args[0]);
			System.out.println(args[1]);
			System.out.println(args[2]);
			currentRoute = args[0];
			currentStop = Integer.parseInt(args[1]);
			destinationStop = Integer.parseInt(args[2]);
		} else {
			currentStop = 3;
			currentRoute = "Express2";
			destinationStop = 2;
		}

		System.out.println("Starting PassengerSubscriber...");
		subscriberMain(domainId, sampleCount);
	}

	private static void subscriberMain(int domainId, int sampleCount) {

		DomainParticipant participant = null;
		Subscriber subscriber = null;
		Topic accidentTopic = null;
		Topic positionTopic = null;
		DataReaderListener listener = null;
		AccidentDataReader accidentReader = null;
		PositionDataReader positionReader = null;

		try {

			// create participant
			participant = DomainParticipantFactory.TheParticipantFactory.create_participant(domainId,
					DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT, null /* listener */, StatusKind.STATUS_MASK_NONE);
			if (participant == null) {
				System.err.println("create_participant error\n");
				return;
			}

			// create subscriber
			subscriber = participant.create_subscriber(DomainParticipant.SUBSCRIBER_QOS_DEFAULT, null /* listener */,
					StatusKind.STATUS_MASK_NONE);
			if (subscriber == null) {
				System.err.println("create_subscriber error\n");
				return;
			}

			// create topic
			/* Register type before creating topic */
			String accidentTypeName = AccidentTypeSupport.get_type_name();
			AccidentTypeSupport.register_type(participant, accidentTypeName);
			String positionTypeName = PositionTypeSupport.get_type_name();
			PositionTypeSupport.register_type(participant, positionTypeName);

			accidentTopic = participant.create_topic("CPTS464 SBOGGAVA ACC", accidentTypeName,
					DomainParticipant.TOPIC_QOS_DEFAULT, null /* listener */, StatusKind.STATUS_MASK_NONE);
			if (accidentTopic == null) {
				System.err.println("create_topic error\n");
				return;
			}
			positionTopic = participant.create_topic("CPTS464 SBOGGAVA POS", positionTypeName,
					DomainParticipant.TOPIC_QOS_DEFAULT, null /* listener */, StatusKind.STATUS_MASK_NONE);
			if (positionTopic == null) {
				System.err.println("create_topic error\n");
				return;
			}

			// create reader
			listener = new AccidentListener();
			accidentReader = (AccidentDataReader) subscriber.create_datareader(accidentTopic,
					Subscriber.DATAREADER_QOS_DEFAULT, listener, StatusKind.STATUS_MASK_ALL);
			if (accidentReader == null) {
				System.err.println("create_datareader accident error\n");
				return;
			}
			positionReader = (PositionDataReader) subscriber.create_datareader(positionTopic,
					Subscriber.DATAREADER_QOS_DEFAULT, listener, StatusKind.STATUS_MASK_ALL);
			if (positionReader == null) {
				System.err.println("create_datareader accident error\n");
				return;
			}

			final long receivePeriodSec = 4;
			for (int count = 0; (sampleCount == 0) || (count < sampleCount); ++count) {
				try {
					Thread.sleep(receivePeriodSec * 1000); // in millisec
				} catch (InterruptedException ix) {
					System.err.println("INTERRUPTED");
					break;
				}
			}

		} finally {
			// shutdown
			if (participant != null) {
				participant.delete_contained_entities();
				DomainParticipantFactory.TheParticipantFactory.delete_participant(participant);
			}
		}
	}

	// calculating stops left
	// there is a better way to do this, submission deadline was very close
	private static int stopsLeftCounter(int dest, int curr) {
		if(dest - curr > 0) {
			return dest - curr;
		} else {
			if(dest - curr < 0) {
				if(dest + 1 == curr) {
					return 5;
				}
				if(dest + 2 == curr) {
					return 4;
				}
				if (dest + 3 ==curr) {
					return 3;
				}
				if(dest + 4 == curr) {
					return 2;
				}
				if(dest + 5 == curr) {
					return 1;
				}
			}
		}
		return 0;
	}


	private static class AccidentListener extends DataReaderAdapter {

		AccidentSeq _dataSeq = new AccidentSeq();
		SampleInfoSeq _infoSeq = new SampleInfoSeq();
		PositionSeq _dataSeqPosition = new PositionSeq();
		int flag = 0;
		int countToDestination = 9999;
		int stopsLeft = 99999;
		String accidentBus = "";
		public void on_data_available(DataReader reader) {
			flag = 0;
			AccidentDataReader AccidentReader = null;
			PositionDataReader Position_reader = null;
			if (reader.get_topicdescription().get_name().contains("POS")) {
				try {
				Position_reader = (PositionDataReader) reader;
				Position_reader.take(_dataSeqPosition, _infoSeq,
				ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
				SampleStateKind.ANY_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE,
				InstanceStateKind.ANY_INSTANCE_STATE);

				for (int i = 0; i < _dataSeqPosition.size(); ++i) {
					SampleInfo info = (SampleInfo) _infoSeq.get(i);

					if (info.valid_data) {
						Position p = _dataSeqPosition.get(i);
						
						if(onBus == false) {
							if(p.stopNumber == currentStop && p.route.equalsIgnoreCase(currentRoute) && stopsLeftCounter(destinationStop, p.stopNumber) > 0 &&!p.vehicle.equalsIgnoreCase(accidentBus)) {	
								currentBus = p.vehicle;
								countToDestination = Math.abs(p.numStops - p.stopNumber);
								System.out.println("Getting on bus " + p.vehicle + " at stop " + p.stopNumber + " at " + p.timestamp + ", " + p.trafficConditions + " " + stopsLeftCounter(destinationStop, p.stopNumber) + " stops left");
								onBus = true;
								currentStop = p.stopNumber;
							}
						} else {
							if (currentBus.length() > 0 && currentBus.equalsIgnoreCase(p.vehicle)) {
								System.out.println("Passenger on bus " + p.vehicle);
								 countToDestination = Math.abs(p.numStops - currentStop);
								 stopsLeft = stopsLeftCounter(destinationStop, p.stopNumber);
								System.out.println(p.vehicle + " arriving at stop " + p.stopNumber + ", " + p.trafficConditions+", stops left " + stopsLeftCounter(destinationStop, p.stopNumber));
								if(destinationStop == p.stopNumber) {
									System.out.println("Passenger arrived at destination stop " + p.stopNumber + " at " + p.timestamp);
									System.exit(0);
								}
							}
						}
						 flag = 1;
					}
				}
				} catch (RETCODE_NO_DATA noData) {
				// No data to process
				System.out.println("No data error");
				}
			} else if (reader.get_topicdescription().get_name().contains("ACC")){
				AccidentReader = (AccidentDataReader) reader;

				try {
					AccidentReader.take(_dataSeq, _infoSeq,
					ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
					SampleStateKind.ANY_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE,
					InstanceStateKind.ANY_INSTANCE_STATE);

					for (int i = 0; i < _dataSeq.size(); ++i) {
						SampleInfo info = (SampleInfo) _infoSeq.get(i);

						if (info.valid_data) {
							Accident a = _dataSeq.get(i);
							if( currentBus.length() > 0 && currentBus.equalsIgnoreCase(a.vehicle) && a.stopNumber != destinationStop) {
								System.out.println("accident with current bus " + a.vehicle + " accident at stop: " + a.stopNumber);
								System.out.println("Getting off bus " + a.vehicle + " at stop " + a.stopNumber + " at " + a.timestamp);
								currentStop = a.stopNumber;
								onBus = false;
								accidentBus = a.vehicle;
							} 
							flag = 0;
						}
					}
				} catch (RETCODE_NO_DATA noData) {
				// No data to process
				}
			}

			if (flag == 1) {
				Position_reader.return_loan(_dataSeqPosition, _infoSeq);
			} else {
				AccidentReader.return_loan(_dataSeq, _infoSeq);
			}
			}
		}

}
