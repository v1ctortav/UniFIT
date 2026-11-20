-- Exclui um admin (PERMANENTE): troque os valores marcados com <<< e rode no SQL Editor.
do $$
declare
  v_email            text    := 'admin@exemplo.com';  -- <<< admin que será excluído
  v_herdeiro_email   text    := null;                 -- <<< admin que herda aulas e mensagens (null = excluir)
  v_manter_mensagens boolean := true;                 -- <<< true = mantém as respostas dele no chat (exige herdeiro)

  v_id        uuid;
  v_herdeiro  uuid;
  v_aulas     int;
  v_mensagens int;
begin
  select id into v_id from public.usuarios where lower(email) = lower(v_email) and is_admin;
  if v_id is null then
    raise exception 'Nenhum ADMIN encontrado com o e-mail % (clientes não são excluídos por este script)', v_email;
  end if;

  if (select count(*) from public.usuarios where is_admin) <= 1 then
    raise exception 'Este é o único admin. Crie/promova outro antes de excluí-lo (select public.promover_admin(''email'');)';
  end if;

  if v_herdeiro_email is not null then
    select id into v_herdeiro from public.usuarios where lower(email) = lower(v_herdeiro_email) and is_admin;
    if v_herdeiro is null then
      raise exception 'Herdeiro % não encontrado ou não é admin', v_herdeiro_email;
    end if;
    if v_herdeiro = v_id then
      raise exception 'O herdeiro não pode ser o próprio admin que será excluído';
    end if;
  end if;

  select count(*) into v_aulas     from public.aulas     where criado_por = v_id;
  select count(*) into v_mensagens from public.mensagens where autor_id   = v_id;

  if v_herdeiro is not null then
    update public.aulas set criado_por = v_herdeiro where criado_por = v_id;
  end if;

  if v_manter_mensagens and v_herdeiro is not null then
    update public.mensagens set autor_id = v_herdeiro where autor_id = v_id;
  end if;

  update public.exercicios    set criado_por = null where criado_por = v_id;
  update public.treinos       set criado_por = null where criado_por = v_id;
  update public.treinos_aluno set criado_por = null where criado_por = v_id;

  delete from auth.users where id = v_id;

  raise notice 'Admin % excluído. Aulas: % (%). Mensagens: % (%).',
    v_email,
    v_aulas,     case when v_herdeiro is not null then 'transferidas' else 'excluídas' end,
    v_mensagens, case when v_manter_mensagens and v_herdeiro is not null then 'mantidas' else 'excluídas' end;
end $$;
