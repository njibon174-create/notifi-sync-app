-- Ensure deleting a device removes all of its notifications.
-- This matches the dashboard confirmation copy and prevents orphaned rows.

do $$
declare
  fk_name text;
begin
  select con.conname
    into fk_name
  from pg_constraint con
  join pg_class rel on rel.oid = con.conrelid
  join pg_namespace nsp on nsp.oid = rel.relnamespace
  where nsp.nspname = 'public'
    and rel.relname = 'notifications'
    and con.contype = 'f'
    and con.confrelid = 'public.devices'::regclass
    and exists (
      select 1
      from unnest(con.conkey) as attnum
      join pg_attribute a
        on a.attrelid = con.conrelid
       and a.attnum = attnum
      where a.attname = 'device_id'
    )
  limit 1;

  if fk_name is not null then
    execute format('alter table public.notifications drop constraint %I', fk_name);
  end if;
end $$;

alter table public.notifications
  add constraint notifications_device_id_fkey
  foreign key (device_id)
  references public.devices(id)
  on delete cascade;
