-- Table: bank_routing_lookup

-- DROP TABLE bank_routing_lookup;

CREATE TABLE bank_routing_lookup
(
  routing_number character varying(9) NOT NULL,
  short_name character varying(18) NOT NULL,
  name character varying(36),
  state character varying(2),
  city character varying(25),
  transfer_eligible character varying(1),
  settlement_only character varying(1),
  securities_transfer_status character varying(1),
  date_last_revised character varying(8),
  CONSTRAINT pk_bank_routing_lookup PRIMARY KEY (routing_number, short_name)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE bank_routing_lookup
  OWNER TO postgres;

-- Table: download_results

-- DROP TABLE download_results;

CREATE TABLE download_results
(
  date_time_stamp character varying(50) NOT NULL,
  data text,
  is_found boolean,
  reason text,
  CONSTRAINT pk_download_results PRIMARY KEY (date_time_stamp)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE download_results
  OWNER TO postgres;
