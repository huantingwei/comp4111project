package comp4111project.Model;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import comp4111project.QueryManager;

public class Transaction {
	private int id;
	private ConcurrentHashMap<Integer, Integer> modifiedBooks;
	private Vector<Action> actions;
	
	public Transaction(int id) {
		this.id = id;
		actions = new Vector<Action>();
		modifiedBooks = new ConcurrentHashMap<Integer, Integer>();
	}
	
	/**
	 * This function add an action to a transaction
	 * @param action
	 * @return -1 if invalid action, 1 if valid action
	 */
	public int addAction(Action action) {
		int id = action.getBookID();
		String name = action.getName();
		int bookAvailable;
		int newStatus = -1;
		
		bookAvailable = modifiedBooks.containsKey(id) ? modifiedBooks.get(id) : QueryManager.getInstance().bookAvailable(id);
		
		if((bookAvailable == 1 && name.equals("loan"))) {
			newStatus = 0;
		}
		else if(bookAvailable == 0 && name.equals("return")) {
			newStatus = 1;
		}
		
		if(newStatus == 0 || newStatus == 1) {
			if(modifiedBooks.containsKey(id)) { modifiedBooks.replace(id, newStatus); }
			else { modifiedBooks.put(id, newStatus); }
			return 1;
		}
		else {
			return -1;
		}
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
