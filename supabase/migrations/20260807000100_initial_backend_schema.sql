-- Initial backend schema for cross-device notification sync
-- Supabase Auth handles users in auth.users; no custom users table is created.

create extension if not exists pgcrypto;

create table if not exists public.devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  device_name text not null,
  device_model text,
  push_token text,
  last_active timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  device_id uuid not null references public.devices (id) on delete cascade,
  type text not null check (type in ('sms', 'app')),
  app_package_name text,
  sender text not null,
  title text,
  body text not null,
  original_timestamp timestamptz not null,
  is_read boolean not null default false,
  created_at timestamptz not null default now()
);

alter table public.devices enable row level security;
alter table public.notifications enable row level security;

-- Devices policies

drop policy if exists "devices_select_own" on public.devices;
create policy "devices_select_own"
  on public.devices
  for select
  using (auth.uid() = user_id);

drop policy if exists "devices_insert_own" on public.devices;
create policy "devices_insert_own"
  on public.devices
  for insert
  with check (auth.uid() = user_id);

drop policy if exists "devices_update_own" on public.devices;
create policy "devices_update_own"
  on public.devices
  for update
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "devices_delete_own" on public.devices;
create policy "devices_delete_own"
  on public.devices
  for delete
  using (auth.uid() = user_id);

-- Notifications policies

drop policy if exists "notifications_select_own" on public.notifications;
create policy "notifications_select_own"
  on public.notifications
  for select
  using (auth.uid() = user_id);

drop policy if exists "notifications_insert_own" on public.notifications;
create policy "notifications_insert_own"
  on public.notifications
  for insert
  with check (
    auth.uid() = user_id
    and exists (
      select 1
      from public.devices d
      where d.id = device_id
        and d.user_id = auth.uid()
    )
  );

drop policy if exists "notifications_update_own" on public.notifications;
create policy "notifications_update_own"
  on public.notifications
  for update
  using (auth.uid() = user_id)
  with check (
    auth.uid() = user_id
    and exists (
      select 1
      from public.devices d
      where d.id = device_id
        and d.user_id = auth.uid()
    )
  );

drop policy if exists "notifications_delete_own" on public.notifications;
create policy "notifications_delete_own"
  on public.notifications
  for delete
  using (auth.uid() = user_id);

-- Indexes for common query paths

create index if not exists idx_devices_user_id on public.devices (user_id);
create index if not exists idx_devices_last_active on public.devices (last_active desc);

create index if not exists idx_notifications_user_id on public.notifications (user_id);
create index if not exists idx_notifications_device_id on public.notifications (device_id);
create index if not exists idx_notifications_created_at on public.notifications (created_at desc);
create index if not exists idx_notifications_user_created_at on public.notifications (user_id, created_at desc);
create index if not exists idx_notifications_original_timestamp on public.notifications (original_timestamp desc);

-- Required grants for Supabase API roles

grant usage on schema public to authenticated, service_role;
grant select, insert, update, delete on public.devices to authenticated, service_role;
grant select, insert, update, delete on public.notifications to authenticated, service_role;
