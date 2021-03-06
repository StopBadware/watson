# community report related schemas

# --- !Ups

CREATE TABLE cr_types (
  id SERIAL PRIMARY KEY,
  cr_type VARCHAR(64) UNIQUE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO cr_types (cr_type) VALUES ('Drive-by download'), ('Executable'), ('Links to badware');

CREATE TABLE cr_sources (
  id SERIAL PRIMARY KEY,
  short_name VARCHAR(6) UNIQUE NOT NULL,
  full_name VARCHAR(20) NOT NULL, 
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO cr_sources (short_name, full_name) VALUES 
  ('sbworg', 'StopBadware website'),
  ('sbw', 'StopBadware staff'),
  ('mmd', 'MalwareMustDie'),
  ('vetted', 'Vetted Researcher');

CREATE TABLE community_reports (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  ip BIGINT DEFAULT NULL,
  description VARCHAR(2048) DEFAULT NULL,
  bad_code VARCHAR(2048) DEFAULT NULL,
  cr_type_id INTEGER DEFAULT NULL REFERENCES cr_types (id) ON DELETE SET NULL,
  cr_source_id INTEGER DEFAULT NULL REFERENCES cr_sources (id) ON DELETE SET NULL,
  reported_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON community_reports (uri_id);
CREATE INDEX ON community_reports (cr_type_id, cr_source_id, reported_at);

CREATE TABLE cr_notes (
  id SERIAL PRIMARY KEY,
  cr_id INTEGER NOT NULL REFERENCES community_reports (id) ON DELETE CASCADE,
  author INTEGER NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  note VARCHAR(512) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON cr_notes (cr_id, author);

# --- !Downs

DROP TABLE IF EXISTS cr_notes CASCADE;
DROP TABLE IF EXISTS community_reports CASCADE;
DROP TABLE IF EXISTS cr_sources CASCADE;
DROP TABLE IF EXISTS cr_types CASCADE;