create table if not exists public.mensagens (
  id         bigint generated always as identity primary key,
  aluno_id   uuid not null references public.usuarios(id) on delete cascade,
  autor_id   uuid not null default auth.uid() references public.usuarios(id) on delete cascade,
  texto      text not null check (char_length(btrim(texto)) between 1 and 2000),
  lida       boolean not null default false,
  criada_em  timestamptz not null default now()
);
create index if not exists mensagens_aluno_data_idx on public.mensagens(aluno_id, criada_em);

alter table public.mensagens enable row level security;
create policy mensagens_select on public.mensagens for select to authenticated
  using (aluno_id = (select auth.uid()) or public.is_admin());
create policy mensagens_insert on public.mensagens for insert to authenticated
  with check (autor_id = (select auth.uid()) and (public.is_admin() or aluno_id = (select auth.uid())));
create policy mensagens_update on public.mensagens for update to authenticated
  using (aluno_id = (select auth.uid()) or public.is_admin())
  with check (aluno_id = (select auth.uid()) or public.is_admin());

grant select, insert on public.mensagens to authenticated;
grant update (lida) on public.mensagens to authenticated;

create or replace function public.autor_mensagem(p_id uuid)
returns jsonb language sql stable security definer set search_path = public
as $$
  select jsonb_build_object('nome', nome, 'foto', foto_perfil, 'is_admin', is_admin)
  from public.usuarios
  where id = p_id and (is_admin or id = auth.uid() or public.is_admin());
$$;
revoke all on function public.autor_mensagem(uuid) from public, anon;
grant execute on function public.autor_mensagem(uuid) to authenticated;

create or replace view public.conversas with (security_invoker = on) as
select m.aluno_id,
       max(m.criada_em) as ultima_em,
       (array_agg(m.texto order by m.criada_em desc))[1] as ultima_mensagem,
       count(*) filter (where not m.lida and m.autor_id = m.aluno_id)  as nao_lidas_admin,
       count(*) filter (where not m.lida and m.autor_id <> m.aluno_id) as nao_lidas_aluno
from public.mensagens m
group by m.aluno_id;
grant select on public.conversas to authenticated;

alter table public.mensagens replica identity full;
alter publication supabase_realtime add table public.mensagens;

alter table public.usuarios add column if not exists visto_em timestamptz;
grant update (visto_em) on public.usuarios to authenticated;

create or replace function public.resumo_online()
returns jsonb language sql stable security definer set search_path = public
as $$
  select jsonb_build_object(
    'alunos_online',      count(*) filter (where not is_admin and visto_em > now() - interval '2 minutes'),
    'alunos_total',       count(*) filter (where not is_admin),
    'professores_online', count(*) filter (where is_admin and visto_em > now() - interval '2 minutes'),
    'professores_total',  count(*) filter (where is_admin))
  from public.usuarios
  where public.is_admin();
$$;
revoke all on function public.resumo_online() from public, anon;
grant execute on function public.resumo_online() to authenticated;

create table if not exists public.treinos_realizados (
  id           bigint generated always as identity primary key,
  user_id      uuid not null default auth.uid() references public.usuarios(id) on delete cascade,
  nome         text not null,
  minutos      integer not null check (minutos >= 0),
  exercicios   integer not null check (exercicios >= 0),
  realizado_em timestamptz not null default now()
);
create index if not exists treinos_realizados_user_idx on public.treinos_realizados(user_id, realizado_em);

alter table public.treinos_realizados enable row level security;
create policy treinos_realizados_select on public.treinos_realizados for select to authenticated
  using (user_id = (select auth.uid()) or public.is_admin());
create policy treinos_realizados_insert on public.treinos_realizados for insert to authenticated
  with check (user_id = (select auth.uid()));
grant select, insert on public.treinos_realizados to authenticated;
