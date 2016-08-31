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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DapaParser extends Parser {
	final static String BID_ANN_LIST = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=421&cfn=HI_PEB_Announce_Lst";
	final static String BID_RES_LIST = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=428&cfn=HI_PEB_BidResult_Lst";
	final static String NEGO_ANN_LIST = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=441&cfn=HI_PEB_OpenNego_Lst";
	final static String NEGO_RES_LIST = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=443&cfn=HI_PEB_OpenNegoResult_Lst";
	
	final static String BID_ANN_INF = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=421&cfn=HI_PEB_Announce_Inf&pointNumb=";
	final static String BID_RES_INF = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=428&cfn=HI_PEB_BidResult_Inf";
	final static String NEGO_ANN_INF = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=441&cfn=HI_PEB_OpenNego_Inf";
	final static String NEGO_RES_INF = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=443&cfn=HI_PEB_OpenNegoResult_Inf";
	
	final static String BID_ANN_PRICE = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=421&cfn=HI_PEB_PrePrice_Inf";
	final static String BID_RES_PRICE = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=428&cfn=HI_PEB_MultiPrePrice_Inf";
	final static String NEGO_RES_PRICE = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=443&cfn=HI_PEB_OpenNegoMultiPrePrice_Inf1";
	
	// For SQL setup.
	Connection db_con;
	java.sql.Statement st;
	ResultSet rs;
	
	URL url;
	HttpURLConnection con;
	HashMap<String, String> formData;
	String sd;
	String ed;
	String op;
	int totalItems;
	int curItem;
	
	GetFrame f; // TEST
	
	public DapaParser(String sd, String ed, String op, GetFrame f) throws SQLException, ClassNotFoundException {
		sd = sd.replaceAll("-", "%2F");
		ed = ed.replaceAll("-", "%2F");
		
		this.sd = sd;
		this.ed = ed;
		this.op = op;
		this.f = f;
		
		totalItems = 0;
		curItem = 0;
		
		formData = new HashMap<String, String>();
		
		// Set up SQL connection.
		Class.forName("com.mysql.jdbc.Driver");
		db_con = DriverManager.getConnection("jdbc:mysql://localhost/"+Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
		st = db_con.createStatement();
		rs = null;
	}
	
	public static void main(String args[]) throws IOException, ClassNotFoundException, SQLException {
		DapaParser tester = new DapaParser("2008/03/01", "2008/03/31", "�������", null);
		
		tester.run();
	}
	
	public void openConnection(String path) throws IOException {
		url = new URL(path);
		con = (HttpURLConnection) url.openConnection();
		
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4");
	}
	
	public String getResponse(String param) throws IOException {
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(param);
		wr.flush();
		wr.close();
		
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
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
	
	public void getList() throws IOException, SQLException {
		String path = "";
		if (op.equals("��������")) path = DapaParser.BID_ANN_LIST;
		else if (op.equals("�������")) path = DapaParser.BID_RES_LIST;
		else if (op.equals("�������")) path = DapaParser.NEGO_ANN_LIST;
		else if (op.equals("������")) path = DapaParser.NEGO_RES_LIST;
		else {
			System.out.println("Declare the operation!");
			return;
		}
		
		openConnection(path);
		
		String param = "";
		
		if (op.equals("��������") || op.equals("�������")) {
			param = "pageNo=1&startPageNo=1&pagePerRow=10&txtBidxDateFrom="+sd+"&txtBidxDateTo="+ed;
		}
		else if (op.equals("�������") || op.equals("������")) {
			param = "pageNo=1&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+sd+"&txtNegoDateTo="+ed;
		}
		
		Document doc = Jsoup.parse(getResponse(param));
		if (doc.getElementsContainingOwnText("��ü �Ǽ� :").size() != 0) {
			totalItems = Integer.parseInt(doc.getElementsContainingOwnText("��ü �Ǽ� :").text().split(" ")[3]);
		}
		else totalItems = 0;
		
		String svrKey = "";
		if (op.equals("�������")) {
			svrKey = doc.getElementsByAttributeValue("name", "hidSvrKey").first().val();
		}
		
		int page = 1;
		int index = 1;
		
		Element table = doc.getElementsByTag("table").get(0);
		Elements rows = table.getElementsByTag("tr");
		
		for (int i = 0; i < totalItems; i++) {
			if (op.equals("�������") || op.equals("������")) curItem++;
			Element row = rows.get(index);
			if (f != null) {
				if (op.equals("��������")) f.updateInfo(row.getElementsByTag("td").get(3).text(), false);
				else if (op.equals("�������")) f.updateInfo(row.getElementsByTag("td").get(1).text(), true);
				else if (op.equals("�������")) f.updateInfo(row.getElementsByTag("td").get(2).text(), false);
				else if (op.equals("������")) f.updateInfo(row.getElementsByTag("td").get(1).text(), true);
				f.repaint();
			}
			boolean enter = parseListRow(row);
			if (enter) {
				// Collect post request parameters.
				Elements inputs = row.getElementsByTag("input");
				formData.clear();
				for (Element input : inputs) {
					formData.put(input.attr("name"), input.val());
				}
				if (op.equals("�������")) {
					formData.put("hidSvrKey", svrKey);
				}
				
				getInfo(index - 1, page); // Load the info page and continue parsing.
			}
			
			if (i % 10 == 9 && i < (totalItems - 1)) {
				index = 1;
				page++;
				
				// Load next listing page.
				openConnection(path);
				if (op.equals("��������") || op.equals("�������")) {
					param = "pageNo="+page+"&startPageNo=1&pagePerRow=10&txtBidxDateFrom="+sd+"&txtBidxDateTo="+ed;
				}
				else if (op.equals("�������") || op.equals("������")) {
					param = "pageNo="+page+"&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+sd+"&txtNegoDateTo="+ed;
				}
				doc = Jsoup.parse(getResponse(param));
				table = doc.getElementsByTag("table").get(0);
				rows = table.getElementsByTag("tr");
			}
			else {
				index++;
			}
		}
	}
	
	public boolean parseListRow(Element row) throws SQLException {
		Elements data = row.getElementsByTag("td");
		boolean enter = true;
		
		if (op.equals("��������")) {
			String[] bidID = data.get(2).text().replaceAll("\u00a0", "").split(" ");
			String bidType = bidID[1]; // ��������
			String bidCode = bidID[2]; // �����ȣ
			String facilNum = data.get(3).text(); // �����ȣ
			String org = data.get(5).text(); // ���ֱ��
			String[] deadlines = data.get(6).text().split(" ");
			String paperDeadline = ""; // ���������� �����Ͻ�
			String openDate = ""; // ��������
			if (deadlines.length == 4) {
				paperDeadline = null;
				openDate = deadlines[2] + " " + deadlines[3];
			}
			else if (deadlines.length == 6) {
				paperDeadline = deadlines[2] + " " + deadlines[3];
				openDate = deadlines[4] + " " + deadlines[5];
			}
			else if (deadlines.length == 7) {
				paperDeadline = deadlines[3] + " " + deadlines[4];
				openDate = deadlines[5] + " " + deadlines[6];
			}
			else if (deadlines.length == 8) {
				paperDeadline = deadlines[4] + " " + deadlines[5];
				openDate = deadlines[6] + " " + deadlines[7];
			}
			else if (deadlines.length == 1) {
				paperDeadline = null;
				openDate = deadlines[0] + " 00:00:00";
			}
			String compType = data.get(7).text().split(" ")[0]; // �����
			String priceOpen = data.get(8).text(); // ���ʿ�������
			
			boolean exists = false;
			
			String[] codeSplit = bidCode.split("-");
			String bidno = codeSplit[0];
			String bidver = codeSplit[1];
			
			String where = "WHERE �����ȣ=\""+bidno+"\" AND " +
					"����="+bidver+" AND " +
					"�����ȣ=\""+facilNum+"\" AND " +
					"���ֱ��=\""+org+"\"";
			String sql = "SELECT EXISTS(SELECT �����ȣ FROM dapabidinfo "+where+");";
			rs = st.executeQuery(sql);
			if (rs.first()) exists = rs.getBoolean(1);
			
			if (exists) {
				// Check the bid version and update level from the DB.
				sql = "SELECT ����, ���ʿ������� FROM dapabidinfo "+where;
				rs = st.executeQuery(sql);
				int finished = 0;
				String dbPriceOpen = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbPriceOpen = rs.getString(2) == null ? "" : rs.getString(2);
				}
				if (finished > 0) {
					if (dbPriceOpen.equals(priceOpen)) enter = false;
					else {
						sql = "UPDATE dapabidinfo SET ���ʿ�������=\""+priceOpen+"\" "+where;
						st.executeUpdate(sql);
					}
				}
				else if (!dbPriceOpen.equals(priceOpen)) {
					sql = "UPDATE dapabidinfo SET ���ʿ�������=\""+priceOpen+"\" "+where;
					st.executeUpdate(sql);
				}
			}
			else {
				// If entry doesn't exists in db, insert new row.
				sql = "INSERT INTO dapabidinfo (�����ȣ, ����, ��������, �����ȣ, ���ֱ��, �����Ͻ�, �����, ���ʿ�������) VALUES (" +
						"\""+bidno+"\", " +
						""+bidver+", " +
						"\""+bidType+"\", " +
						"\""+facilNum+"\", " +
						"\""+org+"\", " +
						"\""+openDate+"\", " +
						"\""+compType+"\", " +
						"\""+priceOpen+"\");";
				st.executeUpdate(sql);
				if (paperDeadline != null) {
					sql = "UPDATE dapabidinfo SET ���������⸶���Ͻ�=\""+paperDeadline+"\" " + where;
					st.executeUpdate(sql);
				}
				if (compType.equals("�������")) {
					sql = "UPDATE dapabidinfo SET ����=1 " + where;
					st.executeUpdate(sql);
					enter = false;
				}
			}
		}
		else if (op.equals("�������")) {
			String[] bidID = data.get(0).text().split(" ");
			String bidType = bidID[0]; // ��������
			String bidCode = bidID[1]; // �����ȣ-����
			String facilNum = data.get(1).text().replaceAll("\u00a0", ""); // �����ȣ
			String org = data.get(3).text(); // ���ֱ��
			String compType = data.get(4).text(); // �����
			String openDate = data.get(7).text() + ":00"; // �����Ͻ�
			String result = data.get(8).text(); // �������
			
			boolean exists = false;
			
			String[] codeSplit = bidCode.split("-");
			String bidno = codeSplit[0]; // �����ȣ
			String bidver = codeSplit[1]; // ����
			
			String where = "WHERE �����ȣ=\""+bidno+"\" AND " +
					"����="+bidver+" AND " +
					"�����ȣ=\""+facilNum+"\" AND " +
					"���ֱ��=\""+org+"\"";
			String sql = "SELECT EXISTS(SELECT �����ȣ FROM dapabidinfo "+where+");";
			rs = st.executeQuery(sql);
			if (rs.first()) exists = rs.getBoolean(1);
			
			if (exists) {
				// Check the bid version and update level from the DB.
				sql = "SELECT �Ϸ�, ������� FROM dapabidinfo "+where;
				rs = st.executeQuery(sql);
				int finished = 0;
				String dbResult = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbResult = rs.getString(2) == null ? "" : rs.getString(2);
				}
				if (finished > 0) {
					if (dbResult.equals(result)) enter = false;
					else {
						sql = "UPDATE dapabidinfo SET �������=\""+result+"\" "+where;
						st.executeUpdate(sql);
					}
				}
				else if (!dbResult.equals(result)) {
					sql = "UPDATE dapabidinfo SET �������=\""+result+"\" "+where;
					st.executeUpdate(sql);
				}
			}
			else {
				// If entry doesn't exists in db, insert new row.
				sql = "INSERT INTO dapabidinfo (�����ȣ, ����, ��������, �����ȣ, ���ֱ��, �����Ͻ�, �����, �������) VALUES (" +
						"\""+bidno+"\", " +
						""+bidver+", " +
						"\""+bidType+"\", " +
						"\""+facilNum+"\", " +
						"\""+org+"\", " +
						"\""+openDate+"\", " +
						"\""+compType+"\", " +
						"\""+result+"\");";
				st.executeUpdate(sql);
			}
		}
		else if (op.equals("�������")) {
			String[] datefrag = data.get(1).text().split(" ");
			String negoDate = ""; // �����Ͻ�
			if (datefrag.length == 4) negoDate = datefrag[2] + " " + datefrag[3] + ":00";
			else if (datefrag.length == 5) negoDate = datefrag[3] + " " + datefrag[4] + ":00";
			String facilNum = data.get(2).text(); // �����ȣ
			String negoVer = data.get(3).text(); // ����
			String org = data.get(5).text(); // ���ֱ��
			String prog = data.get(9).text(); // �������
			
			boolean exists = false;
			
			String where = "WHERE �����ȣ=\""+facilNum+"\" AND " +
					"����="+negoVer+" AND " +
					"���ֱ��=\""+org+"\"";
			String sql = "SELECT EXISTS(SELECT �����ȣ FROM dapanegoinfo "+where+");";
			rs = st.executeQuery(sql);
			if (rs.first()) exists = rs.getBoolean(1);
			
			if (exists) {
				// Check the bid version and update level from the DB.
				sql = "SELECT ����, ������� FROM dapanegoinfo "+where;
				rs = st.executeQuery(sql);
				int finished = 0;
				String dbProg = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbProg = rs.getString(2) == null ? "" : rs.getString(2);
				}
				if (finished > 0) {
					if (dbProg.equals(prog)) enter = false;
					else {
						sql = "UPDATE dapanegoinfo SET �������=\""+prog+"\" "+where;
						st.executeUpdate(sql);
					}
				}
				else if (!dbProg.equals(prog)) {
					sql = "UPDATE dapanegoinfo SET �������=\""+prog+"\" "+where;
					st.executeUpdate(sql);
				}
			}
			else {
				// If entry doesn't exists in db, insert new row.
				sql = "INSERT INTO dapanegoinfo (����, �����ȣ, ���ֱ��, �����Ͻ�, �������) VALUES (" +
						""+negoVer+", " +
						"\""+facilNum+"\", " +
						"\""+org+"\", " +
						"\""+negoDate+"\", " +
						"\""+prog+"\");";
				st.executeUpdate(sql);
			}
		}
		else if (op.equals("������")) {
			String negoDate = data.get(0).text() + ":00"; // �����Ͻ�
			String facilNum = data.get(1).text(); // �����ȣ
			String negoVer = data.get(2).text(); // ����
			String org = data.get(4).text(); // ���ֱ��
			String result = data.get(7).text(); // ������
			
			boolean exists = false;
			
			String where = "WHERE �����ȣ=\""+facilNum+"\" AND " +
					"����="+negoVer+" AND " +
					"���ֱ��=\""+org+"\"";
			String sql = "SELECT EXISTS(SELECT �����ȣ FROM dapanegoinfo "+where+");";
			rs = st.executeQuery(sql);
			if (rs.first()) exists = rs.getBoolean(1);
			
			if (exists) {
				// Check the bid version and update level from the DB.
				sql = "SELECT �Ϸ�, ������ FROM dapanegoinfo "+where;
				rs = st.executeQuery(sql);
				int finished = 0;
				String dbResult = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbResult = rs.getString(2) == null ? "" : rs.getString(2);
				}
				if (finished > 0) {
					if (dbResult.equals(result)) enter = false;
					else {
						sql = "UPDATE dapanegoinfo SET ������=\""+result+"\" "+where;
						st.executeUpdate(sql);
					}
				}
				else if (!dbResult.equals(result)) {
					sql = "UPDATE dapanegoinfo SET ������=\""+result+"\" "+where;
					st.executeUpdate(sql);
				}
			}
			else {
				// If entry doesn't exists in db, insert new row.
				sql = "INSERT INTO dapanegoinfo (����, �����ȣ, ���ֱ��, �����Ͻ�, ������) VALUES (" +
						""+negoVer+", " +
						"\""+facilNum+"\", " +
						"\""+org+"\", " +
						"\""+negoDate+"\", " +
						"\""+result+"\");";
				st.executeUpdate(sql);
			}
		}
		
		return enter;
	}
	
	public void getInfo(int hidNumb, int page) throws IOException, SQLException {
		String path = "";
		
		if (op.equals("��������")) path = DapaParser.BID_ANN_INF;
		else if (op.equals("�������")) path = DapaParser.BID_RES_INF;
		else if (op.equals("�������")) path = DapaParser.NEGO_ANN_INF;
		else if (op.equals("������")) path = DapaParser.NEGO_RES_INF;
		else {
			System.out.println("Declare the operation!");
			return;
		}
		
		// Load info page.
		if (path.equals(DapaParser.BID_ANN_INF)) path += hidNumb;
		openConnection(path);
		String param = "";
		if (op.equals("��������") || op.equals("�������")) {
			param = "hidChkNumb=0&pageNo="+page+"&startPageNo=1&pagePerRow=10&txtBidxDateFrom="+sd+"&txtBidxDateTo="+ed+"&";
		}
		else if (op.equals("�������")) {
			param = "hidChkNumb=0&pageNo="+page+"&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+sd+"&txtNegoDateTo="+ed+"&";
		}
		else if (op.equals("������")) {
			param = "hidChkNumb=0&pageNo=1&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+sd+"&txtNegoDateTo="+ed+"&";
		}
		
		Iterator<Entry<String, String>> it = formData.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			if (pair.getValue() != null) {
				String val = pair.getValue().toString();
				if (val.contains("�뿪")) val = val.replaceAll("�뿪", "%BF%EB%BF%AA");
				else if (val.contains("�ü�")) val = val.replaceAll("�ü�", "%BD%C3%BC%B3");
				else if (val.contains("����")) val = val.replaceAll("����", "%B0%A8%B8%AE");
				else if (val.contains("����")) val = val.replaceAll("����", "%B0%F8%BF%F8");
				else if (val.contains("����")) val = val.replaceAll("����", "%BA%B8%BC%F6");
				else if (val.contains("å��")) val = val.replaceAll("å��", "%C3%A5%C0%D3");
				else if (val.contains("������")) val = val.replaceAll("������", "%B9%E6%C6%F7%B1%B3");
				else if (val.contains("ǥ�ص�����")) val = val.replaceAll("ǥ�ص�����", "%C7%A5%C1%D8%B5%B5%BC%B3%B0%E8");
				else if (val.contains("����")) val = val.replaceAll("����", "%BC%B3%B0%E8");
				else if (val.contains("��⹰")) val = val.replaceAll("��⹰", "%C6%F3%B1%E2%B9%B0");
				else if (val.contains("���")) val = val.replaceAll("���", "%C6%F3%B1%E2");
				else if (val.contains("��")) val = val.replaceAll("��", "%C6%F3");
				else if (val.contains("��������")) val = val.replaceAll("��������", "%C1%F6%BF%AA%C1%A4%B9%D0");
				else if (val.contains("�ǹ�����")) val = val.replaceAll("�ǹ�����", "%B0%C7%B9%B0%BE%C8%C0%FC");
				else if (val.contains("���о���")) val = val.replaceAll("���о���", "%C1%A4%B9%D0%BE%C8%C0%FC");
				else if (val.contains("����")) val = val.replaceAll("����", "%BE%C8%C0%FC");
				else if (val.contains("����")) val = val.replaceAll("����", "%B0%E6%C0%EF");
				else if (val.contains("��")) val = val.replaceAll("��", "%BD%C3");
				else if (val.contains("��")) val = val.replaceAll("��", "%BA%B9");
				else if (val.contains("��")) val = val.replaceAll("��", "%BA%B8");
				else if (val.contains("��")) val = val.replaceAll("��", "%A4%D1");
				else if (val.contains("��")) val = val.replaceAll("��", "%C0%B0");
				else if (val.contains("��")) val = val.replaceAll("��", "%C0%B8");
				else if (val.contains("��")) val = val.replaceAll("��", "%B0%F8");
				else if (val.contains("��Ÿ")) val = val.replaceAll("��Ÿ", "%B1%E2%C5%B8");
				param += pair.getKey() + "=" + val + "&";
			}
		}
		param = param.substring(0, param.length()-1);
		
		// Start parsing.
		Document doc = Jsoup.parse(getResponse(param));
		String where = parseInfo(doc);
		
		if (where != null) {
			boolean getprice = false;
			if (op.equals("��������")) {
				if (doc.getElementsByAttributeValue("alt", "���ʿ��񰡰���ȸ").size() > 0) getprice = true;
			}
			else if (op.equals("�������") || op.equals("������")) {
				if (doc.getElementsByAttributeValue("alt", "����������ȸ").size() > 0) getprice = true;
			}
			
			if (getprice) {
				Element form = doc.getElementsByAttributeValue("name", "form").get(0);
				Elements formElements = form.children();
				
				formData.clear();
				for (Element e : formElements) {
					if (e.tagName().equals("input")) {
						String key = e.attr("name");
						String val = e.val();
						formData.put(key, val);
					}
				}
				
				getPrice(where);
			}
			else {
				if (op.equals("��������")) {
					String sql = "UPDATE dapabidinfo SET ����=1 "+where;
					st.executeUpdate(sql);
				}
				else if (op.equals("�������")) {
					String sql = "UPDATE dapabidinfo SET �Ϸ�=1 "+where;
					st.executeUpdate(sql);
				}
				else if (op.equals("�������")) {
					String sql = "UPDATE dapanegoinfo SET ����=1 "+where;
					st.executeUpdate(sql);
				}
				else if (op.equals("������")) {
					String sql = "UPDATE dapanegoinfo SET �Ϸ�=1 "+where;
					st.executeUpdate(sql);
				}
			}
		}
	}

	public String parseInfo(Document doc) throws SQLException {
		String where = "";
		
		if (op.equals("��������")) {
			Element infoTable = doc.getElementsByTag("table").first();
			Elements infos = infoTable.getElementsByTag("th");
			
			String bidno = ""; // �����ȣ
			String bidver = ""; // ����
			String facilNum = ""; // �����ȣ
			String org = ""; // ���ֱ��
			String license = ""; // �����Ī
			String bidMethod = ""; // �������
			String hasBase = ""; // ���ʿ������뿩��
			String prelim = ""; // �����ɻ�
			String selectMethod = ""; // �����ڰ������
			
			for (int j = 0; j < infos.size(); j++) {
				String key = infos.get(j).text();
				if (key.equals("�����ȣ-����")) {
					String check = infos.get(j).nextElementSibling().text();
					if (check.split(" ").length > 1) {
						String value = infos.get(j).nextElementSibling().text().split(" ")[1];					
						bidno = value.split("-")[0];
						bidver = value.split("-")[1];
					}
					else return null;
				}
				else if (key.equals("�����ȣ")) {
					facilNum = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("���ֱ��")) {
					org = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("�������")) {
					bidMethod = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("���ʿ������뿩��")) {
					hasBase = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("�����ɻ�")) {
					prelim = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("�����ڰ������")) {
					selectMethod = infos.get(j).nextElementSibling().text();
				}
			}
			
			if (doc.getElementsByAttributeValue("summary", "�׷�, �����Ī�� �����ֽ��ϴ�").size() != 0) {
				Element licenseTable = doc.getElementsByAttributeValue("summary", "�׷�, �����Ī�� �����ֽ��ϴ�").get(0);
				Elements licenses = licenseTable.getElementsByTag("tr"); // Rows for the table of licenses
				
				for (int j = 1; j < licenses.size(); j++) {
					String li = licenses.get(j).getElementsByTag("td").get(1).text();
					li = li.replaceAll("\u00a0", "");
					license += " " + li;
				}
				System.out.println(license.length());
				if (license.length() > 254) license = license.substring(0, 254);
			}
			
			where = "WHERE �����ȣ=\""+bidno+"\" AND " +
					"����="+bidver+" AND " +
					"�����ȣ=\""+facilNum+"\" AND " +
					"���ֱ��=\""+org+"\"";
			String sql = "UPDATE dapabidinfo SET �����Ī=\""+license+"\", " +
					"�������=\""+bidMethod+"\", " +
					"���ʿ������뿩��=\""+hasBase+"\", " +
					"�����ɻ�=\""+prelim+"\", " +
					"�����ڰ������=\""+selectMethod+"\" " + where;
			st.executeUpdate(sql);
		}
		else if (op.equals("�������")) {
			Element infoTable = doc.getElementsByTag("table").first();
			Elements infos = infoTable.getElementsByTag("th");
			
			String bidno = ""; // �����ȣ
			String bidver = ""; // ����
			String facilNum = ""; // �����ȣ
			String org = ""; // ���ֱ��
			String bidMethod = ""; // �������
			String prelim = ""; // �����ɻ�
			String selectMethod = ""; // �����ڰ������
			String basePrice = "0"; // ���ʿ��񰡰�
			String expPrice = "0"; // ��������
			String rate = ""; // ������
			String bidBound = ""; // ����������
			String bidPrice = "0"; // �����ݾ�
			String comp = "0"; // ������
			
			for (int j = 0; j < infos.size(); j++) {
				String key = infos.get(j).text();
				if (key.equals("�����ȣ")) {
					String check = infos.get(j).nextElementSibling().text();
					if (check.split(" ").length > 1) {
						String value = infos.get(j).nextElementSibling().text().split(" ")[1];					
						bidno = value.split("-")[0];
						bidver = value.split("-")[1];
					}
					else return null;
				}
				else if (key.equals("�����ȣ")) {
					facilNum = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("���ֱ��")) {
					org = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("�������")) {
					bidMethod = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("�����ɻ�")) {
					prelim = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("�����ڰ������")) {
					selectMethod = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("���ʿ��񰡰�")) {
					basePrice = infos.get(j).nextElementSibling().text();
					basePrice = basePrice.replaceAll("[^\\d]", "");
					if (basePrice.equals("")) basePrice = "0";
				}
				else if (key.equals("��������")) {
					expPrice = infos.get(j).nextElementSibling().text();
					expPrice = expPrice.replaceAll("[^\\d]", "");
					if (expPrice.equals("")) expPrice = "0";
				}
				else if (key.equals("������(%)")) {
					rate = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("����������(%)")) {
					bidBound = infos.get(j).nextElementSibling().text();
				}
			}
			
			String summary = "����, ��ü�ڵ� ��ü��,��ǥ��,�����ݾ�,������,����� �˼� �ֽ��ϴ�.";
			if (doc.getElementsByAttributeValue("summary", summary).size() != 0) {
				Element resultTable = doc.getElementsByAttributeValue("summary", summary).get(0);
				if (resultTable.getElementsByTag("tr").size() > 1) {
					Element top = resultTable.getElementsByTag("tr").get(1);
					
					bidPrice = top.getElementsByTag("td").get(4).text();
					bidPrice = bidPrice.replaceAll("[^\\d]", "");
					if (bidPrice.equals("")) bidPrice = "0";
					
					if (doc.getElementsContainingOwnText("��ü �Ǽ� :").size() > 0) {
						comp = doc.getElementsContainingOwnText("��ü �Ǽ� :").text().split(" ")[3];
						if (comp.equals("")) comp = "0";
					}
				}
			}
			
			where = "WHERE �����ȣ=\""+bidno+"\" AND " +
					"����="+bidver+" AND " +
					"�����ȣ=\""+facilNum+"\" AND " +
					"���ֱ��=\""+org+"\"";
			String sql = "UPDATE dapabidinfo SET �������=\""+bidMethod+"\", " +
					"�����ɻ�=\""+prelim+"\", " +
					"���ʿ��񰡰�="+basePrice+", " +
					"��������="+expPrice+", " +
					"������=\""+rate+"\", " +
					"����������=\""+bidBound+"\", " +
					"�����ݾ�="+bidPrice+", " +
					"������="+comp+", " +
					"�����ڰ������=\""+selectMethod+"\" " + where;
			st.executeUpdate(sql);
		}
		else if (op.equals("�������")) {
			String negno = ""; // �����ȣ
			String negoVer = ""; // ����
			String facilNum = ""; // �����ȣ
			String org = ""; // ���ֱ��
			String compType = ""; // �����
			String selectMethod = ""; // �����ڰ������
			String bidMethod = ""; // �������
			String hasBase = ""; // ���ʿ������뿩��
			String paperDeadline = ""; // ���������⸶���ð�
			String negoMethod = ""; // ��������
			String basePrice = "0"; // ���ʿ��񰡰�
			String lowerBound = ""; // ����
			String upperBound = ""; // ����
			String bidBound = ""; // ����������
			String license = ""; // �����Ī
			
			Element infoTable = doc.getElementsByTag("table").first();
			Elements infos = infoTable.getElementsByTag("th"); // Headers for table of details
			for (int j = 0; j < infos.size(); j++) {
				String key = infos.get(j).text();
				if (key.equals("�����ȣ-����")) {
					String check = infos.get(j).nextElementSibling().text();
					if (check.split(" ").length > 1) {
						String value = infos.get(j).nextElementSibling().text().split(" ")[1];					
						negno = value.split("-")[0];
						negoVer = value.split("-")[1];
					}
					else return null;
				}
				else if (key.equals("���ֱ��")) {
					org = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("�����ȣ")) {
					facilNum = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("�����")) {
					compType = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("�����ڰ������")) {
					selectMethod = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("�������")) {
					bidMethod = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("���ʿ������뿩��")) {
					hasBase = infos.get(j).nextElementSibling().text();
				}
				else if (key.equals("���������⸶���ð�")) {
					paperDeadline = infos.get(j).nextElementSibling().text() + ":00";
				}
				else if (key.equals("��������")) {
					negoMethod = infos.get(j).nextElementSibling().text();
				}
			}
			
			Element licenseTable = null;
			Element minPriceTable = null;
			Elements tableNames = doc.getElementsByTag("caption");
			for (int j = 0; j < tableNames.size(); j++) {
				String name = tableNames.get(j).text();
				if (name.equals("�׷�")) {
					licenseTable = tableNames.get(j).parent();
				}
				else if (name.equals("���ʿ��񰡰� ��ȸ")) {
					minPriceTable = tableNames.get(j).parent();
				}
			}
			
			if (minPriceTable != null) {
				Elements minPrices = minPriceTable.getElementsByTag("th");
				for (int j = 0; j < minPrices.size(); j++) {
					String key = minPrices.get(j).text();
					if (key.equals("����(%)")) {
						lowerBound = minPrices.get(j).nextElementSibling().text();
					}
					else if (key.equals("����(%)")) {
						upperBound = minPrices.get(j).nextElementSibling().text();
					}
					else if (key.equals("����������(%)")) {
						bidBound = minPrices.get(j).nextElementSibling().text();
					}
					else if (key.equals("���ʿ��񰡰�")) {
						basePrice = minPrices.get(j).nextElementSibling().text();
						basePrice = basePrice.replaceAll("[^\\d]", "");
						if (basePrice.equals("")) basePrice = "0";
					}
				}
			}
			
			if (licenseTable != null) {
				Elements licenses = licenseTable.getElementsByTag("tr"); // Rows for the table of licenses
				
				// Compile the licenses into one string.
				for (int j = 1; j < licenses.size(); j++) {
					String li = licenses.get(j).getElementsByTag("td").get(1).text();
					li = li.replaceAll("\u00a0", "");
					license += " " + li;
				}
				if (license.length() > 255) license = license.substring(0, 254);
			}
			
			where = "WHERE �����ȣ=\""+facilNum+"\" AND " +
					"����="+negoVer+" AND " +
					"���ֱ��=\""+org+"\"";
			
			String sql = "UPDATE dapanegoinfo SET �����=\""+compType+"\", " +
					"�����ڰ������=\""+selectMethod+"\", " +
					"�������=\""+bidMethod+"\", " +
					"���ʿ������뿩��=\""+hasBase+"\", " +
					"���������⸶���Ͻ�=\""+paperDeadline+"\", " +
					"��������=\""+negoMethod+"\", " +
					"����=\""+upperBound+"\", " +
					"����=\""+lowerBound+"\", " +
					"����������=\""+bidBound+"\", " +
					"���ʿ��񰡰�="+basePrice+", " +
					"�����Ī=\""+license+"\", " +
					"����=1, " +
					"�����ȣ=\""+negno+"\" " + where;
			st.executeUpdate(sql);
		}
		else if (op.equals("������")) {
			String negno = ""; // �����ȣ
			String negoVer = ""; // ����
			String org = ""; // ���ֱ��
			String facilNum = ""; // �����ȣ
			String compType = ""; // �����
			String selectMethod = ""; // �����ڰ������
			String bidMethod = ""; // �������
			String hasBase = ""; // ���ʿ������뿩��
			String paperDeadline = ""; // ���������⸶���ð�
			String negoMethod = ""; // ��������
			String basePrice = "0"; // ���ʿ��񰡰�
			String lowerBound = ""; // ����
			String upperBound = ""; // ����
			String bidBound = ""; // ����������
			String expPrice = "0"; // ��������
			String bidPrice = "0"; // �����ݾ�
			String comp = "0"; // ������
			
			Element infoTable = doc.getElementsByTag("table").first();
			Elements infos = infoTable.getElementsByTag("th"); // Headers for table of details
			for (int j = 0; j < infos.size(); j++) {
				String key = infos.get(j).text();
				String value = infos.get(j).nextElementSibling().text();
				if (key.equals("�����ȣ-����")) {
					String check = infos.get(j).nextElementSibling().text();
					if (check.split(" ").length > 1) {
						value = infos.get(j).nextElementSibling().text().split(" ")[1];					
						negno = value.split("-")[0];
						negoVer = value.split("-")[1];
					}
					else return null;
				}
				else if (key.equals("���ֱ��")) {
	        		org = value;
		        }
				else if (key.equals("�����ȣ")) {
	        		facilNum = value;
		        }
				else if (key.equals("�����ȣ")) {
	        		facilNum = value;
		        }
				else if (key.equals("�����")) {
	        		compType = value;
		        }
				else if (key.equals("�����ڰ������")) {
	        		selectMethod = value;
		        }
				else if (key.equals("�������")) {
	        		bidMethod = value;
		        }
				else if (key.equals("���ʿ������뿩��")) {
	        		hasBase = value;
		        }
				else if (key.equals("���������⸶���Ͻ�") && value.length() == 16) {
					value.replaceAll("/", "-");
					value += ":00";
					paperDeadline = value;
				}
				else if (key.equals("��������")) {
					negoMethod = value;
				}
				else if (key.equals("���ʿ��񰡰�")) {
			       	value = value.replaceAll("[^\\d]", "");
			       	if (value.equals("")) value = "0";
			       	basePrice = value;
				}
				else if (key.equals("��������")) {
					value = value.replaceAll("[^\\d]", "");
			       	if (value.equals("")) value = "0";
			       	expPrice = value;
				}
				else if (key.equals("����(%)")) {
	        		lowerBound = value;
		        }
				else if (key.equals("����(%)")) {
					upperBound = value;
				}
				else if (key.equals("����������(%)")) {
					bidBound = value;
				}
			}
			
			String summary = "������������ ���� ����,��ü�ڵ�,��ü��,��ǥ��,�����ݾ�,������,��� �����Ͻø� �� �� �ֽ��ϴ�";
			if (doc.getElementsByAttributeValue("summary", summary).size() != 0) {
				Element resultTable = doc.getElementsByAttributeValue("summary", summary).get(0);
				if (resultTable.getElementsByTag("tr").size() > 1) {
					Element top = resultTable.getElementsByTag("tr").get(1);
					if (top.text().length() > 1) {
						bidPrice = top.getElementsByTag("td").get(4).text();
						
						bidPrice = bidPrice.replaceAll("[^\\d]", "");
						if (bidPrice.equals("")) bidPrice = "0";
						
						if (doc.getElementsContainingOwnText("��ü �Ǽ� :").size() > 0) {
							comp = doc.getElementsContainingOwnText("��ü �Ǽ� :").text().split(" ")[3];
							if (comp.equals("")) comp = "0";
						}
					}
				}
			}
			
			where = "WHERE �����ȣ=\""+facilNum+"\" AND " +
					"����="+negoVer+" AND " +
					"���ֱ��=\""+org+"\"";
			String sql = "UPDATE dapanegoinfo SET �����=\""+compType+"\", " +
					"�����ڰ������=\""+selectMethod+"\", " +
					"�������=\""+bidMethod+"\", " +
					"���ʿ������뿩��=\""+hasBase+"\", " +
					"���������⸶���Ͻ�=\""+paperDeadline+"\", " +
					"��������=\""+negoMethod+"\", " +
					"����=\""+upperBound+"\", " +
					"����=\""+lowerBound+"\", " +
					"����������=\""+bidBound+"\", " +
					"���ʿ��񰡰�="+basePrice+", " +
					"��������="+expPrice+", " +
					"�����ݾ�="+bidPrice+", " +
					"������="+comp+", " +
					"�����ȣ=\""+negno+"\" " + where;
			System.out.println(sql);
			st.executeUpdate(sql);
		}
		
		return where;
	}
	
	public void getPrice(String where) throws IOException, SQLException {
		String path = "";
		
		if (op.equals("��������")) path = DapaParser.BID_ANN_PRICE;
		else if (op.equals("�������")) path = DapaParser.BID_RES_PRICE;
		else if (op.equals("������")) path = DapaParser.NEGO_RES_PRICE;
		else {
			System.out.println("Declare the operation!");
			return;
		}
		
		// Load new price page.
		openConnection(path);
		String param = ""; 
		Iterator it = formData.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			if (pair.getValue() != null) {
				String val = pair.getValue().toString();
				if (val.contains("�뿪")) val = val.replaceAll("�뿪", "%BF%EB%BF%AA");
				else if (val.contains("�ü�")) val = val.replaceAll("�ü�", "%BD%C3%BC%B3");
				else if (val.contains("����")) val = val.replaceAll("����", "%B0%A8%B8%AE");
				else if (val.contains("����")) val = val.replaceAll("����", "%B0%F8%BF%F8");
				else if (val.contains("����")) val = val.replaceAll("����", "%BA%B8%BC%F6");
				else if (val.contains("å��")) val = val.replaceAll("å��", "%C3%A5%C0%D3");
				else if (val.contains("������")) val = val.replaceAll("������", "%B9%E6%C6%F7%B1%B3");
				else if (val.contains("ǥ�ص�����")) val = val.replaceAll("ǥ�ص�����", "%C7%A5%C1%D8%B5%B5%BC%B3%B0%E8");
				else if (val.contains("����")) val = val.replaceAll("����", "%BC%B3%B0%E8");
				else if (val.contains("��⹰")) val = val.replaceAll("��⹰", "%C6%F3%B1%E2%B9%B0");
				else if (val.contains("���")) val = val.replaceAll("���", "%C6%F3%B1%E2");
				else if (val.contains("��")) val = val.replaceAll("��", "%C6%F3");
				else if (val.contains("��������")) val = val.replaceAll("��������", "%C1%F6%BF%AA%C1%A4%B9%D0");
				else if (val.contains("�ǹ�����")) val = val.replaceAll("�ǹ�����", "%B0%C7%B9%B0%BE%C8%C0%FC");
				else if (val.contains("���о���")) val = val.replaceAll("���о���", "%C1%A4%B9%D0%BE%C8%C0%FC");
				else if (val.contains("����")) val = val.replaceAll("����", "%BE%C8%C0%FC");
				else if (val.contains("����")) val = val.replaceAll("����", "%B0%E6%C0%EF");
				else if (val.contains("��")) val = val.replaceAll("��", "%BD%C3");
				else if (val.contains("��")) val = val.replaceAll("��", "%BA%B9");
				else if (val.contains("��")) val = val.replaceAll("��", "%BA%B8");
				else if (val.contains("��")) val = val.replaceAll("��", "%A4%D1");
				else if (val.contains("��")) val = val.replaceAll("��", "%C0%B0");
				else if (val.contains("��")) val = val.replaceAll("��", "%C0%B8");
				else if (val.contains("��")) val = val.replaceAll("��", "%B0%F8");
				else if (val.contains("��Ÿ")) val = val.replaceAll("��Ÿ", "%B1%E2%C5%B8");
				param += pair.getKey() + "=" + val + "&";
			}
		}
		param = param.substring(0, param.length()-1);
		
		Document doc = Jsoup.parse(getResponse(param));
		parsePricePage(doc, where);
	}
	
	public void parsePricePage(Document doc, String where) throws SQLException {
		if (op.equals("��������")) {
			String summary = "���ʿ��񰡰�, ����, ����, ����������, ���ΰǰ������, ���ο��ݺ����, �򰡱��رݾ�, �����ʱݾ�, ������ʱݾ�, �빫�������, ��Ÿ��������, �Ϲݰ�������غ�, ����������, ���볭�̵������ �����ֽ��ϴ�.";
			if (doc.getElementsByAttributeValue("summary", summary).size() > 0) {
	    		Element priceTable = doc.getElementsByAttributeValue("summary", summary).get(0);
	    		Elements hs = priceTable.getElementsByTag("th");
		        
	    		String lowerBound = ""; // ����
	    		String upperBound = ""; // ����
	    		String bidBound = ""; // ����������
	    		String rate = ""; // ������
	    		String basePrice = ""; // ���ʿ��񰡰�
	    		
		        for (int k = 0; k < hs.size(); k++) {
		        	String head = hs.get(k).text();
		        	if (head.equals("����(%)")) {
		        		lowerBound = hs.get(k).nextElementSibling().text();
		        	}
		        	else if (head.equals("����(%)")) {
		        		upperBound = hs.get(k).nextElementSibling().text();
		        	}
		        	else if (head.equals("����������(%)")) {
		        		bidBound = hs.get(k).nextElementSibling().text();
		        	}
					else if (head.equals("������(%)")) {
						rate = hs.get(k).nextElementSibling().text();
					}
		        	else if (head.equals("���ʿ��񰡰�")) {
			        	basePrice = hs.get(k).nextElementSibling().text();
			        	basePrice = basePrice.replaceAll("[^\\d]", "");
		        	}
		        }
		        
		        String sql = "UPDATE dapabidinfo SET ����=\""+lowerBound+"\", " +
						"����=\""+upperBound+"\", " +
						"����������=\""+bidBound+"\", " +
						"������=\""+rate+"\", " +
						"���ʿ��񰡰�="+basePrice+" " + where;
				st.executeUpdate(sql);
	    	}
			String sql = "UPDATE dapabidinfo SET ����=1 "+where;
			st.executeUpdate(sql);
		}
		else if (op.equals("�������")) {
			String summary = "���ù�ȣ�� ���� �ݾװ� ��ü���� �����ֽ��ϴ�.";
			if (doc.getElementsByAttributeValue("summary", summary).size() > 0) {
				Element dupTable = doc.getElementsByAttributeValue("summary", summary).get(0);
				Elements dupRows = dupTable.getElementsByTag("tr");
				
				for (int x = 1; x <= 5; x++) {
					Elements r = dupRows.get(x).children();
					for (int y = 0; y < 9; y += 3) {
						String dupNo = r.get(y).text();
						String dupPrice = r.get(y + 1).text();
						dupPrice = dupPrice.replaceAll(",", "");
						dupPrice = dupPrice.replaceAll("��", "");
						String dupCom = r.get(y + 2).text();
						String s = "UPDATE dapabidinfo SET ����" + dupNo + "=" + dupPrice + ", ����" + dupNo + "=" + dupCom + " " + where;
	        			st.executeUpdate(s);
					}
				}
			}
			String sql = "UPDATE dapabidinfo SET �Ϸ�=1 " + where;
			st.executeUpdate(sql);
		}
		else if (op.equals("������")) {
			String summary = "�������񰡰ݿ� ���� ��ȣ, �ݾ�, ȸ���� �� �� �ֽ��ϴ�";
			if (doc.getElementsByAttributeValue("summary", summary).size() > 0) { 
				Element dupTable = doc.getElementsByAttributeValue("summary", summary).get(0);
				Elements dupRows = dupTable.getElementsByTag("tr");
				
				for (int x = 1; x <= 5; x++) {
					Elements r = dupRows.get(x).children();
					for (int y = 0; y < 9; y += 3) {
						String dupNo = r.get(y).text();
						String dupPrice = r.get(y + 1).text();
						dupPrice = dupPrice.replaceAll("[^\\d]", "");
						String dupCom = r.get(y + 2).text();
						String s = "UPDATE dapanegoinfo SET ����" + dupNo + "=" + dupPrice + ", ����" + dupNo + "=" + dupCom + " " + where;
	        			st.executeUpdate(s);
					}
				}
			}
			String sql = "UPDATE dapanegoinfo SET �Ϸ�=1 " + where;
			st.executeUpdate(sql);
		}
	}

	public void run() {
		curItem = 0;
		try {
			setOption("��������");
			getList();
			setOption("�������");
			getList();
			setOption("�������");
			getList();
			setOption("������");
			getList();
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}

	public int getTotal() throws IOException {
		String path = DapaParser.BID_RES_LIST;
		String param = "pageNo=1&startPageNo=1&pagePerRow=10&txtBidxDateFrom="+sd+"&txtBidxDateTo="+ed;
		
		openConnection(path);
		Document doc = Jsoup.parse(getResponse(param));
		if (doc.getElementsContainingOwnText("��ü �Ǽ� :").size() != 0) {
			totalItems = Integer.parseInt(doc.getElementsContainingOwnText("��ü �Ǽ� :").text().split(" ")[3]);
		}
		else totalItems = 0;
		
		path = DapaParser.NEGO_RES_LIST;
		param = "pageNo=1&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+sd+"&txtNegoDateTo="+ed;
		
		openConnection(path);
		doc = Jsoup.parse(getResponse(param));
		if (doc.getElementsContainingOwnText("��ü �Ǽ� :").size() != 0) {
			totalItems += Integer.parseInt(doc.getElementsContainingOwnText("��ü �Ǽ� :").text().split(" ")[3]);
		}
		else totalItems += 0;
		
		return totalItems;
	}
	
	public void setDate(String sd, String ed) {
		sd = sd.replaceAll("-", "%2F");
		ed = ed.replaceAll("-", "%2F");
		
		this.sd = sd;
		this.ed = ed;
	}

	public void setOption(String op) {
		this.op = op;
	}

	public int getCur() {
		return curItem;
	}
}
