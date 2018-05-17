package seed;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
    db.useExtension(AccountDao.class, dao -> dao.insert(TestData.newAccount().build()));
    // when it is closed
    CloseAccountResponse res = close(1L);
    // then it worked
    assertThat(res.getSuccess(), is(true));
    // and the account is closed
    db.useExtension(AccountDao.class, dao -> {
      assertThat(dao.read(1L).getStatus(), is(AccountStatus.CLOSED));
    });
  }

  private CreateAccountResponse create(Account.Builder account) {
    CreateAccountRequest req = CreateAccountRequest.newBuilder().setAccount(account.build()).build();
    // this is boilerplate-y, not a fan grpc's approach here
    StubObserver<CreateAccountResponse> res = new StubObserver<>();
    server.createAccount(req, res);
    assertThat(res.values.size(), is(1));
    assertThat(res.completed, is(true));
    return res.values.get(0);
  }

  private CloseAccountResponse close(long accountId) {
    CloseAccountRequest req = CloseAccountRequest.newBuilder().setId(accountId).build();
    // this is boilerplate-y, not a fan grpc's approach here
    StubObserver<CloseAccountResponse> res = new StubObserver<>();
    server.closeAccount(req, res);
    assertThat(res.values.size(), is(1));
    assertThat(res.completed, is(true));
    return res.values.get(0);
  }

}
