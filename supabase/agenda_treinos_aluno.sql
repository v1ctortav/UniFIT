alter table public.treinos
  add column if not exists dia_semana smallint check (dia_semana between 0 and 6);

create table if not exists public.treinos_aluno (
  id          bigint generated always as identity primary key,
  user_id     uuid not null references public.usuarios(id) on delete cascade,
  dia_semana  smallint not null check (dia_semana between 0 and 6),
  treino_id   bigint references public.treinos(id) on delete set null,
  nome        text not null,
  descricao   text not null default '',
  categoria   text not null default '',
  exercicios  jsonb not null default '[]'::jsonb check (jsonb_typeof(exercicios) = 'array'),
  criado_por  uuid default auth.uid() references public.usuarios(id) on delete set null,
  criado_em   timestamptz not null default now(),
  unique (user_id, dia_semana)
);

create index if not exists treinos_aluno_user_idx on public.treinos_aluno(user_id);

create trigger trg_validar_treino_aluno
  before insert or update on public.treinos_aluno
  for each row execute function public.validar_exercicios_treino();

alter table public.treinos_aluno enable row level security;

create policy treinos_aluno_select on public.treinos_aluno
  for select to authenticated using (user_id = (select auth.uid()) or public.is_admin());
create policy treinos_aluno_insert on public.treinos_aluno
  for insert to authenticated with check (public.is_admin());
create policy treinos_aluno_update on public.treinos_aluno
  for update to authenticated using (public.is_admin()) with check (public.is_admin());
create policy treinos_aluno_delete on public.treinos_aluno
  for delete to authenticated using (public.is_admin());

grant select, insert, update, delete on public.treinos_aluno to authenticated;

create or replace function public.nome_instrutor(p_id uuid)
returns text language sql stable security definer set search_path = public
as $$ select nome from public.usuarios where id = p_id and is_admin = true; $$;

revoke all on function public.nome_instrutor(uuid) from public, anon;
grant execute on function public.nome_instrutor(uuid) to authenticated;

create or replace view public.aulas_detalhadas
with (security_invoker = on) as
 select a.id, a.criado_por, a.nome, a.data, a.horario_inicio, a.horario_fim, a.vagas,
    public.vagas_ocupadas(a.id) as vagas_ocupadas,
    greatest(a.vagas - public.vagas_ocupadas(a.id), 0) as vagas_disponiveis,
    (exists (select 1 from public.matriculas_aula m
              where m.aula_id = a.id and m.user_id = auth.uid())) as inscrito,
    public.nome_instrutor(a.criado_por) as nome_instrutor
   from public.aulas a;

grant select on public.aulas_detalhadas to authenticated;

create or replace function public.remover_exercicio_dos_treinos()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
  update public.treinos t
     set exercicios = coalesce((select jsonb_agg(x) from jsonb_array_elements(t.exercicios) x
                                 where (x ->> 'exercicio_id')::bigint <> old.id), '[]'::jsonb)
   where t.exercicios @> jsonb_build_array(jsonb_build_object('exercicio_id', old.id));
  update public.treinos_aluno t
     set exercicios = coalesce((select jsonb_agg(x) from jsonb_array_elements(t.exercicios) x
                                 where (x ->> 'exercicio_id')::bigint <> old.id), '[]'::jsonb)
   where t.exercicios @> jsonb_build_array(jsonb_build_object('exercicio_id', old.id));
  return old;
end;
$$;

revoke all on function public.remover_exercicio_dos_treinos() from public, anon, authenticated;

drop trigger if exists trg_remover_exercicio_dos_treinos on public.exercicios;
create trigger trg_remover_exercicio_dos_treinos
  after delete on public.exercicios
  for each row execute function public.remover_exercicio_dos_treinos();
