import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class KorailParser extends Parser {
	
	// For SQL setup.
	Connection db_con;
	java.sql.Statement st;
	ResultSet rs;
	
	final static String BASE_PATH = "http://ebid.korail.com/bid/forward.jsp";
	final static String PROD_ANN_LIST = "http://ebid.korail.com/bid/bidNoticeListA31.jsp";
	final static String SERV_ANN_LIST = "http://ebid.korail.com/bid/bidNoticeListA51.jsp";
	final static String RES_LIST = "http://ebid.korail.com/bid/openBidList.jsp?";
	
	WebDriver driver;
	
	URL url;
	HttpURLConnection con;
	HashMap<String, String> formData;
	String sd;
	String ed;
	String op;
	String wt;
	String it;
	int totalItems;
	int curItem;
	
	public KorailParser(String sd, String ed, String op) {
		this.sd = sd;
		this.ed = ed;
		if (op.length() == 4) {
			this.op = op;
			this.wt = op.substring(0, 2);
			this.it = op.substring(2, 4);
		}
	}
	
	public static void main(String args[]) throws IOException {
		KorailParser tester = new KorailParser("2016-08-01", "2016-08-23", "물품공고");
		
		tester.getList();
	}

	public String httpsConnection(String path) throws IOException {
		System.out.println("Connecting with HTTPS to : " + path);
		
		url = new URL(path);
		HttpsURLConnection scon = (HttpsURLConnection) url.openConnection();
		scon.setRequestMethod("GET");
		scon.setRequestProperty("User-Agent", "Mozilla/5.0");
		scon.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4");
		
		BufferedReader in = new BufferedReader(new InputStreamReader(scon.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		return response.toString();
	}
	
	public void openConnection(String path, String method) throws IOException {
		url = new URL(path);
		con = (HttpURLConnection) url.openConnection();
		
		con.setRequestMethod(method);
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4");
	}
	
	public String getResponse(String param, String method) throws IOException {
		if (method.equals("POST")) {
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(param);
			wr.flush();
			wr.close();
		}
		
		int responseCode = con.getResponseCode();
		System.out.println("\nSending " + method + " request to URL : " + url);
		System.out.println("Post parameters : " + param);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		return response.toString();
	}

	public void initialize() {
		driver = new HtmlUnitDriver();
	}
	
	public void getList() throws IOException {
		initialize();
		String path = "";
		
		String param = "";
		Document doc = null;
		if (op.equals("물품공고")) {
			path = KorailParser.PROD_ANN_LIST;
			
			param += "notice_date1=" + sd;
			param += "&notice_date2=" + ed;
			param += "&order_by=BIDXDATE";
			param += "&asc=false";
			param += "&menu_code=A.3.1";
			param += "&page_number=1";
			
			openConnection(path, "POST");
			doc = Jsoup.parse(getResponse(param, "POST"));
		}
		else if (op.equals("용역공고")) {
			path = KorailParser.SERV_ANN_LIST;
			
			param += "notice_date1=" + sd;
			param += "&notice_date2=" + ed;
			param += "&order_by=BIDXDATE";
			param += "&asc=false";
			param += "&menu_code=A.5.1";
			param += "&page_number=1";
			
			openConnection(path, "POST");
			doc = Jsoup.parse(getResponse(param, "POST"));
		}
		else if (op.equals("물품결과")) {
			path = KorailParser.RES_LIST;
			
			path += "notice_date1=" + sd;
			path += "&notice_date2=" + ed;
			path += "&x=13";
			path += "&y=13";
			path += "&order_by=BIDXCODE";
			path += "&asc=false";
			path += "&menu_code=A.3.9";
			path += "&page_number=1";
			
			openConnection(path, "GET");
			doc = Jsoup.parse(getResponse(null, "GET"));
		}
		else if (op.equals("용역결과")) {
			path = KorailParser.RES_LIST;
			
			path += "notice_date1=" + sd;
			path += "&notice_date2=" + ed;
			path += "&x=13";
			path += "&y=13";
			path += "&order_by=BIDXCODE";
			path += "&asc=false";
			path += "&menu_code=A.5.7";
			path += "&page_number=1";
			
			openConnection(path, "GET");
			doc = Jsoup.parse(getResponse(null, "GET"));
		}
		Element form = doc.getElementsByAttributeValue("name", "PAGE_VIEW").first();
		Element listTable = null;
		if (op.equals("물품공고")) listTable = form.getElementsByTag("table").get(7);
		else if (op.equals("용역공고")) listTable = form.getElementsByTag("table").get(7);
		else if (op.equals("물품결과")) listTable = form.getElementsByTag("table").get(4);
		else if (op.equals("용역결과")) listTable = form.getElementsByTag("table").get(4);
		Elements rows = listTable.getElementsByTag("tr");
		for (int i = 4; i < 7; i += 2) {
			parseListRow(rows.get(i));
		}
		driver.close();
	}
	
	public void parseListRow(Element row) throws IOException {
		Elements data = row.getElementsByTag("td");
		if (op.equals("물품공고") || op.equals("용역공고")) {
			String bidno = Jsoup.parse(data.get(0).html().split("<br>")[0]).text();
			String bidType = Jsoup.parse(data.get(0).html().split("<br>")[1]).text();
			String org = Jsoup.parse(data.get(1).html().split("<br>")[1]).text();
			String annType = Jsoup.parse(data.get(2).html().split("<br>")[0]).text();
			String selectMethod = Jsoup.parse(data.get(2).html().split("<br>")[1]).text();
			String startDate = Jsoup.parse(data.get(3).html().split("<br>")[0]).text();
			String endDate = Jsoup.parse(data.get(3).html().split("<br>")[1]).text();
			String annDate = Jsoup.parse(data.get(4).html().split("<br>")[0]).text();
			String prog = Jsoup.parse(data.get(4).html().split("<br>")[1]).text();
			
			String link = data.get(0).getElementsByTag("a").first().attr("href");
			getItem(link);
		}
		else if (op.equals("물품결과") || op.equals("용역결과")) {
			String bidno = data.get(0).text(); // 공고번호차수
			String annType = data.get(2).text();
			String org = data.get(3).text();
			String openDate = data.get(4).text();
			
		}
	}
	
	public void getItem(String link) throws IOException {
		Document framePage = Jsoup.parse(httpsConnection(link));
		String tableLink = framePage.getElementsByTag("frame").first().attr("src");
		String frameLink = "";
		String[] urls = link.split("/");
		for (int i = 0; i < urls.length - 1; i++) {
			frameLink += urls[i];
			frameLink += "/";
		}
		frameLink += tableLink;
		
		driver.get(frameLink);
		Document doc = Jsoup.parse(driver.getPageSource());
		System.out.println(doc.html());
	}
	
	public void run() {
		
	}
	
	public int getTotal() throws IOException, ClassNotFoundException, SQLException {
		return totalItems;
	}

	public void setDate(String sd, String ed) {
		this.sd = sd;
		this.ed = ed;
	}

	public void setOption(String op) {
		if (op.length() == 4) {
			this.op = op;
			this.wt = op.substring(0, 2);
			this.it = op.substring(2, 4);
		}
	}

	public int getCur() {
		return curItem;
	}
}
