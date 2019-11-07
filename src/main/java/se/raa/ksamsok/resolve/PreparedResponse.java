package se.raa.ksamsok.resolve;

import java.util.ArrayList;

class PreparedResponse {

	private String response;
	private ArrayList<String> replaceUris = new ArrayList<>();
	private boolean gone = false;


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

	public void setGone(boolean gone) {
		this.gone=gone;
	}

	public boolean isGone() {
		return gone;
	}
}
