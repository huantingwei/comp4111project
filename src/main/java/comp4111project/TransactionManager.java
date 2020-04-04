package comp4111project;

import java.util.concurrent.ConcurrentHashMap;

import comp4111project.Model.Action;
import comp4111project.Model.Transaction;


public class TransactionManager {

	
	private final String RETURN = "return";
	private final String LOAN = "loan";
	
	private final int TX_LIMIT = 100;
	
	ConcurrentHashMap<Long, Transaction> transactions;
	long txID;
	
	private TransactionManager() {
		transactions = new ConcurrentHashMap<Long, Transaction>();
		txID = 1;
	}
    private static class BillPushSingleton {
        private static final TransactionManager INSTANCE = new TransactionManager();
    }
    
    public static TransactionManager getInstance() {
        return BillPushSingleton.INSTANCE;
    }
    
    /**
     * This method creates a transaction
     * @return transaction id
     */
    public long createTx() {
    	if(transactions.size() >= TX_LIMIT) {
    		return -1;
    	}
    	transactions.put(txID, new Transaction(txID));
    	txID++;
    	return txID - 1;
    }
    
    /**
     * This function add an action to the existing transaction
     * @param id
     * @param actionName
     * @param bookID
     * @return -1 if invalid action, 1 if valid action
     */
    public int addActionToTx(long id, String actionName, long bookID) {
    	// validate action type
    	if(!actionName.equals(RETURN) && !actionName.equals(LOAN)) {
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
    public int commitTx(long id) {
    	Transaction tx;
    	try {
    		tx = transactions.get(id);
    		return tx.executeAction();
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
    public int cancelTx(long id) {
    	try{
    		transactions.remove(id);
    		return 1;
    	} catch (Exception e){
    		return -1;
    	}
    }
}
