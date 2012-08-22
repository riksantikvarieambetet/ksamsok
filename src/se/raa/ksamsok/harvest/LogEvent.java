package se.raa.ksamsok.harvest;

/**
 * Enkel böna som representerar en logghändelse.
 */
public class LogEvent {

	// koder för meddelanden som sparas i db
	public static final int EVENT_INFO = 0;
	public static final int EVENT_ERROR = 1;
	public static final int EVENT_WARNING = 2;

	private String serviceId;
	private int eventType;
	private String eventTime;
	private String message;

	/**
	 * Skapa ny instans.
	 * @param serviceId tjänste-id
	 * @param eventType händelsetyp
	 * @param eventTime händelsetid i formaterat strängformat
	 * @param message meddelandetext
	 */
	public LogEvent(String serviceId, int eventType, String eventTime, String message) {
		this.serviceId = serviceId;
		this.eventType = eventType;
		this.eventTime = eventTime;
		this.message = message;
	}

	/**
	 * Getter för tjänste-id
	 * @return tjänste-id
	 */
	public String getServiceId() {
		return serviceId;
	}

	/**
	 * Getter för händelsetyp.
	 * @return händelsetyp.
	 */
	public int getEventType() {
		return eventType;
	}

	/**
	 * Getter för händelsetid.
	 * @return händelsetid
	 */
	public String getEventTime() {
		return eventTime;
	}

	/**
	 * Getter för meddelande.
	 * @return loggmeddelande
	 */
	public String getMessage() {
		return message;
	}
}
