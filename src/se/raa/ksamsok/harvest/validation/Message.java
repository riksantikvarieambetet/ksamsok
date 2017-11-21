package se.raa.ksamsok.harvest.validation;

/**
 * Container for message and its information
 * Should not be used in hashmap/hashtable
 * @author Martin Duveborg
 */
public class Message{
	public String messageText = "";
	public int firstOccuranceRow;
	public int firstOccuranceCol;
	public int totalOccurances = 1;
	
	// för att kunna använda contains()
	public boolean equals(Object o){	
		if(o instanceof Message){
			Message m = (Message)o;
			return m.messageText.equals(this.messageText);
		}
		return false;
	}
	
	
	public boolean showAdditionalInformation(){
		return firstOccuranceCol > 0 || firstOccuranceRow > 0 || totalOccurances > 1;
	}

	public int hashCode() {
		return 42; // any arbitrary constant will do
	}
}