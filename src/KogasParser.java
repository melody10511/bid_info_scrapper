import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class KogasParser {
	final static String BID_ANN_LIST = "http://bid.kogas.or.kr/supplier/contents/bid/bid_list_notice_frm.jsp";
	final static String BID_RES_LIST = "http://bid.kogas.or.kr/supplier/contents/bid/bid_list_result_frm.jsp";
	
	public static void main(String args[]) throws IOException {
		String path = "http://bid.kogas.or.kr/supplier/contents/bid/bid_list_result_frm.jsp";
		URL url = new URL(path);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4");
		
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		Document doc = Jsoup.parse(response.toString());
		System.out.println(doc.html());
	}
}
