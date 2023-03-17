package webcrawler.processor;

import webcrawler.LinkProcessor;

public class PzyProcessor implements LinkProcessor {

	private static final String THUMB_NAIL = "/t/";
	private static final String THUMB_NAIL_REPLACE = "/i/";

	@Override
	public String process(String link) {
		if (link.contains(THUMB_NAIL)) {
			return link.replace(THUMB_NAIL, THUMB_NAIL_REPLACE);
		}
		return link;
	}

}
