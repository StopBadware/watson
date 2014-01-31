# uris, sources, blacklist_events, review_requests, reviews, users, review_data schemas

# --- !Ups

CREATE TYPE SOURCE AS ENUM ('GOOG', 'NSF', 'TTS', 'SBW', 'SBWCR');
CREATE TYPE CLOSED_REASON AS ENUM ('ABUSIVE', 'ADMINISTRATIVE', 'REVIEWED_BAD', 'REVIEWED_CLEAN', 'NO_PARTNERS_REPORTING');
CREATE TYPE REVIEW_STATUS AS ENUM (
  'NEW', 
  'REJECTED', 
  'REOPENED', 
  'PENDING_BAD', 
  'CLOSED_BAD', 
  'CLOSED_CLEAN',
  'CLOSED_NO_LONGER_REPORTED',
  'CLOSED_WITHOUT_REVIEW'
);
CREATE TYPE ROLE AS ENUM ('USER', 'REVIEWER', 'VERIFIER', 'ADMIN');

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

INSERT INTO sources VALUES 
  ('GOOG','Google'),
  ('NSF','NSFocus'),
  ('TTS','ThreatTrack'),
  ('SBW','StopBadware'),
  ('SBWCR','StopBadware Community Reports');

CREATE TABLE blacklist_events (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  source SOURCE NOT NULL,
  blacklisted BOOLEAN NOT NULL DEFAULT TRUE,
  blacklisted_at TIMESTAMP NOT NULL,
  unblacklisted_at TIMESTAMP DEFAULT NULL,
  UNIQUE (uri_id, source, blacklisted_at)
);

CREATE INDEX ON blacklist_events (uri_id);
CREATE INDEX ON blacklist_events (source);
CREATE INDEX ON blacklist_events (blacklisted, blacklisted_at, unblacklisted_at);

CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  roles ROLE ARRAY DEFAULT NULL,
  username VARCHAR(16) UNIQUE NOT NULL,
  email VARCHAR(256) UNIQUE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
);

CREATE TABLE review_data (
  id SERIAL PRIMARY KEY
);

CREATE TABLE review_tags (
  id SERIAL PRIMARY KEY,
  name VARCHAR(16) UNIQUE NOT NULL,
  description VARCHAR(64) DEFAULT NULL,
  hex_color CHAR(6) NOT NULL DEFAULT '000000',
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX ON review_tags (active);

CREATE TABLE reviews (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  reviewed_by INTEGER DEFAULT NULL REFERENCES users (id) ON DELETE RESTRICT,
  verified_by INTEGER DEFAULT NULL REFERENCES users (id) ON DELETE RESTRICT,
  review_data_id INTEGER DEFAULT NULL REFERENCES review_data (id) ON DELETE RESTRICT,
  review_tag_ids INTEGER ARRAY DEFAULT NULL,
  status REVIEW_STATUS NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  status_updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON reviews (uri_id, review_data_id);
CREATE INDEX ON reviews (status, created_at, status_updated_at);
CREATE INDEX ON reviews (reviewed_by, verified_by);

CREATE TABLE review_requests (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  open BOOLEAN NOT NULL DEFAULT TRUE,
  email VARCHAR(256) NOT NULL,
  ip BIGINT DEFAULT NULL,
  requester_notes VARCHAR(2048) DEFAULT NULL,
  requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
  closed_at TIMESTAMP DEFAULT NULL,
  closed_reason CLOSED_REASON DEFAULT NULL,
  review_id INTEGER DEFAULT NULL REFERENCES reviews (id) ON DELETE RESTRICT
);

CREATE INDEX ON review_requests (uri_id, review_id);
CREATE INDEX ON review_requests (open, requested_at, closed_at);
CREATE INDEX ON review_requests (email, ip);
CREATE INDEX ON review_requests (closed_reason);

# --- !Downs

DROP TABLE uris CASCADE;
DROP TABLE sources CASCADE;
DROP TABLE blacklist_events CASCADE;
DROP TABLE users CASCADE;
DROP TABLE reviews CASCADE;
DROP TABLE review_tags CASCADE;
DROP TABLE review_data CASCADE;
DROP TABLE review_requests CASCADE;
DROP TYPE SOURCE;
DROP TYPE CLOSED_REASON;
DROP TYPE REVIEW_STATUS;