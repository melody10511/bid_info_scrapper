import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Resources {
	final static String CONFIG_FILE = "config.ini";
	
	static String[] COLUMNS = { "", "���������ȣ", "���������Ͻ�", "�������ѻ���", "���ʱݾ�", "�����ݾ�", "�����ݾ�", "��÷����1", "��÷����15", "������", "�����Ͻ�(����)", "�����Ȳ", "������", "�����", "��ȸ��", "������", "������", "�������", "�����", "���̵����", "�������" };
	static String[] SITES = { "��������", "��������û", "LH����", "���ΰ���", "�ѱ�����ȸ" };
	
	static String[] NARA_WORKS = { "��ü", "����", "�뿪", "��ǰ", "����", "����", "����", "��Ÿ", "�ΰ�" };
	static String[] LH_WORKS = { "��ü", "�ü�����", "�뿪", "��ǰ", "��������" };
	static String[] LETS_WORKS = { "��ü", "�ü�����", "����뿪", "��ǰ����", "�Ϲݿ뿪" };
	static String[] EX_WORKS = { "��ü", "����", "�뿪", "��ǰ" };
	
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
