
-- Don't really like the name "transaction", as to me that implies multiple
-- entries; I considered calling this "account_entry" and then making "transaction"
-- an entity that ties multiple account entries together (e.g. both sides of a
-- transfer), but then the temptation is to make transactins double-entry, which
-- is not a good idea as this isn't an accounting system. So avoiding all of that.
CREATE TABLE transaction (
  id INT AUTO_INCREMENT,
  time TIMESTAMP WITH TIME ZONE,
  account_id INT,
  amount BIGINT,
  FOREIGN KEY (account_id) REFERENCES account (id));