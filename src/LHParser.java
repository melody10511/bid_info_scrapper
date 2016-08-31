import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LHParser extends Parser {
		
	// For SQL setup.
	Connection con;
	java.sql.Statement st;
	ResultSet rs;
	
	final static String BID_ANN = "http://ebid.lh.or.kr/ebid.et.tp.cmd.BidMasterListCmd.dev";
	final static String BID_RES = "http://ebid.lh.or.kr/ebid.et.tp.cmd.TenderOpenListCmd.dev";
	
	boolean search; // True if the results were searched by dates.
	String sd; // Start date of search. Could be blank.
	String ed; // End date of search. Could be blank.
	String op; // Option for stating which page you want to parse.
	String url; // For current working URL.
	
	WebDriver driver;
	Document parsedMainPage; // Jsoup parsed version of the main page.
	WebElement startDate; // Start date for search.
	WebElement endDate; // End date for search.
	
	int totalItems; // Total number of items found.
	int curItem;
	
	public LHParser(String sd, String ed, String op) throws ClassNotFoundException, SQLException {
		if (sd.equals("") && ed.equals("")) {
			search = false;
		}
		else {
			search = true;
		}
		this.sd = sd;
		this.ed = ed;
		this.op = op;
		
		curItem = 0;
		
		// Set up SQL connection.
		Class.forName("com.mysql.jdbc.Driver");
		Connection con = null;
		con = DriverManager.getConnection("jdbc:mysql://localhost/" + Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
		st = con.createStatement();
		rs = null;
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, MalformedURLException, IOException {
		LHParser parser = new LHParser("2016/01/01", "2016/08/01", "���");
		
		//parser.op = "����";
		//parser.parseBidAnn();
		parser.op = "���";
		parser.parseBidRes();
	}
	
	public void initialize() {
		System.setProperty("webdriver.ie.driver", Resources.IE_DRIVER_PATH + "IEDriverServer.exe");
		DesiredCapabilities ieCapabilities = DesiredCapabilities.internetExplorer();
		ieCapabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
		ieCapabilities.setCapability(InternetExplorerDriver.UNEXPECTED_ALERT_BEHAVIOR, UnexpectedAlertBehaviour.IGNORE);
		driver = new InternetExplorerDriver(ieCapabilities);
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	
	public void searchByDate() throws IOException {
		WebElement searchTable = null;
		
		if (op.equals("����")) {
			searchTable = driver.findElement(By.xpath("//table[@summary='����������ȸ']"));
			startDate = driver.findElement(By.xpath("//input[@name='s_tndrdocAcptOpenDtm']"));
			endDate = driver.findElement(By.xpath("//input[@name='s_tndrdocAcptEndDtm']"));
		}
		else if (op.equals("���")) {
			searchTable = driver.findElement(By.xpath("//form[@name='sform']"));
			startDate = driver.findElement(By.xpath("//input[@name='s_openDtm1']"));
			endDate = driver.findElement(By.xpath("//input[@name='s_openDtm2']"));
		}
		
		startDate.clear();
		endDate.clear();
		
		startDate.sendKeys(sd);
		endDate.sendKeys(ed);
		searchTable.findElement(By.xpath("//input[@class='Limage']")).click();
	}
	
	public int getTotal() throws IOException {
		initialize();
		url = BID_RES;
		setOption("���");
		
		driver.get(url);
		driver.findElement(By.xpath("//table[@summary='�������']"));
		searchByDate();
		driver.findElement(By.xpath("//table[@summary='�������']"));
		parsedMainPage = Jsoup.parse(driver.getPageSource());
		Element paging = parsedMainPage.getElementsByAttributeValue("name", "pform").first();
		if (paging.getElementsByTag("ul").size() > 0) {
			totalItems = Integer.parseInt(paging.getElementsByTag("ul").first().child(0).text().split(" ")[1]);
		}
		else totalItems = 0;
		
		driver.close();
		
		return totalItems;
	}
	
	public void setDate(String sd, String ed) {
		this.sd = sd;
		this.ed = ed;
	}
	
	public void setOption(String op) {
		this.op = op;
	}
	
	public void parseBidAnn() throws IOException, SQLException {
		String keys[] = { "��������", "��������", "�뿪����", "��������", "�����", "�������", "�������", "�����ڼ������", "������", "�����Ͻ�", "���ʱݾ�", "���谡��" };
		ArrayList<String> keylist = new ArrayList<String>(Arrays.asList(keys));
		
		try {
		initialize();
		url = BID_ANN;
		
		driver.get(url); // Main listing page.
		driver.findElement(By.xpath("//table[@summary='�������']"));
		if (search) {
			searchByDate();
			driver.findElement(By.xpath("//table[@summary='�������']"));
		}
		parsedMainPage = Jsoup.parse(driver.getPageSource());
		
		totalItems = Integer.parseInt(parsedMainPage.getElementsByAttributeValue("name", "pform").first().getElementsByTag("ul").first().child(0).text().split(" ")[1]);
		Element listing = parsedMainPage.getElementsByTag("table").get(1);
		Elements rows = listing.getElementsByTag("tr");
		
		String[] bidNums = new String[10];
		String[] workTypes = new String[10];
		String[] bidTypes = new String[10];
		String[] compTypes = new String[10];
		String[] deadlines = new String[10];
		String[] orgs = new String[10];
		String[] progs = new String[10];
		
		for(int i = 1; i < rows.size(); i++) {
			Elements data = rows.get(i).getElementsByTag("td");
			bidNums[i-1] = data.get(0).text();
			workTypes[i-1] = data.get(1).text();
			bidTypes[i-1] = data.get(2).text();
			compTypes[i-1] = data.get(4).text();
			deadlines[i-1] = data.get(5).text();
			orgs[i-1] = data.get(6).text();
			progs[i-1] = data.get(7).text();
		}
		
		int index = 0;
		for(int i = 0; i < totalItems; i++) {
			System.out.println(index);
			boolean enter = true;
			boolean exists = false;
			String bidnum = bidNums[index];
			String workType = workTypes[index];
			String bidType = bidTypes[index];
			String compType = compTypes[index];
			String deadline = deadlines[index] + ":00";
			String org = orgs[index];
			String prog = progs[index];
			String where = "WHERE �����ȣ=\"" + bidnum + "\"";
			
			// Check if the �����ȣ already exists in the DB.
			rs = st.executeQuery("SELECT EXISTS(SELECT �����ȣ FROM lhbidinfo " + where + ")");
			if (rs.first()) exists = rs.getBoolean(1);
			
			if (exists) {
				System.out.println(bidnum + " exists.");
				// Check the bid version and update level from the DB.
				rs = st.executeQuery("SELECT ����, ������Ȳ FROM lhbidinfo " + where);
				int finished = 0;
				String dbProg = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbProg = rs.getString(2);
				}
				if (finished > 0) {
					if (dbProg.equals(prog)) enter = false;
					else {
						String sql = "UPDATE exbidinfo SET ������Ȳ=\"" + prog + "\" " + where;
						st.executeUpdate(sql);
					}
				}
			}
			else {
				String sql = "INSERT INTO lhbidinfo (�����ȣ, ����, �з�, �����, ������������, ��������, ������Ȳ) VALUES (" +
						"\"" + bidnum + "\", " +
						"\"" + workType + "\", " +
						"\"" + bidType + "\", " +
						"\"" + compType + "\", " +
						"\"" + deadline + "\", " +
						"\"" + org + "\", " +
						"\"" + prog + "\");";
				st.executeUpdate(sql);
			}
			
			if (enter) {
				driver.findElement(By.xpath("//table[@summary='�������']")).findElements(By.tagName("tr")).get(index + 1).click(); // Getting the first item in the table
				checkAlert(); // For catching unexpected alerts.
				driver.findElement(By.xpath("//table[@summary='�����Ϲ�����']")); // Check if page has loaded.
				
				Document infoPage = Jsoup.parse(driver.getPageSource());
				Elements infoHeaders = infoPage.getElementsByTag("th");
				boolean hasBase = false;
				String conPrice = "0";
				for(int j = 0; j < infoHeaders.size(); j++) {
					String key = infoHeaders.get(j).text();
					if (keylist.contains(key)) {
						if (infoHeaders.get(j).nextElementSibling() != null) {
							String value = infoHeaders.get(j).nextElementSibling().text();
							if (key.equals("�뿪����") || key.equals("��������")) {
								key = "��������";
							}
							else if (key.equals("�����Ͻ�")) {
								value += ":00";
							}
							else if (key.equals("���ʱݾ�")) {
								value = value.split(" ")[0];
								value = value.replaceAll(",", "");
								value = value.replaceAll("��", "");
								if (!Resources.isInteger(value)) value = "0";
								hasBase = true;
								String sql = "UPDATE lhbidinfo SET " + key + "=" + value + " " + where;
								st.executeUpdate(sql);
								continue;
							}
							else if (key.equals("���谡��")) {
								value = value.split(" ")[0];
								value = value.replaceAll(",", "");
								value = value.replaceAll("��", "");
								if (!Resources.isInteger(value)) value = "0";
								conPrice = value;
								continue;
							}
							System.out.println(key + " : " + value);
							String sql = "UPDATE lhbidinfo SET " + key + "=\"" + value + "\" " + where;
							st.executeUpdate(sql);
						}
					}
				}
				if (!hasBase) {
					System.out.println("���谡�� : " + conPrice);
					String sql = "UPDATE lhbidinfo SET ���ʱݾ�=" + conPrice + " " + where;
					st.executeUpdate(sql);
				}
				st.executeUpdate("UPDATE lhbidinfo SET ����=1 " + where);
				driver.navigate().back();
			}
			
			// Next page algorithm.
			if (i % 10 == 9 && i < (totalItems - 1)) {
				index = 0;
				if (driver.findElement(By.xpath("//img[@alt='����������']")) != null) {
					driver.findElement(By.xpath("//img[@alt='����������']")).click();
				}
				
				driver.findElement(By.xpath("//table[@summary='�������']")).findElement(By.tagName("td")); // For checking page load status.
				parsedMainPage = Jsoup.parse(driver.getPageSource());
				listing = parsedMainPage.getElementsByTag("table").get(1);
				rows = listing.getElementsByTag("tr");
				for(int x = 1; x < rows.size(); x++) {
					Elements data = rows.get(x).getElementsByTag("td");
					bidNums[x-1] = data.get(0).text();
					workTypes[x-1] = data.get(1).text();
					bidTypes[x-1] = data.get(2).text();
					compTypes[x-1] = data.get(4).text();
					deadlines[x-1] = data.get(5).text();
					orgs[x-1] = data.get(6).text();
					progs[x-1] = data.get(7).text();
				}
			}
			else {
				driver.findElement(By.xpath("//table[@summary='�������']")).findElement(By.tagName("td")); // For checking page load status.
				parsedMainPage = Jsoup.parse(driver.getPageSource());
				listing = parsedMainPage.getElementsByTag("table").get(1);
				rows = listing.getElementsByTag("tr");
				for(int x = 1; x < rows.size(); x++) {
					Elements data = rows.get(x).getElementsByTag("td");
					bidNums[x-1] = data.get(0).text();
					workTypes[x-1] = data.get(1).text();
					bidTypes[x-1] = data.get(2).text();
					compTypes[x-1] = data.get(4).text();
					deadlines[x-1] = data.get(5).text();
					orgs[x-1] = data.get(6).text();
					progs[x-1] = data.get(7).text();
				}
				index++;
			}
			System.out.println();
		}
		driver.close();
		} catch (Exception e) {
			driver.close();
			Logger.getGlobal().log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void parseBidRes() throws IOException, SQLException {
		curItem = 0;
		
		try {
		initialize();
		url = BID_RES;
		
		driver.get(url);
		WebElement listTable = driver.findElement(By.xpath("//table[@summary='�������']"));
		if (search) {
			searchByDate();
			listTable = driver.findElement(By.xpath("//table[@summary='�������']"));
		}
		parsedMainPage = Jsoup.parse(driver.getPageSource());
		totalItems = Integer.parseInt(parsedMainPage.getElementsByAttributeValue("name", "pform").first().getElementsByTag("ul").first().child(0).text().split(" ")[1]);
		System.out.println(totalItems);
		
		Element listing = parsedMainPage.getElementsByTag("table").get(1);
		Elements rows = listing.getElementsByTag("tr");
		
		String[] bidNums = new String[10];
		String[] workTypes = new String[10];
		String[] bidTypes = new String[10];
		String[] openDates = new String[10];
		String[] avails = new String[10];
		
		for(int i = 1; i < rows.size(); i++) {
			Elements data = rows.get(i).getElementsByTag("td");
			bidNums[i-1] = data.get(0).text();
			workTypes[i-1] = data.get(1).text();
			bidTypes[i-1] = data.get(2).text();
			openDates[i-1] = data.get(4).text();
			avails[i-1] = data.get(5).text();
		}
		
		int index = 0;
		for(int i = 0; i < totalItems; i++) {
			boolean exists = false;
			boolean enter = true;
			
			curItem++;
			
			String bidnum = bidNums[index];
			String workType = workTypes[index];
			String bidType = bidTypes[index];
			String openDate = openDates[index] + ":00";
			
			String avail = avails[index];
			if (avail.equals("����") || avail.equals("�����")) {
				enter = false;
			}
			
			String where = "WHERE �����ȣ=\"" + bidnum + "\"";
			
			// Check if the �����ȣ already exists in the DB.
			rs = st.executeQuery("SELECT EXISTS(SELECT �����ȣ FROM lhbidinfo " + where + ")");
			if (rs.first()) exists = rs.getBoolean(1);
					
			if (exists) {
				System.out.println(bidnum + " exists.");
				// Check the bid version and update level from the DB.
				rs = st.executeQuery("SELECT �Ϸ�, �������� FROM lhbidinfo " + where);
				int finished = 0;
				String dbResult = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbResult = rs.getString(2);
				}
				if (finished > 0) {
					if (dbResult.equals(avail))	enter = false;
					else {
						String sql = "UPDATE exbidinfo SET ��������=\"" + avail + "\" " + where;
						st.executeUpdate(sql);
					}
				}
			}
			else {
				String sql = "INSERT INTO lhbidinfo (�����ȣ, ����, �з�, �����Ͻ�, ��������) VALUES (" +
						"\"" + bidnum + "\", " +
						"\"" + workType + "\", " +
						"\"" + bidType + "\", " +
						"\"" + openDate + "\", " +
						"\"" + avail + "\");";
				if (avail.equals("����") || avail.equals("�����")) st.executeUpdate("UPDATE lhbidinfo SET �Ϸ�=1 " + where);
			}
			
			if (enter) {
				System.out.println(driver.getCurrentUrl());
				driver.findElement(By.xpath("//table[@summary='�������']")).findElements(By.tagName("tr")).get(index + 1).click();
				//driver.findElement(By.xpath("//table[@summary='�������']")).findElements(By.tagName("tr")).get(index + 1).click();
				checkAlert(); // For catching unexpected alerts.
				driver.findElement(By.xpath("//table[@summary='�⺻����']"));
				Document infoPage = Jsoup.parse(driver.getPageSource());
				
				Element infoTable = infoPage.getElementsByAttributeValue("summary", "�⺻����").first();
				Elements infoHeaders = infoTable.getElementsByTag("th");
				for (int j = 0; j < infoHeaders.size(); j++) {
					String key = infoHeaders.get(j).text();
					if (infoHeaders.get(j).nextElementSibling() != null) {
						if (key.equals("��������")) {
							String value = infoHeaders.get(j).nextElementSibling().text();
							value = value.replaceAll("[^\\d]", "");
							if (value.equals("")) value = "0";
							st.executeUpdate("UPDATE lhbidinfo SET ������������=" + value + " " + where);
						}
					}
				}
				
				Element dupTable = infoPage.getElementById("�⺻����").getElementsByTag("table").get(1);
				Elements dupRows = dupTable.getElementsByTag("tr");
				int companies = 0;
				String[] chosen = new String[4];
				int chosenIndex = 0;
				
				for (int j = 2; j < dupRows.size(); j++) {
					Elements dupData = dupRows.get(j).getElementsByTag("td");
					if (j == 9) {
						String no = dupData.get(0).text();
						Element p = dupData.get(1);
						String price = p.text();
						price = price.replaceAll(",", "");
						String color = p.attr("style");
						if (color.contains("COLOR:") || color.contains("color:")) {
							chosen[chosenIndex] = price;
							chosenIndex++;
							
							String s = "UPDATE lhbidinfo SET ���ð���" + chosenIndex + "=" + price + " " + where;
							st.executeUpdate(s);
						}
						String comp = dupData.get(2).text();
						companies += Integer.parseInt(comp);
						
						System.out.println(no + " : " + price + " - " + comp);
						String s = "UPDATE lhbidinfo SET ����" + no + "=" + price + ", ����" + no + "=" + comp + " " + where;
						st.executeUpdate(s);
					}
					else {
						for (int k = 0; k < 6; k += 3) {
							String no = dupData.get(k).text();
							Element p = dupData.get(k+1);
							String price = p.text();
							price = price.replaceAll(",", "");
							String color = p.attr("style");
							if (color.contains("COLOR:") || color.contains("color:")) {
								chosen[chosenIndex] = price;
								chosenIndex++;
								
								String s = "UPDATE lhbidinfo SET ���ð���" + chosenIndex + "=" + price + " " + where;
								st.executeUpdate(s);
							}
							String comp = dupData.get(k+2).text();
							companies += Integer.parseInt(comp);
							
							System.out.println(no + " : " + price + " - " + comp);
							String s = "UPDATE lhbidinfo SET ����" + no + "=" + price + ", ����" + no + "=" + comp + " " + where;
							st.executeUpdate(s);
						}
					}
				}
				
				long expPrice = 0L;
				for (int j = 0; j < 4; j++) {
					expPrice += Long.parseLong(chosen[j]);
				}
				if ( (expPrice % 4) > 0 ) {
					expPrice = (expPrice / 4) + 1;
				}
				else expPrice = expPrice / 4;
				companies = companies / 2;
				System.out.println("������ : " + companies);
				String s = "UPDATE lhbidinfo SET ������=" + companies + " " + where;
				st.executeUpdate(s);
				System.out.println("�������� : " + expPrice);
				String s2 = "UPDATE lhbidinfo SET �����ݾ�=" + expPrice + " " + where;
				st.executeUpdate(s2);
				
				if (infoPage.getElementsByAttributeValue("summary", "�������").size() == 1) {
					Element resultTable = infoPage.getElementsByAttributeValue("summary", "�������").first();
					Elements resultRows = resultTable.getElementsByTag("tr");
					for (int j = 1; j < resultRows.size(); j++) {
						Elements data = resultRows.get(j).getElementsByTag("td");
						String r = data.get(6).text();
						if (!(r.equals("�����������̸�") || r.equals("������"))) {
							String price = data.get(3).text();
							price = price.replaceAll(",", "");
							System.out.println("�����ݾ� : " + price);
							
							String sql = "UPDATE lhbidinfo SET �����ݾ�=" + price + " " + where;
							st.executeUpdate(sql);
							break;
						}
					}
				}
				else if (infoPage.getElementsByAttributeValue("summary", "�������").size() > 1) {
					Elements tables = infoPage.getElementsByAttributeValue("summary", "�������");
					for (int j = 0; j < tables.size(); j++) {
						Element resultTable = tables.get(j);
						Elements resultRows = resultTable.getElementsByTag("tr");
						if (resultRows.get(1).text().length() > 14) {
							boolean done = false;
							for (int k = 1; k < resultRows.size(); k++) {
								Elements data = resultRows.get(k).getElementsByTag("td");
								String r = data.get(6).text();
								if (!(r.equals("�����������̸�") || r.equals("������"))) {
									String price = data.get(3).text();
									price = price.replaceAll(",", "");
									System.out.println("�����ݾ� : " + price);
									done = true;
									
									String sql = "UPDATE lhbidinfo SET �����ݾ�=" + price + " " + where;
									st.executeUpdate(sql);
									break;
								}
							}
							if (done) break;
						}
					}
				}
				else if (infoPage.getElementsByAttributeValue("summary", "�ɻ����ڻ�����").size() == 1) {
					Element resultTable = infoPage.getElementsByAttributeValue("summary", "�ɻ����ڻ�����").first();
					String price = resultTable.getElementsByTag("tr").get(2).getElementsByTag("td").get(4).text();
					price = price.replaceAll(",", "");
					System.out.println("���Ȱ��� : " + price);
					
					String sql = "UPDATE lhbidinfo SET �����ݾ�=" + price + " " + where;
					st.executeUpdate(sql);
				}
				st.executeUpdate("UPDATE lhbidinfo SET �Ϸ�=1 " + where);
				// Fetching item info.
				System.out.println(driver.getCurrentUrl());
				
				driver.navigate().back();
			}
			
			if (i % 10 == 9 && i < (totalItems - 1)) {
				index = 0;
				WebDriverWait wait = new WebDriverWait(driver, 5);
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//img[@alt='����������']")));
				driver.findElement(By.xpath("//img[@alt='����������']")).click();
				
				listTable = driver.findElement(By.xpath("//table[@summary='�������']")); // For checking page load status.
				System.out.println(listTable.getText());
				do {
					parsedMainPage = Jsoup.parse(driver.getPageSource());
					listing = parsedMainPage.getElementsByTag("table").get(1);
					rows = listing.getElementsByTag("tr");
				} while (rows.get(1).getElementsByTag("td").size() == 0);
				for(int x = 1; x < rows.size(); x++) {
					Elements data = rows.get(x).getElementsByTag("td");
					bidNums[x-1] = data.get(0).text();
					workTypes[x-1] = data.get(1).text();
					bidTypes[x-1] = data.get(2).text();
					openDates[x-1] = data.get(4).text();
					avails[x-1] = data.get(5).text();
				}
			}
			else {
				WebDriverWait wait = new WebDriverWait(driver, 5);
				wait.until(ExpectedConditions.urlToBe("http://ebid.lh.or.kr/ebid.et.tp.cmd.TenderOpenListCmd.dev"));
				//listTable = driver.findElement(By.xpath("//table[@summary='�������']")); // For checking page load status.
				//System.out.println(listTable.getText());
				do {
					parsedMainPage = Jsoup.parse(driver.getPageSource());
					listing = parsedMainPage.getElementsByTag("table").get(1);
					rows = listing.getElementsByTag("tr");
				} while (rows.get(1).getElementsByTag("td").size() == 0);
				for(int x = 1; x < rows.size(); x++) {
					Elements data = rows.get(x).getElementsByTag("td");
					bidNums[x-1] = data.get(0).text();
					workTypes[x-1] = data.get(1).text();
					bidTypes[x-1] = data.get(2).text();
					openDates[x-1] = data.get(4).text();
					avails[x-1] = data.get(5).text();
				}
				index++;
			}
			System.out.println();
		}
		driver.close();
		} catch (Exception e) {
			driver.close();
			e.printStackTrace();
		}
	}
	
	public void checkAlert() {
	    try {
	        WebDriverWait wait = new WebDriverWait(driver, 1);
	        wait.until(ExpectedConditions.alertIsPresent());
	        Alert alert = driver.switchTo().alert();
	        alert.accept();
	    } catch (Exception e) {
	    	
	    }
	}

	public void run() {
		try {
			setOption("����");
			parseBidAnn();
			setOption("���");
			parseBidRes();
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}

	public int getCur() {
		return curItem;
	}
}
