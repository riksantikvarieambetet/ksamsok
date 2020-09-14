package se.raa.ksamsok.lucene.exception;

import se.raa.ksamsok.lucene.SamsokProtocol;

public class SamsokProtocolException extends Exception {

	String specificMessage;

	public SamsokProtocolException(String generalMessage, String specificMessage) {
		super(generalMessage);
		this.specificMessage = specificMessage;
	}

	public String getSpecificMessage() {
		return specificMessage;
	}
}
