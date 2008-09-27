package netgrok.data;

import prefuse.action.Action;

public class LockAction extends Action {

	Data d;
	
	public LockAction(Data d)
	{
		this.d = d;
	}
	
	@Override
	public void run(double arg0) {
		d.getLock().lock();
	}

}
