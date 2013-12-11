# google_rescans schema

# --- !Ups

CREATE TABLE google_rescans (
  id SERIAL PRIMARY KEY,
  uri_id INTEGER NOT NULL REFERENCES uris (id) ON DELETE RESTRICT,
  related_uri_id INTEGER DEFAULT NULL,
  status VARCHAR(16) NOT NULL,
  requested_via VARCHAR(32) NOT NULL,
  rescanned_at TIMESTAMP NOT NULL,
  UNIQUE (uri_id, related_uri_id, rescanned_at)
);

CREATE INDEX ON google_rescans (uri_id, related_uri_id);
CREATE INDEX ON google_rescans (status, requested_via);

# --- !Downs

DROP TABLE google_rescans CASCADE;