package webcrawler.processor;

import webcrawler.LinkProcessor;

public class ICGProcessor implements LinkProcessor {

	private static final String DOMAIN_NAME = "www.indiancinemagallery.com";
	private static final String THUMB_NAIL = "thumbs/thumbs_";
	private static final String THUMB_NAIL_REPLACE = "";

	@Override
	public String process(String link) {
		if (link.contains(DOMAIN_NAME)) {
			return link.replace(THUMB_NAIL, THUMB_NAIL_REPLACE);
		}
		return link;
	}

}
