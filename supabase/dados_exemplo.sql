-- Dados fictícios para testar o app (rode no SQL Editor depois dos outros scripts).
do $$
declare u record;
begin
  for u in select * from (values
    ('a1111111-1111-4111-8111-111111111111'::uuid, 'cliente1@unifit.com', 'Ana Souza',        false),
    ('b1000000-0000-4000-8000-000000000001'::uuid, 'fernanda@gmail.com',  'Fernanda Ribeiro', true),
    ('b1000000-0000-4000-8000-000000000002'::uuid, 'mariana@gmail.com',   'Mariana Costa',    false),
    ('b1000000-0000-4000-8000-000000000003'::uuid, 'lucas@gmail.com',     'Lucas Pereira',    false),
    ('b1000000-0000-4000-8000-000000000004'::uuid, 'juliana@gmail.com',   'Juliana Alves',    false),
    ('b1000000-0000-4000-8000-000000000005'::uuid, 'rafael@gmail.com',    'Rafael Lima',      false),
    ('b1000000-0000-4000-8000-000000000006'::uuid, 'beatriz@gmail.com',   'Beatriz Rocha',    false),
    ('b1000000-0000-4000-8000-000000000007'::uuid, 'gabriel@gmail.com',   'Gabriel Santos',   false)
  ) as t(id, email, nome, admin)
  loop
    if not exists (select 1 from auth.users where id = u.id or email = u.email) then
      insert into auth.users (
        instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
        raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
        confirmation_token, recovery_token, email_change_token_new, email_change,
        email_change_token_current, phone_change, phone_change_token, reauthentication_token,
        is_sso_user, is_anonymous)
      values (
        '00000000-0000-0000-0000-000000000000', u.id, 'authenticated', 'authenticated', u.email,
        extensions.crypt('Teste@123', extensions.gen_salt('bf')), now(),
        jsonb_build_object('provider', 'email', 'providers', jsonb_build_array('email'), 'is_admin', u.admin),
        jsonb_build_object('nome', u.nome),
        now() - interval '30 days', now(),
        '', '', '', '', '', '', '', '', false, false);

      insert into auth.identities (id, provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
      values (gen_random_uuid(), u.id::text, u.id,
              jsonb_build_object('sub', u.id::text, 'email', u.email, 'email_verified', true),
              'email', now(), now(), now());
    end if;
  end loop;
end $$;

update public.planos p
set nome_plano = v.plano, status = v.ativo, data_vencimento = v.venc::date
from (values
  ('cliente1@unifit.com', 'Plano Premium',    true,  '2026-12-19'),
  ('cliente2@unifit.com', 'Plano Mensal',     false, '2026-11-09'),
  ('mariana@gmail.com',   'Plano Trimestral', true,  '2027-01-15'),
  ('lucas@gmail.com',     'Plano Mensal',     true,  '2026-12-05'),
  ('juliana@gmail.com',   'Plano Anual',      true,  '2027-08-01'),
  ('rafael@gmail.com',    'Plano Mensal',     false, '2026-11-10'),
  ('beatriz@gmail.com',   'Plano Premium',    true,  '2026-12-28'),
  ('gabriel@gmail.com',   'Plano Mensal',     true,  '2026-11-30')
) as v(email, plano, ativo, venc)
join public.usuarios us on us.email = v.email
where p.user_id = us.id;

update public.contratos c
set texto = public.gerar_texto_contrato(us.nome, us.email, p.nome_plano)
from public.usuarios us
join public.planos p on p.user_id = us.id
where c.user_id = us.id;

update public.medidas m
set peso = v.peso, altura = v.altura, gordura_corporal = v.bf, historico_pesos = v.hist::double precision[]
from (values
  ('cliente1@unifit.com', 62.5, 1.65, 24.0, '{0,0,0,0,66.0,65.2,64.8,64.0,63.5,63.1,62.5,0}'),
  ('cliente2@unifit.com', 84.0, 1.80, 19.5, '{88.0,87.2,86.5,86.0,85.8,85.1,85.0,84.6,84.3,84.1,84.0,0}'),
  ('mariana@gmail.com',   58.2, 1.62, 22.0, '{0,0,0,0,0,0,60.5,59.8,59.1,58.7,58.2,0}'),
  ('lucas@gmail.com',     76.4, 1.76, 15.0, '{72.0,72.5,73.1,73.8,74.2,74.9,75.3,75.8,76.0,76.2,76.4,0}'),
  ('juliana@gmail.com',   64.0, 1.70, 21.5, '{0,0,0,0,0,0,0,0,66.2,65.0,64.0,0}'),
  ('rafael@gmail.com',    91.3, 1.83, 24.5, '{95.0,94.1,93.6,93.0,92.8,92.0,91.9,91.5,91.3,0,91.3,0}'),
  ('beatriz@gmail.com',   55.0, 1.58, 20.0, '{0,0,0,57.0,56.6,56.1,55.9,55.6,55.4,55.2,55.0,0}'),
  ('gabriel@gmail.com',   70.8, 1.74, 13.5, '{0,0,0,0,0,0,0,0,0,69.9,70.8,0}')
) as v(email, peso, altura, bf, hist)
join public.usuarios us on us.email = v.email
where m.user_id = us.id;

insert into public.exercicios (nome, categoria, equipamentos, repeticoes, series, link_youtube, criado_por)
select v.nome, v.cat, v.eq::text[], v.rep, v.ser, 'https://www.youtube.com/watch?v=' || v.yt,
       'a3333333-3333-4333-8333-333333333333'
from (values
  ('Supino inclinado com halteres', 'Peito',  '{"Banco inclinado","Halteres"}',            12, 3, 'sh5ZsRpEZCw'),
  ('Crucifixo com halteres',        'Peito',  '{"Banco reto","Halteres"}',                 12, 3, 'UFASlayaWaE'),
  ('Puxada frontal',                'Costas', '{"Polia alta","Barra larga"}',              10, 4, '0lNuPeUdKpk'),
  ('Remada curvada',                'Costas', '{"Barra","Anilhas"}',                       10, 4, 'G8l_8chR5BE'),
  ('Levantamento terra',            'Costas', '{"Barra","Anilhas"}',                        8, 3, 'NVpwlMFrB3I'),
  ('Desenvolvimento com halteres',  'Ombro',  '{"Banco com encosto","Halteres"}',          10, 4, 'iZ7k3sJ-wZk'),
  ('Elevação lateral',              'Ombro',  '{"Halteres"}',                              12, 4, 'nnH63icHYXY'),
  ('Leg press',                     'Pernas', '{"Leg press 45°","Anilhas"}',               12, 4, 'K5n2vg3oZa4'),
  ('Afundo com halteres',           'Pernas', '{"Halteres"}',                              10, 3, 'K5NhC44yuR8'),
  ('Rosca direta',                  'Braços', '{"Barra W","Anilhas"}',                     12, 3, 'eTIRaAwnX30'),
  ('Tríceps pulley',                'Braços', '{"Polia alta","Barra reta"}',               12, 3, 'nMqQNGo4Jtg')
) as v(nome, cat, eq, rep, ser, yt)
where not exists (select 1 from public.exercicios e where e.nome = v.nome);

update public.treinos set dia_semana = 5 where nome = 'Treino A — Força' and dia_semana is null;

insert into public.treinos (nome, descricao, categoria, dia_semana, exercicios, criado_por)
select v.nome, v.descr, v.cat, v.dia,
       (select coalesce(jsonb_agg(jsonb_build_object('exercicio_id', e.id, 'series', it.s, 'repeticoes', it.r) order by it.ord), '[]'::jsonb)
          from jsonb_to_recordset(v.itens) as it(nome text, s int, r int, ord int)
          join public.exercicios e on e.nome = it.nome),
       'a3333333-3333-4333-8333-333333333333'
from (values
  ('Peito e Tríceps', 'Foco em peitoral e tríceps com cargas moderadas.', 'Peito', 1,
   '[{"nome":"Supino reto","s":4,"r":10,"ord":1},{"nome":"Supino inclinado com halteres","s":3,"r":12,"ord":2},{"nome":"Crucifixo com halteres","s":3,"r":12,"ord":3},{"nome":"Tríceps pulley","s":3,"r":12,"ord":4}]'::jsonb),
  ('Costas e Bíceps', 'Puxadas e remadas para costas largas, finalizando no bíceps.', 'Costas', 2,
   '[{"nome":"Puxada frontal","s":4,"r":10,"ord":1},{"nome":"Remada curvada","s":4,"r":10,"ord":2},{"nome":"Levantamento terra","s":3,"r":8,"ord":3},{"nome":"Rosca direta","s":3,"r":12,"ord":4}]'::jsonb),
  ('Pernas completo', 'Quadríceps, glúteos e posteriores em um só treino.', 'Pernas', 3,
   '[{"nome":"Agachamento livre","s":4,"r":10,"ord":1},{"nome":"Leg press","s":4,"r":12,"ord":2},{"nome":"Afundo com halteres","s":3,"r":10,"ord":3}]'::jsonb),
  ('Ombros definidos', 'Desenvolvimento e elevações para deltoides.', 'Ombro', 4,
   '[{"nome":"Desenvolvimento com halteres","s":4,"r":10,"ord":1},{"nome":"Elevação lateral","s":4,"r":12,"ord":2}]'::jsonb),
  ('Superiores iniciante', 'Treino leve de corpo superior para quem está começando.', 'Peito', 1,
   '[{"nome":"Supino reto","s":3,"r":12,"ord":1},{"nome":"Puxada frontal","s":3,"r":12,"ord":2},{"nome":"Desenvolvimento com halteres","s":3,"r":12,"ord":3},{"nome":"Rosca direta","s":3,"r":12,"ord":4}]'::jsonb),
  ('Inferiores iniciante', 'Base de força para pernas com foco na técnica.', 'Pernas', 3,
   '[{"nome":"Leg press","s":3,"r":15,"ord":1},{"nome":"Agachamento livre","s":3,"r":12,"ord":2},{"nome":"Afundo com halteres","s":3,"r":12,"ord":3}]'::jsonb),
  ('Costas e postura', 'Fortalece a cadeia posterior e melhora a postura.', 'Costas', 6,
   '[{"nome":"Remada curvada","s":3,"r":12,"ord":1},{"nome":"Puxada frontal","s":3,"r":12,"ord":2},{"nome":"Elevação lateral","s":3,"r":15,"ord":3}]'::jsonb)
) as v(nome, descr, cat, dia, itens)
where not exists (select 1 from public.treinos t where t.nome = v.nome);

insert into public.treinos_aluno (user_id, dia_semana, treino_id, nome, descricao, categoria, exercicios, criado_por)
select us.id, v.dia, t.id, t.nome, t.descricao, t.categoria, t.exercicios, 'a3333333-3333-4333-8333-333333333333'
from (values
  ('cliente1@unifit.com', 1, 'Peito e Tríceps'),
  ('cliente1@unifit.com', 2, 'Costas e Bíceps'),
  ('cliente1@unifit.com', 3, 'Pernas completo'),
  ('cliente1@unifit.com', 4, 'Ombros definidos'),
  ('cliente1@unifit.com', 5, 'Treino A — Força'),
  ('cliente2@unifit.com', 1, 'Superiores iniciante'),
  ('cliente2@unifit.com', 3, 'Inferiores iniciante'),
  ('mariana@gmail.com',   2, 'Costas e postura'),
  ('mariana@gmail.com',   4, 'Pernas completo'),
  ('mariana@gmail.com',   6, 'Ombros definidos'),
  ('lucas@gmail.com',     1, 'Peito e Tríceps'),
  ('lucas@gmail.com',     3, 'Costas e Bíceps'),
  ('lucas@gmail.com',     5, 'Pernas completo'),
  ('juliana@gmail.com',   4, 'Superiores iniciante'),
  ('beatriz@gmail.com',   1, 'Inferiores iniciante'),
  ('beatriz@gmail.com',   4, 'Costas e postura'),
  ('gabriel@gmail.com',   2, 'Peito e Tríceps'),
  ('gabriel@gmail.com',   5, 'Costas e Bíceps')
) as v(email, dia, treino)
join public.usuarios us on us.email = v.email
join public.treinos t on t.nome = v.treino
on conflict (user_id, dia_semana) do nothing;

insert into public.treinos_aluno (user_id, dia_semana, treino_id, nome, descricao, categoria, exercicios, criado_por)
select us.id, 4, null, 'Treino personalizado — quinta', 'Montado pelo professor para o seu nível.', 'Força',
       jsonb_build_array(
         jsonb_build_object('exercicio_id', (select id from public.exercicios where nome = 'Agachamento livre'), 'series', 3, 'repeticoes', 12),
         jsonb_build_object('exercicio_id', (select id from public.exercicios where nome = 'Supino reto'), 'series', 3, 'repeticoes', 10),
         jsonb_build_object('exercicio_id', (select id from public.exercicios where nome = 'Remada curvada'), 'series', 3, 'repeticoes', 10)),
       'a3333333-3333-4333-8333-333333333333'
from public.usuarios us where us.email = 'cliente2@unifit.com'
on conflict (user_id, dia_semana) do nothing;

insert into public.aulas (criado_por, nome, data, horario_inicio, horario_fim, vagas)
select (select id from public.usuarios where email = v.prof), v.nome, v.data::date, v.ini, v.fim, v.vagas
from (values
  ('admin@unifit.com',   'Funcional',   '2026-11-18', '18:00', '19:00', 12),
  ('fernanda@gmail.com', 'Yoga',        '2026-11-19', '07:00', '08:00', 15),
  ('admin@unifit.com',   'Funcional',   '2026-11-19', '12:00', '13:00', 12),
  ('admin@unifit.com',   'HIIT',        '2026-11-19', '18:30', '19:15', 3),
  ('fernanda@gmail.com', 'Zumba',       '2026-11-19', '20:00', '21:00', 20),
  ('fernanda@gmail.com', 'Pilates',     '2026-11-20', '08:00', '09:00', 8),
  ('admin@unifit.com',   'Spinning',    '2026-11-20', '19:00', '20:00', 10),
  ('fernanda@gmail.com', 'Alongamento', '2026-11-21', '09:00', '09:45', 20),
  ('admin@unifit.com',   'Muay Thai',   '2026-11-21', '18:00', '19:30', 12),
  ('fernanda@gmail.com', 'Yoga',        '2026-11-22', '09:00', '10:00', 15),
  ('admin@unifit.com',   'Funcional',   '2026-11-23', '07:00', '08:00', 12),
  ('fernanda@gmail.com', 'Pilates',     '2026-11-23', '18:00', '19:00', 8),
  ('admin@unifit.com',   'HIIT',        '2026-11-24', '12:00', '12:45', 10),
  ('fernanda@gmail.com', 'Zumba',       '2026-11-24', '19:00', '20:00', 20),
  ('admin@unifit.com',   'Spinning',    '2026-11-25', '07:00', '08:00', 10),
  ('fernanda@gmail.com', 'Yoga',        '2026-11-26', '07:00', '08:00', 15),
  ('admin@unifit.com',   'Funcional',   '2026-11-26', '18:00', '19:00', 12),
  ('admin@unifit.com',   'Muay Thai',   '2026-11-27', '18:00', '19:30', 12)
) as v(prof, nome, data, ini, fim, vagas)
where not exists (
  select 1 from public.aulas a
  where a.nome = v.nome and a.data = v.data::date and a.horario_inicio = v.ini);

insert into public.matriculas_aula (aula_id, user_id)
select a.id, us.id
from (values
  ('HIIT',     '2026-11-19', '18:30', 'cliente1@unifit.com'),
  ('HIIT',     '2026-11-19', '18:30', 'mariana@gmail.com'),
  ('HIIT',     '2026-11-19', '18:30', 'lucas@gmail.com'),
  ('Yoga',     '2026-11-19', '07:00', 'juliana@gmail.com'),
  ('Yoga',     '2026-11-19', '07:00', 'beatriz@gmail.com'),
  ('Funcional','2026-11-19', '12:00', 'cliente2@unifit.com'),
  ('Funcional','2026-11-19', '12:00', 'gabriel@gmail.com'),
  ('Spinning', '2026-11-22', '18:00', 'cliente1@unifit.com'),
  ('Spinning', '2026-11-22', '18:00', 'cliente2@unifit.com'),
  ('Spinning', '2026-11-22', '18:00', 'rafael@gmail.com'),
  ('Pilates',  '2026-11-20', '08:00', 'mariana@gmail.com'),
  ('Pilates',  '2026-11-20', '08:00', 'beatriz@gmail.com'),
  ('Zumba',    '2026-11-24', '19:00', 'juliana@gmail.com'),
  ('Zumba',    '2026-11-24', '19:00', 'cliente1@unifit.com')
) as v(nome, data, ini, email)
join public.aulas a on a.nome = v.nome and a.data = v.data::date and a.horario_inicio = v.ini
join public.usuarios us on us.email = v.email
on conflict do nothing;

insert into public.notificacoes (user_id, nome, descricao, tipo_icone, lida, criada_em)
select us.id, v.nome, v.descr, v.tipo, v.lida, now() - v.ha::interval
from public.usuarios us
cross join (values
  ('Bem-vindo(a) ao UniFit',     'Seu cadastro foi concluído. Confira seu plano e o contrato no perfil.', 'sistema', true,  '3 days'),
  ('Novo treino na sua agenda',  'Seu professor montou os treinos desta semana. Veja em Treinos.',        'geral',   false, '5 hours'),
  ('Aulas da semana liberadas',  'Yoga, Pilates, HIIT e Muay Thai com vagas abertas. Garanta a sua!',     'geral',   false, '40 minutes')
) as v(nome, descr, tipo, lida, ha)
where us.is_admin = false
  and us.email <> 'cliente2@unifit.com'
  and not exists (select 1 from public.notificacoes n where n.user_id = us.id and n.nome = v.nome);

insert into public.notificacoes (user_id, nome, descricao, tipo_icone, lida, criada_em)
select us.id, v.nome, v.descr, v.tipo, v.lida, now() - v.ha::interval
from (values
  ('cliente2@unifit.com', 'Plano vencido',               'Seu plano venceu em 09/11. Regularize o pagamento para continuar treinando.', 'sistema', false, '2 hours'),
  ('rafael@gmail.com',    'Plano vencido',               'Seu plano venceu em 10/11. Regularize o pagamento para continuar treinando.', 'sistema', false, '1 hour'),
  ('admin@unifit.com',    'Turma de HIIT lotada',        'A aula de HIIT de hoje às 18:30 atingiu o limite de vagas.',                  'geral',   false, '25 minutes'),
  ('admin@unifit.com',    'Relatório semanal',           'O resumo de frequência da semana já está disponível.',                        'sistema', true,  '1 day'),
  ('fernanda@gmail.com',  'Nova inscrição em Pilates',   'Duas alunas se inscreveram na aula de Pilates de amanhã.',                     'geral',   false, '3 hours'),
  ('fernanda@gmail.com',  'Bem-vinda à equipe',          'Seu acesso de professora foi liberado. Cadastre suas aulas.',                 'sistema', true,  '2 days')
) as v(email, nome, descr, tipo, lida, ha)
join public.usuarios us on us.email = v.email
where not exists (select 1 from public.notificacoes n where n.user_id = us.id and n.nome = v.nome);

insert into public.pesos_exercicio (user_id, exercicio_id, peso, registrado_em)
select us.id, e.id, v.peso, now() - v.ha::interval
from (values
  ('cliente1@unifit.com', 'Supino reto',                  25.0, '21 days'),
  ('cliente1@unifit.com', 'Supino reto',                  27.5, '14 days'),
  ('cliente1@unifit.com', 'Supino reto',                  30.0, '6 days'),
  ('cliente1@unifit.com', 'Agachamento livre',            40.0, '12 days'),
  ('cliente1@unifit.com', 'Agachamento livre',            45.0, '5 days'),
  ('cliente1@unifit.com', 'Desenvolvimento com halteres', 8.0,  '7 days'),
  ('cliente2@unifit.com', 'Puxada frontal',               45.0, '10 days'),
  ('cliente2@unifit.com', 'Supino reto',                  50.0, '9 days'),
  ('mariana@gmail.com',   'Leg press',                    80.0, '15 days'),
  ('mariana@gmail.com',   'Leg press',                    90.0, '4 days'),
  ('lucas@gmail.com',     'Levantamento terra',           70.0, '8 days'),
  ('lucas@gmail.com',     'Rosca direta',                 20.0, '3 days')
) as v(email, exercicio, peso, ha)
join public.usuarios us on us.email = v.email
join public.exercicios e on e.nome = v.exercicio
where not exists (
  select 1 from public.pesos_exercicio p
  where p.user_id = us.id and p.exercicio_id = e.id and p.peso = v.peso);
