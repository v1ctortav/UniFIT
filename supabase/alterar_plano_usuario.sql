-- Altera o plano e o vencimento de um aluno: troque os valores marcados com <<< e rode no SQL Editor.
do $$
declare
  v_email      text    := 'aluno@exemplo.com';  -- <<< e-mail do aluno
  v_plano      text    := 'premium';            -- <<< 'comum' ou 'premium'
  v_vencimento date    := '2026-12-10';         -- <<< nova data de vencimento (AAAA-MM-DD)
  v_ativo      boolean := true;                 -- <<< true = ativo | false = inativo

  v_id   uuid;
  v_nome text;
  v_admin boolean;
begin
  select id, is_admin into v_id, v_admin
    from public.usuarios where lower(email) = lower(trim(v_email));
  if v_id is null then
    raise exception 'Nenhum usuário com o e-mail %', v_email;
  end if;
  if v_admin then
    raise exception '% é admin: admins não têm plano', v_email;
  end if;

  select nome into v_nome from public.planos_disponiveis where codigo = lower(trim(v_plano));
  if v_nome is null then
    raise exception 'Plano "%" não existe. Use um destes: %', v_plano,
      (select string_agg(codigo, ', ' order by ordem) from public.planos_disponiveis);
  end if;

  insert into public.planos (user_id, nome_plano, status, data_vencimento, dia_vencimento)
  values (v_id, v_nome, v_ativo, v_vencimento, extract(day from v_vencimento)::smallint)
  on conflict (user_id) do update
     set nome_plano      = excluded.nome_plano,
         status          = excluded.status,
         data_vencimento = excluded.data_vencimento,
         dia_vencimento  = excluded.dia_vencimento;

  update public.contratos
     set texto = regexp_replace(texto, 'PLANO: [^.]*\.', 'PLANO: ' || v_nome || '.')
   where user_id = v_id;

  update public.pagamentos set status = 'cancelado'
   where user_id = v_id and status = 'pendente';

  insert into public.notificacoes (user_id, nome, descricao, tipo_icone)
  values (v_id, 'Plano atualizado',
          v_nome || (case when v_ativo then ' ativo' else ' inativo' end)
          || ' • vencimento em ' || to_char(v_vencimento, 'DD/MM/YYYY') || '.', 'sistema');

  raise notice 'OK: % agora está no % (%), vencimento %', v_email, v_nome,
    case when v_ativo then 'ativo' else 'inativo' end, to_char(v_vencimento, 'DD/MM/YYYY');
end $$;

select u.nome, u.email, p.nome_plano, p.status, p.data_vencimento, p.dia_vencimento
  from public.planos p join public.usuarios u on u.id = p.user_id
 order by u.nome;
