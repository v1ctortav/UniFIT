alter table public.notificacoes drop constraint if exists notificacoes_tipo_icone_check;
alter table public.notificacoes
  add constraint notificacoes_tipo_icone_check check (tipo_icone in ('sistema', 'geral', 'treino'));

alter table public.notificacoes add column if not exists referencia text;
create index if not exists notificacoes_user_referencia_idx
  on public.notificacoes (user_id, referencia, criada_em desc);

create or replace function public.notificar_mensagem()
returns trigger
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_ref        text := 'chat:' || new.aluno_id;
  v_autor_nome text;
  v_aluno_nome text;
  v_texto      text := left(new.texto, 90) || case when char_length(new.texto) > 90 then '…' else '' end;
begin
  select nome into v_autor_nome from public.usuarios where id = new.autor_id;
  select nome into v_aluno_nome from public.usuarios where id = new.aluno_id;

  if new.autor_id = new.aluno_id then
    insert into public.notificacoes (user_id, nome, descricao, tipo_icone, referencia)
    select u.id, 'Nova mensagem de ' || coalesce(v_aluno_nome, 'aluno'), v_texto, 'geral', v_ref
    from public.usuarios u
    where u.is_admin
      and not exists (select 1 from public.notificacoes n
                      where n.user_id = u.id and n.referencia = v_ref
                        and n.criada_em > now() - interval '30 minutes');
  else
    if not exists (select 1 from public.notificacoes n
                   where n.user_id = new.aluno_id and n.referencia = v_ref
                     and n.criada_em > now() - interval '30 minutes') then
      insert into public.notificacoes (user_id, nome, descricao, tipo_icone, referencia)
      values (new.aluno_id,
              'Nova mensagem de ' || coalesce('Prof. ' || split_part(v_autor_nome, ' ', 1), 'um professor'),
              v_texto, 'geral', v_ref);
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_notificar_mensagem on public.mensagens;
create trigger trg_notificar_mensagem
  after insert on public.mensagens
  for each row execute function public.notificar_mensagem();
