package netgrok.view.table;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import netgrok.data.Data;

import prefuse.data.Table;
import prefuse.data.event.TableListener;
import prefuse.util.collections.IntIterator;
// TODO: synchronize with data so that table handles real time packet collection
// TODO: specialize to show a predefined set of attributes
public class TupleTableModel implements MouseListener, TableModel, TableListener {

	Table t;
	Data d;
	Set<TableModelListener> my_listeners;
	
	int [] rowOrder;
	String indexed_column;
	boolean ascending;
	boolean table_changed = false;
	private static final int MAXROWS=100;
	
	private static int min(int a, int b)
	{
		if(a < b)
			return a;
		return b;
	}
	
	public TupleTableModel(Table t, Data d)
	{
		this.t = t;
		this.d = d;
		rowOrder = new int[min(MAXROWS,t.getRowCount())];
		for(int i = 0; i < rowOrder.length; i++)
		{
			rowOrder[i] = i;
		}
		my_listeners = new HashSet<TableModelListener>();
		
		d.addTableListener(this, t);
	}
	
	public TupleTableModel(Table t)
	{
		this(t,Data.getData());
	}
		
	private void reIndex(String column_name)
	{
		d.getLock().lock();
		//System.out.println("re-indexing");
		if(!table_changed && indexed_column == column_name)
		{
			ascending = !ascending;
			//System.out.println("ascending: "+ascending);
		}
		else if(table_changed)
		{
			table_changed = false;
		}
		else
		{
			indexed_column = column_name;
			ascending = true;
		}
		
		if(column_name == null)
		{
			int new_size = min(MAXROWS,t.getRowCount());
			if(rowOrder.length != new_size)
			{
				rowOrder = new int[new_size];
			}
			for(int i = 0; i < rowOrder.length; i++)
			{
				rowOrder[i] = i;
			}
		}
		else
		{
			
			int new_size = min(MAXROWS,t.getRowCount());
			if(rowOrder.length != new_size)
			{
				rowOrder = new int[new_size];
			}
			// TODO: rowsSortedBy doesn't seem to work
			IntIterator ii = t.rowsSortedBy(indexed_column, ascending);
			int i = 0;
			//System.out.print("New row order: ");
			while(ii.hasNext() && i < rowOrder.length)
			{
				assert (i < rowOrder.length);
				int next = ii.nextInt();
				//System.out.print(""+next+",");
				assert (next < rowOrder.length && next >= 0);
				rowOrder[i] = next;
				i++;
			}
			//System.out.println("");
			assert(i == rowOrder.length);
		}
		d.getLock().unlock();
		TableModelEvent e = new TableModelEvent(this);
		notifyListeners(e);
	}
	
	public void addTableModelListener(TableModelListener arg0) {
		my_listeners.add(arg0);
	}
	
	private void notifyListeners(TableModelEvent e)
	{
		Iterator i = my_listeners.iterator();
		while(i.hasNext())
		{
			TableModelListener tml = (TableModelListener)i.next();
			tml.tableChanged(e);
		}
	}

	public Class<?> getColumnClass(int columnIndex) {
		return Object.class;
		// TODO: get the actual class of what's in the column, maybe?
		//Class<?> c = t.getColumnType(columnIndex);
		//if(c == null)
		//{
		//	System.out.println("Found null in column class "+columnIndex);
		//	return String.class;
		//}
		//else
		//	return c;
	}

	public int getColumnCount() {
		return t.getColumnCount();
	}

	public String getColumnName(int arg0) {
		String s = t.getColumnName(arg0);
		if(s == null)
		{
			//System.out.println("Found null column name");
			s = new String("null");
		}
		return s;//t.getColumnName(arg0);
		//return new String("Header "+arg0);
	}

	public int getRowCount() {
		int count = rowOrder.length;
		//System.out.println("Row count: "+count);
		return count;
	}

	public Object getValueAt(int row, int column) {
		int new_row = 0;
		if(row >= rowOrder.length)
		{
			//System.out.println("Row "+row+" not indexed!");
			new_row = row;
		}
		else
		{
			if(ascending)
			{
				new_row = rowOrder[row];
			}
			else
			{
				new_row = rowOrder[rowOrder.length-row-1];
			}
		}
		//System.out.println("Old row: "+row+"  New row: "+new_row);
		Object o = t.get(new_row,column);
		if(o == null)
		{
			//System.out.println("Found null in getValueAt("+row+","+column+")");
			return new String("null");
		}
		else
			return o;
	}

	public boolean isCellEditable(int arg0, int arg1) {
		return t.isCellEditable(arg0, arg1);
	}

	public void removeTableModelListener(TableModelListener arg0) {
		my_listeners.remove(arg0);
	}

	public void setValueAt(Object arg0, int arg1, int arg2) {
		t.set(arg1, arg2, arg0);
	}

	public void mouseClicked(MouseEvent e) {
		Object o = e.getSource();
		if(o instanceof JTableHeader)
		{
			JTableHeader h = (JTableHeader) o;
			int i = h.getColumnModel().getColumnIndexAtX(e.getX());
			String column_name = t.getColumnName(i);
			reIndex(column_name);
		}
	}

	public void mouseEntered(MouseEvent e) {
		
	}

	public void mouseExited(MouseEvent e) {
		
	}

	public void mousePressed(MouseEvent e) {
		
	}

	public void mouseReleased(MouseEvent e) {
		
	}

	public synchronized void tableChanged(Table arg0, int arg1, int arg2, int arg3, int arg4) {
		// TODO Auto-generated method stub
		//System.out.println("Table changed");
		table_changed = true;
		reIndex(indexed_column);
	}

}
