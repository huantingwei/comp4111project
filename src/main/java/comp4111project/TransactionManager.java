package comp4111project;

import java.util.Vector;

import comp4111project.Model.Action;
import comp4111project.Model.Transaction;


public class TransactionManager {

	
	private final String RETURN = "return";
	private final String LOAN = "loan";
	
	private final int TX_LIMIT = 100;
	
	Vector<Transaction> transactions;
	int txID;
	
	private TransactionManager() {
		transactions = new Vector<Transaction>();
		txID = 0;
	}
    private static class BillPushSingleton {
        private static final TransactionManager INSTANCE = new TransactionManager();
    }
    
    public static TransactionManager getInstance() {
        return BillPushSingleton.INSTANCE;
    }
    
    
    public int createTx() {
    	if(transactions.size() >= TX_LIMIT) {
    		return -1;
    	}
    	// TODO: what happen if txID overflow?
    	transactions.add(new Transaction(txID++));
    	return txID;
    }
    
    /**
     * This function add an action to the existing transaction
     * @param id
     * @param actionName
     * @param bookID
     * @return -1 if invalid action, 1 if valid action
     */
    public int addActionToTx(int id, String actionName, int bookID) {
    	// validate action type
    	if(actionName != RETURN || actionName != LOAN) {
    		return -1;
    	}
    	else {
    		Transaction tx = transactions.get(id);
    		int result = tx.addAction(new Action(actionName, bookID));
    		return result;
    	}
    }
	/**
	 * This function executes all actions within one transaction
	 * @return 1: successful commit
	 * @return -1: unsuccessful commit
	 */
    public int commitTx(int id) {
    	return transactions.get(id).executeAction();
    }
    /**
     * 
     * @param id
     * @return 1: successful cancellation
     * @return -1: unsuccessful cancellation
     */
    public int cancelTx(int id) {
    	try{
    		transactions.remove(id);
    		return 1;
    	} catch (Exception e){
    		return -1;
    	}
    }
}
