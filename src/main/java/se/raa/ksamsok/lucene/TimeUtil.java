package se.raa.ksamsok.lucene;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Date;

import static se.raa.ksamsok.lucene.ContentHelper.IX_CENTURY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_DECADE;

public class TimeUtil {

	private static final Logger logger = LogManager.getLogger(TimeUtil.class);

	// iso8601-datumparser, trådsäker
	private static final DateTimeFormatter isoDateTimeFormatter = ISODateTimeFormat.dateOptionalTimeParser();

	// konstanter för century och decade
	// från vilken tidpunkt ska vi skapa särskilda index för århundraden och årtionden
	static final Integer century_start = -1999;
	// till vilken tidpunkt ska vi skapa särskilda index för århundraden och årtionden
	static final Integer century_stop = 2010;
	// sträng med årtal som måste vara före century_start
	static final String old_times = "-9999";

	static String parseYearFromISO8601DateAndTransform(String value) {
		String result = null;
		try {
			// tills vidare godkänner vi också "x f.kr" och "y e.kr" skiftlägesokänsligt här
			value = value.toLowerCase();
			int dotkrpos = value.indexOf(".kr");
			int year;
			if (dotkrpos > 0) {
				year = Integer.parseInt(value.substring(0, dotkrpos - 1).trim());
				boolean canBeNegative = false;
				final char character = value.charAt(dotkrpos - 1);
				switch (character) {
				case 'f':
					// TODO: Nu så tar vi * f.kr år och tar detta -1.
					// egentligen vill vi låta det vara som det är om året explicit inte är 0.
					// i humaniora så avser 100 f.kr 100 f.kr och inte 99 f.kr.

					// vi godtar inte 0 f.Kr (eller negativa år före kristus)
					if (year > 0 ) {
						// TODO: fundera på om 1 före kristus verkligen ska vara år 0 - år 0 finns väl inte?
						year = -year + 1; // 1 fkr är år 0
						canBeNegative = true;
					} else {
						throw new NumberFormatException();
					}
					// obs - inget break
				case 'e':
					// TODO: "numerfixen" görs numera på solr-sidan, ändra returtyp?
					// här måste året vara icke-negativt (men 0 är ok)
					if (canBeNegative || year >= 0) {
						result = String.valueOf(year);
					}
					//result = transformNumberToLuceneString(year);
					break;
					default:
						// måste vara f eller e för att sätta result
						logger.warn("Only f or e is handled in parseYearFromISO8601DateAndTransform, but found " + character);
				}
			} else {
				DateTime dateTime = isoDateTimeFormatter.parseDateTime(value);
				year = dateTime.getYear();
				// TODO: "numerfixen" görs numera på solr-sidan, ändra returtyp?
				result = String.valueOf(year);
				//result = transformNumberToLuceneString(year);
			}
			
		} catch (Exception ignore) {
			// läggs till som "problem" i addToDoc om denna metod returnerar null
		}
		return result;
	}

	// parse av iso8601-datum
	static Date parseISO8601Date(String dateStr) {
		Date date = null;
		if (dateStr != null) {
			try {
				DateTime dateTime = isoDateTimeFormatter.parseDateTime(dateStr);
				date = dateTime.toDate();
			} catch (Exception ignore) {
			}
		}
		return date;
	}

	// tolkar och indexerar ett iso-datum som yyyy-mm-dd om det inte ligger i framtiden
	static Date parseAndIndexISO8601DateAsDate(String identifier, String index, String dateStr, IndexProcessor ip) {
		Date date = parseISO8601Date(dateStr);
		if (date != null) {
			if (date.after(new Date())) {
				ContentHelper.addProblemMessage("The date in '" + index + "' for " + identifier  + " is in the future: " + dateStr);
			} else {
				ip.setCurrent(index);
				ip.addToDoc(ContentHelper.formatDate(date, false));
			}
		} else {
			// TODO: vill man ha med identifier i varningmeddelandet? Kan dock bli
			//       många loggrader då detta verkar vara ett vanligt fel
			ContentHelper.addProblemMessage("Could not interpret '" + index + "' as ISO8601: " + dateStr);
		}
		return date;
	}

	static void expandDecadeAndCentury(String fromTime, String toTime,
			String[] contextTypePrefixes, IndexProcessor ip) {
		// timeInfoExists, decade och century
		if (fromTime != null || toTime != null) {
			// bara då vi ska skapa århundraden och årtionden
			Integer start=century_start, stop=century_stop;
			// start=senaste av -2000 och fromTime, om fromTime==null så används -2000
			// stop= tidigaste av 2010 och toTime, om toTime==null så används 2010
			String myFromTime=null, myToTime=null;
			if (fromTime!=null) myFromTime = fromTime;
			if (toTime!=null) myToTime = toTime;

			// om bara ena värdet finns så är det en tidpunkt, inte ett tidsintervall
			if (myFromTime==null) {
				myFromTime=myToTime;
			}
			if (myToTime==null) {
				myToTime=myFromTime;
			}
			
			myFromTime=tidyTimeString(myFromTime);
			start=latest(myFromTime, start);

			myToTime=tidyTimeString(myToTime);
			stop=earliest(myToTime, stop);
				
			Integer runner=start;
			Integer insideRunner=0;
			String dTimeValue=decadeString(runner);
			String cTimeValue=centuryString(runner);

			while (runner<=stop) {
				dTimeValue=decadeString(runner);
				ip.setCurrent(IX_DECADE, contextTypePrefixes);
				ip.addToDoc(dTimeValue);
				if (insideRunner%100==0){
					cTimeValue=centuryString(runner);
					ip.setCurrent(IX_CENTURY, contextTypePrefixes);
					ip.addToDoc(cTimeValue);
				}
				runner+=10;
				insideRunner+=10;
			}
			//slutvillkorskontroller
			if (!dTimeValue.equals(decadeString(stop)) && stop>century_start) {
				dTimeValue=decadeString(stop);
				ip.setCurrent(IX_DECADE, contextTypePrefixes);
				ip.addToDoc(dTimeValue);
			}
			if (!cTimeValue.equals(centuryString(stop)) && stop>century_start) {
				cTimeValue=centuryString(stop);
				ip.setCurrent(IX_CENTURY, contextTypePrefixes);
				ip.addToDoc(cTimeValue);
			}
		}
	}

	static Integer latest(String aString, Integer aInteger) {
		Integer sLatest;
		try {
			sLatest=Math.max(Integer.parseInt(aString),aInteger);
		} catch (Exception e) {
			logger.error("Fel i fromTime: " + aString + " längd: " + aString.length() + " : " + e.getMessage());
			throw e;
		}
		return sLatest;
	}

	static Integer earliest(String aString, Integer aInteger) {
		Integer sEarliest;
		try {
			sEarliest=Math.min(Integer.parseInt(aString),aInteger);
		} catch (Exception e) {
			logger.error("Fel i toTime: " + aString + " längd: " + aString.length() + " : " + e.getMessage());
			throw e;
		}
		return sEarliest;
	}

	static String decadeString(Integer aInteger) {
		Integer decadeFloor=(aInteger/10)*10;
		if (aInteger < 0) {
			decadeFloor -= 10;
		}
        return String.valueOf(decadeFloor);
	}

	static String centuryString(Integer aInteger) {
		Integer centuryFloor=(aInteger/100)*100;
		if (aInteger < 0) {
			centuryFloor -= 100;
		}
        return String.valueOf(centuryFloor);
	}
	
	static String tidyTimeString(String aString) {
		String timeString=aString;
		try {
			if ((timeString.length()>5) && timeString.startsWith("-")) {
				// troligen årtal före -10000
				timeString=old_times;
			}
			//else if (timeString.indexOf("-"==5)) {
			//	timeString=timeString.substring(0, 4);
			// (innefattas av nästa case)
			//}
			else if (timeString.length()>4 && !timeString.startsWith("-")) {
				timeString=timeString.substring(0, 4);
			}
		}
		catch (Exception e) {
			logger.error("Fel i tidyTimeString: " + timeString + " : " + e.getMessage());
			throw e;
		}
		return timeString.trim();
	}

}
