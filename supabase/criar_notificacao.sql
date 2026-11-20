-- Cria uma notificação: troque os valores marcados com <<< e rode no SQL Editor.
do $$
declare
  v_destino   text := 'cliente@exemplo.com';  -- <<< e-mail, 'clientes', 'admins' ou 'todos'
  v_titulo    text := 'Aviso da academia';    -- <<<
  v_descricao text := 'A academia funcionará em horário especial no feriado.';  -- <<<
  v_tipo      text := 'geral';  -- <<< 'geral', 'sistema' ou 'treino'
  v_qtd int;
begin
  if v_tipo not in ('geral', 'sistema', 'treino') then
    raise exception 'Tipo inválido: % (use geral, sistema ou treino)', v_tipo;
  end if;
  if btrim(coalesce(v_titulo, '')) = '' then
    raise exception 'Digite o título da notificação';
  end if;

  insert into public.notificacoes (user_id, nome, descricao, tipo_icone)
  select u.id, btrim(v_titulo), coalesce(v_descricao, ''), v_tipo
  from public.usuarios u
  where case lower(btrim(v_destino))
          when 'todos'    then true
          when 'clientes' then not u.is_admin
          when 'admins'   then u.is_admin
          else lower(u.email) = lower(btrim(v_destino))
        end;

  get diagnostics v_qtd = row_count;
  if v_qtd = 0 then
    raise exception 'Nenhum usuário encontrado para "%"', v_destino;
  end if;

  raise notice 'Notificação "%" criada para % usuário(s).', v_titulo, v_qtd;
end $$;
