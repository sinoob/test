package webcrawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.hibernate.mapping.Array;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import webcrawler.processor.PzyProcessor;

public class ImgCrawler {

	private static final String[] IMGS = { ".jpg", ".JPG", ".png", ".PNG", ".jpeg", ".JPEG", ".gif", ".GIF" };

	static Scanner sc = new Scanner(System.in); // user input taking

	private List<LinkProcessor> processors = new ArrayList<>();

	public static void main(String[] args) {
		String url = "t=1521314&page=31";
		try {
			new ImgCrawler().getImageLinks(url);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main2(String[] args) throws IOException {
		System.out.print("Enter Base URL to crawl: ");
		String baseUrl = sc.next();
		System.out.print("Enter From Page Number : ");
		String fromPageStr = sc.next();
		System.out.print("Enter To Page Number : ");
		String toPageStr = sc.next();
		int fromPage = Integer.parseInt(fromPageStr);
		int toPage = Integer.parseInt(toPageStr);

		for (int i = fromPage; i <= toPage; i++) {
			String url = new StringBuilder().append(baseUrl).append("&page=").append(i).toString();
			new ImgCrawler().getImageLinks(url);
		}

	}

	public List<String> getImageLinks(String url) throws IOException {

		List<String> imageLinks = new ArrayList<String>();
		Document doc = Jsoup.connect(url).get();
		Elements links = doc.select("img");
		Elements refs = doc.getElementsByAttribute("href");
		// System.out.println(refs);
		if (links == null) {
			links = refs;
		} else {
			links.addAll(refs);
		}
		for (Element link : links) {
			String source = link.attr("src") == "" ? link.attr("href") : link.attr("src");
			if (source.startsWith("/") || source.contains("xossip")) {
				continue;
			}
			if (endsWithImage(source)) {
				for (LinkProcessor p : processors) {
					source = p.process(source);
				}
				System.out.println(source);
				imageLinks.add(source);
			}
		}
		return imageLinks;
	}

	public void addProcessor(LinkProcessor p) {
		processors.add(p);
	}

	private boolean endsWithImage(String link) {
		for (String ext : IMGS) {
			if (link.endsWith(ext))
				return true;
		}
		return false;
	}
}
