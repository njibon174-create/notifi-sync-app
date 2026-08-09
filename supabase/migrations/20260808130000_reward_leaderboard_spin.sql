-- Reward offers, leaderboard entries, and device spin cooldown status.
-- These tables are user-scoped and protected by RLS.

create or replace function public.server_now()
returns timestamptz
language sql
stable
as $$
  select now();
$$;

grant execute on function public.server_now() to authenticated, service_role;

create table public.reward_offers (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade not null,
  label text not null,
  description text,
  coin_cost integer not null,
  is_active boolean default true,
  sort_order integer default 0,
  created_at timestamptz default now()
);

alter table public.reward_offers enable row level security;

create policy "Users manage own offers" on public.reward_offers
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

grant select, insert, update, delete on public.reward_offers to authenticated, service_role;

create index if not exists idx_reward_offers_user_active_sort
  on public.reward_offers (user_id, is_active, sort_order);

create table public.leaderboard (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade not null,
  display_name text not null,
  coins integer not null default 0,
  rank integer,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

alter table public.leaderboard enable row level security;

create policy "Users manage own leaderboard" on public.leaderboard
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

grant select, insert, update, delete on public.leaderboard to authenticated, service_role;

create index if not exists idx_leaderboard_user_coins
  on public.leaderboard (user_id, coins desc);

create table public.spin_status (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade not null,
  device_id uuid references public.devices(id) on delete cascade not null unique,
  last_spin_at timestamptz,
  is_unlocked boolean default false,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

alter table public.spin_status enable row level security;

create policy "Users manage own spin status" on public.spin_status
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

grant select, insert, update, delete on public.spin_status to authenticated, service_role;

create index if not exists idx_spin_status_user_device
  on public.spin_status (user_id, device_id);
