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
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class LetsParser extends Parser {
	
	// For SQL setup.
	Connection db_con;
	java.sql.Statement st;
	ResultSet rs;
	
	final static String ANN_LIST = "http://ebid.kra.co.kr/bid/notice/all/list.do";
	final static String RES_LIST = "http://ebid.kra.co.kr/res/all/list.do";
	
	final static String ANN_INFO = "http://ebid.kra.co.kr/bid/notice/all/view.do";
	final static String RES_INFO = "http://ebid.kra.co.kr/res/all/view.do";
	
	URL url;
	HttpURLConnection con;
	HashMap<String, String> formData;
	String sd;
	String ed;
	String op;
	int totalItems;
	int curItem;
	
	public LetsParser(String sd, String ed, String op) throws SQLException, ClassNotFoundException {
		this.sd = sd;
		this.ed = ed;
		this.op = op;
		
		totalItems = 0;
		curItem = 0;
		
		formData = new HashMap<String, String>();
		
		// Set up SQL connection.
		Class.forName("com.mysql.jdbc.Driver");
		db_con = DriverManager.getConnection("jdbc:mysql://localhost/"+Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
		st = db_con.createStatement();
		rs = null;
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		LetsParser tester = new LetsParser("2016-07-01", "2016-08-16", "����");
		
		tester.getList();
	}
	
	public void run() {
		try {
			curItem = 0;
			setOption("����");
			getList();
			setOption("���");
			getList();
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
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
	
	public void getList() throws IOException, SQLException {
		String path = "";
		if (op.equals("����")) path = LetsParser.ANN_LIST;
		else if (op.equals("���")) path = LetsParser.RES_LIST;
		else {
			System.out.println("Declare the operation!");
			return;
		}
		
		openConnection(path, "POST");
		
		String param = "";
		
		if (op.equals("����")) {
			param = "pageIndex=1&openDateFrom="+sd+"&openDateTo="+ed;
		}
		else if (op.equals("���")) {
			param = "is_from_main=true&page=1&open_date_from="+sd+"&open_date_to="+ed;
		}
		
		Document doc = Jsoup.parse(getResponse(param, "POST"));
		Elements rows = doc.getElementsByTag("table").get(1).getElementsByTag("tr");
		
		int items = rows.size();
		int page = 1;
		
		do {
			if (rows.get(1).text().length() > 24) {
				for (int i = 1; i < rows.size(); i++) {
					if (op.equals("���")) curItem++;
					Element row = rows.get(i);
					boolean enter = parseListRow(row);
					
					if (enter) {
						String bn = row.getElementsByTag("td").get(1).text();
						String br = row.getElementsByTag("td").get(6).text();
						getInfo(bn, br);
					}
				}
				page++;
				boolean nextPage = false;
				Element pagingdiv = doc.getElementsByAttributeValue("class", "bid_paging").first();
				Elements pages = pagingdiv.getElementsByTag("a");
				String pagestr = page + "";
				
				for (int i = 0; i < pages.size(); i++) {
					String text = pages.get(i).text();
					System.out.println(text);
					if (text.equals(pagestr) || text.equals("���� ������� �̵�")) {
						nextPage = true;
						break;
					}
				}
				if (nextPage) {
					openConnection(path, "POST");
					
					if (op.equals("����")) {
						param = "pageIndex="+page+"&openDateFrom="+sd+"&openDateTo="+ed;
					}
					else if (op.equals("���")) {
						param = "is_from_main=true&page="+page+"&open_date_from="+sd+"&open_date_to="+ed;
					}
					
					doc = Jsoup.parse(getResponse(param, "POST"));
					rows = doc.getElementsByTag("table").get(1).getElementsByTag("tr");
					items = rows.size();
				}
				else {
					items = 0;
				}
			}
			else {
				items = 0;
			}
		} while(items > 0);
	}

	public boolean parseListRow(Element row) throws SQLException {
		boolean enter = true;
		Elements data = row.getElementsByTag("td");
		
		if (op.equals("����")) {
			String bidno = data.get(1).text(); // �����ȣ
			String place = data.get(2).text(); // �����
			String compType = data.get(4).text(); // �����
			String deadline = data.get(6).text() + " 00:00:00"; // ��������
			String bidMethod = data.get(7).text(); // �������
			String prog = data.get(8).text(); // ����
			
			boolean exists = false;
				
			String where = "WHERE �����ȣ=\""+bidno+"\"";
				
			String sql = "SELECT EXISTS(SELECT �����ȣ FROM letsrunbidinfo "+where+")";
			rs = st.executeQuery(sql);
			if (rs.first()) exists = rs.getBoolean(1);
				
			if (exists) {
				sql = "SELECT ����, ������� FROM letsrunbidinfo " + where;
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
						sql = "UPDATE letsrunbidinfo SET �������=\""+prog+"\" "+where;
						st.executeUpdate(sql);
					}
				}
				else if (!dbProg.equals(prog)) {
					sql = "UPDATE letsrunbidinfo SET �������=\""+prog+"\" "+where;
					st.executeUpdate(sql);
				}
			}
			else {
				sql = "INSERT INTO letsrunbidinfo (�����ȣ, �����, �����, ��������, �������, �������) VALUES (" +
					"\""+bidno+"\", " +
					"\""+place+"\", " +
					"\""+compType+"\", " +
					"\""+deadline+"\", " +
					"\""+bidMethod+"\", " +
					"\""+prog+"\");";
				st.executeUpdate(sql);
			}
		}
		else if (op.equals("���")) {
			String bidno = data.get(1).text(); // �����ȣ
			String workType = data.get(3).text(); // ��������
			String compType = data.get(4).text(); // �����
			String openDate = data.get(5).text(); // �����Ͻ�
			String result = data.get(6).text(); // ��������
			
			boolean exists = false;
			
			String where = "WHERE �����ȣ=\""+bidno+"\"";
			
			String sql = "SELECT EXISTS(SELECT �����ȣ FROM letsrunbidinfo "+where+")";
			rs = st.executeQuery(sql);
			if (rs.first()) exists = rs.getBoolean(1);
			
			if (exists) {
				sql = "SELECT �Ϸ�, �������� FROM letsrunbidinfo " + where;
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
						sql = "UPDATE letsrunbidinfo SET ��������=\""+result+"\" "+where;
						st.executeUpdate(sql);
					}
				}
				else if (!dbResult.equals(result)) {
					sql = "UPDATE letsrunbidinfo SET ��������=\""+result+"\" "+where;
					st.executeUpdate(sql);
				}
			}
			else {
				sql = "INSERT INTO letsrunbidinfo (�����ȣ, ��������, �����, �����Ͻ�, ��������) VALUES (" +
					"\""+bidno+"\", " +
					"\""+workType+"\", " +
					"\""+compType+"\", " +
					"\""+openDate+"\", " +
					"\""+result+"\");";
				st.executeUpdate(sql);
			}
		}
		
		return enter;
	}
	
	public void getInfo(String bidno, String result) throws IOException, SQLException {
		if (op.equals("���")) {
			String path = LetsParser.RES_INFO;
		
			openConnection(path, "POST");
			
			String param = "";
			param = "b_code="+bidno.substring(3)+"&b_type=1&is_from_main=true&page=1&open_date_from="+sd+"&open_date_to="+ed;
			Document doc = Jsoup.parse(getResponse(param, "POST"));
			parseInfo(doc, bidno);
				
			if (!result.equals("����") && !result.equals("�������")) {
				parseResult(bidno);
			}
			
			parseAnn(bidno);
			String sql = "UPDATE letsrunbidinfo SET �Ϸ�=1 WHERE �����ȣ=\"" + bidno + "\";";
			st.executeUpdate(sql);
		}
		else if (op.equals("����")) {
			String path = LetsParser.ANN_INFO;
			path += "?bCode="+bidno.substring(3)+"&b_code="+bidno.substring(3);
			
			openConnection(path, "GET");
			Document doc = Jsoup.parse(getResponse(null, "GET"));
			Elements captions = doc.getElementsByTag("caption");
			
			String selectMethod = ""; // �����ڰ������
			String workType = ""; // ��������
			String place = ""; // �����
			String bidMethod = ""; // �������
			String deadline = ""; // ��������
			String openDate = ""; // �����Ͻ�
			String priceMethod = ""; // �������ݹ��
			String expPrice = "0"; // �����ݾ�
			String basePrice = "0"; // ���񰡰ݱ��ʱݾ�
			
			for (int j = 0; j < captions.size(); j++) {
				if (captions.get(j).text().equals("�������� �����ü ǥ")) {
					Element infoTable = captions.get(j).parent();
					Elements infos = infoTable.getElementsByTag("th"); // Headers for table of details
					for (int k = 0; k < infos.size(); k++) {
						String key = infos.get(k).text();
						if (key.equals("�����")) {
							place = infos.get(k).nextElementSibling().text();
						}
						else if (key.equals("������������")) {
							bidMethod = infos.get(k).nextElementSibling().text();
						}
						else if (key.equals("��������")) {
							workType = infos.get(k).nextElementSibling().text();
						}
						else if (key.equals("�����ڰ������")) {
							selectMethod = infos.get(k).nextElementSibling().text();
						}
					}
				}
				else if (captions.get(j).text().equals("�����������ǥ")) {
					Element timeTable = captions.get(j).parent();
					Elements infos = timeTable.getElementsByTag("th"); // Headers for table of details
					for (int k = 0; k < infos.size(); k++) {
						String key = infos.get(k).text();
						if (key.equals("��������")) {
							deadline = infos.get(k).nextElementSibling().text();
						}
						else if (key.equals("�����Ͻ�")) {
							openDate = infos.get(k).nextElementSibling().text();
						}
					}
				}
				else if (captions.get(j).text().equals("������������ǥ")) {
					Element priceTable = captions.get(j).parent();
					Elements infos = priceTable.getElementsByTag("th"); // Headers for table of details
					for (int k = 0; k < infos.size(); k++) {
						String key = infos.get(k).text();
						if (key.equals("�������ݹ��")) {
							priceMethod = infos.get(k).nextElementSibling().text();
						}
						else if (key.equals("�����ݾ�")) {
							expPrice = infos.get(k).nextElementSibling().text();
							expPrice = expPrice.replaceAll("[^\\d]", "");
							if (expPrice.equals("")) expPrice = "0";
						}
						else if (key.equals("�����ݾ�")) {
							basePrice = infos.get(k).nextElementSibling().text();
							basePrice = basePrice.replaceAll("[^\\d]", "");
							if (basePrice.equals("")) basePrice = "0";
						}
					}
				}
			}
			
			String sql = "UPDATE letsrunbidinfo SET " +
					"�����=\"" + place + "\", " +
					"��������=\"" + workType + "\", " +
					"�������=\"" + bidMethod + "\", " +
					"�����ڼ������=\"" + selectMethod + "\", " +
					"�������ݹ��=\"" + priceMethod + "\", " +
					"��������=" + expPrice + ", " +
					"���񰡰ݱ��ʱݾ�=" + basePrice + ", " +
					"��������=\"" + deadline + "\", " +
					"����=1, " +
					"�����Ͻ�=\"" + openDate + "\" WHERE �����ȣ=\"" + bidno + "\";";
			st.executeUpdate(sql);
		}
	}
	
	public void parseInfo(Document doc, String bidno) throws SQLException {
		String where = "WHERE �����ȣ=\"" + bidno + "\"";
		
		String selectMethod = ""; // �����ڼ������
		String expPrice = "0"; // ��������
		String basePrice = "0"; // ���񰡰ݱ��ʱݾ�
		String rate = ""; // ����������
		String boundPrice = "0"; // �������ѱݾ�
		
		Element infoTable = null;
		Element expPriceTable = null;
		Element dupPriceTable = null;
		Element minPriceTable = null;
		Elements tableNames = doc.getElementsByTag("caption");
		for (int j = 0; j < tableNames.size(); j++) {
			String name = tableNames.get(j).text();
			if (name.equals("��������")) {
				infoTable = tableNames.get(j).parent();
			}
			else if (name.equals("������������")) {
				expPriceTable = tableNames.get(j).parent();
			}
			else if (name.equals("���� ���񰡰ݺ� ���û�Ȳ") || name.equals("���� ���񰡰ݺ� ���û�Ȳ ǥ")) {
				dupPriceTable = tableNames.get(j).parent();
			}
			else if (name.equals("������ ������") || name.equals("���ݽɻ�")) {
				minPriceTable = tableNames.get(j).parent();
			}
		}
		
		if (infoTable != null) {
			Elements infos = infoTable.getElementsByTag("th"); // Headers for table of details
			for (int j = 0; j < infos.size(); j++) {
				String key = infos.get(j).text();
				if (key.equals("�����ڼ������")) {
					selectMethod = infos.get(j).nextElementSibling().text();
				}
			}
		}
		
		if (expPriceTable != null) {
			Elements expPrices = expPriceTable.getElementsByTag("th");
			for (int j = 0; j < expPrices.size(); j++) {
				String key = expPrices.get(j).text();
				if (key.equals("��������")) {
					String value = expPrices.get(j).nextElementSibling().text();
					expPrice = value.replaceAll("[^\\d]", "");
					if (expPrice.equals("")) expPrice = "0";
				}
				else if (key.equals("���񰡰ݱ��ʱݾ�")) {
					String value = expPrices.get(j).nextElementSibling().text();
					basePrice = value.replaceAll("[^\\d]", "");
					if (basePrice.equals("")) basePrice = "0";
				}
			}
		}
		
		if (minPriceTable != null) {
			Elements minPrices = minPriceTable.getElementsByTag("th");
			for (int j = 0; j < minPrices.size(); j++) {
				String key = minPrices.get(j).text();
				String value = minPrices.get(j).nextElementSibling().text();
				if (key.equals("����������")) {
					rate = value;
				}
				else if (key.equals("�������ѱݾ�")) {
					boundPrice = value.replaceAll("[^\\d]", "");
					if (boundPrice.equals("")) boundPrice = "0";
				}
			}
		}
		
		if (dupPriceTable != null) {
			Elements dupPriceRows = dupPriceTable.getElementsByTag("tr");
			int companies = 0;
			for (int x = 1; x <= 5; x++) {
				Elements r = dupPriceRows.get(x).children();
				for (int y = 0; y < 9; y += 3) {
					String dupNo = r.get(y).text();
					String dupPrice = r.get(y + 1).text();
					dupPrice = dupPrice.replaceAll("[^\\d]", "");
					String dupCom = r.get(y + 2).text().trim();
					String s = "UPDATE letsrunbidinfo SET ����" + dupNo + "=" + dupPrice + ", ����" + dupNo + "=" + dupCom + " " + where;
					st.executeUpdate(s);
	        		companies += Integer.parseInt(dupCom);
				}
				System.out.println("dup prices fetched");
			}
			st.executeUpdate("UPDATE letsrunbidinfo SET ������=" + companies + " " + where);
		}
		
		String sql = "UPDATE letsrunbidinfo SET " +
				"�����ڼ������=\"" + selectMethod + "\", " +
				"��������=" + expPrice + ", " +
				"���񰡰ݱ��ʱݾ�=" + basePrice + ", " +
				"�������ѱݾ�=" + boundPrice + ", " +
				"����������=\"" + rate + "\" " + where;
		st.executeUpdate(sql);
	}
	
	public void parseResult(String bidno) throws IOException, SQLException {
		String path = "http://ebid.kra.co.kr/res/result/bd_list_result_company_1.do?page=1&b_code="+bidno.substring(3)+"&select_method=11";
		
		openConnection(path, "GET");
		
		Document doc = Jsoup.parse(getResponse(null, "GET"));
		Element resultTable = null;
		Elements captions = doc.getElementsByTag("caption");
		for (int j = 0; j < captions.size(); j++) {
			String name = captions.get(j).text();
			if (name.equals("��ü��Ȳ") || name.equals("������ü ��Ȳ")) {
				resultTable = captions.get(j).parent();
				if (resultTable.getElementsByTag("tr").get(1).text().length() > 18) {
					Element top = resultTable.getElementsByTag("tr").get(1);
					String topPrice = top.getElementsByTag("td").get(4).text();
					topPrice = topPrice.replaceAll(",", "");
					System.out.println("�����ݾ� : " + topPrice);
					st.executeUpdate("UPDATE letsrunbidinfo SET �����ݾ�=" + topPrice + " WHERE �����ȣ =\""+bidno+"\"");
				}
			}
		}
	}
	
	public void parseAnn(String bidno) throws IOException, SQLException {
		String path = "http://ebid.kra.co.kr/bid/popup/bd_view_notice.do?b_code="+bidno.substring(3);
		
		openConnection(path, "GET");
		
		Document doc = Jsoup.parse(getResponse(null, "GET"));
		Elements captions = doc.getElementsByTag("caption");
		
		String place = ""; // �����
		String bidMethod = ""; // �������
		String deadline = ""; // ��������
		String openDate = ""; // �����Ͻ�
		String priceMethod = ""; // �������ݹ��
		
		for (int j = 0; j < captions.size(); j++) {
			if (captions.get(j).text().equals("�������� �����ü ǥ")) {
				Element infoTable = captions.get(j).parent();
				Elements infos = infoTable.getElementsByTag("th"); // Headers for table of details
				for (int k = 0; k < infos.size(); k++) {
					String key = infos.get(k).text();
					if (key.equals("�����")) {
						place = infos.get(k).nextElementSibling().text();
					}
					else if (key.equals("������������")) {
						bidMethod = infos.get(k).nextElementSibling().text();
					}
				}
			}
			else if (captions.get(j).text().equals("�����������ǥ")) {
				Element timeTable = captions.get(j).parent();
				Elements infos = timeTable.getElementsByTag("th"); // Headers for table of details
				for (int k = 0; k < infos.size(); k++) {
					String key = infos.get(k).text();
					if (key.equals("��������")) {
						deadline = infos.get(k).nextElementSibling().text();
					}
					else if (key.equals("�����Ͻ�")) {
						openDate = infos.get(k).nextElementSibling().text();
					}
				}
			}
			else if (captions.get(j).text().equals("������������ǥ")) {
				Element priceTable = captions.get(j).parent();
				Elements infos = priceTable.getElementsByTag("th"); // Headers for table of details
				for (int k = 0; k < infos.size(); k++) {
					String key = infos.get(k).text();
					if (key.equals("�������ݹ��")) {
						priceMethod = infos.get(k).nextElementSibling().text();
					}
				}
			}
		}
		
		String sql = "UPDATE letsrunbidinfo SET " +
				"�����=\"" + place + "\", " +
				"�������=\"" + bidMethod + "\", " +
				"�������ݹ��=\"" + priceMethod + "\", " +
				"��������=\"" + deadline + "\", " +
				"�����Ͻ�=\"" + openDate + "\" WHERE �����ȣ=\"" + bidno + "\";";
		st.executeUpdate(sql);
	}
	
	public int getTotal() throws IOException {
		String path = LetsParser.RES_LIST;
		String param = "is_from_main=true&page=1&open_date_from="+sd+"&open_date_to="+ed;
		
		openConnection(path, "POST");
		Document doc = Jsoup.parse(getResponse(param, "POST"));
		Element pagingdiv = doc.getElementsByAttributeValue("class", "bid_paging").first();
		Element lastButton = pagingdiv.getElementsByAttributeValue("class", "btns last_page").first();
		if (lastButton != null) {
			String lastpath = "http://ebid.kra.co.kr/res/all/";
			lastpath += lastButton.attr("href");
			openConnection(lastpath, "GET");
			
			Document lastPage = Jsoup.parse(getResponse(null, "GET"));
			Elements rows = lastPage.getElementsByTag("table").get(1).getElementsByTag("tr");
			String lastindex = rows.get(rows.size() - 1).getElementsByTag("td").first().text();
			totalItems = Integer.parseInt(lastindex);
		}
		else {
			Elements rows = doc.getElementsByTag("table").get(1).getElementsByTag("tr");
			String lastindex = rows.get(rows.size() - 1).getElementsByTag("td").first().text();
			if (lastindex.contains("�����ϴ�")) totalItems = 0;
			else totalItems = Integer.parseInt(lastindex);
		}
		
		return totalItems;
	}

	public void setDate(String sd, String ed) {
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
