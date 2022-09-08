package se.raa.ksamsok.api.util;

import com.rometools.rome.feed.rss.Guid;
import com.rometools.rome.feed.rss.Item;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.impl.ConverterForRSS20;

/**
 * Simple override for the ROME rss library and its rss 2.0 converter that
 * always sets permaLink to false if one sets the uri and we want it to be true
 * as the uri is an url and it is permanent.
 * See http://www.w3schools.com/rss/rss_tag_guid.asp and WEB-INF/classes/rome.properties
 */
public class RSS20FixIsPermaLinkConverter extends ConverterForRSS20 {

	public RSS20FixIsPermaLinkConverter() {
		// same key as default rss 2.0 handler to override it 
		super("rss_2.0");
	}

	@Override
	protected Item createRSSItem(SyndEntry sEntry) {
		// Get the item from super
		Item item = super.createRSSItem(sEntry);
		// and then override the permaLink value since we know it is a permaLink
		Guid guid = item.getGuid();
		if (guid != null) {
			guid.setPermaLink(true);
		}
		return item;
	}
}
