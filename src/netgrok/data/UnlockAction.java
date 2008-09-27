package netgrok.data;

import prefuse.action.Action;

public class UnlockAction extends Action {

	Data d;
	
	public UnlockAction(Data d)
	{
		this.d = d;
	}
	
	@Override
	public void run(double arg0) {
		d.getLock().unlock();
	}

}
