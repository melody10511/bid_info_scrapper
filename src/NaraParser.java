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

public class NaraParser extends Parser {
	
	final static String ANN_LIST = "http://www.g2b.go.kr:8101/ep/tbid/tbidList.do?";
	final static String RES_LIST = "http://www.g2b.go.kr:8101/ep/result/listPageIntegrationBidResult.do?";
	
	final static String BASE_INF = "http://www.g2b.go.kr:8081/ep/price/baseamt/selectBaseAmtDtlPopup.do?";
	
	final static String PROD_LIST = "http://www.g2b.go.kr:8101/ep/result/prodBidResultCateList.do?";
	final static String SELECT_LIST = "http://www.g2b.go.kr:8101/ep/result/listFrnBidResultCate.do?";
	final static String STPL_LIST = "http://www.g2b.go.kr:8101/ep/result/stplBidResultCateList.do?";
	final static String LEASE_LIST = "http://www.g2b.go.kr:8101/ep/result/leaseBidResultCateList.do?";
	
	final static String FACIL_RES = "http://www.g2b.go.kr:8101/ep/result/facilBidResultDtl.do?";
	final static String SERV_RES = "http://www.g2b.go.kr:8101/ep/result/serviceBidResultDtl.do?";
	final static String PROD_RES = "http://www.g2b.go.kr:8101/ep/result/prodBidResultDtl.do?";
	final static String LEASE_RES = "http://www.g2b.go.kr:8101/ep/result/leaseBidResultDtl.do?";
	final static String SELECT_RES = "http://www.g2b.go.kr:8101/ep/result/selectFrnBidResultDtl.do?";
	final static String STPL_RES = "http://www.g2b.go.kr:8101/ep/result/stplBidResultDtl.do?";
	final static String PRIV_RES = "http://www.g2b.go.kr:8101/ep/result/privateBidResultDtl.do?";
	
	final static String DUP_PRICE = "http://www.g2b.go.kr:8101/ep/open/pdamtCalcResultDtl.do?";
	
	// For SQL setup.
	Connection db_con;
	java.sql.Statement st;
	ResultSet rs;
	
	URL url;
	HttpURLConnection con;
	String sd;
	String ed;
	String op;
	int totalItems;
	int curItem;
	
	public NaraParser(String sd, String ed, String op) throws ClassNotFoundException, SQLException {
		sd = sd.replaceAll("-", "%2F");
		ed = ed.replaceAll("-", "%2F");
		
		this.sd = sd;
		this.ed = ed;
		this.op = op;
		totalItems = 0;
		curItem = 0;
		
		// Set up SQL connection.
		Class.forName("com.mysql.jdbc.Driver");
		db_con = DriverManager.getConnection("jdbc:mysql://localhost/"+Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
		st = db_con.createStatement();
		rs = null;
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		NaraParser tester = new NaraParser("2016/08/30", "2016/08/30", "����");
		
		tester.getList();
		tester.setOption("���");
		tester.getList();
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
		if (op.equals("����")) {
			path = NaraParser.ANN_LIST;
		}
		else if (op.equals("���")) {
			path = NaraParser.RES_LIST;
		}
		else {
			System.out.println("�ɼ��� �����ϼ���!");
			return;
		}
		
		path += "searchType=1"; // Search by open date;
		if (op.equals("����")) path += "&bidSearchType=1"; // Search by open date;
		else if (op.equals("���")) path += "&bidSearchType=2"; // Search by open date;
		path += "&searchDtType=2"; // Search by open date;
		path += "&fromOpenBidDt="+sd; // Starting date;
		path += "&toOpenBidDt="+ed; // Starting date;
		path += "&radOrgan=1"; // Radical organ is 1!
		path += "&budgetCompare=UP"; // Budget compare.
		path += "&regYn=Y"; // Regyn is Y for some reason.
		path += "&recordCountPerPage=100"; // Record per page;
		path += "&useTotalCount=Y"; // Show total count;
		path += "&currentPageNo=1"; // Page number index.
		
		openConnection(path, "GET");
		Document doc = Jsoup.parse(getResponse(null, "GET"));
		Element listTable = doc.getElementsByTag("tbody").first();
		Elements rows = listTable.getElementsByTag("tr");
		
		// Get the total count
		Element countdiv = doc.getElementsByAttributeValue("class", "inforight").first();
		totalItems = Integer.parseInt(countdiv.text().replaceAll("[^\\d]", ""));
		
		int index = 0;
		int page = 1;
		for (int i = 0; i < totalItems; i++) {
			if (op.equals("���")) curItem++;
			System.out.println(rows.get(index).text());
			parseListRow(rows.get(index));
			
			if (((i % 100) == 99) && (i < (totalItems - 1))) {
				index = 0;
				page++;
				if (page <= 10) path = path.substring(0, path.length() - 1) + page;
				else if (page <= 100) path = path.substring(0, path.length() - 2) + page;
				else if (page <= 1000) path = path.substring(0, path.length() - 3) + page;
				openConnection(path, "GET");
				doc = Jsoup.parse(getResponse(null, "GET"));
				listTable = doc.getElementsByTag("tbody").first();
				rows = listTable.getElementsByTag("tr");
			}
			else {
				index++;
			}
		}
	}
	
	public void parseListRow(Element row) throws SQLException, IOException {
		boolean enter = true;
		boolean exists = false;
		Elements data = row.getElementsByTag("td");
		
		if (op.equals("����")) {
			String workType = data.get(0).text(); // ����
			String bidno = data.get(1).text(); // �����ȣ����
			String bidType = data.get(2).text(); // ����з�
			String annOrg = data.get(4).text(); // ������
			String demOrg = data.get(5).text(); // ������
			String compType = data.get(6).text(); // �����
			
			String where = "WHERE �����ȣ����=\"" + bidno + "\"";
			
			rs = st.executeQuery("SELECT EXISTS(SELECT �����ȣ���� FROM narabidinfo " + where + ")");
			if (rs.first()) exists = rs.getBoolean(1);
			
			if (exists) {
				String sql = "SELECT ���� FROM narabidinfo " + where;
				rs = st.executeQuery(sql);
				
				int finished = 0;
				if (rs.first()) {
					finished = rs.getInt(1);
				}

				if (finished > 0) enter = false;
				
				sql = "UPDATE narabidinfo SET ������=\"" + annOrg + "\" " + where;
				st.executeUpdate(sql);
			}
			else {
				String sql = "INSERT INTO narabidinfo (����, �����ȣ����, ����з�, ������, ������, �����) VALUES (" +
						"\"" + workType + "\", " +
						"\"" + bidno + "\", " +
						"\"" + bidType + "\", " +
						"\"" + annOrg + "\", " +
						"\"" + demOrg + "\", " +
						"\"" + compType + "\")";
				st.executeUpdate(sql);
			}
			
			if (enter) {
				String itemPath = data.get(3).getElementsByTag("a").first().attr("href");
				openConnection(itemPath, "GET");
				Document doc = Jsoup.parse(getResponse(null, "GET"));
				parseInfo(doc, bidno, workType);
			}
		}
		else if (op.equals("���")) {
			String workType = data.get(0).text(); // ����
			String bidno = data.get(1).text(); // �����ȣ����
			String reno = data.get(2).text().equals("") ? "0" : data.get(2).text(); // ��������ȣ
			String demOrg = data.get(4).text(); // ������
			String openDate = data.get(5).text(); // �����Ͻ�
			String comp = data.get(6).text().equals("") ? "0" : data.get(6).text(); // ������
			String bidPrice = data.get(8).text().equals("") ? "0" : data.get(8).text(); // �����ݾ�
			bidPrice = bidPrice.replaceAll("[^\\d.]", "");
			String result = data.get(10).text(); // �����Ȳ
			String script = row.getElementsByTag("a").first().attr("href");
			
			String where = "WHERE �����ȣ���� = \"" + bidno + "\" AND ��������ȣ=" + reno;
			
			if (!result.equals("����ȸ")) {
				rs = st.executeQuery("SELECT EXISTS(SELECT �����ȣ���� FROM narabidinfo " + where + ")");
				if (rs.first()) exists = rs.getBoolean(1);
				
				if (exists) {
					String sql = "SELECT �Ϸ�, �����Ȳ, ��� FROM narabidinfo " + where;
					rs = st.executeQuery(sql);
					
					int finished = 0;
					String dbResult = "";
					int indexed = 0;
					if (rs.first()) {
						finished = rs.getInt(1);
						dbResult = rs.getString(2) == null ? "" : rs.getString(2);
						indexed = rs.getInt(3);
					}
					
					if (finished > 0) {
						if (dbResult.equals(result)) enter = false;
						else {
							if (result.equals("����") || result.equals("������")) enter = false;
							sql = "UPDATE narabidinfo SET �����Ȳ=\"" + result + "\" " + where;
							st.executeUpdate(sql);
						}
					}
					else if (!dbResult.equals(result)) {
						if (result.equals("����") || result.equals("������")) {
							enter = false;
							sql = "UPDATE narabidinfo SET �Ϸ�=1 " + where;
							st.executeUpdate(sql);
						}
						sql = "UPDATE narabidinfo SET �����Ȳ=\"" + result + "\" " + where;
						st.executeUpdate(sql);
					}
					
					if (indexed == 0) {
						sql = "UPDATE narabidinfo SET ���=1 " + where;
						st.executeUpdate(sql);
					}
				}
				else {
					String sql = "INSERT INTO narabidinfo (����, �����ȣ����, ������, ���������Ͻ�, ���������Ͻ�, �����Ȳ, ��������ȣ, ������, �����ݾ�, ���) VALUES (" +
							"\"" + workType + "\", " +
							"\"" + bidno + "\", " +
							"\"" + demOrg + "\", " +
							"\"" + openDate + "\", " +
							"\"" + openDate + "\", " +
							"\"" + result + "\", " +
							"" + reno + ", " +
							"" + comp + ", " +
							"" + bidPrice + ", 1)"; // ADDED ���
					st.executeUpdate(sql);
					if (result.equals("����") || result.equals("������")) {
						enter = false;
						sql = "UPDATE narabidinfo SET �Ϸ�=1 " + where;
						st.executeUpdate(sql);
					}
				}
				
				if (enter) {
					script = script.substring(20, script.length() - 2);
					String[] inputs = script.split(",");
					String tid = inputs[0].replaceAll("\'", "").trim();
					String bidcate = inputs[3].replaceAll("\'", "").trim();
					
					String itempath = "";
					if (tid.equals("1") || tid.equals("9")) itempath = NaraParser.PROD_RES; // ��ǰ
					else if (tid.equals("2")) itempath = NaraParser.SELECT_RES; // ����
					else if (tid.equals("3")) itempath = NaraParser.FACIL_RES; // ����
					else if (tid.equals("4")) itempath = NaraParser.STPL_RES; // ����
					else if (tid.equals("5")) itempath = NaraParser.SERV_RES; // �뿪
					else if (tid.equals("6") || tid.equals("7")) itempath = NaraParser.LEASE_RES; // ����
					else if (tid.equals("20")) itempath = NaraParser.PRIV_RES; // �ΰ�
					
					itempath += "bidcate=" + bidcate;
					itempath += "&bidno=";
					itempath += bidno.split("-")[0];
					itempath += "&bidseq=";
					itempath += bidno.split("-")[1];
					
					System.out.println(itempath);
					
					openConnection(itempath, "GET");
					Document resDoc = Jsoup.parse(getResponse(null, "GET"));
					parseRes(resDoc, where, bidno, bidcate, reno, tid);
				}
			}
			else {
				script = script.substring(22, script.length() - 2);
				String[] inputs = script.split(",");
				String tid = inputs[0].replaceAll("\'", "").trim();
				
				String listpath = "";
				if (tid.equals("1") || tid.equals("9")) listpath = NaraParser.PROD_LIST; // ��ǰ
				else if (tid.equals("2")) listpath = NaraParser.SELECT_LIST; // ����
				else if (tid.equals("4")) listpath = NaraParser.STPL_LIST; // ����
				else if (tid.equals("6") || tid.equals("7")) listpath = NaraParser.LEASE_LIST; // ����
					
				listpath += "bidno=" + bidno.split("-")[0];
				listpath += "&bidseq=" + bidno.split("-")[1];
					
				openConnection(listpath, "GET");
				Document listDoc = Jsoup.parse(getResponse(null, "GET"));
				Elements items = listDoc.getElementsByTag("tbody").first().getElementsByTag("tr");
				for (int i = 0; i < items.size(); i++) {
					Elements infos = items.get(i).getElementsByTag("td");
					String cateno = infos.get(0).text();
					reno = infos.get(1).text();
					bidPrice = infos.get(5).text().equals("") ? "0" : infos.get(5).text();
					bidPrice = bidPrice.replaceAll("[^\\d.]", "");
					result = infos.get(7).text();
						
					where = "WHERE �����ȣ���� = \"" + bidno + "\" AND ��������ȣ=" + reno + " AND �����з�=" + cateno;
					System.out.println(where);
						
					boolean itemexists = false;
					boolean itementer = true;
						
					rs = st.executeQuery("SELECT EXISTS(SELECT �����ȣ���� FROM narabidinfo " + where + ")");
					if (rs.first()) itemexists = rs.getBoolean(1);
					
					if (itemexists) {
						String sql = "SELECT �Ϸ�, �����Ȳ FROM narabidinfo " + where;
						rs = st.executeQuery(sql);
						
						int finished = 0;
						String dbResult = "";
						if (rs.first()) {
							finished = rs.getInt(1);
							dbResult = rs.getString(2) == null ? "" : rs.getString(2);
						}

						if (finished > 0) {
							if (dbResult.equals(result)) itementer = false;
							else {
								if (result.equals("����") || result.equals("������")) itementer = false;
								sql = "UPDATE narabidinfo SET �����Ȳ=\"" + result + "\" " + where;
								st.executeUpdate(sql);
							}
						}
						else if (!dbResult.equals(result)){
							if (result.equals("����") || result.equals("������")) {
								itementer = false;
								sql = "UPDATE narabidinfo SET �Ϸ�=1 " + where;
								st.executeUpdate(sql);
							}
							sql = "UPDATE narabidinfo SET �����Ȳ=\"" + result + "\" " + where;
							st.executeUpdate(sql);
						}
					}
					else {
						String sql = "INSERT INTO narabidinfo (����, �����ȣ����, ��������ȣ, ������, ���������Ͻ�, ���������Ͻ�, ������, �����ݾ�, �����з�, �����Ȳ) VALUES (" +
								"\"" + workType + "\", " +
								"\"" + bidno + "\", " +
								"" + reno + ", " +
								"\"" + demOrg + "\", " +
								"\"" + openDate + "\", " +
								"\"" + openDate + "\", " +
								"" + comp + ", " +
								"" + bidPrice + ", " +
								"" + cateno + ", " +
								"\"" + result + "\")";
						st.executeUpdate(sql);
						System.out.println(sql);
						if (result.equals("����") || result.equals("������")) {
							itementer = false;
							sql = "UPDATE narabidinfo SET �Ϸ�=1 " + where;
							st.executeUpdate(sql);
						}
					}
					
					if (i == 0) {
						String sql = "UPDATE narabidinfo SET ���=1 " + where; // ADDED ���
						st.executeUpdate(sql);
					}
					
					if (itementer) {
						String itempath = "";
						if (tid.equals("1") || tid.equals("9")) itempath = NaraParser.PROD_RES; // ��ǰ
						else if (tid.equals("2")) itempath = NaraParser.SELECT_RES; // ����
						else if (tid.equals("3")) itempath = NaraParser.FACIL_RES; // ����
						else if (tid.equals("4")) itempath = NaraParser.STPL_RES; // ����
						else if (tid.equals("5")) itempath = NaraParser.SERV_RES; // �뿪
						else if (tid.equals("6") || tid.equals("7")) itempath = NaraParser.LEASE_RES; // ����
						else if (tid.equals("20")) itempath = NaraParser.PRIV_RES; // �ΰ�
						
						itempath += "bidcate=" + cateno;
						itempath += "&bidno=";
						itempath += bidno.split("-")[0];
						itempath += "&bidseq=";
						itempath += bidno.split("-")[1];
						
						System.out.println(itempath);
						
						openConnection(itempath, "GET");
						Document resDoc = Jsoup.parse(getResponse(null, "GET"));
						parseRes(resDoc, where, bidno, cateno, reno, tid);
					}
				}
				
			}
		}
	}
	
	public void parseInfo(Document doc, String bidno, String workType) throws SQLException, IOException {
		if (op.equals("����")) {
			String where = "WHERE �����ȣ����=\"" + bidno + "\"";
			
			String basePrice = "0"; // ���ʱݾ�
			String bidMethod = ""; // �������
			String rebid = ""; // ������
			String exec = ""; // �����
			String obs = ""; // ��ȸ��
			String openDate = ""; // �����Ͻ�
			String priceMethod = ""; // �������
			String limit = ""; // �������ѻ���
			String pricing = ""; // ���񰡰�
			String level = ""; // ���̵����
			
			int duplicate = 0; // For catching duplicating headers.
			Elements keys = doc.getElementsByTag("th");
			for (int j = 0; j < keys.size(); j++) {
				String key = keys.get(j).text();
				if (key.equals("�������")) {
					bidMethod = keys.get(j).nextElementSibling().text();
				}
				else if (key.equals("������")) {
					rebid = keys.get(j).nextElementSibling().text();
				}
				else if (key.equals("�����")) {
					exec = keys.get(j).nextElementSibling().text();
				}
				else if (key.equals("��ȸ��(�����)") || key.equals("������ȸ��(�����)")) {
					obs = keys.get(j).nextElementSibling().text();
				}
				else if (key.equals("�����")) {
					exec = keys.get(j).nextElementSibling().text();
					obs = keys.get(j).nextElementSibling().text();
				}
				else if (key.equals("����(����)�Ͻ�") || key.equals("����(����)�Ͻ�") || key.equals("�����Ͻ�") || key.equals("������������ �����Ͻ�")) {
					if (duplicate == 0) {
						openDate = keys.get(j).nextElementSibling().text();
						if (openDate.contains("(")) {
							String[] parts = openDate.split(" ");
							openDate = parts[0] + " " + parts[1];
						}
						duplicate++;
					}
				}
				else if (key.equals("�������")) {
					priceMethod = keys.get(j).nextElementSibling().text();
				}
				else if (key.equals("������������")) {
					limit = Jsoup.parse(keys.get(j).nextElementSibling().html().split("<br>")[0]).text();
					if (limit.length() > 254) limit = limit.substring(0, 255);
				}
			}
			
			if (doc.getElementsByClass("table_list_baseEstiPriceTbl").size() > 0) {
				Element baseTable = doc.getElementsByClass("table_list_baseEstiPriceTbl").first();
				if (baseTable.getElementsByTag("tr").size() == 2) {
					int pi = 0;
					Elements hs = baseTable.getElementsByTag("th");
					for (int x = 0; x < hs.size(); x++) {
						if (hs.get(x).text().equals("���ʱݾ�")) {
							pi = x;
							break;
						}
					}
					if (baseTable.getElementsByTag("td").size() > 1) {
						String price = baseTable.getElementsByTag("td").get(pi).text();
						basePrice = price.replaceAll("[^\\d.]", "");
						if (basePrice.equals("")) basePrice = "0";
					}
				}
				else if (baseTable.getElementsByTag("tr").size() > 2) {
					Elements baserows = baseTable.getElementsByTag("tr");
					Elements bdata = baserows.get(1).getElementsByTag("td");
					basePrice = bdata.get(1).text();
					basePrice = basePrice.replaceAll("[^\\d.]", "");
					if (basePrice.equals("")) basePrice = "0";
					
					String sql = "UPDATE narabidinfo SET �����з�=1, ���ʱݾ�=" + basePrice + " " + where;
					st.executeUpdate(sql);
					
					for (int y = 2; y < baserows.size(); y++) {
						Elements data = baserows.get(y).getElementsByTag("td");
						String dnum = data.get(0).text();
						String dprice = data.get(1).text();
						dprice = dprice.replaceAll("[^\\d.]", "");

						String temp = "CREATE TEMPORARY TABLE tmptable SELECT * FROM narabidinfo " + where;
						st.executeUpdate(temp);
						temp = "UPDATE tmptable SET �����з�="+dnum+", ���ʱݾ�="+dprice+";";
						st.executeUpdate(temp);
						temp = "INSERT INTO narabidinfo SELECT * FROM tmptable " + where + " LIMIT 1;";
						st.executeUpdate(temp);
						temp = "DROP TEMPORARY TABLE IF EXISTS tmptable";
						st.executeUpdate(temp);
					}
				}
			}
			
			if (doc.getElementsByAttributeValue("title", "���ʱݾ� ��ȸ").size() > 0) {
				String basepath = NaraParser.BASE_INF;
				if (workType.equals("����")) basepath += "taskClCd=3";
				else if (workType.equals("��ǰ")) basepath += "taskClCd=1";
				else if (workType.equals("�뿪")) basepath += "taskClCd=5";
				basepath += "&bidno=" + bidno.split("-")[0];
				basepath += "&bidseq=" + bidno.split("-")[1];
				
				openConnection(basepath, "GET");
				
				Document priceDoc = Jsoup.parse(getResponse(null, "GET"));
				Elements heads = priceDoc.getElementsByTag("th");
				for (Element h : heads) {
					if (h.text().equals("���񰡰�")) {
						pricing = h.nextElementSibling().text();
					}
					else if (h.text().equals("���ݽɻ����")) {
						Elements lis = h.nextElementSibling().getElementsByTag("li");
						for (Element l : lis) {
							if (l.text().contains("���̵����")) {
								level = l.text();
								level = level.replaceAll("[^\\d.]", "");
								break;
							}
						}
					}
				}
			}
			
			String sql = "UPDATE narabidinfo SET �������=\"" + bidMethod + "\", " +
					"������=\"" + rebid + "\", " +
					"�����=\"" + exec + "\", " +
					"��ȸ��=\"" + obs + "\", " +
					"���������Ͻ�=\"" + openDate + "\", " +
					"���������Ͻ�=\"" + openDate + "\", " +
					"�������=\"" + priceMethod + "\", " +
					"�������ѻ���=\"" + limit + "\", " +
					"����=1, " +
					"���񰡰�=\"" + pricing + "\", " +
					"���̵�=\"" + level + "\" " + where;
			st.executeUpdate(sql);
		}
	}
	
	public void parseRes(Document doc, String where, String bidno, String bidcate, String reno, String tid) throws SQLException, IOException {
		Elements keys = doc.getElementsByTag("th");
		
		boolean hasDup = false;
		boolean hasRebids = false;
		String openDate = ""; // ���������Ͻ�
		String exec = ""; // �����
		String obs = ""; // ��ȸ��
		String annOrg = ""; // ������
		String demOrg = ""; // ������
		
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i).text();
			if (key.equals("���������Ͻ�") || key.equals("�����Ͻ�")) {
				openDate = keys.get(i).nextElementSibling().text();
			}
			else if (key.equals("�����")) {
				exec = keys.get(i).nextElementSibling().text();
			}
			else if (key.equals("��ȸ��(�����)")) {
				obs = keys.get(i).nextElementSibling().text();
			}
			else if (key.equals("������")) {
				annOrg = keys.get(i).nextElementSibling().text();
			}
			else if (key.equals("������")) {
				demOrg = keys.get(i).nextElementSibling().text();
			}
			else if (key.equals("�������� �� ��������")) {
				Element buttonDiv = keys.get(i).nextElementSibling();
				if (buttonDiv.getElementsByTag("a").size() > 0) hasDup = true;
			}
			else if (key.equals("��������ȣ")) {
				hasRebids = true;
			}
		}
		
		String sql = "UPDATE narabidinfo SET ���������Ͻ�=\"" + openDate + "\", " +
				"�����=\"" + exec + "\", " +
				"��ȸ��=\"" + obs + "\", " +
				"������=\"" + annOrg + "\", " +
				"������=\"" + demOrg + "\" " + where;
		st.executeUpdate(sql);
		
		if (hasDup) {
			String duppath = NaraParser.DUP_PRICE;
			duppath += "bidno=";
			duppath += bidno.split("-")[0];
			duppath += "&bidseq=";
			duppath += bidno.split("-")[1];
			duppath += "&rebidno=";
			duppath += reno; // ��������ȣ
			duppath += "&bidcate=";
			duppath += bidcate; // �����з�
			
			openConnection(duppath, "GET");
			
			Document dup = Jsoup.parse(getResponse(null, "GET"));
			parseDup(dup, where);
		}
		else {
			sql = "UPDATE narabidinfo SET �Ϸ�=1 " + where;
			st.executeUpdate(sql);
		}
					
		if (hasRebids) {
			if (Integer.parseInt(reno) > 0) {
				System.out.println("Has rebids");
				reno = (Integer.parseInt(reno) - 1) + "";
				where = "WHERE �����ȣ���� = \"" + bidno + "\" AND ��������ȣ=" + reno + " AND �����з�=" + bidcate;
				
				boolean rexists = false;
				
				rs = st.executeQuery("SELECT EXISTS(SELECT �����ȣ���� FROM narabidinfo " + where + ");");
				if (rs.next()) rexists = rs.getBoolean(1);
				
				if (rexists) {
					
				}
				
				String itempath = "";
				if (tid.equals("1") || tid.equals("9")) itempath = NaraParser.PROD_RES; // ��ǰ
				else if (tid.equals("2")) itempath = NaraParser.SELECT_RES; // ����
				else if (tid.equals("3")) itempath = NaraParser.FACIL_RES; // ����
				else if (tid.equals("4")) itempath = NaraParser.STPL_RES; // ����
				else if (tid.equals("5")) itempath = NaraParser.SERV_RES; // �뿪
				else if (tid.equals("6") || tid.equals("7")) itempath = NaraParser.LEASE_RES; // ����
				else if (tid.equals("20")) itempath = NaraParser.PRIV_RES; // �ΰ�
				
				itempath += "bidcate=" + bidcate;
				itempath += "&bidno=";
				itempath += bidno.split("-")[0];
				itempath += "&bidseq=";
				itempath += bidno.split("-")[1];
				itempath += "&rebidno=";
				itempath += reno;
				itempath += "&progressInfo=%C0%E7%C0%D4%C2%FB%B0%B3%C2%FB%B0%E1%B0%FA";
				
				System.out.println(itempath);
				
				openConnection(itempath, "GET");
				Document resDoc = Jsoup.parse(getResponse(null, "GET"));
				parseRes(resDoc, where, bidno, bidcate, reno, tid);
			}
		}
	}
	
	public void parseDup(Document doc, String where) throws SQLException {
		Elements heads = doc.getElementsByTag("th");
		String expPrice = "0"; // ��������
		
		for (Element h : heads) {
			if (h.text().contains("��÷����")) {
				String ind = h.text().split(" ")[1];
				String price = h.nextElementSibling().text();
				price = price.replaceAll("[^\\d.]", "");
				String comp = h.nextElementSibling().nextElementSibling().text();
				
				String sql = "UPDATE narabidinfo SET ����" + ind + "=" + price + ", ����" + ind + "=" + comp + " " + where;
				st.executeUpdate(sql);
			}
			else if (h.text().contains("�ۼ��ð�")) {
				String dupTime = h.nextElementSibling().text();
				String sql = "UPDATE narabidinfo SET �����ۼ��ð�=\"" + dupTime + "\" " + where;
				st.executeUpdate(sql);
			}
			else if (h.text().equals("��������")) {
				String price = h.nextElementSibling().text();
				expPrice = price.replaceAll("[^\\d.]", "");
				if (expPrice.equals("")) expPrice = "0";
			}
		}
		
		String sql = "UPDATE narabidinfo SET �Ϸ�=1, " + 
				"��������=" + expPrice + " " + where;
		st.executeUpdate(sql);
	}
	
	public void run() {
		curItem = 0;
		try {
			setOption("����");
			getList();
			setOption("���");
			getList();
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}		
	}

	public int getTotal() throws IOException {
		String path = NaraParser.RES_LIST;
		
		path += "searchType=1"; // Search by open date;
		path += "&bidSearchType=2"; // Search by open date;
		path += "&searchDtType=2"; // Search by open date;
		path += "&fromOpenBidDt="+sd; // Starting date;
		path += "&toOpenBidDt="+ed; // Starting date;
		path += "&radOrgan=1"; // Radical organ is 1!
		path += "&budgetCompare=UP"; // Budget compare.
		path += "&regYn=Y"; // Regyn is Y for some reason.
		path += "&recordCountPerPage=100"; // Record per page;
		path += "&useTotalCount=Y"; // Show total count;
		path += "&currentPageNo=1"; // Page number index.
		
		openConnection(path, "GET");
		Document doc = Jsoup.parse(getResponse(null, "GET"));
		
		// Get the total count
		Element countdiv = doc.getElementsByAttributeValue("class", "inforight").first();
		totalItems = Integer.parseInt(countdiv.text().replaceAll("[^\\d]", ""));
		
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
