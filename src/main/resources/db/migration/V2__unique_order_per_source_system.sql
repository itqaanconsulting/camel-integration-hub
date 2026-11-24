-- One delivery record per order from a source system: enforces idempotent order intake.
create unique index ux_order_deliveries_source_external_order
    on order_deliveries(source_system, external_order_id);
