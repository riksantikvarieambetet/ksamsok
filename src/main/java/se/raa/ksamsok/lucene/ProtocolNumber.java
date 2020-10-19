package se.raa.ksamsok.lucene;

import se.raa.ksamsok.api.exception.BadParameterException;

import java.util.ArrayList;

public class ProtocolNumber implements Comparable<ProtocolNumber> {
	ArrayList<Integer> numberSequence = new ArrayList<>();
	String protocolNumberString;

	protected ProtocolNumber(String protocolNumberString) throws BadParameterException {
		if (protocolNumberString == null) {
			throw new  BadParameterException("Protokollversion får inte vara tomt", "ProtocolNumber", protocolNumberString, true);
		}
		if (!protocolNumberString.contains(".")) {
			throw new BadParameterException("Protokollversion måste innehålla minst en punkt", "ProtocolNumber", protocolNumberString, true);
		}
		String[] splitProtocol = protocolNumberString.split("\\.");
		for (String str : splitProtocol) {
			Integer integer;
			try {
				integer = Integer.valueOf(str);
			} catch (NumberFormatException e) {
				throw new BadParameterException("Protokollversion måste vara numeriskt", "ProtocolNumber", protocolNumberString, true);
			}
			numberSequence.add(integer);
		}
		this.protocolNumberString = protocolNumberString;
	}

	public String toString() {
		return protocolNumberString;
	}

	@Override
	public int compareTo(ProtocolNumber that) {
		int cmp = 0;
		int i = 0;
		while (cmp == 0) {
			Integer thisInt = null;
			Integer thatInt = null;
			if (this.numberSequence.size() > i) {
				thisInt = this.numberSequence.get(i);
			}
			if (that.numberSequence.size() > i) {
				thatInt = that.numberSequence.get(i++);
			}
			if (thisInt != null && thatInt != null) {
				// they both have numbers, compare them!
				cmp = thisInt - thatInt;
			} else if (thisInt != null && thisInt > 0) {
				// that doesn't have a number here, so this is bigger
				cmp = 1;
			} else if (thatInt != null && thatInt > 0) {
				// this doesn't have a number here, so that is bigger
				cmp = -1;
			}
			if (cmp == 0 && (thisInt == null || thatInt == null)) {
				// we've reached the end and they're the same
				break;
			}
		}
		return cmp;
	}

	public boolean equals(ProtocolNumber that) {
		return this.compareTo(that) == 0;
	}

}
