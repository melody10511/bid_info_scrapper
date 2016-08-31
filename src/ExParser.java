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
		ExParser tester = new ExParser("2016-04-01", "2016-04-30", "������");
		
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
		if (wt.equals("����")) path += "const";
		else if (wt.equals("�뿪")) path += "serv";
		else if (wt.equals("��ǰ")) path += "buy";
		
		if (it.equals("����")) path += ExParser.ANN_LIST;
		else if (it.equals("���")) path += ExParser.RES_LIST;
		
		if (wt.equals("��ǰ")) {
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
		totalItems = Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("��")[0].replaceAll("[^\\d]", ""));
		System.out.println("��ü �� : " + totalItems);
		Element listing = doc.getElementsByTag("table").get(0);
		Elements rows = listing.getElementsByTag("tr");
		
		int page = 1;
		int index = 1;
		int startnum = 1;
		int endnum = 10;
		for (int i = 0; i < totalItems; i++) {
			if (it.equals("���")) curItem++;
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
		if (it.equals("����")) {
			String bidno = data.get(1).text(); // �����ȣ
			String area = data.get(2).text(); // ����
			String compType = data.get(6).text(); // �����
			String prog = data.get(8).text(); // �������
			where = "WHERE �����ȣ=\"" + bidno + "\"";
			
			String sql = "SELECT EXISTS(SELECT �����ȣ FROM exbidinfo " + where + ")";
			rs = st.executeQuery(sql);
			if (rs.first()) exists = rs.getBoolean(1);
			
			if (exists) {
				// Check the bid version and update level from the DB.
				sql = "SELECT ���� FROM exbidinfo " + where;
				rs = st.executeQuery(sql);
				int finished = 0;
				if (rs.first()) {
					finished = rs.getInt(1);
				}
				if (finished > 0) enter = false;
			}
			else {
				sql = "INSERT INTO exbidinfo (�����ȣ, �з�, ����, �����, �������) VALUES (" +
						"\""+bidno+"\", \"" + wt + "\", \"" + area + "\", \"" + compType + "\", \"" + prog + "\");";
				st.executeUpdate(sql);
			}
		}
		else if (it.equals("���")) {
			String bidno = data.get(1).text(); // �����ȣ
			String area = data.get(2).text(); // ����
			String compType = data.get(5).text(); // �����
			String openDate = data.get(6).text(); // �����Ͻ�
			String prog = data.get(7).text(); // ���
			where = "WHERE �����ȣ=\"" + bidno + "\"";
			
			// Check if the �����ȣ already exists in the DB.
			rs = st.executeQuery("SELECT EXISTS(SELECT �����ȣ FROM exbidinfo " + where + ")");
			if (rs.first()) exists = rs.getBoolean(1);
					
			if (exists) {
				System.out.println(bidno + " exists.");
				// Check the bid version and update level from the DB.
				rs = st.executeQuery("SELECT �Ϸ�, ������� FROM exbidinfo " + where);
				int finished = 0;
				String dbResult = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbResult = rs.getString(2) == null ? "" : rs.getString(2);
				}
				if (finished > 0) {
					if (!dbResult.equals(prog)) {
						String sql = "UPDATE exbidinfo SET �������=\"" + prog + "\" " + where;
						st.executeUpdate(sql);
					}
					enter = false;
				}
			}
			else {
				String sql = "INSERT INTO exbidinfo (�����ȣ, �з�, ����, �����, �����Ͻ�, �������) VALUES (" +
						"\""+bidno+"\", \"" + wt + "\", \"" + area + "\", \"" + compType + "\", \"" + openDate + "\", \"" + prog + "\");";
				st.executeUpdate(sql);
			}
		}
		
		if (enter) {
			Element link = row.getElementsByTag("td").get(3).getElementsByTag("a").first();
			String itempath = ExParser.BASE_PATH;
			if (wt.equals("����")) itempath += "const";
			else if (wt.equals("�뿪")) itempath += "serv";
			else if (wt.equals("��ǰ")) itempath += "buy";
			
			if (it.equals("����")) {
				itempath += ExParser.ANN_INF;
				itempath += link.attr("href");
			}
			else if (it.equals("���")) {
				itempath += ExParser.RES_INF;
				itempath += link.attr("href").substring(2);
			}
			
			openConnection(itempath, "GET");
			Document itemdoc = Jsoup.parse(getResponse(null, "GET"));
			parseInfo(itemdoc, itempath, where);
		}
	}
	
	public void parseInfo(Document doc, String itempath, String where) throws SQLException, IOException {
		if (it.equals("����")) {
			String annDate = ""; // ��������
			String hasDup = ""; // �����������뿩��
			String hasRebid = ""; // ��������뿩��
			String elecBid = ""; // ������������
			String hasCommon = ""; // �������ް��ɿ���
			String fieldTour = ""; // ���弳��ǽÿ���
			String mustCommon = ""; // ���������ǹ�����
			String openDate = ""; // �����Ͻ�
			String protoPrice = ""; // ����ݾ�
			
			Elements tables = doc.getElementsByTag("caption");
			Elements headers = doc.getElementsByTag("th");
			for (Element h : headers) {
				String key = h.text().replaceAll(" ", "");
				if (key.equals("��������")) {
					annDate = h.nextElementSibling().text();
					annDate += " 00:00:00";
				}
				else if (key.equals("�����������뿩��")) {
					hasDup = h.nextElementSibling().text();
				}
				else if (key.equals("��������뿩��")) {
					hasRebid = h.nextElementSibling().text();
				}
				else if (key.equals("������������")) {
					elecBid = h.nextElementSibling().text();
				}
				else if (key.equals("�������ް��ɿ���")) {
					hasCommon = h.nextElementSibling().text();
				}
				else if (key.equals("���弳��ǽÿ���")) {
					fieldTour = h.nextElementSibling().text();
				}
				else if (key.equals("���������ǹ�����")) {
					mustCommon = h.nextElementSibling().text();
				}
				else if (key.equals("����ݾ�")) {
					protoPrice = h.nextElementSibling().text();
					protoPrice = protoPrice.replaceAll("[^\\d.]", "");
					if (protoPrice.equals("")) protoPrice = "0";
				}
				else if (key.equals("�����Ͻ�")) {
					openDate = h.nextElementSibling().text() + ":00";							
				}
			}
			
			for (int j = 0; j < tables.size(); j++) {
				String caption = tables.get(j).text();
				
				if (caption.equals("�������񰡰�")) {
					Element dupTable = tables.get(j).parent();
					Elements dupData = dupTable.getElementsByTag("td");
					
					for (int k = 0; k < dupData.size(); k++) {
						String dupPrice = dupData.get(k).text();
						dupPrice = dupPrice.replaceAll(",", "");
						st.executeUpdate("UPDATE exbidinfo SET ����"+(k+1)+"="+dupPrice+" " + where);
					}
				}
			}
			String sql = "UPDATE exbidinfo SET ����=1, " +
					"��������=\"" + annDate + "\", " +
					"������������=\"" + hasDup + "\", " +
					"��������뿩��=\"" + hasRebid + "\", " +
					"������������=\"" + elecBid + "\", " +
					"�������ް��ɿ���=\"" + hasCommon + "\", " +
					"���弳��ǽÿ���=\"" + fieldTour + "\", " +
					"���������ǹ�����=\"" + mustCommon + "\", " +
					"����ݾ�=" + protoPrice + ", " +
					"�����Ͻ�=\"" + openDate + "\" " + where;
			st.executeUpdate(sql);
		}
		else if (it.equals("���")) {
			Elements headers = doc.getElementsByTag("th");
			String annDate = ""; // ��������
			String expPrice = "0"; // ��������
			String protoPrice = "0"; // ����ݾ�
			String bidPrice = "0"; // �����ݾ�
			String comp = "0"; // ������
			
			for (Element h : headers) {
				String key = h.text();
				
				if (key.equals("��������")) {
					annDate = h.nextElementSibling().text() + " 00:00:00";
				}
				else if (key.equals("��������")) {
					expPrice = h.nextElementSibling().text();
					expPrice = expPrice.replaceAll("[^\\d.]", "");
					if (expPrice.equals("")) expPrice = "0";
				}
				else if (key.equals("���谡��")) {
					protoPrice = h.nextElementSibling().text();
					protoPrice = protoPrice.replaceAll("[^\\d.]", "");
					if (protoPrice.equals("")) protoPrice = "0";
				}
			}
			
			Elements captions = doc.getElementsByTag("caption");
			for (Element c : captions) {
				if (c.text().equals("������ü")) {
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
			if (wt.equals("����") || wt.equals("��ǰ")) buttonDiv = doc.getElementsByAttributeValue("class", "center_btn_area").first();
			else if (wt.equals("�뿪")) buttonDiv = doc.getElementsByAttributeValue("class", "btn_area").first();
			
			if (buttonDiv.getElementsContainingText("�����ǽð��").size() > 0 || buttonDiv.getElementsContainingOwnText("�������").size() > 0) {
				String pricepath = "";
				if (wt.equals("����")) pricepath = ExParser.CONST_PRICE;
				else if (wt.equals("�뿪")) pricepath = ExParser.SERV_PRICE;
				else if (wt.equals("��ǰ")) pricepath = ExParser.BUY_PRICE;
				
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
							String sql = "UPDATE exbidinfo SET ����"+ind+"="+price+" " + where;
							st.executeUpdate(sql);
						}
					}
				}
			}
			
			if (buttonDiv.getElementsContainingText("������ü��Ȳ").size() > 0) {
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
			
			String sql = "UPDATE exbidinfo SET �Ϸ�=1, " +
					"��������=\"" + annDate + "\", " +
					"��������=" + expPrice + ", " +
					"����ݾ�=" + protoPrice + ", " +
					"�����ݾ�=" + bidPrice + ", " +
					"������=" + comp + " " + where;
			st.executeUpdate(sql);
		}
	}
	
	public void run() {
		try {
			curItem = 0;
			setOption("�������");
			getList();
			setOption("������");
			getList();
			setOption("�뿪����");
			getList();
			setOption("�뿪���");
			getList();
			setOption("��ǰ����");
			getList();
			setOption("��ǰ���");
			getList();
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int getTotal() throws IOException {
		setOption("������");
		String path = getPage();
		openConnection(path, "GET");
		Document doc = Jsoup.parse(getResponse(null, "GET"));
		totalItems = Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("��")[0].replaceAll("[^\\d]", ""));
		setOption("�뿪���");
		path = getPage();
		openConnection(path, "GET");
		doc = Jsoup.parse(getResponse(null, "GET"));
		totalItems += Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("��")[0].replaceAll("[^\\d]", ""));
		setOption("��ǰ���");
		path = getPage();
		openConnection(path, "GET");
		doc = Jsoup.parse(getResponse(null, "GET"));
		totalItems += Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("��")[0].replaceAll("[^\\d]", ""));
		
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
