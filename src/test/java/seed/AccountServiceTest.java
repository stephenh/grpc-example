package seed;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static seed.TestData.read;
import static seed.TestData.save;

import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Test;

public class AccountServiceTest {

  private Jdbi db;
  private AccountService server;

  @Before
  public void setup() {
    db = TestData.newDb();
    server = new AccountService(db);
  }

  @Test
  public void shouldAllowCreatingAccounts() {
    // given a request to make an account
    Account.Builder account = TestData.newAccount();
    // when executed
    CreateAccountResponse res = create(account);
    // then it was successful
    assertThat(res.getSuccess(), is(true));
    // and we see the account in the database
    db.useExtension(AccountDao.class, dao -> {
      assertThat(dao.count(), is(1L));
    });
    // and we got back the new account id
    assertThat(res.getId(), is(1L));
  }

  @Test
  public void shouldNotAllowCreatingClosedAccounts() {
    // given a request to make a closed account
    Account.Builder account = TestData.newAccount().setStatus(AccountStatus.CLOSED);
    // when executed
    CreateAccountResponse res = create(account);
    // then it failed
    assertThat(res.getSuccess(), is(false));
    assertThat(res.getErrorsList(), hasItems("Accounts must be created in the OPEN status"));
    // and we don't see the account
    db.useExtension(AccountDao.class, dao -> {
      assertThat(dao.count(), is(0L));
    });
  }

  @Test
  public void shouldCloseAccounts() {
    // given an existing account
    Account a = save(db, TestData.newAccount());
    // when it is closed
    CloseAccountResponse res = close(a.getId());
    // then it worked
    assertThat(res.getSuccess(), is(true));
    // and the account is closed
    assertThat(read(db, a).getStatus(), is(AccountStatus.CLOSED));
  }

  @Test
  public void shouldCloseAnAlreadyClosedAccounts() {
    // given an existing closed account
    Account a = save(db, TestData.newAccount().setStatus(AccountStatus.CLOSED));
    // when it is re-closed
    CloseAccountResponse res = close(a.getId());
    // then we treat it as a no-op
    assertThat(res.getSuccess(), is(true));
    assertThat(read(db, a).getStatus(), is(AccountStatus.CLOSED));
  }

  private CreateAccountResponse create(Account.Builder account) {
    CreateAccountRequest req = CreateAccountRequest.newBuilder().setAccount(account.build()).build();
    return StubObserver.<CreateAccountResponse> getSync(o -> server.createAccount(req, o));
  }

  private CloseAccountResponse close(long accountId) {
    CloseAccountRequest req = CloseAccountRequest.newBuilder().setId(accountId).build();
    return StubObserver.<CloseAccountResponse> getSync(o -> server.closeAccount(req, o));
  }

}
