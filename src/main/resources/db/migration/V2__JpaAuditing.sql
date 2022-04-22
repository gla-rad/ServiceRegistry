-- Update the instance table to support JPA Auditioning
ALTER TABLE instance ALTER published_at type timestamp USING published_at::timestamp;
ALTER TABLE instance ALTER last_updated_at type timestamp USING last_updated_at::timestamp;

-- Update the ledgerrequest table to support JPA Auditioning
ALTER TABLE ledgerrequest ALTER created_at type timestamp USING created_at::timestamp;
ALTER TABLE ledgerrequest ALTER last_updated_at type timestamp USING last_updated_at::timestamp;