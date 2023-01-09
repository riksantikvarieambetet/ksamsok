package se.raa.ksamsok.resolve;

import java.util.ArrayList;

class PreparedResponse {

	private String response;
	private ArrayList<String> replacedByUris = new ArrayList<>();
	private boolean gone = false;


	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public ArrayList<String> getReplacedByUris() {
		return replacedByUris;
	}

	public void setReplacedByUris(ArrayList<String> replacedByUris) {
		this.replacedByUris = replacedByUris;
	}

	public void addReplacedByUri(String replaceUri) {
		replacedByUris.add(replaceUri);
	}

	public void setGone(boolean gone) {
		this.gone=gone;
	}

	public boolean isGone() {
		return gone;
	}
}
