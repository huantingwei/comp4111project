package comp4111project.Model;

public class Action {
	private String action;
	private long bookid;
	
	public Action(String action, long bookid) {
		this.action = action;
		this.bookid = bookid;
	}
	
	public String getName() {
		return action;
	}
	public long getBookID() {
		return bookid;
	}
	
	

}
