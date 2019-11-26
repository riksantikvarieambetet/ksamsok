package se.raa.ksamsok.resolve;

import java.util.ArrayList;

class PreparedResponse {

	private String response;
	private ArrayList<String> replaceUris = new ArrayList<>();


	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public ArrayList<String> getReplaceUris() {
		return replaceUris;
	}

	public void setReplaceUris(ArrayList<String> replaceUris) {
		this.replaceUris = replaceUris;
	}

	public void addReplaceUri(String replaceUri) {
		replaceUris.add(replaceUri);
	}
}
