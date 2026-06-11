create table order_deliveries (
    integration_id uuid primary key,
    external_order_id varchar(100) not null,
    source_system varchar(100) not null,
    customer_email varchar(254) not null,
    total_amount numeric(19, 2) not null,
    currency varchar(3) not null,
    processing_lane varchar(30) not null,
    received_at timestamp with time zone not null,
    status varchar(30) not null,
    attempts integer not null,
    error_message varchar(2000),
    updated_at timestamp with time zone not null
);

create index idx_order_deliveries_status_updated_at
    on order_deliveries(status, updated_at);
