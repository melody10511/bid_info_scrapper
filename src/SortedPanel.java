import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.ResultSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class SortedPanel extends JPanel {
		// For SQL setup.
		Connection con;
		java.sql.Statement st;
		ResultSet rs;
		
		String[] works = { "공사", "용역" };
		
		JPanel optionPanel;
		JComboBox siteDrop;
		
		JTable data;
		
		JPanel bottomPanel;
		JComboBox workDrop;
		JCheckBox twoCheck;
		JCheckBox threeCheck;
		JCheckBox fourCheck;
		JTextArea quant;
		JButton searchButton;
		JButton excelButton;
		
		public SortedPanel() {
			super();
			
			this.setLayout(new BorderLayout());
			
			optionPanel = new JPanel();
			siteDrop = new JComboBox(Resources.SITES);
			optionPanel.add(new JLabel("사이트 : "));
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
			
			JScrollPane scroll = new JScrollPane(data);
			
			bottomPanel = new JPanel();
			workDrop = new JComboBox(works);
			twoCheck = new JCheckBox("2.00");
			threeCheck = new JCheckBox("3.00");
			fourCheck = new JCheckBox("4.00");
			quant = new JTextArea(1, 5);
			quant.setText("10000");
			searchButton = new JButton("검색");
			searchButton.addActionListener(new SortedSearchListener());
			excelButton = new JButton("엑셀저장");
			excelButton.addActionListener(new SortedExcelListener());
			
			bottomPanel.add(new JLabel("구분 : "));
			bottomPanel.add(workDrop);
			bottomPanel.add(twoCheck);
			bottomPanel.add(threeCheck);
			bottomPanel.add(fourCheck);
			JLabel q = new JLabel("조회건수 : ");
			q.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
			bottomPanel.add(q);
			bottomPanel.add(quant);
			bottomPanel.add(searchButton);
			bottomPanel.add(excelButton);
			bottomPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			
			this.add(optionPanel, BorderLayout.NORTH);
			this.add(scroll, BorderLayout.CENTER);
			this.add(bottomPanel, BorderLayout.SOUTH);
		}
		
		private class SortedSearchListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				
			}
		}
		
		private class SortedExcelListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				
			}
		}
	}
