import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

public class NegoPanel extends JPanel {
		// For SQL setup.
		Connection con;
		java.sql.Statement st;
		ResultSet rs;
		
		JPanel optionPanel;
		JComboBox siteDrop;
		
		JTable data;
		
		ArrayList<NegoSearchPanel> searchPanels;
		
		JPanel bottomPanel;
		JComboBox workDrop;
		JTextField orgInput;
		JButton orgSearch;
		JCheckBox dateCheck;
		DatePicker startDate;
		DatePicker endDate;
		JButton searchButton;
		JButton excelButton;
		
		public NegoPanel() {
			super();
			
			this.setLayout(new BorderLayout());
			
			searchPanels = new ArrayList<NegoSearchPanel>(10);
			
			optionPanel = new JPanel();
			siteDrop = new JComboBox(Resources.SITES);
			siteDrop.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String site = siteDrop.getSelectedItem().toString();
					for (int i = 0; i < 10; i++) {
						searchPanels.get(i).changeWork(site);
					}
				}
			});
			optionPanel.add(new JLabel("����Ʈ : "));
			optionPanel.add(siteDrop);
			
			DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
			rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
			data = new JTable(new DefaultTableModel(Resources.COLUMNS, 0));
			for (int i = 0; i < data.getColumnCount(); i++) {
				data.getColumn(Resources.COLUMNS[i]).setCellRenderer(rightRender);
			}
			data.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
			        data.scrollRectToVisible(data.getCellRect(data.getRowCount() - 1, 0, true));
			    }
			});
			data.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
			data.setIntercellSpacing(new Dimension(1, 1));
			
			JScrollPane scroll = new JScrollPane(data);
			
			bottomPanel = new JPanel();
			bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
			for (int i = 0; i < 10; i++) {
				NegoSearchPanel nsp = new NegoSearchPanel();
				bottomPanel.add(nsp);
				searchPanels.add(nsp);
			}
			
			JScrollPane bottomScroll = new JScrollPane(bottomPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			bottomScroll.setPreferredSize(new Dimension(this.getWidth(), 400));
			
			this.add(optionPanel, BorderLayout.NORTH);
			this.add(scroll, BorderLayout.CENTER);
			this.add(bottomScroll, BorderLayout.SOUTH);
		}
		
		public void adjustColumns() {
			final TableColumnModel columnModel = data.getColumnModel();
			columnModel.getColumn(0).setPreferredWidth(50);
			columnModel.getColumn(1).setPreferredWidth(130);
			columnModel.getColumn(2).setPreferredWidth(100);
			columnModel.getColumn(3).setPreferredWidth(70);
			for (int i = 4; i < 9; i++) {
				int width = 50;
				for (int j = 0; j < data.getRowCount(); j++) {
					TableCellRenderer renderer = data.getCellRenderer(j, i);
					Component comp = data.prepareRenderer(renderer, j, i);
					width = Math.max(comp.getPreferredSize().width + 1, width);
				}
				columnModel.getColumn(i).setPreferredWidth(width);
			}
			columnModel.getColumn(9).setPreferredWidth(50);
			columnModel.getColumn(10).setPreferredWidth(100); // �����Ͻ�(����)
			columnModel.getColumn(12).setPreferredWidth(70); // ������
			columnModel.getColumn(13).setPreferredWidth(70); // �����
			columnModel.getColumn(14).setPreferredWidth(70); // ��ȸ��
			columnModel.getColumn(15).setPreferredWidth(130);
			columnModel.getColumn(16).setPreferredWidth(130);
			columnModel.getColumn(17).setPreferredWidth(70);
			columnModel.getColumn(18).setPreferredWidth(80); // �����
		}
		
		private class NegoSearchPanel extends JPanel {
			JComboBox workDrop;
			JTextField orgInput;
			JButton orgSearch;
			JCheckBox dateCheck;
			DatePicker startDate;
			DatePicker endDate;
			JButton searchButton;
			JButton excelButton;
			
			public NegoSearchPanel() {
				super();
				workDrop = new JComboBox(Resources.NARA_WORKS);
				orgInput = new JTextField(15);
				orgSearch = new JButton("�˻�");
				orgSearch.addActionListener(new OrgListener());
				dateCheck = new JCheckBox();
				startDate = new JDatePicker(Calendar.getInstance().getTime());
				startDate.setTextEditable(true);
				endDate = new JDatePicker(Calendar.getInstance().getTime());
				endDate.setTextEditable(true);
				searchButton = new JButton("�˻�");
				searchButton.addActionListener(new NegoSearchListener());
				excelButton = new JButton("��������");
				excelButton.addActionListener(new NegoExcelListener());
				
				this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
				this.add(new JLabel("���� : "));
				this.add(workDrop);
				JLabel o = new JLabel("���ֱ�� : ");
				o.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
				this.add(o);
				this.add(orgInput);
				this.add(orgSearch);
				JLabel d = new JLabel("�����Ͻ� ");
				d.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
				this.add(d);
				this.add(dateCheck);
				this.add((JComponent) startDate);
				this.add(new JLabel(" ~ "));
				this.add((JComponent) endDate);
				this.add(searchButton);
				this.add(excelButton);
			}
			
			public void changeWork(String site) {
				workDrop.removeAllItems();
				if (site.equals("��������")) {
					DefaultComboBoxModel model = new DefaultComboBoxModel(Resources.NARA_WORKS);
					workDrop.setModel(model);
				}
				else if (site.equals("LH����")) {
					DefaultComboBoxModel model = new DefaultComboBoxModel(Resources.LH_WORKS);
					workDrop.setModel(model);
				}
				else if (site.equals("�ѱ�����ȸ")) {
					DefaultComboBoxModel model = new DefaultComboBoxModel(Resources.LETS_WORKS);
					workDrop.setModel(model);
				}
				else if (site.equals("���ΰ���")) {
					DefaultComboBoxModel model = new DefaultComboBoxModel(Resources.EX_WORKS);
					workDrop.setModel(model);
				}
			}
			
			private class OrgListener implements ActionListener {
				public void actionPerformed(ActionEvent e) {
					String site = siteDrop.getSelectedItem().toString();
					try {
						OrgFrame o = new OrgFrame(orgInput, site, false);
					} catch (ClassNotFoundException | SQLException e1) {
						Logger.getGlobal().log(Level.WARNING, e1.getMessage(), e1);
						e1.printStackTrace();
					}
				}
			}
			
			private class NegoSearchListener implements ActionListener {
				public void actionPerformed(ActionEvent e) {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					String sd = null;
					String ed = null;
					String today = sdf.format(new Date()) + " 00:00:00";
					
					if (dateCheck.isSelected()) {
						if ((startDate.getModel().getValue() != null) && (endDate.getModel().getValue() != null)) {
							sd = sdf.format(startDate.getModel().getValue());
							ed = sdf.format(endDate.getModel().getValue());
						}
						else {
							JOptionPane.showMessageDialog(null, "��¥�� �ٸ��� �Է����ֽʽÿ�.");
							return;
						}
					}
					
					// Set up SQL connection.
					try {
						Class.forName("com.mysql.jdbc.Driver");
					} catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					}
					Connection con = null;
					try {
						String site = siteDrop.getSelectedItem().toString();
						String workType = "";
						if (!site.equals("��������û")) workType = workDrop.getSelectedItem().toString();
						String org = orgInput.getText();
						String tableName = "";
						
						con = DriverManager.getConnection("jdbc:mysql://localhost/" + Resources.SCHEMA, Resources.DB_ID, Resources.DB_PW);
						st = con.createStatement();
						rs = null;
						
						if (site.equals("��������")) {
							if (workType.equals("��ü")) tableName = "";
							else if (workType.equals("����")) tableName = "narafacilinfo";
							else if (workType.equals("�뿪")) tableName = "naraservinfo";
							else if (workType.equals("��ǰ")) tableName = "naraprodinfo";
							else tableName = "nararestinfo";
						}
						else if (site.equals("LH����")) tableName = "lhbidinfo";
						else if (site.equals("��������û")) tableName = "dapanegoinfo";
						else if (site.equals("�ѱ�����ȸ")) tableName = "letsrunbidinfo";
						else if (site.equals("���ΰ���")) tableName = "exbidinfo";
						
						String sql = "SELECT * FROM " + tableName + " WHERE ";
						if (!(workType.equals("��ü") && site.equals("��������"))) {
							if (!org.equals("")) {
								if (site.equals("��������")) sql += "������=\"" + org + "\" AND ";
								else if (site.equals("LH����")) sql += "��������=\"" + org + "\" AND ";
								else if (site.equals("��������û")) sql += "���ֱ��=\"" + org + "\" AND ";
								else if (site.equals("���ΰ���")) sql += "����=\"" + org + "\" AND ";
								else if (site.equals("�ѱ�����ȸ")) sql += "�����=\"" + org + "\" AND ";
							}
							if (dateCheck.isSelected()) {
								if (site.equals("��������")) sql += "���������Ͻ� >= \"" + sd + "\" AND ���������Ͻ� <= \"" + ed + "\" AND ";
								else sql += "�����Ͻ� >= \"" + sd + "\" AND �����Ͻ� <= \"" + ed + "\" AND ";
							}
							if (!site.equals("��������û") && !site.equals("��������") && !workType.equals("��ü")) {
								if (site.equals("LH����")) sql += "����=\"" + workType + "\" AND ";
								else if (site.equals("���ΰ���")) sql += "�з�=\"" + workType + "\" AND ";
								else if (site.equals("�ѱ�����ȸ")) sql += "��������=\"" + workType + "\" AND ";
							}
							if (site.equals("��������")) sql += "�Ϸ� > 0 ";
							else sql += "�Ϸ� > 0 ";
							
							// Add unopened notis
							sql += "UNION SELECT * FROM " + tableName + " WHERE ";
							if (!org.equals("")) {
								if (site.equals("��������")) sql += "������=\"" + org + "\" AND ";
								else if (site.equals("LH����")) sql += "��������=\"" + org + "\" AND ";
								else if (site.equals("��������û")) sql += "���ֱ��=\"" + org + "\" AND ";
								else if (site.equals("���ΰ���")) sql += "����=\"" + org + "\" AND ";
								else if (site.equals("�ѱ�����ȸ")) sql += "�����=\"" + org + "\" AND ";
							}
							if (!site.equals("��������û") && !site.equals("��������") && !workType.equals("��ü")) {
								if (site.equals("LH����")) sql += "����=\"" + workType + "\" AND ";
								else if (site.equals("���ΰ���")) sql += "�з�=\"" + workType + "\" AND ";
								else if (site.equals("�ѱ�����ȸ")) sql += "��������=\"" + workType + "\" AND ";
							}
							if (site.equals("��������")) sql += "���������Ͻ� >= \"" + today + "\" ORDER BY ���������Ͻ�, �����ȣ����";
							else sql += "�����Ͻ� >= \"" + today + "\" ORDER BY �����Ͻ�, �����ȣ";
						}	
						else {
							String[] tables = { "narafacilinfo", "naraprodinfo", "naraservinfo", "nararestinfo" };
							for (String t : tables) {
								if (!t.equals("narafacilinfo")) sql += "UNION ";
								sql += "SELECT * FROM " + t + " WHERE ";
								if (!org.equals("")) sql += "������=\"" + org + "\" AND ";
								if (dateCheck.isSelected()) sql += "���������Ͻ� >= \"" + sd + "\" AND ���������Ͻ� <= \"" + ed + "\" AND ";
								sql += "�Ϸ� > 0 ";
								
								sql += "UNION SELECT * FROM " + t + " WHERE ";
								if (!org.equals("")) sql += "������=\"" + org + "\" AND ";
								sql += "���������Ͻ� >= \"" + today + "\" ";
							}
							sql += "ORDER BY ���������Ͻ�, �����ȣ����";
						}
						
						rs = st.executeQuery(sql);
						
						DefaultTableModel m = (DefaultTableModel) data.getModel();
						m.setRowCount(0);
						int index = 1;
						while(rs.next()) {
							String bidno = "";
							if (site.equals("��������")) bidno = rs.getString("�����ȣ����");
							else bidno = rs.getString("�����ȣ");
							
							String date = ""; 
							if (site.equals("��������")) date = rs.getString("���������Ͻ�");
							else date = rs.getString("�����Ͻ�");
							if (date.length() == 21) {
								date = date.substring(2, 4) + date.substring(5, 7) + date.substring(8, 10) + " " + date.substring(11, 16);
							}
							
							String limit = "-";
							if (site.equals("��������û")) limit = rs.getString("�����Ī");
							else if (site.equals("��������") || site.equals("���ΰ���")) limit = rs.getString("�������ѻ���");
							
							String bPrice = "";
							if (site.equals("LH����") || site.equals("��������")) bPrice = rs.getString("���ʱݾ�");
							else if (site.equals("��������û")) bPrice = rs.getString("���ʿ��񰡰�");
							else if (site.equals("���ΰ���")) bPrice = rs.getString("����ݾ�");
							else if (site.equals("�ѱ�����ȸ")) bPrice = rs.getString("���񰡰ݱ��ʱݾ�");
							if (bPrice == null) bPrice = "";
							if (!bPrice.equals("") && !(bPrice.equals("0") || bPrice.equals("0.00"))) {
								double amount = Double.parseDouble(bPrice);
								DecimalFormat formatter = new DecimalFormat("#,###");
								bPrice = formatter.format(amount);
							}
							else bPrice = "-";
							
							String ePrice = ""; 
							if (site.equals("LH����")) ePrice = rs.getString("�����ݾ�");
							else ePrice = rs.getString("��������");
							if (ePrice == null) ePrice = "";
							if (!ePrice.equals("") && !(ePrice.equals("0") || ePrice.equals("0.00"))) {
								double amount = Double.parseDouble(ePrice);
								DecimalFormat formatter = new DecimalFormat("#,###");
								ePrice = formatter.format(amount);
							}
							else ePrice = "-";
							
							String tPrice = rs.getString("�����ݾ�");
							if (tPrice == null) tPrice = "";
							if (!tPrice.equals("") && !(tPrice.equals("0") || tPrice.equals("0.00"))) {
								double amount = Double.parseDouble(tPrice);
								DecimalFormat formatter = new DecimalFormat("#,###");
								tPrice = formatter.format(amount);
							}
							else tPrice = "-";
							
							String dPrice1 = rs.getString("����1");
							if (dPrice1 == null) dPrice1 = "";
							if (!dPrice1.equals("") && !(dPrice1.equals("0") || dPrice1.equals("0.00"))) {
								double amount = Double.parseDouble(dPrice1);
								DecimalFormat formatter = new DecimalFormat("#,###");
								dPrice1 = formatter.format(amount);
							}
							else dPrice1 = "-";
							
							String dPrice2 = rs.getString("����15");
							if (dPrice2 == null) dPrice2 = "";
							if (!dPrice2.equals("") && !(dPrice2.equals("0") || dPrice2.equals("0.00"))) {
								double amount = Double.parseDouble(dPrice2);
								DecimalFormat formatter = new DecimalFormat("#,###");
								dPrice2 = formatter.format(amount);
							}
							else dPrice2 = "-";
							
							String comp = "";
							if (site.equals("LH����") || site.equals("���ΰ���")) comp = rs.getString("������");
							else if (site.equals("��������û") || site.equals("�ѱ�����ȸ") || site.equals("��������")) comp = rs.getString("������");
							if (comp == null) comp = "";
							if (!comp.equals("") && !(comp.equals("0") || comp.equals("0.00"))) {
								double amount = Double.parseDouble(comp);
								DecimalFormat formatter = new DecimalFormat("#,###");
								comp = formatter.format(amount);
							}
							else comp = "-";
							
							String eDate = "";
							if (site.equals("��������")) eDate = rs.getString("���������Ͻ�");
							else eDate = rs.getString("�����Ͻ�");
							if (eDate != null) {
								if (eDate.length() == 21) {
									eDate = eDate.substring(2, 4) + eDate.substring(5, 7) + eDate.substring(8, 10) + " " + eDate.substring(11, 16);
								}
							}
							
							String prog = "";
							if (site.equals("��������")) prog = rs.getString("�����Ȳ");
							else if (site.equals("LH����")) prog = rs.getString("��������");
							else if (site.equals("�ѱ�����ȸ")) prog = rs.getString("��������");
							else if (site.equals("���ΰ���")) prog = rs.getString("�������");
							else if (site.equals("��������û")) prog = rs.getString("������");
							
							String rebid = "";
							
							String exec = "";
							if (site.equals("��������")) exec = rs.getString("�����");
							
							String obs = "";
							if (site.equals("��������")) obs = rs.getString("��ȸ��");
							
							String annOrg = "";
							if (site.equals("��������")) annOrg = rs.getString("������");
							else if (site.equals("LH����")) annOrg = rs.getString("��������");
							else if (site.equals("�ѱ�����ȸ")) annOrg = rs.getString("�����");
							else if (site.equals("��������û")) annOrg = rs.getString("���ֱ��");
							else if (site.equals("���ΰ���")) annOrg = rs.getString("����");
							
							String demOrg = "";
							if (site.equals("��������")) demOrg = rs.getString("������");
							else if (site.equals("LH����")) demOrg = rs.getString("��������");
							else if (site.equals("�ѱ�����ȸ")) demOrg = rs.getString("�����");
							else if (site.equals("��������û")) demOrg = rs.getString("���ֱ��");
							else if (site.equals("���ΰ���")) demOrg = rs.getString("����");
							
							String bidType = "";
							if (site.equals("��������") || site.equals("�ѱ�����ȸ") || site.equals("LH����")) bidType = rs.getString("�������");
							else if (site.equals("��������û")) bidType = rs.getString("�������");
							
							String compType = rs.getString("�����");
							
							String p = "0";
							m.addRow(new Object[] { index, bidno, date, limit, bPrice, ePrice, tPrice, dPrice1, dPrice2, 
									comp, eDate, prog, rebid, exec, obs, annOrg, demOrg, bidType, compType, p });
							index++;
						}
						
						adjustColumns();
						con.close();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				}
			}
		
			private class NegoExcelListener implements ActionListener {
				public void actionPerformed(ActionEvent e) {
					String sd = null;
					String ed = null;
					
					if (dateCheck.isSelected()) {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
						
						sd = sdf.format(startDate.getModel().getValue());
						ed = sdf.format(endDate.getModel().getValue());
					}
					
					try {
						String site = siteDrop.getSelectedItem().toString();
						String org = orgInput.getText().equals("") ? null : orgInput.getText();
						String workType = null;
						if (!site.equals("��������û")) {
							if (!workDrop.getSelectedItem().toString().equals("��ü")) {
								workType = workDrop.getSelectedItem().toString();
							}
						}
						
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
						String curDate = sdf.format(Calendar.getInstance().getTime());
						ExcelWriter ew = new ExcelWriter(site, "����");
						if (dateCheck.isSelected()) ew.setOptions(sd, ed, org, null, workType);
						else ew.setOptions(null, null, org, null, workType);
						ew.toExcel();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}
