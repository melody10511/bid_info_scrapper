import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
	final static String FACIL_ANN_LIST = "http://ebid.korail.com/bid/bidNoticeListA41.jsp";
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
	
	public static void main(String args[]) throws IOException, SQLException {
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
	
	public void getList() throws IOException, SQLException {
		initialize();
		String path = "";
		
		String param = "";
		Document doc = null;
		if (it.equals("공고")) {
			if (wt.equals("물품")) {
				path = KorailParser.PROD_ANN_LIST;
				param += "menu_code=A.3.1";
			}
			else if (wt.equals("용역")) {
				path = KorailParser.SERV_ANN_LIST;
				param += "menu_code=A.5.1";
			}
			else if (wt.equals("공사")) {
				path = KorailParser.FACIL_ANN_LIST;
				param += "menu_code=A.4.1";
			}
			
			param += "&notice_date1=" + sd;
			param += "&notice_date2=" + ed;
			param += "&order_by=BIDXDATE";
			param += "&asc=false";
			param += "&page_number=1";
			
			openConnection(path, "POST");
			doc = Jsoup.parse(getResponse(param, "POST"));
		}
		else if (it.equals("결과")) {
			path = KorailParser.RES_LIST;
			
			if (wt.equals("물품")) path += "menu_code=A.3.9"; 
			else if (wt.equals("용역")) path += "menu_code=A.5.7";
			else if (wt.equals("공사")) path += "menu_code=A.4.8";
			
			path += "&notice_date1=" + sd;
			path += "&notice_date2=" + ed;
			path += "&x=13";
			path += "&y=13";
			path += "&order_by=BIDXCODE";
			path += "&asc=false";
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
	
	public void parseListRow(Element row) throws IOException, SQLException {
		Elements data = row.getElementsByTag("td");
		if (it.equals("공고")) {
			String bidno = Jsoup.parse(data.get(0).html().split("<br>")[0]).text(); // 공고번호
			String bidType = Jsoup.parse(data.get(0).html().split("<br>")[1]).text(); // 입찰방식
			String org = Jsoup.parse(data.get(1).html().split("<br>")[1]).text(); // 발주부서
			String annType = Jsoup.parse(data.get(2).html().split("<br>")[0]).text(); // 공고분류
			//String selectMethod = Jsoup.parse(data.get(2).html().split("<br>")[1]).text(); // 낙찰자선정
			String endDate = Jsoup.parse(data.get(3).html().split("<br>")[1]).text(); // 투찰종료일시
			String prog = Jsoup.parse(data.get(4).html().split("<br>")[1]).text(); // 공고상태
			
			boolean enter = true;
			boolean exists = false;
			String where = "WHERE 공고번호=\"" + bidno + "\"";
			
			rs = st.executeQuery("SELECT EXISTS(SELECT 공고번호 FROM korailbidinfo " + where + ")");
			if (rs.next()) exists = rs.getBoolean(1);
			
			if (exists) {
				String sql = "SELECT 공고, 공고상태 FROM korailbidinfo " + where;
				rs = st.executeQuery(sql);
				
				int finished = 0;
				String dbProg = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbProg = rs.getString(2);
				}

				if (finished > 0) {
					if (dbProg.equals(prog)) enter = false;
					else {
						sql = "UPDATE korailbidinfo SET 공고상태=\"" + prog + "\" " + where;
						st.executeUpdate(sql);
					}
				}
				else if (!dbProg.equals(prog)) {
					sql = "UPDATE korailbidinfo SET 공고상태=\"" + prog + "\" " + where;
					st.executeUpdate(sql);
				}
			}
			else {
				// If entry doesn't exists in db, insert new row.
				String sql = "INSERT INTO korailbidinfo (공고번호, 입찰방식, 발주부서, 공고분류, 투찰종료일시, 공고상태) VALUES (" +
						"\""+bidno+"\", " +
						"\""+bidType+"\", " +
						"\""+org+"\", " +
						"\""+annType+"\", " +
						"\""+endDate+"\", " +
						"\""+prog+"\");";
				st.executeUpdate(sql);
			}
			
			if (enter) {
				String link = data.get(0).getElementsByTag("a").first().attr("href");
				getItem(link, where);
			}
		}
		else if (it.equals("결과")) {
			String bidno = data.get(0).text(); // 공고번호차수
			String annType = data.get(2).text(); // 공고분류
			String org = data.get(3).text(); // 발주부서
			String openDate = data.get(4).text(); // 개찰일시
			
			boolean enter = true;
			boolean exists = false;
			String where = "WHERE 공고번호=\"" + bidno + "\"";
			
			rs = st.executeQuery("SELECT EXISTS(SELECT 공고번호 FROM korailbidinfo " + where + ")");
			if (rs.next()) exists = rs.getBoolean(1);
			
			if (exists) {
				String sql = "SELECT 완료 FROM korailbidinfo " + where;
				rs = st.executeQuery(sql);
				
				int finished = 0;
				if (rs.first()) {
					finished = rs.getInt(1);
				}

				if (finished > 0) {
					enter = false;
				}
			}
			else {
				// If entry doesn't exists in db, insert new row.
				String sql = "INSERT INTO korailbidinfo (공고번호, 발주부서, 공고분류, 개찰일시) VALUES (" +
						"\""+bidno+"\", " +
						"\""+org+"\", " +
						"\""+annType+"\", " +
						"\""+openDate+"\");";
				st.executeUpdate(sql);
			}
			
			if (enter) {
				String link = data.get(0).getElementsByTag("a").first().attr("href");
				getItem(link, where);
			}
		}
	}
	
	public void getItem(String link, String where) throws IOException, SQLException {
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
		if (it.equals("공고")) parseNoti(doc, where);
		else parseRes(doc, where);
	}
	
	public void parseNoti(Document doc, String where) throws SQLException {
		Elements heads = doc.getElementsByTag("th");
		
		String workType = ""; // 공고구분
		String compType = ""; // 계약방법
		String hasRe = ""; // 재입찰 허용여부
		String openDate = ""; // 개찰일시
		String priceMethod = ""; // 예가방식
		String totalPrice = ""; // 총예가갯수
		String range = ""; // 예가범위
		String basePrice = ""; // 기초금액
		
		for (Element h : heads) {
			String key = h.text();
			if (key.equals("공고구분")) {
				workType = h.nextElementSibling().text();
			}
			else if (key.equals("계약방법")) {
				compType = h.nextElementSibling().text();
			}
			else if (key.equals("재입찰 허용여부")) {
				hasRe = h.nextElementSibling().text();
			}
			else if (key.equals("개찰일자")) {
				openDate = h.nextElementSibling().text();
			}
			else if (key.equals("예가방식")) {
				priceMethod = h.nextElementSibling().text();
			}
			else if (key.equals("총예가갯수")) {
				totalPrice = h.nextElementSibling().text();
			}
			else if (key.equals("예가범위")) {
				range = h.nextElementSibling().text();
			}
			else if (key.equals("예가기초금액")) {
				basePrice = h.nextElementSibling().text();
			}
		}
		
		String sql = "UPDATE korailbidinfo SET 공고구분=\""+workType+"\", " +
				"계약방법=\""+compType+"\", " +
				"재입찰허용여부=\""+hasRe+"\", " +
				"개찰일시="+openDate+", " +
				"예가방식=\""+priceMethod+"\", " +
				"총예가갯수="+totalPrice+", " +
				"예가범위=\""+range+"\", " +
				"기초금액="+basePrice+" " + where;
		st.executeUpdate(sql);
	}
	
	public void parseRes(Document doc, String where) throws SQLException {
		Elements heads = doc.getElementsByTag("td");
		
		String compType = ""; // 계약방법
		String openDate = ""; // 개찰일시
		String basePrice = ""; // 기초금액
		String expPrice = ""; // 예정가격
		String comp = ""; // 참가수
		String result = ""; // 개찰결과
		String bidPrice = ""; // 투찰금액
		
		for (Element h : heads) {
			String key = h.text();
			if (key.equals("계약방법")) {
				compType = h.nextElementSibling().text();
			}
			else if (key.equals("개찰일시")) {
				openDate = h.nextElementSibling().text();
			}
			else if (key.equals("기초(예가)금액")) {
				basePrice = h.nextElementSibling().text();
			}
			else if (key.equals("예정가격")) {
				expPrice = h.nextElementSibling().text();
			}
			else if (key.equals("입찰업체수")) {
				comp = h.nextElementSibling().text();
			}
			else if (key.equals("개찰결과")) {
				result = h.nextElementSibling().text();
			}
		}
		
		Element priceTable = doc.getElementById("pre_price-tbd");
		if (priceTable != null) {
			Elements rows = priceTable.getElementsByTag("tr");
			int ind = 1;
			for (int i = 1; i <= 5; i += 2) {
				Elements data = rows.get(i).getElementsByTag("td");
				
				for (int j = 1; j <= 14; j += 2) {
					String price = data.get(j).text();
					String dup = data.get(j + 1).text();
					
					String s = "UPDATE korailbidinfo SET 복수" + ind + "=" + price + ", 복참" + ind + "=" + dup + " " + where;
        			st.executeUpdate(s);
        			ind++;
				}
	 		}
		}
		
		Element chosenTable = doc.getElementById("sel_pre_price-bd-scroll");
		if (chosenTable != null) {
			Element row = chosenTable.getElementsByTag("tr").get(1);
			
			Elements data = row.getElementsByTag("td");
			
			int ind = 1;
			for (int i = 1; i <= 10; i += 3) {
				String price = data.get(i).text();
				String s = "UPDATE korailbidinfo SET 선택" + ind + "=" + price + " " + where;
				st.executeUpdate(s);
				ind++;
			}
		}
		
		Element detailTable = doc.getElementById("gen_fact_2-bd-scroll");
		if (detailTable != null) {
			Element row = detailTable.getElementsByTag("tr").get(2);
			bidPrice = row.getElementsByTag("td").get(4).text();
		}
		
		String sql = "UPDATE korailbidinfo SET 계약방법=\""+compType+"\", " +
				"개찰일시=\""+openDate+"\", " +
				"개찰결과=\""+result+"\", " +
				"참가수="+comp+", " +
				"투찰금액="+bidPrice+", " +
				"예정가격="+expPrice+", " +
				"기초금액="+basePrice+" " + where;
		st.executeUpdate(sql);
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
