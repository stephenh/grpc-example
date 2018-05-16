
Design Thought Process
======================

I really like schema-driven APIs, so considered:

* Swagger, which would provide more REST-native across lots of languages.

  Would be a good choice but I've not used it before, so I don't want to get distracted learning it. Would be fun though.

* GraphQL, like the idea, but seems like more internal API, vs. public API, and don't need the cross-entity graph/etc. for this.

* grpc-java, which is not natively REST friendly, so also more of an internal-only API choice, but going with this as I've used it before.

  (Granted, Seed is Go and I remember [Twirp](https://blog.twitch.tv/twirp-a-sweet-new-rpc-framework-for-go-5f2febbf35f) going by, so apologies if grpc is a red flag.)

For storage, considered:

* Just keeping everything in-memory as objects, given this is a homework assignment, but that seemed overly cute.

* Postgres, I like it, but local config for just running my code seems annoying.

* So sqlite or h2 seemed like a good "zero-setup" choice, while still being "real" storage.

  Using sqlite/h2 also seems I can probably cheat and have unit tests use the real database instead of mocking/stubbing things out.

* For JDBC/ORM/etc., I haven't used [JDBI](http://jdbi.org/) before, but seems simple.

