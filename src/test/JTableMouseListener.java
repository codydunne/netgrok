package test;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import netgrok.view.table.*;


public class JTableMouseListener implements MouseListener {

	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		Object o = e.getSource();
		if(o instanceof JTable)
		{
			JTable t = (JTable) o;
			int [] rows = t.getSelectedRows();
			System.out.print("Rows: ");
			for(int i = 0; i < rows.length; i++)
			{
				System.out.print(""+rows[i	]+",");
			}
			System.out.println("");
			TableModel tm = t.getModel();
			if(tm instanceof TupleTableModel)
			{
				TupleTableModel ttm = (TupleTableModel)tm;
				// TODO: tell ttm that the rows are interesting
			}
		}
	}

}
