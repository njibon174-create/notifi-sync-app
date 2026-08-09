-- Wheel configuration per user
create table if not exists public.wheel_config (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade not null,
  segments jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id)
);

create index if not exists wheel_config_user_idx on public.wheel_config (user_id);

alter table public.wheel_config enable row level security;

drop policy if exists "wheel_config_select_own" on public.wheel_config;
create policy "wheel_config_select_own"
  on public.wheel_config
  for select
  using (auth.uid() = user_id);

drop policy if exists "wheel_config_insert_own" on public.wheel_config;
create policy "wheel_config_insert_own"
  on public.wheel_config
  for insert
  with check (auth.uid() = user_id);

drop policy if exists "wheel_config_update_own" on public.wheel_config;
create policy "wheel_config_update_own"
  on public.wheel_config
  for update
  using (auth.uid() = user_id);

drop policy if exists "wheel_config_delete_own" on public.wheel_config;
create policy "wheel_config_delete_own"
  on public.wheel_config
  for delete
  using (auth.uid() = user_id);