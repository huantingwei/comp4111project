package comp4111project;

import java.util.concurrent.ConcurrentHashMap;

import comp4111project.Model.Action;
import comp4111project.Model.Transaction;


public class TransactionManager {

	
	private final String RETURN = "return";
	private final String LOAN = "loan";
	
	private final int TX_LIMIT = 100;
	
	ConcurrentHashMap<Integer, Transaction> transactions;
	int txID;
	
	private TransactionManager() {
		transactions = new ConcurrentHashMap<Integer, Transaction>();
		txID = 1;
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
    	transactions.put(txID++, new Transaction(txID));
    	return txID - 1;
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
    	if(!actionName.equals(RETURN) && !actionName.equals(LOAN)) {
    		System.out.println("wrong action name: " + actionName);
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
    	Transaction tx;
    	try {
    		tx = transactions.get(id);
    		tx.executeAction();
    		return 1;
    	} catch (Exception e) {
    		return -1;
    	}
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
