
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Arrays;

import com.rti.dds.domain.*;
import com.rti.dds.infrastructure.*;
import com.rti.dds.subscription.*;
import com.rti.dds.topic.*;
import com.rti.ndds.config.*;

// ===========================================================================

public class OperatorSubscriber {
	// -----------------------------------------------------------------------
	// Public Methods
	// -----------------------------------------------------------------------

	public static void main(String[] args) {
		// --- Get domain ID --- //
		int domainId = 0;
		if (args.length >= 1) {
			domainId = Integer.valueOf(args[0]).intValue();
		}

		// -- Get max loop count; 0 means infinite loop --- //
		int sampleCount = 0;
		if (args.length >= 2) {
			sampleCount = Integer.valueOf(args[1]).intValue();
		}

		subscriberMain(domainId, sampleCount);
	}

	private OperatorSubscriber() {
		super();
	}


	private static void subscriberMain(int domainId, int sampleCount) {

		DomainParticipant participant = null;
		Subscriber subscriber = null;
		Topic topic = null;
		Topic positionTopic = null;
		DataReaderListener listener = null;
		AccidentDataReader readerAccident = null;
		PositionDataReader readerPosition = null;

		try {

			// --- Create participant --- //
			participant = DomainParticipantFactory.TheParticipantFactory.create_participant(domainId,
					DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT, null /* listener */, StatusKind.STATUS_MASK_NONE);
			if (participant == null) {
				System.err.println("create_participant error\n");
				return;
			}

			// --- Create subscriber --- //
			subscriber = participant.create_subscriber(DomainParticipant.SUBSCRIBER_QOS_DEFAULT, null /* listener */,
					StatusKind.STATUS_MASK_NONE);
			if (subscriber == null) {
				System.err.println("create_subscriber error\n");
				return;
			}

			// --- Create topic --- //

			/* Register type before creating topic */
			String typeName = AccidentTypeSupport.get_type_name();
			AccidentTypeSupport.register_type(participant, typeName);

			String typeName1 = PositionTypeSupport.get_type_name();
			PositionTypeSupport.register_type(participant, typeName1);

			topic = participant.create_topic("CPTS464 SBOGGVA ACC", typeName, DomainParticipant.TOPIC_QOS_DEFAULT,
					null /* listener */, StatusKind.STATUS_MASK_NONE);
			if (topic == null) {
				System.err.println("create_topic error\n");
				return;
			}

			positionTopic = participant.create_topic("CPTS464 SBOGGAVA POS", typeName1, DomainParticipant.TOPIC_QOS_DEFAULT,
					null /* listener */, StatusKind.STATUS_MASK_NONE);
			if (positionTopic == null) {
				System.err.println("create_topic error\n");
				return;
			}

			// --- Create readerAccident --- //
			listener = new AccidentListener();

			readerAccident = (AccidentDataReader) subscriber.create_datareader(topic, Subscriber.DATAREADER_QOS_DEFAULT,
					listener, StatusKind.STATUS_MASK_ALL);
			if (readerAccident == null) {
				System.err.println("create_datareader error\n");
				return;
			}

			readerPosition = (PositionDataReader) subscriber.create_datareader(positionTopic, Subscriber.DATAREADER_QOS_DEFAULT,
					listener, StatusKind.STATUS_MASK_ALL);
			if (readerPosition == null) {
				System.err.println("create_datareader error\n");
				return;
			}

			// --- Wait for data --- //
            System.out.format("%10s %10s %10s %10s %10s %10s %10s %10s %10s %5s %10s %10s %10s", "Message Type\t", "|", "Route", "|",
            "Vehicle", "|" ,"Stops", "|", "TimeBetweenStops", "|", "Fill", "|", "Timestamp");
            
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

			// --- Shutdown --- //

			if (participant != null) {
				participant.delete_contained_entities();

				DomainParticipantFactory.TheParticipantFactory.delete_participant(participant);
			}
		}
	}

	private static class AccidentListener extends DataReaderAdapter {

		AccidentSeq _dataSeq = new AccidentSeq();
		SampleInfoSeq _infoSeq = new SampleInfoSeq();
		PositionSeq pdata = new PositionSeq();
		int flag = 0;

		public void on_data_available(DataReader reader) {
			AccidentDataReader AccidentReader = null;
            PositionDataReader Position_reader = null;

			if (reader.get_topicdescription().get_name().contains("POS")) {
				try {
					Position_reader = (PositionDataReader) reader;
					Position_reader.take(pdata, _infoSeq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
							SampleStateKind.ANY_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE,
							InstanceStateKind.ANY_INSTANCE_STATE);

					
					for (int i = 0; i < pdata.size(); ++i) {
						SampleInfo info = (SampleInfo) _infoSeq.get(i);

						if (info.valid_data) {
							Position p = pdata.get(i);
                            System.out.println("");
            System.out.format("%10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s", "Position\t", "|", p.route, "|",
            p.vehicle, "|" , p.stopNumber, "|", p.timeBetweenStops, "|", p.fillInRatio, "|", p.timestamp);
                            System.out.println("");
							flag = 1;

						}
					}
				} catch (RETCODE_NO_DATA noData) {
					// No data to process
					System.out.println("Error");
				}
			} else {
				AccidentReader = (AccidentDataReader) reader;

				try {
					AccidentReader.take(_dataSeq, _infoSeq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
							SampleStateKind.ANY_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE,
							InstanceStateKind.ANY_INSTANCE_STATE);

					for (int i = 0; i < _dataSeq.size(); ++i) {
						SampleInfo info = (SampleInfo) _infoSeq.get(i);

						if (info.valid_data) {

							Accident a = _dataSeq.get(i);
		
                            System.out.println("");
                            System.out.format("%10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s", "Accident", "|", a.route, "|",
                            a.vehicle, "|" , a.stopNumber, "|","----", "|", "----", "|", a.timestamp);
                                            System.out.println("");
							flag = 0;

						}
					}
				} catch (RETCODE_NO_DATA noData) {
					// No data to process
				}
			}

        //    System.out.println(reader.get_topicdescription().get_name());

			if (flag == 1) {
				Position_reader.return_loan(pdata, _infoSeq);
			} else {
				AccidentReader.return_loan(_dataSeq, _infoSeq);
			}
		}
	}
}