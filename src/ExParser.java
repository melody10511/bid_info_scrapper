import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ExParser extends Parser {
	final static String BASE_PATH = "http://ebid.ex.co.kr/ebid/jsps/ebid/";
	final static String ANN_LIST = "/bidNoti/bidNotiCompanyList.jsp?";
	final static String RES_LIST = "/bidResult/bidResultList.jsp?";
	final static String ANN_INF = "/bidNoti/";
	final static String RES_INF = "/bidResult/";
	
	final static String CONST_PRICE = "http://ebid.ex.co.kr/ebid/jsps/ebid/const/bidResult/bidResultNego.jsp?";
	final static String SERV_PRICE = "http://ebid.ex.co.kr/ebid/jsps/ebid/serv/bidResult/bidResultNego2.jsp?";
	final static String BUY_PRICE = "http://ebid.ex.co.kr/ebid/jsps/ebid/buy/bidResult/bidResultNego.jsp?";
	
	// For SQL setup.
	Connection db_con;
	java.sql.Statement st;
	ResultSet rs;
	
	URL url;
	HttpURLConnection con;
	String sd;
	String ed;
	String op;
	String wt;
	String it;
	int totalItems;
	int curItem;
	
	public ExParser(String sd, String ed, String op) throws ClassNotFoundException, SQLException {
		sd = sd.replaceAll("-", "");
		ed = ed.replaceAll("-", "");
		
		this.sd = sd;
		this.ed = ed;
		if (op.length() == 4) {
			this.op = op;
			this.wt = op.substring(0, 2);
			this.it = op.substring(2, 4);
		}
		totalItems = 0;
		curItem = 0;
		
		// Set up SQL connection.
		Class.forName("com.mysql.jdbc.Driver");
		db_con = DriverManager.getConnection("jdbc:mysql://localhost/"+Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
		st = db_con.createStatement();
		rs = null;
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		ExParser tester = new ExParser("2016-04-01", "2016-04-30", "공사결과");
		
		tester.run();
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
	
	public String getPage() throws IOException {
		String path = ExParser.BASE_PATH;
		if (wt.equals("공사")) path += "const";
		else if (wt.equals("용역")) path += "serv";
		else if (wt.equals("물품")) path += "buy";
		
		if (it.equals("공고")) path += ExParser.ANN_LIST;
		else if (it.equals("결과")) path += ExParser.RES_LIST;
		
		if (wt.equals("물품")) {
			path += "startDate=" + sd;
			path += "&endDate=" + ed;
		}
		else {
			path += "s_noti_date=" + sd;
			path += "&e_noti_date=" + ed;
		}
		
		return path;
	}
	
	public void getList() throws IOException, SQLException {
		String path = getPage();
		
		openConnection(path, "GET");
		Document doc = Jsoup.parse(getResponse(null, "GET"));
		totalItems = Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("건")[0].replaceAll("[^\\d]", ""));
		System.out.println("전체 건 : " + totalItems);
		Element listing = doc.getElementsByTag("table").get(0);
		Elements rows = listing.getElementsByTag("tr");
		
		int page = 1;
		int index = 1;
		int startnum = 1;
		int endnum = 10;
		for (int i = 0; i < totalItems; i++) {
			if (it.equals("결과")) curItem++;
			Element row = rows.get(index);
			parseListRow(row);
			
			if (i % 10 == 9 && i < (totalItems - 1)) {
				index = 1;
				page++;
				startnum += 10;
				endnum += 10;
				String nextpage = path + "&page=" + page;
				nextpage += "&startnum=" + startnum;
				nextpage += "&endnum=" + endnum;
				
				openConnection(nextpage, "GET");
				doc = Jsoup.parse(getResponse(null, "GET"));
				listing = doc.getElementsByTag("table").get(0);
				rows = listing.getElementsByTag("tr");
			}
			else {
				index++;
			}
		}
	}
	
	public void parseListRow(Element row) throws SQLException, IOException {
		boolean enter = true;
		boolean exists = false;
		String where = "";
		Elements data = row.getElementsByTag("td");
		if (it.equals("공고")) {
			String bidno = data.get(1).text(); // 공고번호
			String area = data.get(2).text(); // 지역
			String compType = data.get(6).text(); // 계약방법
			String prog = data.get(8).text(); // 공고상태
			where = "WHERE 공고번호=\"" + bidno + "\"";
			
			String sql = "SELECT EXISTS(SELECT 공고번호 FROM exbidinfo " + where + ")";
			rs = st.executeQuery(sql);
			if (rs.first()) exists = rs.getBoolean(1);
			
			if (exists) {
				// Check the bid version and update level from the DB.
				sql = "SELECT 공고 FROM exbidinfo " + where;
				rs = st.executeQuery(sql);
				int finished = 0;
				if (rs.first()) {
					finished = rs.getInt(1);
				}
				if (finished > 0) enter = false;
			}
			else {
				sql = "INSERT INTO exbidinfo (공고번호, 분류, 지역, 계약방법, 공고상태) VALUES (" +
						"\""+bidno+"\", \"" + wt + "\", \"" + area + "\", \"" + compType + "\", \"" + prog + "\");";
				st.executeUpdate(sql);
			}
		}
		else if (it.equals("결과")) {
			String bidno = data.get(1).text(); // 공고번호
			String area = data.get(2).text(); // 지역
			String compType = data.get(5).text(); // 계약방법
			String openDate = data.get(6).text(); // 개찰일시
			String prog = data.get(7).text(); // 결과
			where = "WHERE 공고번호=\"" + bidno + "\"";
			
			// Check if the 공고번호 already exists in the DB.
			rs = st.executeQuery("SELECT EXISTS(SELECT 공고번호 FROM exbidinfo " + where + ")");
			if (rs.first()) exists = rs.getBoolean(1);
					
			if (exists) {
				System.out.println(bidno + " exists.");
				// Check the bid version and update level from the DB.
				rs = st.executeQuery("SELECT 완료, 결과상태 FROM exbidinfo " + where);
				int finished = 0;
				String dbResult = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbResult = rs.getString(2) == null ? "" : rs.getString(2);
				}
				if (finished > 0) {
					if (!dbResult.equals(prog)) {
						String sql = "UPDATE exbidinfo SET 결과상태=\"" + prog + "\" " + where;
						st.executeUpdate(sql);
					}
					enter = false;
				}
			}
			else {
				String sql = "INSERT INTO exbidinfo (공고번호, 분류, 지역, 계약방법, 개찰일시, 결과상태) VALUES (" +
						"\""+bidno+"\", \"" + wt + "\", \"" + area + "\", \"" + compType + "\", \"" + openDate + "\", \"" + prog + "\");";
				st.executeUpdate(sql);
			}
		}
		
		if (enter) {
			Element link = row.getElementsByTag("td").get(3).getElementsByTag("a").first();
			String itempath = ExParser.BASE_PATH;
			if (wt.equals("공사")) itempath += "const";
			else if (wt.equals("용역")) itempath += "serv";
			else if (wt.equals("물품")) itempath += "buy";
			
			if (it.equals("공고")) {
				itempath += ExParser.ANN_INF;
				itempath += link.attr("href");
			}
			else if (it.equals("결과")) {
				itempath += ExParser.RES_INF;
				itempath += link.attr("href").substring(2);
			}
			
			openConnection(itempath, "GET");
			Document itemdoc = Jsoup.parse(getResponse(null, "GET"));
			parseInfo(itemdoc, itempath, where);
		}
	}
	
	public void parseInfo(Document doc, String itempath, String where) throws SQLException, IOException {
		if (it.equals("공고")) {
			String annDate = ""; // 공고일자
			String hasDup = ""; // 복수예가적용여부
			String hasRebid = ""; // 재입찰허용여부
			String elecBid = ""; // 전자입찰여부
			String hasCommon = ""; // 공동수급가능여부
			String fieldTour = ""; // 현장설명실시여부
			String mustCommon = ""; // 공동수급의무여부
			String openDate = ""; // 개찰일시
			String protoPrice = ""; // 설계금액
			
			Elements tables = doc.getElementsByTag("caption");
			Elements headers = doc.getElementsByTag("th");
			for (Element h : headers) {
				String key = h.text().replaceAll(" ", "");
				if (key.equals("공고일자")) {
					annDate = h.nextElementSibling().text();
					annDate += " 00:00:00";
				}
				else if (key.equals("복수예가적용여부")) {
					hasDup = h.nextElementSibling().text();
				}
				else if (key.equals("재입찰허용여부")) {
					hasRebid = h.nextElementSibling().text();
				}
				else if (key.equals("전자입찰여부")) {
					elecBid = h.nextElementSibling().text();
				}
				else if (key.equals("공동수급가능여부")) {
					hasCommon = h.nextElementSibling().text();
				}
				else if (key.equals("현장설명실시여부")) {
					fieldTour = h.nextElementSibling().text();
				}
				else if (key.equals("공동수급의무여부")) {
					mustCommon = h.nextElementSibling().text();
				}
				else if (key.equals("설계금액")) {
					protoPrice = h.nextElementSibling().text();
					protoPrice = protoPrice.replaceAll("[^\\d.]", "");
					if (protoPrice.equals("")) protoPrice = "0";
				}
				else if (key.equals("개찰일시")) {
					openDate = h.nextElementSibling().text() + ":00";							
				}
			}
			
			for (int j = 0; j < tables.size(); j++) {
				String caption = tables.get(j).text();
				
				if (caption.equals("복수예비가격")) {
					Element dupTable = tables.get(j).parent();
					Elements dupData = dupTable.getElementsByTag("td");
					
					for (int k = 0; k < dupData.size(); k++) {
						String dupPrice = dupData.get(k).text();
						dupPrice = dupPrice.replaceAll(",", "");
						st.executeUpdate("UPDATE exbidinfo SET 복수"+(k+1)+"="+dupPrice+" " + where);
					}
				}
			}
			String sql = "UPDATE exbidinfo SET 공고=1, " +
					"공고일자=\"" + annDate + "\", " +
					"복수예가여부=\"" + hasDup + "\", " +
					"재입찰허용여부=\"" + hasRebid + "\", " +
					"전자입찰여부=\"" + elecBid + "\", " +
					"공동수급가능여부=\"" + hasCommon + "\", " +
					"현장설명실시여부=\"" + fieldTour + "\", " +
					"공동수급의무여부=\"" + mustCommon + "\", " +
					"설계금액=" + protoPrice + ", " +
					"개찰일시=\"" + openDate + "\" " + where;
			st.executeUpdate(sql);
		}
		else if (it.equals("결과")) {
			Elements headers = doc.getElementsByTag("th");
			String annDate = ""; // 공고일자
			String expPrice = "0"; // 예정가격
			String protoPrice = "0"; // 설계금액
			String bidPrice = "0"; // 투찰금액
			String comp = "0"; // 참여수
			
			for (Element h : headers) {
				String key = h.text();
				
				if (key.equals("공고일자")) {
					annDate = h.nextElementSibling().text() + " 00:00:00";
				}
				else if (key.equals("예정가격")) {
					expPrice = h.nextElementSibling().text();
					expPrice = expPrice.replaceAll("[^\\d.]", "");
					if (expPrice.equals("")) expPrice = "0";
				}
				else if (key.equals("설계가격")) {
					protoPrice = h.nextElementSibling().text();
					protoPrice = protoPrice.replaceAll("[^\\d.]", "");
					if (protoPrice.equals("")) protoPrice = "0";
				}
			}
			
			Elements captions = doc.getElementsByTag("caption");
			for (Element c : captions) {
				if (c.text().equals("입찰업체")) {
					Element resTable = c.parent();
					Elements resRows = resTable.getElementsByTag("tr");
					if (resRows.size() > 1) {
						if (resRows.get(1).text().length() > 13) {
							String price = resRows.get(1).getElementsByTag("td").get(3).text();
							bidPrice = price.replaceAll("[^\\d.]", "");
							System.out.println(bidPrice);
						}
					}
				}
			}
			
			Element buttonDiv = null;
			if (wt.equals("공사") || wt.equals("물품")) buttonDiv = doc.getElementsByAttributeValue("class", "center_btn_area").first();
			else if (wt.equals("용역")) buttonDiv = doc.getElementsByAttributeValue("class", "btn_area").first();
			
			if (buttonDiv.getElementsContainingText("입찰실시결과").size() > 0 || buttonDiv.getElementsContainingOwnText("입찰결과").size() > 0) {
				String pricepath = "";
				if (wt.equals("공사")) pricepath = ExParser.CONST_PRICE;
				else if (wt.equals("용역")) pricepath = ExParser.SERV_PRICE;
				else if (wt.equals("물품")) pricepath = ExParser.BUY_PRICE;
				
				pricepath += itempath.split("\\?")[1];
				openConnection(pricepath, "GET");
				Document pricePage = Jsoup.parse(getResponse(null, "GET"));
				Element priceTable = pricePage.getElementsByAttributeValue("class", "print_table").first();
				if (priceTable != null) {
					Element priceData = priceTable.getElementsByTag("td").get(3);
					for (String s : priceData.text().split(" ")) {
						if (s.contains(")")) {
							String ind = s.split("\\)")[0];
							String price = s.split("\\)")[1];
							
							ind = ind.replaceAll("[^\\d]", "");
							price = price.replaceAll("[^\\d.]", "");
							String sql = "UPDATE exbidinfo SET 복수"+ind+"="+price+" " + where;
							st.executeUpdate(sql);
						}
					}
				}
			}
			
			if (buttonDiv.getElementsContainingText("투찰업체현황").size() > 0) {
				itempath = itempath.replace("bidResultDetail", "bidResult");
				itempath = itempath.replace("notino", "p_notino");
				itempath = itempath.replace("bidno", "p_bidno");
				itempath = itempath.replace("bidseq", "p_bidseq");
				itempath = itempath.replace("state", "p_state");
				
				openConnection(itempath, "GET");
				Document compPage = Jsoup.parse(getResponse(null, "GET"));
				comp = compPage.getElementsByClass("totalCount_001").first().text();
				comp = comp.replaceAll("[^\\d]", "");
				if (comp.equals("")) comp = "0";
			}
			
			String sql = "UPDATE exbidinfo SET 완료=1, " +
					"공고일자=\"" + annDate + "\", " +
					"예정가격=" + expPrice + ", " +
					"설계금액=" + protoPrice + ", " +
					"투찰금액=" + bidPrice + ", " +
					"참가수=" + comp + " " + where;
			st.executeUpdate(sql);
		}
	}
	
	public void run() {
		try {
			curItem = 0;
			setOption("공사공고");
			getList();
			setOption("공사결과");
			getList();
			setOption("용역공고");
			getList();
			setOption("용역결과");
			getList();
			setOption("물품공고");
			getList();
			setOption("물품결과");
			getList();
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int getTotal() throws IOException {
		setOption("공사결과");
		String path = getPage();
		openConnection(path, "GET");
		Document doc = Jsoup.parse(getResponse(null, "GET"));
		totalItems = Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("건")[0].replaceAll("[^\\d]", ""));
		setOption("용역결과");
		path = getPage();
		openConnection(path, "GET");
		doc = Jsoup.parse(getResponse(null, "GET"));
		totalItems += Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("건")[0].replaceAll("[^\\d]", ""));
		setOption("물품결과");
		path = getPage();
		openConnection(path, "GET");
		doc = Jsoup.parse(getResponse(null, "GET"));
		totalItems += Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("건")[0].replaceAll("[^\\d]", ""));
		
		return totalItems;
	}

	public void setDate(String sd, String ed) {
		sd = sd.replaceAll("-", "");
		ed = ed.replaceAll("-", "");
		
		this.sd = sd;
		this.ed = ed;
	}

	public void setOption(String op) {
		this.op = op;
		this.wt = op.substring(0, 2);
		this.it = op.substring(2, 4);
	}

	public int getCur() {
		return curItem;
	}

}
