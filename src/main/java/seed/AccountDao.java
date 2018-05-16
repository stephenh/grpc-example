package seed;

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

// I've not used jdbi before, so not sure how this will scale to N entities, but seems nice for now.

public interface AccountDao {

  @SqlUpdate("CREATE TABLE account (id INT AUTO_INCREMENT, name VARCHAR(200), address VARCHAR(200), ssn VARCHAR(200))")
  void createTable();

  @SqlUpdate("INSERT INTO account (name, address, ssn) VALUES (:name, :address, :ssn)")
  void insert(@BindBean Account user);
  
  @SqlQuery("SELECT COUNT(*) FROM account")
  long count();
  

}