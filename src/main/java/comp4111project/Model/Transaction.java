package comp4111project.Model;

import java.util.Vector;

import comp4111project.QueryManager;

public class Transaction {
	private int id;
	Vector<Action> actions;
	
	public Transaction(int id) {
		this.id = id;
		actions = new Vector<Action>();
	}
	
	/**
	 * This function add an action to a transaction
	 * @param action
	 * @return -1 if invalid action, 1 if valid action
	 */
	public int addAction(Action action) {
		int bookStatus = QueryManager.getInstance().bookStatus(action.getBookID());
		if((bookStatus == 1 && action.getName() == "loan") || (bookStatus == 0 && action.getName()=="return")) {
			actions.add(action);
			return 1;
		}
		return -1;
	}
	
	/**
	 * This function executes all actions within one transaction
	 * @return 1: successful execution for all action
	 * @return -1: unsuccessful execution for at least 1 action
	 */
	public int executeAction() {
		int result;
		for(Action a : actions) {
			Boolean isReturn = (a.getName() == "return") ? true : false; 
			result = QueryManager.getInstance().returnAndLoanBook(Integer.toString(a.getBookID()), isReturn);
			
			if(result != 0) return -1;
		}
		return 1;
	}
	
	public int getID() {
		return id;
	}
	
	
}
