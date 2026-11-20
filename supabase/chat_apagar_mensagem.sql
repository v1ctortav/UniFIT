alter table public.mensagens add column if not exists apagada boolean not null default false;

alter table public.mensagens drop constraint if exists mensagens_texto_check;
alter table public.mensagens add constraint mensagens_texto_check
  check (apagada or (char_length(btrim(texto)) >= 1 and char_length(btrim(texto)) <= 2000));

create or replace function public.apagar_mensagem(p_id bigint)
returns public.mensagens
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v public.mensagens;
  m public.mensagens;
begin
  select * into v from public.mensagens where id = p_id;
  if v.id is null or v.autor_id is distinct from auth.uid() then
    raise exception 'Você só pode apagar as suas mensagens';
  end if;
  if v.apagada then
    return v;
  end if;

  update public.notificacoes
     set descricao = 'Mensagem apagada'
   where referencia = 'chat:' || v.aluno_id
     and descricao = left(v.texto, 90) || case when char_length(v.texto) > 90 then '…' else '' end;

  update public.mensagens set apagada = true, texto = ''
   where id = p_id
  returning * into m;
  return m;
end;
$$;

revoke all on function public.apagar_mensagem(bigint) from public, anon;
grant execute on function public.apagar_mensagem(bigint) to authenticated;

create or replace view public.conversas with (security_invoker = on) as
select aluno_id,
       max(criada_em) as ultima_em,
       (array_agg(case when apagada then 'Mensagem apagada' else texto end order by criada_em desc))[1] as ultima_mensagem,
       count(*) filter (where not lida and autor_id = aluno_id) as nao_lidas_admin,
       count(*) filter (where not lida and autor_id <> aluno_id) as nao_lidas_aluno
from public.mensagens m
group by aluno_id;
