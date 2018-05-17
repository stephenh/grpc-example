package seed;

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

// I've not used jdbi before, so not sure how this will scale to N entities, but seems nice for now.

public interface AccountDao {

  @SqlUpdate("CREATE TABLE account (id INT AUTO_INCREMENT, status INT NOT NULL, name VARCHAR(200) NOT NULL, address VARCHAR(200) NOT NULL, ssn VARCHAR(200) NOT NULL)")
  void createTable();

  @SqlUpdate("INSERT INTO account (name, status, address, ssn) VALUES (:name, :statusValue, :address, :ssn)")
  @GetGeneratedKeys
  long insert(@BindBean Account account);

  @SqlUpdate("UPDATE account SET name = :name, status = :statusValue, address = :address, ssn = :ssn WHERE id = :id")
  void update(@BindBean Account account);

  @SqlQuery("SELECT * FROM account WHERE id = ?")
  Account read(long id);

  @SqlQuery("SELECT COUNT(*) FROM account")
  long count();

}
