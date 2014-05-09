# hosts/ips/asns related schemas

# --- !Ups

CREATE TABLE autonomous_systems (
  number INTEGER PRIMARY KEY,
  name VARCHAR(256) NOT NULL,
  country CHAR(2) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON autonomous_systems (name, country);

CREATE TABLE ip_asn_mappings (
  id SERIAL PRIMARY KEY,
  ip BIGINT NOT NULL,
  asn INTEGER NOT NULL REFERENCES autonomous_systems (number) ON DELETE RESTRICT,
  first_mapped_at TIMESTAMP NOT NULL DEFAULT NOW(),
  last_mapped_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON ip_asn_mappings (ip);
CREATE INDEX ON ip_asn_mappings (asn);

CREATE TABLE host_ip_mappings (
  id SERIAL PRIMARY KEY,
  reversed_host VARCHAR(256) NOT NULL,
  ip BIGINT NOT NULL,
  first_resolved_at TIMESTAMP NOT NULL DEFAULT NOW(),
  last_resolved_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX ON host_ip_mappings (reversed_host);
CREATE INDEX ON host_ip_mappings (ip);

# --- !Downs

DROP TABLE IF EXISTS host_ip_mappings CASCADE;
DROP TABLE IF EXISTS ip_asn_mappings CASCADE;
DROP TABLE IF EXISTS autonomous_systems CASCADE;