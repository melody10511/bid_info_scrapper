import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Resources {
	final static String CONFIG_FILE = "config.ini";
	
	static String[] COLUMNS = { "", "입찰공고번호", "실제개찰일시", "업종제한사항", "기초금액", "예정금액", "투찰금액", "추첨가격1", "추첨가격15", "참가수", "개찰일시(예정)", "진행상황", "재입찰", "집행관", "입회관", "공고기관", "수요기관", "입찰방식", "계약방식", "난이도계수", "예가방법" };
	static String[] SITES = { "나라장터", "국방조달청", "LH공사", "도로공사", "한국마사회" };
	
	static String[] NARA_WORKS = { "전체", "공사", "용역", "물품", "리스", "외자", "비축", "기타", "민간" };
	static String[] LH_WORKS = { "전체", "시설공사", "용역", "물품", "지급자재" };
	static String[] LETS_WORKS = { "전체", "시설공사", "기술용역", "물품구매", "일반용역" };
	static String[] EX_WORKS = { "전체", "공사", "용역", "물품" };
	
	// DB authentication info.
	static String DB_ID = "root";
	static String DB_PW = "qldjel123";
	
	static String SCHEMA = "bid_db_2";
	static String BASE_PATH = "C:/Users/owner/Documents/";
	
	static String IE_DRIVER_PATH = "C:/Users/owner/Downloads/Bid Data Extractor/Bid Data Extractor/IEDriverServer_Win32_2.53.1/";
	
	public static void initialize() {
		FileReader fr;
		try {
			fr = new FileReader("config.ini");
			BufferedReader br = new BufferedReader(fr);
			
			DB_ID = br.readLine().split("=")[1];
			DB_PW = br.readLine().split("=")[1];
			SCHEMA = br.readLine().split("=")[1];
			BASE_PATH = br.readLine().split("=")[1];
			IE_DRIVER_PATH = br.readLine().split("=")[1];
			
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void setValues(String id, String pw, String sc, String bp, String ie) {
		DB_ID = id;
		DB_PW = pw;
		SCHEMA = sc;
		BASE_PATH = bp;
		IE_DRIVER_PATH = ie;
		
		try {
			FileWriter fw = new FileWriter("config.ini");
			
			fw.write("db_id="+id+"\n");
			fw.write("db_pw="+pw+"\n");
			fw.write("schema="+sc+"\n");
			fw.write("base_path="+bp+"\n");
			fw.write("ie_path="+ie+"\n");
			
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * From StackOverFlow
	 * http://stackoverflow.com/questions/237159/whats-the-best-way-to-check-to-see-if-a-string-represents-an-integer-in-java
	 */
	public static boolean isInteger(String str) {
	    if (str == null) {
	        return false;
	    }
	    int length = str.length();
	    if (length == 0) {
	        return false;
	    }
	    int i = 0;
	    if (str.charAt(0) == '-') {
	        if (length == 1) {
	            return false;
	        }
	        i = 1;
	    }
	    for (; i < length; i++) {
	        char c = str.charAt(i);
	        if (c < '0' || c > '9') {
	            return false;
	        }
	    }
	    return true;
	}
}
