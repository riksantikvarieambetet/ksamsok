package se.raa.ksamsok.harvest;

import java.util.Date;

public class HarvestServiceImpl implements HarvestService {

	String id;
	String serviceType;
	String name;
	String cronString;
	String harvestURL;
	String harvestSetSpec;
	Date lastHarvestDate;
	boolean alwaysHarvestEverything;

	public HarvestServiceImpl() {}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCronString() {
		return cronString;
	}

	public void setCronString(String cronString) {
		this.cronString = cronString;
	}

	public String getHarvestURL() {
		return harvestURL;
	}

	public void setHarvestURL(String harvestURL) {
		this.harvestURL = harvestURL;
	}

	public String getHarvestSetSpec() {
		return harvestSetSpec;
	}

	public void setHarvestSetSpec(String setSpec) {
		this.harvestSetSpec = setSpec;
	}

	public Date getLastHarvestDate() {
		return lastHarvestDate;
	}

	public void setLastHarvestDate(Date lastHarvestDate) {
		this.lastHarvestDate = lastHarvestDate;
	}


	public boolean getAlwaysHarvestEverything() {
		return alwaysHarvestEverything;
	}

	public void setAlwaysHarvestEverything(boolean alwaysHarvestEverything) {
		this.alwaysHarvestEverything = alwaysHarvestEverything;
	}

}
