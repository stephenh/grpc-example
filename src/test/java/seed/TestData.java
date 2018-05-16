package seed;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public class TestData {

  /** Creates a new test database. */
  public static Jdbi newDb() {
    // Use an in-memory database, but DB_CLOSE_DELAY tells it to keep tables between connections
    Jdbi db = Jdbi.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
    db.installPlugin(new SqlObjectPlugin());
    // Currently just re-issue all of the DML each time. Might eventually be cheaper
    // to issue DELETE TABLEs between tests. Not sure.
    //
    // In production would prefer migrations, e.g.
    // https://github.com/stephenh/joist/blob/master/features/src/migrations/java/features/migrations/m0001.java
    db.useExtension(AccountDao.class,  dao -> dao.createTable());

    return db;
  }

  public static Account.Builder newAccount() {
    return Account.newBuilder().setName("bob").setAddress("123 Road").setSsn("111-111-1111");
  }

}
