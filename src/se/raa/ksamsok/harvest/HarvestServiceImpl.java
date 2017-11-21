package se.raa.ksamsok.harvest;

import java.util.Date;

public class HarvestServiceImpl implements HarvestService {

	private String id;
	private String serviceType;
	private String name;
	private String cronString;
	private String harvestURL;
	private String harvestSetSpec;
	private Date lastHarvestDate;
	private Date firstIndexDate;
	private boolean alwaysHarvestEverything;
	private String shortName;
	private boolean paused;

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
		if (lastHarvestDate != null) {
			return new Date(lastHarvestDate.getTime());
		} else {
			return null;
		}
	}

	public void setLastHarvestDate(Date lastHarvestDate) {
		if (lastHarvestDate != null) {
			this.lastHarvestDate = new Date(lastHarvestDate.getTime());
		} else {
			this.lastHarvestDate = null;
		}
	}

	public Date getFirstIndexDate() {
		if (firstIndexDate != null) {
			return new Date(firstIndexDate.getTime());
		} else {
			return null;
		}
	}

	public void setFirstIndexDate(Date date) {
		if (date != null) {
			this.firstIndexDate = new Date(date.getTime());
		} else {
			this.firstIndexDate = null;
		}
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
