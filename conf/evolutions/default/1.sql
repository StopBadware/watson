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
CREATE TYPE URI_INTENT AS ENUM ('FREE_HOST', 'HACKED', 'MALICIOUS');
CREATE TYPE URI_TYPE AS ENUM ('INTERMEDIARY', 'LANDING', 'PAYLOAD');

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
  logins INTEGER NOT NULL DEFAULT 0,
  last_login TIMESTAMP DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE review_tags (
  id SERIAL PRIMARY KEY,
  name VARCHAR(16) UNIQUE NOT NULL,
  description VARCHAR(64) DEFAULT NULL,
  hex_color CHAR(6) NOT NULL DEFAULT '000000',
  open_only BOOLEAN NOT NULL DEFAULT FALSE,
  is_category BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX ON review_tags (active);

CREATE TABLE reviews (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  reviewed_by INTEGER DEFAULT NULL REFERENCES users (id) ON DELETE RESTRICT,
  verified_by INTEGER DEFAULT NULL REFERENCES users (id) ON DELETE RESTRICT,
  open_only_tag_ids INTEGER ARRAY DEFAULT NULL,
  status REVIEW_STATUS NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  status_updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON reviews (uri_id);
CREATE INDEX ON reviews (status, created_at, status_updated_at);
CREATE INDEX ON reviews (reviewed_by, verified_by);

CREATE TABLE review_taggings (
  review_id INTEGER NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
  review_tag_id INTEGER NOT NULL REFERENCES review_tags (id) ON DELETE CASCADE,
  associated_at TIMESTAMP NOT NULL DEFAULT NOW(), 
  PRIMARY KEY (review_id, review_tag_id)
);

CREATE TABLE associated_uris (
  id SERIAL PRIMARY KEY,
  review_id INTEGER NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  resolved BOOLEAN DEFAULT NULL,
  uri_type URI_TYPE DEFAULT NULL,
  intent URI_INTENT DEFAULT NULL,
  associated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  UNIQUE (review_id, uri_id)
);

CREATE INDEX ON associated_uris (review_id);
CREATE INDEX ON associated_uris (uri_id);
CREATE INDEX ON associated_uris (uri_type, intent, resolved);

CREATE TABLE review_notes (
  id SERIAL PRIMARY KEY,
  review_id INTEGER NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
  author INTEGER NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  note VARCHAR(512) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON review_notes (review_id);
CREATE INDEX ON review_notes (author);

CREATE TABLE review_code (
  id SERIAL PRIMARY KEY,
  review_id INTEGER UNIQUE NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
  bad_code VARCHAR(4096) DEFAULT NULL,
  exec_sha2_256 CHAR(64) DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON review_code (review_id);
CREATE INDEX ON review_code (exec_sha2_256);

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
  review_id INTEGER NOT NULL REFERENCES reviews (id) ON DELETE RESTRICT
);

CREATE INDEX ON review_requests (uri_id, review_id);
CREATE INDEX ON review_requests (open, requested_at, closed_at);
CREATE INDEX ON review_requests (email, ip);
CREATE INDEX ON review_requests (closed_reason);

# --- !Downs

DROP TABLE IF EXISTS uris CASCADE;
DROP TABLE IF EXISTS sources CASCADE;
DROP TABLE IF EXISTS blacklist_events CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS review_tags CASCADE;
DROP TABLE IF EXISTS review_taggings CASCADE;
DROP TABLE IF EXISTS review_code CASCADE;
DROP TABLE IF EXISTS review_notes CASCADE;
DROP TABLE IF EXISTS associated_uris CASCADE;
DROP TABLE IF EXISTS review_requests CASCADE;
DROP TYPE IF EXISTS ROLE;
DROP TYPE IF EXISTS SOURCE;
DROP TYPE IF EXISTS CLOSED_REASON;
DROP TYPE IF EXISTS REVIEW_STATUS;
DROP TYPE IF EXISTS URI_INTENT;
DROP TYPE IF EXISTS URI_TYPE;