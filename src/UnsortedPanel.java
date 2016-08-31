import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

public class UnsortedPanel extends JPanel {
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
		JCheckBox dateCheck;
		DatePicker startDate;
		DatePicker endDate;
		JButton searchButton;
		JButton excelButton;
		
		public UnsortedPanel() {
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
			dateCheck = new JCheckBox();
			startDate = new JDatePicker(Calendar.getInstance().getTime());
			startDate.setTextEditable(true);
			endDate = new JDatePicker(Calendar.getInstance().getTime());
			endDate.setTextEditable(true);
			searchButton = new JButton("검색");
			searchButton.addActionListener(new UnsortedSearchListener());
			excelButton = new JButton("엑셀저장");
			excelButton.addActionListener(new UnsortedExcelListener());
			
			bottomPanel.add(new JLabel("구분 : "));
			bottomPanel.add(workDrop);
			JLabel d = new JLabel("개찰일시 ");
			d.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
			bottomPanel.add(d);
			bottomPanel.add(dateCheck);
			bottomPanel.add((JComponent) startDate);
			bottomPanel.add(new JLabel(" ~ "));
			bottomPanel.add((JComponent) endDate);
			bottomPanel.add(searchButton);
			bottomPanel.add(excelButton);
			bottomPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			
			this.add(optionPanel, BorderLayout.NORTH);
			this.add(scroll, BorderLayout.CENTER);
			this.add(bottomPanel, BorderLayout.SOUTH);
		}
		
		private class UnsortedSearchListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				
			}
		}
		
		private class UnsortedExcelListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				
			}
		}
	}