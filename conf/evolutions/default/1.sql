# uris, sources, blacklist_events, review_requests, reviews, users, review_data schemas

# --- !Ups

CREATE TYPE SOURCE AS ENUM ('GOOG', 'NSF', 'TTS', 'SBW', 'SBWCR');
CREATE TYPE REVIEW_STATUS AS ENUM ('ABUSIVE', 'NEW', 'PENDING', 'BAD', 'CLEAN', 'NO_PARTNERS_REPORTING');

CREATE TABLE uris (
  id SERIAL PRIMARY KEY,
  uri VARCHAR(2048) NOT NULL,
  reversed_host VARCHAR(256) NOT NULL,
  hierarchical_part VARCHAR(2048) NOT NULL,
  path VARCHAR(2048),
  sha2_256 CHAR(64) UNIQUE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON uris (reversed_host);
CREATE INDEX ON uris (hierarchical_part);

CREATE TABLE sources (
  abbr SOURCE PRIMARY KEY,
  full_name VARCHAR(64) NOT NULL
);

CREATE TABLE blacklist_events (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  source SOURCE NOT NULL REFERENCES sources (abbr) ON DELETE RESTRICT,
  flagged BOOLEAN NOT NULL DEFAULT TRUE,
  flagged_at TIMESTAMP NOT NULL,
  unflagged_at TIMESTAMP DEFAULT NULL
);

CREATE INDEX ON blacklist_events (uri_id);
CREATE INDEX ON blacklist_events (source);
CREATE INDEX ON blacklist_events (flagged, flagged_at, unflagged_at);

CREATE TABLE review_requests (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  open BOOLEAN NOT NULL DEFAULT TRUE,
  email VARCHAR(256) NOT NULL,
  ip BIGINT DEFAULT NULL,
  requester_notes VARCHAR(2048) DEFAULT NULL,
  requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
  closed_at TIMESTAMP DEFAULT NULL
);

CREATE INDEX ON review_requests (uri_id);
CREATE INDEX ON review_requests (open, requested_at);
CREATE INDEX ON review_requests (email, ip);

CREATE TABLE users (
  id SERIAL PRIMARY KEY
);

CREATE TABLE review_data (
  id SERIAL PRIMARY KEY
);

CREATE TABLE reviews (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  reviewed_by INTEGER DEFAULT NULL REFERENCES users (id) ON DELETE RESTRICT,
  verified_by INTEGER DEFAULT NULL REFERENCES users (id) ON DELETE RESTRICT,
  review_data_id INTEGER DEFAULT NULL REFERENCES review_data (id) ON DELETE RESTRICT,
  status REVIEW_STATUS NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  status_updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON reviews (uri_id, review_data_id);
CREATE INDEX ON reviews (status, created_at, status_updated_at);
CREATE INDEX ON reviews (reviewed_by, verified_by);

# --- !Downs

DROP TABLE blacklist_events;
DROP TABLE sources;
DROP TABLE review_requests;
DROP TABLE reviews;
DROP TABLE users;
DROP TABLE review_data;
DROP TABLE uris;
DROP TYPE SOURCE;
DROP TYPE REVIEW_STATUS;