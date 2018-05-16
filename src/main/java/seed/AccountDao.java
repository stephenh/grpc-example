package seed;

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

// I've not used jdbi before, so not sure how this will scale to N entities, but seems nice for now.

public interface AccountDao {

  @SqlUpdate("CREATE TABLE account (id INT AUTO_INCREMENT, status INT, name VARCHAR(200), address VARCHAR(200), ssn VARCHAR(200))")
  void createTable();

  @SqlUpdate("INSERT INTO account (name, status, address, ssn) VALUES (:name, :statusValue, :address, :ssn)")
  @GetGeneratedKeys
  long insert(@BindBean Account user);

  @SqlUpdate("UPDATE account SET name = :name, status = :statusValue, address = :address, ssn = :ssn WHERE id = :id")
  void update(@BindBean Account user);

  @SqlQuery("SELECT * FROM account WHERE id = ?")
  Account read(long id);

  @SqlQuery("SELECT COUNT(*) FROM account")
  long count();

}
