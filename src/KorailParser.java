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
		KorailParser tester = new KorailParser("2016-08-01", "2016-08-23", "��ǰ����");
		
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
		if (it.equals("����")) {
			if (wt.equals("��ǰ")) {
				path = KorailParser.PROD_ANN_LIST;
				param += "menu_code=A.3.1";
			}
			else if (wt.equals("�뿪")) {
				path = KorailParser.SERV_ANN_LIST;
				param += "menu_code=A.5.1";
			}
			else if (wt.equals("����")) {
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
		else if (it.equals("���")) {
			path = KorailParser.RES_LIST;
			
			if (wt.equals("��ǰ")) path += "menu_code=A.3.9"; 
			else if (wt.equals("�뿪")) path += "menu_code=A.5.7";
			else if (wt.equals("����")) path += "menu_code=A.4.8";
			
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
		if (op.equals("��ǰ����")) listTable = form.getElementsByTag("table").get(7);
		else if (op.equals("�뿪����")) listTable = form.getElementsByTag("table").get(7);
		else if (op.equals("��ǰ���")) listTable = form.getElementsByTag("table").get(4);
		else if (op.equals("�뿪���")) listTable = form.getElementsByTag("table").get(4);
		Elements rows = listTable.getElementsByTag("tr");
		for (int i = 4; i < 7; i += 2) {
			parseListRow(rows.get(i));
		}
		driver.close();
	}
	
	public void parseListRow(Element row) throws IOException, SQLException {
		Elements data = row.getElementsByTag("td");
		if (it.equals("����")) {
			String bidno = Jsoup.parse(data.get(0).html().split("<br>")[0]).text(); // �����ȣ
			String bidType = Jsoup.parse(data.get(0).html().split("<br>")[1]).text(); // �������
			String org = Jsoup.parse(data.get(1).html().split("<br>")[1]).text(); // ���ֺμ�
			String annType = Jsoup.parse(data.get(2).html().split("<br>")[0]).text(); // ����з�
			//String selectMethod = Jsoup.parse(data.get(2).html().split("<br>")[1]).text(); // �����ڼ���
			String endDate = Jsoup.parse(data.get(3).html().split("<br>")[1]).text(); // ���������Ͻ�
			String prog = Jsoup.parse(data.get(4).html().split("<br>")[1]).text(); // �������
			
			boolean enter = true;
			boolean exists = false;
			String where = "WHERE �����ȣ=\"" + bidno + "\"";
			
			rs = st.executeQuery("SELECT EXISTS(SELECT �����ȣ FROM korailbidinfo " + where + ")");
			if (rs.next()) exists = rs.getBoolean(1);
			
			if (exists) {
				String sql = "SELECT ����, ������� FROM korailbidinfo " + where;
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
						sql = "UPDATE korailbidinfo SET �������=\"" + prog + "\" " + where;
						st.executeUpdate(sql);
					}
				}
				else if (!dbProg.equals(prog)) {
					sql = "UPDATE korailbidinfo SET �������=\"" + prog + "\" " + where;
					st.executeUpdate(sql);
				}
			}
			else {
				// If entry doesn't exists in db, insert new row.
				String sql = "INSERT INTO korailbidinfo (�����ȣ, �������, ���ֺμ�, ����з�, ���������Ͻ�, �������) VALUES (" +
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
		else if (it.equals("���")) {
			String bidno = data.get(0).text(); // �����ȣ����
			String annType = data.get(2).text(); // ����з�
			String org = data.get(3).text(); // ���ֺμ�
			String openDate = data.get(4).text(); // �����Ͻ�
			
			boolean enter = true;
			boolean exists = false;
			String where = "WHERE �����ȣ=\"" + bidno + "\"";
			
			rs = st.executeQuery("SELECT EXISTS(SELECT �����ȣ FROM korailbidinfo " + where + ")");
			if (rs.next()) exists = rs.getBoolean(1);
			
			if (exists) {
				String sql = "SELECT �Ϸ� FROM korailbidinfo " + where;
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
				String sql = "INSERT INTO korailbidinfo (�����ȣ, ���ֺμ�, ����з�, �����Ͻ�) VALUES (" +
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
		if (it.equals("����")) parseNoti(doc, where);
		else parseRes(doc, where);
	}
	
	public void parseNoti(Document doc, String where) throws SQLException {
		Elements heads = doc.getElementsByTag("th");
		
		String workType = ""; // ������
		String compType = ""; // �����
		String hasRe = ""; // ������ ��뿩��
		String openDate = ""; // �����Ͻ�
		String priceMethod = ""; // �������
		String totalPrice = ""; // �ѿ�������
		String range = ""; // ��������
		String basePrice = ""; // ���ʱݾ�
		
		for (Element h : heads) {
			String key = h.text();
			if (key.equals("������")) {
				workType = h.nextElementSibling().text();
			}
			else if (key.equals("�����")) {
				compType = h.nextElementSibling().text();
			}
			else if (key.equals("������ ��뿩��")) {
				hasRe = h.nextElementSibling().text();
			}
			else if (key.equals("��������")) {
				openDate = h.nextElementSibling().text();
			}
			else if (key.equals("�������")) {
				priceMethod = h.nextElementSibling().text();
			}
			else if (key.equals("�ѿ�������")) {
				totalPrice = h.nextElementSibling().text();
			}
			else if (key.equals("��������")) {
				range = h.nextElementSibling().text();
			}
			else if (key.equals("�������ʱݾ�")) {
				basePrice = h.nextElementSibling().text();
			}
		}
		
		String sql = "UPDATE korailbidinfo SET ������=\""+workType+"\", " +
				"�����=\""+compType+"\", " +
				"��������뿩��=\""+hasRe+"\", " +
				"�����Ͻ�="+openDate+", " +
				"�������=\""+priceMethod+"\", " +
				"�ѿ�������="+totalPrice+", " +
				"��������=\""+range+"\", " +
				"���ʱݾ�="+basePrice+" " + where;
		st.executeUpdate(sql);
	}
	
	public void parseRes(Document doc, String where) throws SQLException {
		Elements heads = doc.getElementsByTag("td");
		
		String compType = ""; // �����
		String openDate = ""; // �����Ͻ�
		String basePrice = ""; // ���ʱݾ�
		String expPrice = ""; // ��������
		String comp = ""; // ������
		String result = ""; // �������
		String bidPrice = ""; // �����ݾ�
		
		for (Element h : heads) {
			String key = h.text();
			if (key.equals("�����")) {
				compType = h.nextElementSibling().text();
			}
			else if (key.equals("�����Ͻ�")) {
				openDate = h.nextElementSibling().text();
			}
			else if (key.equals("����(����)�ݾ�")) {
				basePrice = h.nextElementSibling().text();
			}
			else if (key.equals("��������")) {
				expPrice = h.nextElementSibling().text();
			}
			else if (key.equals("������ü��")) {
				comp = h.nextElementSibling().text();
			}
			else if (key.equals("�������")) {
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
					
					String s = "UPDATE korailbidinfo SET ����" + ind + "=" + price + ", ����" + ind + "=" + dup + " " + where;
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
				String s = "UPDATE korailbidinfo SET ����" + ind + "=" + price + " " + where;
				st.executeUpdate(s);
				ind++;
			}
		}
		
		Element detailTable = doc.getElementById("gen_fact_2-bd-scroll");
		if (detailTable != null) {
			Element row = detailTable.getElementsByTag("tr").get(2);
			bidPrice = row.getElementsByTag("td").get(4).text();
		}
		
		String sql = "UPDATE korailbidinfo SET �����=\""+compType+"\", " +
				"�����Ͻ�=\""+openDate+"\", " +
				"�������=\""+result+"\", " +
				"������="+comp+", " +
				"�����ݾ�="+bidPrice+", " +
				"��������="+expPrice+", " +
				"���ʱݾ�="+basePrice+" " + where;
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
