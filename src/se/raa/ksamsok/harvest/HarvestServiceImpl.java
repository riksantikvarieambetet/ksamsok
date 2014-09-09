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
	Date firstIndexDate;
	boolean alwaysHarvestEverything;
	String shortName;
	boolean paused;

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
		// add seconds and add ? on either week or day
		// #4445 to make cron string readable 
		
		String[] split = cronString.split(" ");
		String modCronString = cronString;
		
		if (split.length == 5) {

			String one = split[0];
			String two = split[1];
			String three = split[2];
			String four = split[3];
			String five = split[4];

			if (three.equals("*") && (five.equals("*"))) {
				modCronString = one + " " + two + " " + three + " " + four + " ?";
			} else if ((three.equals("*")) && (!five.equals("?"))) {
				// om four är * men six inte är ?, sätt four till ?
				modCronString = one + " " + two + " ? " + four + " " + five;
			} else if ((five.equals("*")) && (!three.equals("?"))) {
				// om six är * men four inte är ?, sätt six till ?
				modCronString = one + " " + two + " " + three + " " + four + " ?";
			}
			
			modCronString = "0 " + modCronString;

		}

		this.cronString = modCronString;
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

	public Date getFirstIndexDate() {
		return firstIndexDate;
	}

	public void setFirstIndexDate(Date date) {
		this.firstIndexDate = date;
	}

	public boolean getAlwaysHarvestEverything() {
		return alwaysHarvestEverything;
	}

	public void setAlwaysHarvestEverything(boolean alwaysHarvestEverything) {
		this.alwaysHarvestEverything = alwaysHarvestEverything;
	}

	@Override
	public String getShortName()
	{
		return shortName;
	}

	@Override
	public void setShortName(String shortName)
	{
		this.shortName = shortName;
	}

	@Override
	public boolean getPaused() {
		return paused;
	}

	@Override
	public void setPaused(boolean paused) {
		this.paused = paused;
	}

}
