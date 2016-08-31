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
		LHParser parser = new LHParser("2016/01/01", "2016/08/01", "결과");
		
		//parser.op = "공고";
		//parser.parseBidAnn();
		parser.op = "결과";
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
		
		if (op.equals("공고")) {
			searchTable = driver.findElement(By.xpath("//table[@summary='입찰공고조회']"));
			startDate = driver.findElement(By.xpath("//input[@name='s_tndrdocAcptOpenDtm']"));
			endDate = driver.findElement(By.xpath("//input[@name='s_tndrdocAcptEndDtm']"));
		}
		else if (op.equals("결과")) {
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
		setOption("결과");
		
		driver.get(url);
		driver.findElement(By.xpath("//table[@summary='목록정보']"));
		searchByDate();
		driver.findElement(By.xpath("//table[@summary='목록정보']"));
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
		String keys[] = { "공고종류", "업종유형", "용역유형", "공사종류", "계약방법", "입찰방법", "입찰방식", "낙찰자선정방법", "재입찰", "개찰일시", "기초금액", "설계가격" };
		ArrayList<String> keylist = new ArrayList<String>(Arrays.asList(keys));
		
		try {
		initialize();
		url = BID_ANN;
		
		driver.get(url); // Main listing page.
		driver.findElement(By.xpath("//table[@summary='목록정보']"));
		if (search) {
			searchByDate();
			driver.findElement(By.xpath("//table[@summary='목록정보']"));
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
			String where = "WHERE 공고번호=\"" + bidnum + "\"";
			
			// Check if the 공고번호 already exists in the DB.
			rs = st.executeQuery("SELECT EXISTS(SELECT 공고번호 FROM lhbidinfo " + where + ")");
			if (rs.first()) exists = rs.getBoolean(1);
			
			if (exists) {
				System.out.println(bidnum + " exists.");
				// Check the bid version and update level from the DB.
				rs = st.executeQuery("SELECT 공고, 공고현황 FROM lhbidinfo " + where);
				int finished = 0;
				String dbProg = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbProg = rs.getString(2);
				}
				if (finished > 0) {
					if (dbProg.equals(prog)) enter = false;
					else {
						String sql = "UPDATE exbidinfo SET 공고현황=\"" + prog + "\" " + where;
						st.executeUpdate(sql);
					}
				}
			}
			else {
				String sql = "INSERT INTO lhbidinfo (공고번호, 업무, 분류, 계약방법, 입찰마감일자, 지역본부, 공고현황) VALUES (" +
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
				driver.findElement(By.xpath("//table[@summary='목록정보']")).findElements(By.tagName("tr")).get(index + 1).click(); // Getting the first item in the table
				checkAlert(); // For catching unexpected alerts.
				driver.findElement(By.xpath("//table[@summary='공고일반정보']")); // Check if page has loaded.
				
				Document infoPage = Jsoup.parse(driver.getPageSource());
				Elements infoHeaders = infoPage.getElementsByTag("th");
				boolean hasBase = false;
				String conPrice = "0";
				for(int j = 0; j < infoHeaders.size(); j++) {
					String key = infoHeaders.get(j).text();
					if (keylist.contains(key)) {
						if (infoHeaders.get(j).nextElementSibling() != null) {
							String value = infoHeaders.get(j).nextElementSibling().text();
							if (key.equals("용역유형") || key.equals("공사종류")) {
								key = "업종유형";
							}
							else if (key.equals("개찰일시")) {
								value += ":00";
							}
							else if (key.equals("기초금액")) {
								value = value.split(" ")[0];
								value = value.replaceAll(",", "");
								value = value.replaceAll("원", "");
								if (!Resources.isInteger(value)) value = "0";
								hasBase = true;
								String sql = "UPDATE lhbidinfo SET " + key + "=" + value + " " + where;
								st.executeUpdate(sql);
								continue;
							}
							else if (key.equals("설계가격")) {
								value = value.split(" ")[0];
								value = value.replaceAll(",", "");
								value = value.replaceAll("원", "");
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
					System.out.println("설계가격 : " + conPrice);
					String sql = "UPDATE lhbidinfo SET 기초금액=" + conPrice + " " + where;
					st.executeUpdate(sql);
				}
				st.executeUpdate("UPDATE lhbidinfo SET 공고=1 " + where);
				driver.navigate().back();
			}
			
			// Next page algorithm.
			if (i % 10 == 9 && i < (totalItems - 1)) {
				index = 0;
				if (driver.findElement(By.xpath("//img[@alt='다음페이지']")) != null) {
					driver.findElement(By.xpath("//img[@alt='다음페이지']")).click();
				}
				
				driver.findElement(By.xpath("//table[@summary='목록정보']")).findElement(By.tagName("td")); // For checking page load status.
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
				driver.findElement(By.xpath("//table[@summary='목록정보']")).findElement(By.tagName("td")); // For checking page load status.
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
		WebElement listTable = driver.findElement(By.xpath("//table[@summary='목록정보']"));
		if (search) {
			searchByDate();
			listTable = driver.findElement(By.xpath("//table[@summary='목록정보']"));
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
			if (avail.equals("유찰") || avail.equals("비공개")) {
				enter = false;
			}
			
			String where = "WHERE 공고번호=\"" + bidnum + "\"";
			
			// Check if the 공고번호 already exists in the DB.
			rs = st.executeQuery("SELECT EXISTS(SELECT 공고번호 FROM lhbidinfo " + where + ")");
			if (rs.first()) exists = rs.getBoolean(1);
					
			if (exists) {
				System.out.println(bidnum + " exists.");
				// Check the bid version and update level from the DB.
				rs = st.executeQuery("SELECT 완료, 개찰내역 FROM lhbidinfo " + where);
				int finished = 0;
				String dbResult = "";
				if (rs.first()) {
					finished = rs.getInt(1);
					dbResult = rs.getString(2);
				}
				if (finished > 0) {
					if (dbResult.equals(avail))	enter = false;
					else {
						String sql = "UPDATE exbidinfo SET 개찰내역=\"" + avail + "\" " + where;
						st.executeUpdate(sql);
					}
				}
			}
			else {
				String sql = "INSERT INTO lhbidinfo (공고번호, 업무, 분류, 개찰일시, 개찰내역) VALUES (" +
						"\"" + bidnum + "\", " +
						"\"" + workType + "\", " +
						"\"" + bidType + "\", " +
						"\"" + openDate + "\", " +
						"\"" + avail + "\");";
				if (avail.equals("유찰") || avail.equals("비공개")) st.executeUpdate("UPDATE lhbidinfo SET 완료=1 " + where);
			}
			
			if (enter) {
				System.out.println(driver.getCurrentUrl());
				driver.findElement(By.xpath("//table[@summary='목록정보']")).findElements(By.tagName("tr")).get(index + 1).click();
				//driver.findElement(By.xpath("//table[@summary='목록정보']")).findElements(By.tagName("tr")).get(index + 1).click();
				checkAlert(); // For catching unexpected alerts.
				driver.findElement(By.xpath("//table[@summary='기본정보']"));
				Document infoPage = Jsoup.parse(driver.getPageSource());
				
				Element infoTable = infoPage.getElementsByAttributeValue("summary", "기본정보").first();
				Elements infoHeaders = infoTable.getElementsByTag("th");
				for (int j = 0; j < infoHeaders.size(); j++) {
					String key = infoHeaders.get(j).text();
					if (infoHeaders.get(j).nextElementSibling() != null) {
						if (key.equals("예정가격")) {
							String value = infoHeaders.get(j).nextElementSibling().text();
							value = value.replaceAll("[^\\d]", "");
							if (value.equals("")) value = "0";
							st.executeUpdate("UPDATE lhbidinfo SET 기존예정가격=" + value + " " + where);
						}
					}
				}
				
				Element dupTable = infoPage.getElementById("기본정보").getElementsByTag("table").get(1);
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
							
							String s = "UPDATE lhbidinfo SET 선택가격" + chosenIndex + "=" + price + " " + where;
							st.executeUpdate(s);
						}
						String comp = dupData.get(2).text();
						companies += Integer.parseInt(comp);
						
						System.out.println(no + " : " + price + " - " + comp);
						String s = "UPDATE lhbidinfo SET 복수" + no + "=" + price + ", 복참" + no + "=" + comp + " " + where;
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
								
								String s = "UPDATE lhbidinfo SET 선택가격" + chosenIndex + "=" + price + " " + where;
								st.executeUpdate(s);
							}
							String comp = dupData.get(k+2).text();
							companies += Integer.parseInt(comp);
							
							System.out.println(no + " : " + price + " - " + comp);
							String s = "UPDATE lhbidinfo SET 복수" + no + "=" + price + ", 복참" + no + "=" + comp + " " + where;
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
				System.out.println("참여수 : " + companies);
				String s = "UPDATE lhbidinfo SET 참가수=" + companies + " " + where;
				st.executeUpdate(s);
				System.out.println("예정가격 : " + expPrice);
				String s2 = "UPDATE lhbidinfo SET 예정금액=" + expPrice + " " + where;
				st.executeUpdate(s2);
				
				if (infoPage.getElementsByAttributeValue("summary", "목록정보").size() == 1) {
					Element resultTable = infoPage.getElementsByAttributeValue("summary", "목록정보").first();
					Elements resultRows = resultTable.getElementsByTag("tr");
					for (int j = 1; j < resultRows.size(); j++) {
						Elements data = resultRows.get(j).getElementsByTag("td");
						String r = data.get(6).text();
						if (!(r.equals("낙찰하한율미만") || r.equals("부적격"))) {
							String price = data.get(3).text();
							price = price.replaceAll(",", "");
							System.out.println("입찰금액 : " + price);
							
							String sql = "UPDATE lhbidinfo SET 투찰금액=" + price + " " + where;
							st.executeUpdate(sql);
							break;
						}
					}
				}
				else if (infoPage.getElementsByAttributeValue("summary", "목록정보").size() > 1) {
					Elements tables = infoPage.getElementsByAttributeValue("summary", "목록정보");
					for (int j = 0; j < tables.size(); j++) {
						Element resultTable = tables.get(j);
						Elements resultRows = resultTable.getElementsByTag("tr");
						if (resultRows.get(1).text().length() > 14) {
							boolean done = false;
							for (int k = 1; k < resultRows.size(); k++) {
								Elements data = resultRows.get(k).getElementsByTag("td");
								String r = data.get(6).text();
								if (!(r.equals("낙찰하한율미만") || r.equals("부적격"))) {
									String price = data.get(3).text();
									price = price.replaceAll(",", "");
									System.out.println("입찰금액 : " + price);
									done = true;
									
									String sql = "UPDATE lhbidinfo SET 투찰금액=" + price + " " + where;
									st.executeUpdate(sql);
									break;
								}
							}
							if (done) break;
						}
					}
				}
				else if (infoPage.getElementsByAttributeValue("summary", "심사대상자상세정보").size() == 1) {
					Element resultTable = infoPage.getElementsByAttributeValue("summary", "심사대상자상세정보").first();
					String price = resultTable.getElementsByTag("tr").get(2).getElementsByTag("td").get(4).text();
					price = price.replaceAll(",", "");
					System.out.println("제안가격 : " + price);
					
					String sql = "UPDATE lhbidinfo SET 투찰금액=" + price + " " + where;
					st.executeUpdate(sql);
				}
				st.executeUpdate("UPDATE lhbidinfo SET 완료=1 " + where);
				// Fetching item info.
				System.out.println(driver.getCurrentUrl());
				
				driver.navigate().back();
			}
			
			if (i % 10 == 9 && i < (totalItems - 1)) {
				index = 0;
				WebDriverWait wait = new WebDriverWait(driver, 5);
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//img[@alt='다음페이지']")));
				driver.findElement(By.xpath("//img[@alt='다음페이지']")).click();
				
				listTable = driver.findElement(By.xpath("//table[@summary='목록정보']")); // For checking page load status.
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
				//listTable = driver.findElement(By.xpath("//table[@summary='목록정보']")); // For checking page load status.
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
			setOption("공고");
			parseBidAnn();
			setOption("결과");
			parseBidRes();
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}

	public int getCur() {
		return curItem;
	}
}
