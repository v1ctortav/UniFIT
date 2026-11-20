create table if not exists public.planos_disponiveis (
  codigo    text primary key,
  nome      text not null,
  valor     numeric(10,2) not null check (valor > 0),
  descricao text not null default '',
  ordem     int not null default 0
);

insert into public.planos_disponiveis (codigo, nome, valor, descricao, ordem) values
  ('comum',   'Plano Comum',   1.00, 'Acesso à academia e às aulas coletivas', 1),
  ('premium', 'Plano Premium', 2.00, 'Tudo do Comum + acompanhamento do professor', 2)
on conflict (codigo) do update
  set nome = excluded.nome, valor = excluded.valor, descricao = excluded.descricao, ordem = excluded.ordem;

alter table public.planos_disponiveis enable row level security;
drop policy if exists planos_disponiveis_select on public.planos_disponiveis;
create policy planos_disponiveis_select on public.planos_disponiveis
  for select to authenticated using (true);
drop policy if exists planos_disponiveis_admin on public.planos_disponiveis;
create policy planos_disponiveis_admin on public.planos_disponiveis
  for all to authenticated using (public.is_admin()) with check (public.is_admin());
grant select on public.planos_disponiveis to authenticated;
grant insert, update, delete on public.planos_disponiveis to authenticated;

alter table public.planos add column if not exists dia_vencimento smallint
  check (dia_vencimento between 1 and 31);
update public.planos
   set dia_vencimento = extract(day from data_vencimento)::smallint
 where data_vencimento is not null and dia_vencimento is null;

update public.planos set nome_plano = 'Plano Comum' where nome_plano = 'Plano Mensal';
alter table public.planos alter column nome_plano set default 'Plano Comum';

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_nome  text    := coalesce(nullif(trim(new.raw_user_meta_data ->> 'nome'), ''), split_part(new.email, '@', 1));
  v_admin boolean := coalesce((new.raw_app_meta_data ->> 'is_admin')::boolean, false);
  v_plano text    := 'Plano Comum';
begin
  insert into public.usuarios (id, nome, email, is_admin)
  values (new.id, v_nome, new.email, v_admin)
  on conflict (id) do nothing;

  if not v_admin then
    insert into public.planos (user_id, nome_plano, status, data_vencimento)
    values (new.id, v_plano, false, null)
    on conflict (user_id) do nothing;

    insert into public.contratos (user_id, texto)
    values (new.id, public.gerar_texto_contrato(v_nome, new.email, v_plano))
    on conflict (user_id) do nothing;

    insert into public.medidas (user_id)
    values (new.id)
    on conflict (user_id) do nothing;
  end if;

  return new;
end;
$$;

create table if not exists public.pagamentos (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null references public.usuarios(id) on delete cascade,
  plano_codigo   text not null references public.planos_disponiveis(codigo),
  valor          numeric(10,2) not null,
  status         text not null default 'pendente'
                 check (status in ('pendente', 'aprovado', 'expirado', 'cancelado', 'erro')),
  mp_payment_id  text unique,
  qr_code        text,
  qr_code_base64 text,
  expira_em      timestamptz,
  criado_em      timestamptz not null default now(),
  pago_em        timestamptz,
  vencimento_novo date
);
create index if not exists pagamentos_user_idx on public.pagamentos (user_id, criado_em desc);

alter table public.pagamentos enable row level security;
drop policy if exists pagamentos_select on public.pagamentos;
create policy pagamentos_select on public.pagamentos
  for select to authenticated using (user_id = (select auth.uid()) or public.is_admin());
revoke all on public.pagamentos from anon;
revoke insert, update, delete, truncate on public.pagamentos from authenticated;
grant select on public.pagamentos to authenticated;

create or replace function public.proximo_vencimento(p_atual date, p_dia int, p_hoje date default current_date)
returns date
language plpgsql
immutable
as $$
declare
  v_base date := coalesce(p_atual, p_hoje);
  v_mes  date;
  v_venc date;
begin
  v_mes  := date_trunc('month', v_base)::date + interval '1 month';
  v_venc := least(v_mes + (p_dia - 1), (v_mes + interval '1 month' - interval '1 day')::date);
  while v_venc <= p_hoje loop
    v_mes  := v_mes + interval '1 month';
    v_venc := least(v_mes + (p_dia - 1), (v_mes + interval '1 month' - interval '1 day')::date);
  end loop;
  return v_venc;
end;
$$;

create or replace function public.confirmar_pagamento(p_pagamento uuid)
returns date
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_pag   public.pagamentos;
  v_plano public.planos;
  v_nome  text;
  v_dia   int;
  v_venc  date;
begin
  select * into v_pag from public.pagamentos where id = p_pagamento for update;
  if v_pag.id is null then
    raise exception 'Pagamento não encontrado';
  end if;
  if v_pag.status = 'aprovado' then
    return v_pag.vencimento_novo;
  end if;

  select nome into v_nome from public.planos_disponiveis where codigo = v_pag.plano_codigo;
  select * into v_plano from public.planos where user_id = v_pag.user_id for update;

  v_dia  := coalesce(v_plano.dia_vencimento, extract(day from coalesce(v_plano.data_vencimento, current_date))::int);
  v_venc := public.proximo_vencimento(v_plano.data_vencimento, v_dia);

  update public.planos
     set status = true,
         nome_plano = v_nome,
         data_vencimento = v_venc,
         dia_vencimento = v_dia
   where user_id = v_pag.user_id;

  update public.contratos
     set texto = regexp_replace(texto, 'PLANO: [^.]*\.', 'PLANO: ' || v_nome || '.')
   where user_id = v_pag.user_id;

  update public.pagamentos
     set status = 'aprovado', pago_em = now(), vencimento_novo = v_venc
   where id = p_pagamento;

  insert into public.notificacoes (user_id, nome, descricao, tipo_icone)
  values (v_pag.user_id, 'Pagamento confirmado',
          v_nome || ' ativo até ' || to_char(v_venc, 'DD/MM/YYYY') || '.', 'sistema');

  return v_venc;
end;
$$;

revoke all on function public.confirmar_pagamento(uuid) from public, anon, authenticated;
grant execute on function public.confirmar_pagamento(uuid) to service_role;

grant usage on schema public to service_role;
grant select on public.planos, public.planos_disponiveis, public.usuarios to service_role;
grant select, insert, update on public.pagamentos to service_role;
