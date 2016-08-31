import java.io.IOException;
import java.sql.SQLException;

public abstract class Parser implements Runnable {
	public abstract int getTotal() throws IOException, ClassNotFoundException, SQLException;
	
	public abstract void setDate(String sd, String ed);
	
	public abstract void setOption(String op);

	public abstract int getCur();
}
