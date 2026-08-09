-- Per-device wallet with an atomic coin-credit RPC.
create table if not exists public.wallets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade not null,
  device_id uuid references public.devices(id) on delete cascade not null unique,
  balance integer not null default 0 check (balance >= 0),
  total_earned integer not null default 0 check (total_earned >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.wallets enable row level security;

create policy "Users manage own wallet" on public.wallets
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

grant select, insert, update on public.wallets to authenticated, service_role;

create or replace function public.add_coins_to_wallet(
  p_device_id uuid,
  p_user_id uuid,
  p_coins_to_add integer
)
returns setof public.wallets
language plpgsql
security invoker
as $$
begin
  if p_coins_to_add <= 0 then
    raise exception 'p_coins_to_add must be positive';
  end if;

  return query
  insert into public.wallets (device_id, user_id, balance, total_earned)
  values (p_device_id, p_user_id, p_coins_to_add, p_coins_to_add)
  on conflict (device_id) do update
    set balance = wallets.balance + excluded.balance,
        total_earned = wallets.total_earned + excluded.total_earned,
        updated_at = now()
  returning *;
end;
$$;

grant execute on function public.add_coins_to_wallet(uuid, uuid, integer)
  to authenticated, service_role;
