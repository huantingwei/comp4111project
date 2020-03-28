package comp4111project.Model;

public class Action {
	private String action;
	private int bookid;
	
	public Action(String action, int bookid) {
		this.action = action;
		this.bookid = bookid;
	}
	
	public String getName() {
		return action;
	}
	public int getBookID() {
		return bookid;
	}
	
	

}
