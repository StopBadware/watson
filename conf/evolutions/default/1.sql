# uris, sources, blacklist_events schemas

# --- !Ups

CREATE TABLE uris (
  id SERIAL PRIMARY KEY,
  uri VARCHAR(2048) NOT NULL,
  reversed_host VARCHAR(256) NOT NULL,
  hierarchical_part VARCHAR(2048) NOT NULL,
  path VARCHAR(2048),
  sha2_256 CHAR(64) UNIQUE NOT NULL,
  created TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON uris (reversed_host);
CREATE INDEX ON uris (hierarchical_part);
CREATE INDEX ON uris (created);

CREATE TABLE sources (
  short_abbr VARCHAR(6) PRIMARY KEY,
  full_name VARCHAR(64)
);

CREATE TABLE blacklist_events (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  source VARCHAR(6) NOT NULL REFERENCES sources (short_abbr) ON DELETE RESTRICT,
  flagged BOOLEAN NOT NULL DEFAULT TRUE,
  flagged_at TIMESTAMP NOT NULL DEFAULT NOW(),
  unflagged_at TIMESTAMP DEFAULT NULL
);

# --- !Downs

DROP TABLE uris;
DROP TABLE sources;
DROP TABLE blacklist_events;