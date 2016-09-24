import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelWriter {
	Connection con;
	java.sql.Statement st;
	ResultSet rs;
	
	String name;
	String defaultPath;
	String basePath;
	String filePath;
	String site;
	String op;
	Workbook workbook;
	Sheet sheet;
	HSSFCellStyle money;
	SimpleDateFormat sdf;
	String today;
	
	String sd;
	String ed;
	String org;
	String rate;
	String workType;
	
	public ExcelWriter(String site, String op) {
		defaultPath = "F:/";
		basePath = Resources.BASE_PATH;
		this.site = site;
		this.op = op;
		workbook = new HSSFWorkbook();
		sheet = workbook.createSheet("BidInfo");
		money = (HSSFCellStyle) workbook.createCellStyle();
		HSSFDataFormat moneyFormat = (HSSFDataFormat) workbook.createDataFormat();
		money.setDataFormat(moneyFormat.getFormat(BuiltinFormats.getBuiltinFormat(42)));
		sdf = new SimpleDateFormat("yyyy-MM-dd");
		today = sdf.format(new Date()) + " 00:00:00";
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		ExcelWriter tester = new ExcelWriter("��������û", "����");
		
		tester.lhBidToExcel();
	}
	
	public void setOptions(String sd, String ed, String org, String rate, String workType) {
		this.sd = sd;
		this.ed = ed;
		this.org = org;
		this.rate = rate;
		this.workType = workType;
	}
	
	public void toExcel() throws ClassNotFoundException, SQLException, IOException {
		if (site.equals("LH����")) {
			lhBidToExcel();
		}
		else if (site.equals("��������û")) {
			if (op.equals("����")) {
				dapaBidToExcel();
			}
			else if (op.equals("����")) {
				dapaNegoToExcel();
			}
		}
		else if (site.equals("�ѱ�����ȸ")) {
			letsrunBidToExcel();
		}
		else if (site.equals("���ΰ���")) {
			exBidToExcel();
		}
	}
	
	public void connectDB() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		con = DriverManager.getConnection("jdbc:mysql://localhost/" + Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
		st = con.createStatement();
		System.out.println("DB Connected");
	}
	
	public void adjustColumns() {
		sheet.setColumnWidth(0, 1800); // ����
		
		// �����ȣ
		if (site.equals("LH����")) sheet.setColumnWidth(1, 2800);
		else if (site.equals("���ΰ���")) sheet.setColumnWidth(1, 3400);
		else if (site.equals("�ѱ�����ȸ")) sheet.setColumnWidth(1, 3750);
		else if (site.equals("��������û")) sheet.setColumnWidth(1, 3400);
		
		for (int i = 8; i < 37; i++) {
			if (i != 21) sheet.setColumnHidden(i, true);
		}
		
		sheet.setColumnWidth(2, 3300); // �����Ͻ�
		sheet.setColumnWidth(3, 2400); // ��������
		sheet.setColumnWidth(4, 3600); // ���ʱݾ�
		sheet.setColumnWidth(5, 3600); // �����ݾ�
		sheet.setColumnWidth(6, 3600); // �����ݾ�
		sheet.setColumnWidth(7, 3600); // ����1
		sheet.setColumnWidth(8, 3600);
		sheet.setColumnWidth(9, 3600);
		sheet.setColumnWidth(10, 3600);
		sheet.setColumnWidth(11, 3600);
		sheet.setColumnWidth(12, 3600);
		sheet.setColumnWidth(13, 3600);
		sheet.setColumnWidth(14, 3600);
		sheet.setColumnWidth(15, 3600);
		sheet.setColumnWidth(16, 3600);
		sheet.setColumnWidth(17, 3600);
		sheet.setColumnWidth(18, 3600);
		sheet.setColumnWidth(19, 3600);
		sheet.setColumnWidth(20, 3600);
		sheet.setColumnWidth(21, 3600); // ����15
		
		if (site.equals("LH����")) {
			for (int i = 39; i < 45; i++) 
				sheet.setColumnHidden(i, true);
			
			sheet.setColumnWidth(38, 3400); // ������������
			sheet.setColumnWidth(44, 3400); // �����ڼ������
			sheet.setColumnWidth(45, 3600); // ���ð���1
			sheet.setColumnWidth(46, 3600);
			sheet.setColumnWidth(47, 3600);
			sheet.setColumnWidth(48, 3600);
			sheet.setColumnWidth(49, 3600);
		}
		else if (site.equals("���ΰ���")) {
			for (int i = 45; i < 53; i++)
				sheet.setColumnHidden(i, true);
			
			sheet.setColumnWidth(40, 3400);
			sheet.setColumnWidth(41, 3400);
			sheet.setColumnWidth(42, 3400);
			sheet.setColumnWidth(43, 3400);
			sheet.setColumnWidth(44, 3400); // �������� �ǹ�����
			sheet.setColumnWidth(45, 1200);
			sheet.setColumnWidth(46, 1200);
			sheet.setColumnWidth(47, 1200);
			sheet.setColumnWidth(48, 1200);
			sheet.setColumnWidth(49, 1200);
			sheet.setColumnWidth(50, 1200);
			sheet.setColumnWidth(51, 1200);
			sheet.setColumnWidth(52, 1200);
			sheet.setColumnWidth(53, 2000); // ����
		}
		else if (site.equals("�ѱ�����ȸ")) {
			for (int i = 46; i < 53; i++)
				sheet.setColumnHidden(i, true);
			
			sheet.setColumnWidth(38, 3400); // ��������
			sheet.setColumnWidth(41, 3600); // �������ѱݾ�
			sheet.setColumnWidth(42, 2800); // ����������
			sheet.setColumnWidth(44, 2900);
			sheet.setColumnWidth(45, 3600); // �����ڰ������
			sheet.setColumnWidth(46, 1500);
			sheet.setColumnWidth(47, 1500);
			sheet.setColumnWidth(48, 1500);
			sheet.setColumnWidth(49, 1500);
			sheet.setColumnWidth(50, 1500);
			sheet.setColumnWidth(51, 1500);
			sheet.setColumnWidth(52, 1500);
		}
		else if (site.equals("��������û")) {
			for (int i = 46; i < 53; i++)
				sheet.setColumnHidden(i, true);
			
			if (op.equals("����")) {
				sheet.setColumnWidth(38, 3850); // ���ֱ��
				sheet.setColumnWidth(41, 4200); // ���ʿ������뿩��
				sheet.setColumnWidth(43, 3850); // �����ڰ������
				sheet.setColumnWidth(44, 3400); // ���������⸶���Ͻ�
				sheet.setColumnWidth(45, 2700);
				sheet.setColumnWidth(46, 1200);
				sheet.setColumnWidth(47, 1200);
				sheet.setColumnWidth(48, 1200);
				sheet.setColumnWidth(49, 1200);
				sheet.setColumnWidth(50, 1200);
				sheet.setColumnWidth(51, 1200);
				sheet.setColumnWidth(52, 1200);
				sheet.setColumnWidth(53, 2700);
			}
			else if (op.equals("����")) {
				sheet.setColumnWidth(38, 3850); // ���ֱ��
				sheet.setColumnWidth(39, 3850); // �����ڰ������
				sheet.setColumnWidth(41, 4200); // ���ʿ������뿩��
				sheet.setColumnWidth(42, 3400); // ���������⸶���Ͻ�
				sheet.setColumnWidth(45, 2700);
				sheet.setColumnWidth(46, 1200);
				sheet.setColumnWidth(47, 1200);
				sheet.setColumnWidth(48, 1200);
				sheet.setColumnWidth(49, 1200);
				sheet.setColumnWidth(50, 1200);
				sheet.setColumnWidth(51, 1200);
				sheet.setColumnWidth(52, 1200);
				sheet.setColumnWidth(53, 2700);
			}
		}
	}
	
	public void labelColumns() {
		int cellIndex = 0;
		
		Row columnNames = sheet.createRow(0);
		columnNames.createCell(cellIndex++).setCellValue("����");
		
		if (site.equals("���ΰ���")) columnNames.createCell(cellIndex++).setCellValue("���������ȣ");
		else if (site.equals("��������û")) columnNames.createCell(cellIndex++).setCellValue("�����ȣ-����");
		else if (site.equals("LH����") || site.equals("�ѱ�����ȸ")) columnNames.createCell(cellIndex++).setCellValue("�����ȣ");
		
		columnNames.createCell(cellIndex++).setCellValue("�����Ͻ�");
		
		if (site.equals("���ΰ���")) columnNames.createCell(cellIndex++).setCellValue("�������ѻ���");
		else if (site.equals("LH����")) columnNames.createCell(cellIndex++).setCellValue("����");
		else if (site.equals("��������û")) columnNames.createCell(cellIndex++).setCellValue("�����Ī[�ڵ�]");
		else if (site.equals("�ѱ�����ȸ")) columnNames.createCell(cellIndex++).setCellValue("��������");
		
		if (site.equals("���ΰ���")) columnNames.createCell(cellIndex++).setCellValue("����ݾ�");
		else columnNames.createCell(cellIndex++).setCellValue("���ʱݾ�");
		columnNames.createCell(cellIndex++).setCellValue("��������");
		columnNames.createCell(cellIndex++).setCellValue("�����ݾ�");
		columnNames.createCell(cellIndex++).setCellValue("1");
		columnNames.createCell(cellIndex++).setCellValue("2");
		columnNames.createCell(cellIndex++).setCellValue("3");
		columnNames.createCell(cellIndex++).setCellValue("4");
		columnNames.createCell(cellIndex++).setCellValue("5");
		columnNames.createCell(cellIndex++).setCellValue("6");
		columnNames.createCell(cellIndex++).setCellValue("7");
		columnNames.createCell(cellIndex++).setCellValue("8");
		columnNames.createCell(cellIndex++).setCellValue("9");
		columnNames.createCell(cellIndex++).setCellValue("10");
		columnNames.createCell(cellIndex++).setCellValue("11");
		columnNames.createCell(cellIndex++).setCellValue("12");
		columnNames.createCell(cellIndex++).setCellValue("13");
		columnNames.createCell(cellIndex++).setCellValue("14");
		columnNames.createCell(cellIndex++).setCellValue("15");
		columnNames.createCell(cellIndex++).setCellValue("1");
		columnNames.createCell(cellIndex++).setCellValue("2");
		columnNames.createCell(cellIndex++).setCellValue("3");
		columnNames.createCell(cellIndex++).setCellValue("4");
		columnNames.createCell(cellIndex++).setCellValue("5");
		columnNames.createCell(cellIndex++).setCellValue("6");
		columnNames.createCell(cellIndex++).setCellValue("7");
		columnNames.createCell(cellIndex++).setCellValue("8");
		columnNames.createCell(cellIndex++).setCellValue("9");
		columnNames.createCell(cellIndex++).setCellValue("10");
		columnNames.createCell(cellIndex++).setCellValue("11");
		columnNames.createCell(cellIndex++).setCellValue("12");
		columnNames.createCell(cellIndex++).setCellValue("13");
		columnNames.createCell(cellIndex++).setCellValue("14");
		columnNames.createCell(cellIndex++).setCellValue("15");
		columnNames.createCell(cellIndex++).setCellValue("������");
		if (site.equals("���ΰ���")) {
			columnNames.createCell(cellIndex++).setCellValue("��������");
			columnNames.createCell(cellIndex++).setCellValue("������������");
			columnNames.createCell(cellIndex++).setCellValue("��������뿩��");
			columnNames.createCell(cellIndex++).setCellValue("������������");
			columnNames.createCell(cellIndex++).setCellValue("�������� ���ɿ���");
			columnNames.createCell(cellIndex++).setCellValue("���弳��ǽÿ���");
			columnNames.createCell(cellIndex++).setCellValue("�������� �ǹ�����");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("����");
		}
		else if (site.equals("LH����")) {
			columnNames.createCell(cellIndex++).setCellValue("������������");
			columnNames.createCell(cellIndex++).setCellValue("�����");
			columnNames.createCell(cellIndex++).setCellValue("��������");
			columnNames.createCell(cellIndex++).setCellValue("�������");
			columnNames.createCell(cellIndex++).setCellValue("�������");
			columnNames.createCell(cellIndex++).setCellValue("�����ڼ������");
			columnNames.createCell(cellIndex++).setCellValue("������");
			columnNames.createCell(cellIndex++).setCellValue("���ð���1");
			columnNames.createCell(cellIndex++).setCellValue("���ð���2");
			columnNames.createCell(cellIndex++).setCellValue("���ð���3");
			columnNames.createCell(cellIndex++).setCellValue("���ð���4");
			columnNames.createCell(cellIndex++).setCellValue("����Ʈ�����ݾ�");
			columnNames.createCell(cellIndex++).setCellValue("��������");
			columnNames.createCell(cellIndex++).setCellValue("�з�");
			columnNames.createCell(cellIndex++).setCellValue("��������");
			columnNames.createCell(cellIndex++).setCellValue("��������");
		}
		else if (site.equals("�ѱ�����ȸ")) {
			columnNames.createCell(cellIndex++).setCellValue("��������");
			columnNames.createCell(cellIndex++).setCellValue("�������");
			columnNames.createCell(cellIndex++).setCellValue("��������");
			columnNames.createCell(cellIndex++).setCellValue("�������ѱݾ�");
			columnNames.createCell(cellIndex++).setCellValue("����������");
			columnNames.createCell(cellIndex++).setCellValue("�����");
			columnNames.createCell(cellIndex++).setCellValue("�������ݹ��");
			columnNames.createCell(cellIndex++).setCellValue("�����ڰ������");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("");
			columnNames.createCell(cellIndex++).setCellValue("�������");
		}
		else if (site.equals("��������û")) {
			if (op.equals("����")) {
				columnNames.createCell(cellIndex++).setCellValue("���ֱ��");
				columnNames.createCell(cellIndex++).setCellValue("�����");
				columnNames.createCell(cellIndex++).setCellValue("�������");
				columnNames.createCell(cellIndex++).setCellValue("���ʿ������뿩��");
				columnNames.createCell(cellIndex++).setCellValue("�����ɻ�");
				columnNames.createCell(cellIndex++).setCellValue("�����ڰ������");
				columnNames.createCell(cellIndex++).setCellValue("���������� �����Ͻ�");
				columnNames.createCell(cellIndex++).setCellValue("����������");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("������");
			}
			else if (op.equals("����")) {
				columnNames.createCell(cellIndex++).setCellValue("���ֱ��");
				columnNames.createCell(cellIndex++).setCellValue("�����ڰ������");
				columnNames.createCell(cellIndex++).setCellValue("�������");
				columnNames.createCell(cellIndex++).setCellValue("���ʿ������뿩��");
				columnNames.createCell(cellIndex++).setCellValue("���������⸶���Ͻ�");
				columnNames.createCell(cellIndex++).setCellValue("��������");
				columnNames.createCell(cellIndex++).setCellValue("����������");
				columnNames.createCell(cellIndex++).setCellValue("����������");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("");
				columnNames.createCell(cellIndex++).setCellValue("������");
			}
		}
	}
	
	public void toFile() throws IOException {
		name = "";
		if (org != null) { name += org + " "; }
		
		if (site.equals("���ΰ���")) { 
			name = "�ѱ����ΰ���";
			if (org != null) {
				name += " " + org;
			}
		}
		else if (site.equals("LH����") && org != null) {
			if (org.equals("�� ��")) org = "����";
			name = "LH " + org;
		}
		else if (site.equals("��������û") && org != null) { name = org; }
		else if (site.equals("�ѱ�����ȸ") && org != null) { name = site + " " + org; }
		else name += site;
		
		if (workType == null) {
			if (site.equals("��������û")) {
				if (op.equals("����")) name += "(����)";
				else if (op.equals("����")) name += "(����)";
			}
			else name += "(��ü)";
		}
		else {
			if (workType.equals("�ü�����")) { name += "(����)"; }
			else if (workType.equals("����뿪")) { name += "(���)"; }
			else if (workType.equals("��ǰ����")) { name += "(��ǰ)"; }
			else if (workType.equals("�Ϲݿ뿪")) { name += "(�Ϲ�)"; }
			else { name += "(" + workType + ")"; }
		}
		
		int fi = 2;
		File f = new File(defaultPath);
		FileOutputStream fos = null;
		if (f.exists()) {
			defaultPath = "F:/" + name + ".xls";
			f = new File(defaultPath);
			while (f.exists() && !f.isDirectory()) {
				defaultPath = "F:/" + name + "-" + fi + ".xls";
				f = new File(defaultPath);
				fi++;
			}
			fos = new FileOutputStream(defaultPath);
		}
		else {
			filePath = basePath + name + ".xls";
			f = new File(filePath);
			while (f.exists() && !f.isDirectory()) {
				filePath = basePath + name + "-" + fi + ".xls";
				f = new File(filePath);
				fi++;
			}
			fos = new FileOutputStream(filePath);
		}
		workbook.write(fos);
		fos.close();
	}
	
	public void naraBidToExcel() throws ClassNotFoundException, SQLException, IOException {
		connectDB();
		
		adjustColumns();
		
		labelColumns();
	}
	
	public void exBidToExcel() throws ClassNotFoundException, SQLException, IOException  {
		connectDB();
		
		adjustColumns();
		
		labelColumns();
		
		String sql = "SELECT * FROM exbidinfo WHERE ";
		if (sd != null || ed != null || org != null || rate != null || workType != null) {
			if ((sd != null) && (ed != null)) {
				sql += "�����Ͻ� >= \"" + sd + "\" AND �����Ͻ� <= \"" + ed + "\" AND ";
			}
			
			if (org != null) {
				sql += "����=\"" + org + "\" AND ";
			}
			if (workType != null) {
				sql += "�з�=\"" + workType + "\" AND ";
			}
		}
		if (sd == null && ed == null) {
			sql += "�����Ͻ� >= \"2016-01-01 00:00:00\" AND ";
		}
		sql += "�Ϸ�=1 ";
		
		sql += "UNION SELECT * FROM exbidinfo WHERE ";
		if (org != null || rate != null || workType != null) {
			if (org != null) {
				sql += "����=\"" + org + "\" AND ";
			}
			if (workType != null) {
				sql += "�з�=\"" + workType + "\" AND ";
			}
		}
		sql += "�����Ͻ� >= \"" + today + "\" ORDER BY �����Ͻ�, �����ȣ;";
		
		System.out.println(sql);
		rs = st.executeQuery(sql);
		
		int rowIndex = 1;
		int cellIndex = 0;
		int index = 1;
		while(rs.next()) {
			Row row = sheet.createRow(rowIndex++);
			cellIndex = 0;
			row.createCell(cellIndex++).setCellValue(index);
			row.createCell(cellIndex++).setCellValue(rs.getString("�����ȣ"));
			String od = rs.getString("�����Ͻ�");
			od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
			row.createCell(cellIndex++).setCellValue(od);
			row.createCell(cellIndex++).setCellValue(rs.getString("�з�"));
			HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
			basePriceCell.setCellStyle(money);
			basePriceCell.setCellValue(rs.getLong("����ݾ�"));
			HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
			expectedPriceCell.setCellStyle(money);
			expectedPriceCell.setCellValue(rs.getLong("��������"));
			HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
			bidPriceCell.setCellStyle(money);
			bidPriceCell.setCellValue(rs.getLong("�����ݾ�"));
			HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell1.setCellStyle(money);
			dupPriceCell1.setCellValue(rs.getLong("����1"));
			HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell2.setCellStyle(money);
			dupPriceCell2.setCellValue(rs.getLong("����2"));
			HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell3.setCellStyle(money);
			dupPriceCell3.setCellValue(rs.getLong("����3"));
			HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell4.setCellStyle(money);
			dupPriceCell4.setCellValue(rs.getLong("����4"));
			HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell5.setCellStyle(money);
			dupPriceCell5.setCellValue(rs.getLong("����5"));
			HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell6.setCellStyle(money);
			dupPriceCell6.setCellValue(rs.getLong("����6"));
			HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell7.setCellStyle(money);
			dupPriceCell7.setCellValue(rs.getLong("����7"));
			HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell8.setCellStyle(money);
			dupPriceCell8.setCellValue(rs.getLong("����8"));
			HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell9.setCellStyle(money);
			dupPriceCell9.setCellValue(rs.getLong("����9"));
			HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell10.setCellStyle(money);
			dupPriceCell10.setCellValue(rs.getLong("����10"));
			HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell11.setCellStyle(money);
			dupPriceCell11.setCellValue(rs.getLong("����11"));
			HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell12.setCellStyle(money);
			dupPriceCell12.setCellValue(rs.getLong("����12"));
			HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell13.setCellStyle(money);
			dupPriceCell13.setCellValue(rs.getLong("����13"));
			HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell14.setCellStyle(money);
			dupPriceCell14.setCellValue(rs.getLong("����14"));
			HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell15.setCellStyle(money);
			dupPriceCell15.setCellValue(rs.getLong("����15"));
			HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell1.setCellStyle(money);
			dupComCell1.setCellValue(rs.getLong("����1"));
			HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell2.setCellStyle(money);
			dupComCell2.setCellValue(rs.getLong("����2"));
			HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell3.setCellStyle(money);
			dupComCell3.setCellValue(rs.getLong("����3"));
			HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell4.setCellStyle(money);
			dupComCell4.setCellValue(rs.getLong("����4"));
			HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell5.setCellStyle(money);
			dupComCell5.setCellValue(rs.getLong("����5"));
			HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell6.setCellStyle(money);
			dupComCell6.setCellValue(rs.getLong("����6"));
			HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell7.setCellStyle(money);
			dupComCell7.setCellValue(rs.getLong("����7"));
			HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell8.setCellStyle(money);
			dupComCell8.setCellValue(rs.getLong("����8"));
			HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell9.setCellStyle(money);
			dupComCell9.setCellValue(rs.getLong("����9"));
			HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell10.setCellStyle(money);
			dupComCell10.setCellValue(rs.getLong("����10"));
			HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell11.setCellStyle(money);
			dupComCell11.setCellValue(rs.getLong("����11"));
			HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell12.setCellStyle(money);
			dupComCell12.setCellValue(rs.getLong("����12"));
			HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell13.setCellStyle(money);
			dupComCell13.setCellValue(rs.getLong("����13"));
			HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell14.setCellStyle(money);
			dupComCell14.setCellValue(rs.getLong("����14"));
			HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell15.setCellStyle(money);
			dupComCell15.setCellValue(rs.getLong("����15"));
			row.createCell(cellIndex++).setCellValue(rs.getInt("������"));
			if (rs.getString("��������") != null) {
				String dd = rs.getString("��������");
				if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,10);
				row.createCell(cellIndex++).setCellValue(dd);
			}
			else row.createCell(cellIndex++).setCellValue("-");
			row.createCell(cellIndex++).setCellValue(rs.getString("������������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("��������뿩��"));
			row.createCell(cellIndex++).setCellValue(rs.getString("������������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�������ް��ɿ���"));
			row.createCell(cellIndex++).setCellValue(rs.getString("���弳��ǽÿ���"));
			row.createCell(cellIndex++).setCellValue(rs.getString("���������ǹ�����"));
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue(rs.getString("����"));
			index++;
		}
		
		toFile();
		
		System.out.println("File created.");
	}
	
	public void lhBidToExcel() throws ClassNotFoundException, SQLException, IOException {
		connectDB();
		
		adjustColumns();
		
		labelColumns();
		
		String sql = "SELECT * FROM lhbidinfo WHERE ";
		if (sd != null || ed != null || org != null || rate != null || workType != null) {
			if ((sd != null) && (ed != null)) {
				sql += "�����Ͻ� >= \"" + sd + "\" AND �����Ͻ� <= \"" + ed + "\" AND ";
			}
			if (org != null) {
				sql += "�������� = \"" + org + "\" AND ";
			}
			if (workType != null) {
				sql += "����=\"" + workType + "\" AND ";
			}
		}
		if (sd == null && ed == null) {
			sql += "�����Ͻ� >= \"2016-01-01 00:00:00\" AND ";
		}
		sql += "�Ϸ�=1 ";
		
		sql += "UNION SELECT * FROM lhbidinfo WHERE ";
		if (org != null || rate != null || workType != null) {
			if (org != null) {
				sql += "�������� = \"" + org + "\" AND ";
			}
			if (workType != null) {
				sql += "����=\"" + workType + "\" AND ";
			}
		}
		sql += "�����Ͻ� >= \"" + today + "\" ORDER BY �����Ͻ�, �����ȣ;";
		
		rs = st.executeQuery(sql);
		
		int rowIndex = 1;
		int cellIndex = 0;
		int index = 1;
		while(rs.next()) {
			Row row = sheet.createRow(rowIndex++);
			cellIndex = 0;
			row.createCell(cellIndex++).setCellValue(index++);
			row.createCell(cellIndex++).setCellValue(rs.getString("�����ȣ"));
			String od = rs.getString("�����Ͻ�");
			if (!(od == null)){
				od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
			}
			else od = "";
			row.createCell(cellIndex++).setCellValue(od);
			row.createCell(cellIndex++).setCellValue(rs.getString("��������"));
			HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
			basePriceCell.setCellStyle(money);
			basePriceCell.setCellValue(rs.getLong("���ʱݾ�"));
			HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
			expectedPriceCell.setCellStyle(money);
			expectedPriceCell.setCellValue(rs.getLong("�����ݾ�"));
			HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
			bidPriceCell.setCellStyle(money);
			bidPriceCell.setCellValue(rs.getLong("�����ݾ�"));
			HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell1.setCellStyle(money);
			dupPriceCell1.setCellValue(rs.getLong("����1"));
			HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell2.setCellStyle(money);
			dupPriceCell2.setCellValue(rs.getLong("����2"));
			HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell3.setCellStyle(money);
			dupPriceCell3.setCellValue(rs.getLong("����3"));
			HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell4.setCellStyle(money);
			dupPriceCell4.setCellValue(rs.getLong("����4"));
			HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell5.setCellStyle(money);
			dupPriceCell5.setCellValue(rs.getLong("����5"));
			HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell6.setCellStyle(money);
			dupPriceCell6.setCellValue(rs.getLong("����6"));
			HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell7.setCellStyle(money);
			dupPriceCell7.setCellValue(rs.getLong("����7"));
			HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell8.setCellStyle(money);
			dupPriceCell8.setCellValue(rs.getLong("����8"));
			HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell9.setCellStyle(money);
			dupPriceCell9.setCellValue(rs.getLong("����9"));
			HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell10.setCellStyle(money);
			dupPriceCell10.setCellValue(rs.getLong("����10"));
			HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell11.setCellStyle(money);
			dupPriceCell11.setCellValue(rs.getLong("����11"));
			HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell12.setCellStyle(money);
			dupPriceCell12.setCellValue(rs.getLong("����12"));
			HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell13.setCellStyle(money);
			dupPriceCell13.setCellValue(rs.getLong("����13"));
			HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell14.setCellStyle(money);
			dupPriceCell14.setCellValue(rs.getLong("����14"));
			HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell15.setCellStyle(money);
			dupPriceCell15.setCellValue(rs.getLong("����15"));
			HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell1.setCellStyle(money);
			dupComCell1.setCellValue(rs.getLong("����1"));
			HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell2.setCellStyle(money);
			dupComCell2.setCellValue(rs.getLong("����2"));
			HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell3.setCellStyle(money);
			dupComCell3.setCellValue(rs.getLong("����3"));
			HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell4.setCellStyle(money);
			dupComCell4.setCellValue(rs.getLong("����4"));
			HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell5.setCellStyle(money);
			dupComCell5.setCellValue(rs.getLong("����5"));
			HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell6.setCellStyle(money);
			dupComCell6.setCellValue(rs.getLong("����6"));
			HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell7.setCellStyle(money);
			dupComCell7.setCellValue(rs.getLong("����7"));
			HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell8.setCellStyle(money);
			dupComCell8.setCellValue(rs.getLong("����8"));
			HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell9.setCellStyle(money);
			dupComCell9.setCellValue(rs.getLong("����9"));
			HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell10.setCellStyle(money);
			dupComCell10.setCellValue(rs.getLong("����10"));
			HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell11.setCellStyle(money);
			dupComCell11.setCellValue(rs.getLong("����11"));
			HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell12.setCellStyle(money);
			dupComCell12.setCellValue(rs.getLong("����12"));
			HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell13.setCellStyle(money);
			dupComCell13.setCellValue(rs.getLong("����13"));
			HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell14.setCellStyle(money);
			dupComCell14.setCellValue(rs.getLong("����14"));
			HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell15.setCellStyle(money);
			dupComCell15.setCellValue(rs.getLong("����15"));
			row.createCell(cellIndex++).setCellValue(rs.getInt("������"));
			if (rs.getString("������������") != null) {
				String dd = rs.getString("������������");
				if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
				row.createCell(cellIndex++).setCellValue(dd);
			}
			else row.createCell(cellIndex++).setCellValue("-");
			row.createCell(cellIndex++).setCellValue(rs.getString("�����"));
			row.createCell(cellIndex++).setCellValue(rs.getString("��������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�����ڼ������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("������"));
			HSSFCell chosenPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
			chosenPriceCell1.setCellStyle(money);
			chosenPriceCell1.setCellValue(rs.getLong("���ð���1"));
			HSSFCell chosenPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
			chosenPriceCell2.setCellStyle(money);
			chosenPriceCell2.setCellValue(rs.getLong("���ð���2"));
			HSSFCell chosenPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
			chosenPriceCell3.setCellStyle(money);
			chosenPriceCell3.setCellValue(rs.getLong("���ð���3"));
			HSSFCell chosenPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
			chosenPriceCell4.setCellStyle(money);
			chosenPriceCell4.setCellValue(rs.getLong("���ð���4"));
			HSSFCell sitePriceCell = (HSSFCell) row.createCell(cellIndex++);
			sitePriceCell.setCellStyle(money);
			sitePriceCell.setCellValue(rs.getLong("������������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("��������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�з�"));
			row.createCell(cellIndex++).setCellValue(rs.getString("��������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("����"));
		}
		
		toFile();
		
		System.out.println("File created.");
	}
	
	public void letsrunBidToExcel() throws ClassNotFoundException, SQLException, IOException {
		connectDB();
		
		adjustColumns();
		
		labelColumns();
		
		String sql = "SELECT * FROM letsrunbidinfo WHERE ";
		if (sd != null || ed != null || org != null || rate != null || workType != null) {
			if ((sd != null) && (ed != null)) {
				sql += "�����Ͻ� >= \"" + sd + "\" AND �����Ͻ� <= \"" + ed + "\" AND ";
			}
			if (org != null) {
				sql += "�����=\"" + org + "\" AND ";
			}
			if (workType != null) {
				sql += "��������=\"" + workType + "\" AND ";
			}
		}
		sql += "�Ϸ�=1 ";
		
		sql += "UNION SELECT * FROM letsrunbidinfo WHERE ";
		if (org != null || rate != null || workType != null) {
			if (org != null) {
				sql += "�����=\"" + org + "\" AND ";
			}
			if (workType != null) {
				sql += "��������=\"" + workType + "\" AND ";
			}
		}
		sql += "�����Ͻ� >= \"" + today + "\" ORDER BY �����Ͻ�, �����ȣ;";
		rs = st.executeQuery(sql);
		
		int rowIndex = 1;
		int cellIndex = 0;
		int index = 1;
		while(rs.next()) {
			Row row = sheet.createRow(rowIndex++);
			cellIndex = 0;
			row.createCell(cellIndex++).setCellValue(index);
			row.createCell(cellIndex++).setCellValue(rs.getString("�����ȣ"));
			String od = rs.getString("�����Ͻ�");
			od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
			row.createCell(cellIndex++).setCellValue(od);
			row.createCell(cellIndex++).setCellValue(rs.getString("��������"));
			HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
			basePriceCell.setCellStyle(money);
			basePriceCell.setCellValue(rs.getLong("���񰡰ݱ��ʱݾ�"));
			HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
			expectedPriceCell.setCellStyle(money);
			expectedPriceCell.setCellValue(rs.getLong("��������"));
			HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
			bidPriceCell.setCellStyle(money);
			bidPriceCell.setCellValue(rs.getLong("�����ݾ�"));
			HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell1.setCellStyle(money);
			dupPriceCell1.setCellValue(rs.getLong("����1"));
			HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell2.setCellStyle(money);
			dupPriceCell2.setCellValue(rs.getLong("����2"));
			HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell3.setCellStyle(money);
			dupPriceCell3.setCellValue(rs.getLong("����3"));
			HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell4.setCellStyle(money);
			dupPriceCell4.setCellValue(rs.getLong("����4"));
			HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell5.setCellStyle(money);
			dupPriceCell5.setCellValue(rs.getLong("����5"));
			HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell6.setCellStyle(money);
			dupPriceCell6.setCellValue(rs.getLong("����6"));
			HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell7.setCellStyle(money);
			dupPriceCell7.setCellValue(rs.getLong("����7"));
			HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell8.setCellStyle(money);
			dupPriceCell8.setCellValue(rs.getLong("����8"));
			HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell9.setCellStyle(money);
			dupPriceCell9.setCellValue(rs.getLong("����9"));
			HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell10.setCellStyle(money);
			dupPriceCell10.setCellValue(rs.getLong("����10"));
			HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell11.setCellStyle(money);
			dupPriceCell11.setCellValue(rs.getLong("����11"));
			HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell12.setCellStyle(money);
			dupPriceCell12.setCellValue(rs.getLong("����12"));
			HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell13.setCellStyle(money);
			dupPriceCell13.setCellValue(rs.getLong("����13"));
			HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell14.setCellStyle(money);
			dupPriceCell14.setCellValue(rs.getLong("����14"));
			HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell15.setCellStyle(money);
			dupPriceCell15.setCellValue(rs.getLong("����15"));
			HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell1.setCellStyle(money);
			dupComCell1.setCellValue(rs.getLong("����1"));
			HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell2.setCellStyle(money);
			dupComCell2.setCellValue(rs.getLong("����2"));
			HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell3.setCellStyle(money);
			dupComCell3.setCellValue(rs.getLong("����3"));
			HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell4.setCellStyle(money);
			dupComCell4.setCellValue(rs.getLong("����4"));
			HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell5.setCellStyle(money);
			dupComCell5.setCellValue(rs.getLong("����5"));
			HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell6.setCellStyle(money);
			dupComCell6.setCellValue(rs.getLong("����6"));
			HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell7.setCellStyle(money);
			dupComCell7.setCellValue(rs.getLong("����7"));
			HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell8.setCellStyle(money);
			dupComCell8.setCellValue(rs.getLong("����8"));
			HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell9.setCellStyle(money);
			dupComCell9.setCellValue(rs.getLong("����9"));
			HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell10.setCellStyle(money);
			dupComCell10.setCellValue(rs.getLong("����10"));
			HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell11.setCellStyle(money);
			dupComCell11.setCellValue(rs.getLong("����11"));
			HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell12.setCellStyle(money);
			dupComCell12.setCellValue(rs.getLong("����12"));
			HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell13.setCellStyle(money);
			dupComCell13.setCellValue(rs.getLong("����13"));
			HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell14.setCellStyle(money);
			dupComCell14.setCellValue(rs.getLong("����14"));
			HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell15.setCellStyle(money);
			dupComCell15.setCellValue(rs.getLong("����15"));
			row.createCell(cellIndex++).setCellValue(rs.getInt("������"));
			if (rs.getString("��������") != null) {
				String dd = rs.getString("��������");
				if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
				row.createCell(cellIndex++).setCellValue(dd);
			}
			else row.createCell(cellIndex++).setCellValue("-");
			row.createCell(cellIndex++).setCellValue(rs.getString("�������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("��������"));
			HSSFCell minPriceCell = (HSSFCell) row.createCell(cellIndex++);
			minPriceCell.setCellStyle(money);
			minPriceCell.setCellValue(rs.getLong("�������ѱݾ�"));
			row.createCell(cellIndex++).setCellValue(rs.getString("����������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�����"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�������ݹ��"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�����ڼ������"));
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue(rs.getString("�������"));
			index++;
		}
		
		toFile();
		
		System.out.println("File created.");
	}
	
	public void dapaNegoToExcel() throws SQLException, ClassNotFoundException, IOException {
		connectDB();
		
		adjustColumns();
		
		labelColumns();
		
		String sql = "SELECT * FROM dapanegoinfo WHERE ";
		if (sd != null || ed != null || org != null || rate != null) {
			if ((sd != null) && (ed != null)) {
				sql += "�����Ͻ� >= \"" + sd + "\" AND �����Ͻ� <= \"" + ed + "\" AND ";
			}
			if (org != null) {
				sql += "���ֱ�� = \"" + org + "\" AND ";
			}
		}
		sql += "�Ϸ�=1 ";
		
		sql += "UNION SELECT * FROM dapanegoinfo WHERE ";
		if (org != null || rate != null) {
			if (org != null) {
				sql += "���ֱ�� = \"" + org + "\" AND ";
			}
		}
		sql += "�����Ͻ� >= \"" + today + "\" ORDER BY �����Ͻ�, �����ȣ;";
		
		rs = st.executeQuery(sql);
		
		int rowIndex = 1;
		int cellIndex = 0;
		int index = 1;
		while(rs.next()) {
			Row row = sheet.createRow(rowIndex++);
			cellIndex = 0;
			row.createCell(cellIndex++).setCellValue(index++);
			row.createCell(cellIndex++).setCellValue(rs.getString("�����ȣ") + "-" + rs.getInt("����"));
			String od = rs.getString("�����Ͻ�");
			od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
			row.createCell(cellIndex++).setCellValue(od);
			row.createCell(cellIndex++).setCellValue(rs.getString("�����Ī"));
			HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
			basePriceCell.setCellStyle(money);
			basePriceCell.setCellValue(rs.getLong("���ʿ��񰡰�"));
			HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
			expectedPriceCell.setCellStyle(money);
			expectedPriceCell.setCellValue(rs.getLong("��������"));
			HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
			bidPriceCell.setCellStyle(money);
			bidPriceCell.setCellValue(rs.getLong("�����ݾ�"));
			HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell1.setCellStyle(money);
			dupPriceCell1.setCellValue(rs.getLong("����1"));
			HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell2.setCellStyle(money);
			dupPriceCell2.setCellValue(rs.getLong("����2"));
			HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell3.setCellStyle(money);
			dupPriceCell3.setCellValue(rs.getLong("����3"));
			HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell4.setCellStyle(money);
			dupPriceCell4.setCellValue(rs.getLong("����4"));
			HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell5.setCellStyle(money);
			dupPriceCell5.setCellValue(rs.getLong("����5"));
			HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell6.setCellStyle(money);
			dupPriceCell6.setCellValue(rs.getLong("����6"));
			HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell7.setCellStyle(money);
			dupPriceCell7.setCellValue(rs.getLong("����7"));
			HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell8.setCellStyle(money);
			dupPriceCell8.setCellValue(rs.getLong("����8"));
			HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell9.setCellStyle(money);
			dupPriceCell9.setCellValue(rs.getLong("����9"));
			HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell10.setCellStyle(money);
			dupPriceCell10.setCellValue(rs.getLong("����10"));
			HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell11.setCellStyle(money);
			dupPriceCell11.setCellValue(rs.getLong("����11"));
			HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell12.setCellStyle(money);
			dupPriceCell12.setCellValue(rs.getLong("����12"));
			HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell13.setCellStyle(money);
			dupPriceCell13.setCellValue(rs.getLong("����13"));
			HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell14.setCellStyle(money);
			dupPriceCell14.setCellValue(rs.getLong("����14"));
			HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell15.setCellStyle(money);
			dupPriceCell15.setCellValue(rs.getLong("����15"));
			HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell1.setCellStyle(money);
			dupComCell1.setCellValue(rs.getLong("����1"));
			HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell2.setCellStyle(money);
			dupComCell2.setCellValue(rs.getLong("����2"));
			HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell3.setCellStyle(money);
			dupComCell3.setCellValue(rs.getLong("����3"));
			HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell4.setCellStyle(money);
			dupComCell4.setCellValue(rs.getLong("����4"));
			HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell5.setCellStyle(money);
			dupComCell5.setCellValue(rs.getLong("����5"));
			HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell6.setCellStyle(money);
			dupComCell6.setCellValue(rs.getLong("����6"));
			HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell7.setCellStyle(money);
			dupComCell7.setCellValue(rs.getLong("����7"));
			HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell8.setCellStyle(money);
			dupComCell8.setCellValue(rs.getLong("����8"));
			HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell9.setCellStyle(money);
			dupComCell9.setCellValue(rs.getLong("����9"));
			HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell10.setCellStyle(money);
			dupComCell10.setCellValue(rs.getLong("����10"));
			HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell11.setCellStyle(money);
			dupComCell11.setCellValue(rs.getLong("����11"));
			HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell12.setCellStyle(money);
			dupComCell12.setCellValue(rs.getLong("����12"));
			HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell13.setCellStyle(money);
			dupComCell13.setCellValue(rs.getLong("����13"));
			HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell14.setCellStyle(money);
			dupComCell14.setCellValue(rs.getLong("����14"));
			HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell15.setCellStyle(money);
			dupComCell15.setCellValue(rs.getLong("����15"));
			row.createCell(cellIndex++).setCellValue(rs.getInt("������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("���ֱ��"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�����ڰ������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("���ʿ������뿩��"));
			if (rs.getString("���������⸶���Ͻ�") != null) {
				String dd = rs.getString("���������⸶���Ͻ�");
				if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
				row.createCell(cellIndex++).setCellValue(dd);
			}
			else row.createCell(cellIndex++).setCellValue("-");
			row.createCell(cellIndex++).setCellValue(rs.getString("��������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�������"));
			String lbound = rs.getString("����������");
			lbound = lbound.replaceAll("[^\\d.]", "");
			if (!lbound.equals("")) lbound += "%";
			row.createCell(cellIndex++).setCellValue(lbound);
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue(rs.getString("����") + " ~ " + rs.getString("����"));
		}
		
		toFile();
		
		System.out.println("File created.");
	}
	
	public void dapaBidToExcel() throws ClassNotFoundException, SQLException, IOException {
		connectDB();
		
		adjustColumns();
		
		labelColumns();
		
		String sql = "SELECT * FROM dapabidinfo WHERE ";
		if (sd != null || ed != null || org != null || rate != null) {
			if ((sd != null) && (ed != null)) {
				sql += "�����Ͻ� >= \"" + sd + "\" AND �����Ͻ� <= \"" + ed + "\" AND ";
			}
			if (org != null) {
				sql += "���ֱ�� = \"" + org + "\" AND ";
			}
		}
		sql += "�Ϸ�=1 ";
		
		// Add unopened bids
		sql += "UNION SELECT * FROM dapabidinfo WHERE ";
		if (org != null || rate != null) {
			if (org != null) {
				sql += "���ֱ�� = \"" + org + "\" AND ";
			}
		}
		sql += "�����Ͻ� >=\"" + today + "\" ORDER BY �����Ͻ�, �����ȣ";
		
		rs = st.executeQuery(sql);
		
		int rowIndex = 1;
		int cellIndex = 0;
		int index = 1;
		while(rs.next()) {
			Row row = sheet.createRow(rowIndex++);
			cellIndex = 0;
			row.createCell(cellIndex++).setCellValue(index++);
			row.createCell(cellIndex++).setCellValue(rs.getString("�����ȣ") + "-" + rs.getInt("����"));
			String od = rs.getString("�����Ͻ�");
			od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
			row.createCell(cellIndex++).setCellValue(od);
			row.createCell(cellIndex++).setCellValue(rs.getString("�����Ī"));
			HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
			basePriceCell.setCellStyle(money);
			basePriceCell.setCellValue(rs.getLong("���ʿ��񰡰�"));
			HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
			expectedPriceCell.setCellStyle(money);
			expectedPriceCell.setCellValue(rs.getLong("��������"));
			HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
			bidPriceCell.setCellStyle(money);
			bidPriceCell.setCellValue(rs.getLong("�����ݾ�"));
			HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell1.setCellStyle(money);
			dupPriceCell1.setCellValue(rs.getLong("����1"));
			HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell2.setCellStyle(money);
			dupPriceCell2.setCellValue(rs.getLong("����2"));
			HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell3.setCellStyle(money);
			dupPriceCell3.setCellValue(rs.getLong("����3"));
			HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell4.setCellStyle(money);
			dupPriceCell4.setCellValue(rs.getLong("����4"));
			HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell5.setCellStyle(money);
			dupPriceCell5.setCellValue(rs.getLong("����5"));
			HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell6.setCellStyle(money);
			dupPriceCell6.setCellValue(rs.getLong("����6"));
			HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell7.setCellStyle(money);
			dupPriceCell7.setCellValue(rs.getLong("����7"));
			HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell8.setCellStyle(money);
			dupPriceCell8.setCellValue(rs.getLong("����8"));
			HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell9.setCellStyle(money);
			dupPriceCell9.setCellValue(rs.getLong("����9"));
			HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell10.setCellStyle(money);
			dupPriceCell10.setCellValue(rs.getLong("����10"));
			HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell11.setCellStyle(money);
			dupPriceCell11.setCellValue(rs.getLong("����11"));
			HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell12.setCellStyle(money);
			dupPriceCell12.setCellValue(rs.getLong("����12"));
			HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell13.setCellStyle(money);
			dupPriceCell13.setCellValue(rs.getLong("����13"));
			HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell14.setCellStyle(money);
			dupPriceCell14.setCellValue(rs.getLong("����14"));
			HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupPriceCell15.setCellStyle(money);
			dupPriceCell15.setCellValue(rs.getLong("����15"));
			HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell1.setCellStyle(money);
			dupComCell1.setCellValue(rs.getLong("����1"));
			HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell2.setCellStyle(money);
			dupComCell2.setCellValue(rs.getLong("����2"));
			HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell3.setCellStyle(money);
			dupComCell3.setCellValue(rs.getLong("����3"));
			HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell4.setCellStyle(money);
			dupComCell4.setCellValue(rs.getLong("����4"));
			HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell5.setCellStyle(money);
			dupComCell5.setCellValue(rs.getLong("����5"));
			HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell6.setCellStyle(money);
			dupComCell6.setCellValue(rs.getLong("����6"));
			HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell7.setCellStyle(money);
			dupComCell7.setCellValue(rs.getLong("����7"));
			HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell8.setCellStyle(money);
			dupComCell8.setCellValue(rs.getLong("����8"));
			HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell9.setCellStyle(money);
			dupComCell9.setCellValue(rs.getLong("����9"));
			HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell10.setCellStyle(money);
			dupComCell10.setCellValue(rs.getLong("����10"));
			HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell11.setCellStyle(money);
			dupComCell11.setCellValue(rs.getLong("����11"));
			HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell12.setCellStyle(money);
			dupComCell12.setCellValue(rs.getLong("����12"));
			HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell13.setCellStyle(money);
			dupComCell13.setCellValue(rs.getLong("����13"));
			HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell14.setCellStyle(money);
			dupComCell14.setCellValue(rs.getLong("����14"));
			HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
			dupComCell15.setCellStyle(money);
			dupComCell15.setCellValue(rs.getLong("����15"));
			row.createCell(cellIndex++).setCellValue(rs.getInt("������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("���ֱ��"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�����"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�������"));
			row.createCell(cellIndex++).setCellValue(rs.getString("���ʿ������뿩��"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�����ɻ�"));
			row.createCell(cellIndex++).setCellValue(rs.getString("�����ڰ������"));
			if (rs.getString("���������⸶���Ͻ�") != null) {
				String dd = rs.getString("���������⸶���Ͻ�");
				if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
				row.createCell(cellIndex++).setCellValue(dd);
			}
			else row.createCell(cellIndex++).setCellValue("-");
			String lbound = rs.getString("����������");
			lbound = lbound.replaceAll("[^\\d.]", "");
			if (!lbound.equals("")) lbound += "%";
			else lbound = "-";
			row.createCell(cellIndex++).setCellValue(lbound);
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			row.createCell(cellIndex++).setCellValue("");
			String rate = rs.getString("������");
			if (rate.equals("-")) {
				rate = rs.getString("����") + " ~ " + rs.getString("����");
			}
			row.createCell(cellIndex++).setCellValue(rate);
		}
		
		toFile();
		
		System.out.println("File created.");
	}
}
