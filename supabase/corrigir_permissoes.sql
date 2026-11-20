grant select, delete on public.usuarios to authenticated;
grant update (nome, foto_perfil) on public.usuarios to authenticated;

grant select, insert, update, delete on public.planos    to authenticated;
grant select, insert, update, delete on public.contratos to authenticated;

grant select, insert, delete on public.notificacoes to authenticated;
grant update (lida) on public.notificacoes to authenticated;

grant select, insert on public.medidas to authenticated;
grant update (peso, altura, gordura_corporal) on public.medidas to authenticated;

grant select, insert, update, delete on public.exercicios      to authenticated;
grant select, insert, update, delete on public.pesos_exercicio to authenticated;
grant select, insert, update, delete on public.treinos         to authenticated;

grant select, insert, update, delete on public.aulas           to authenticated;
grant select, insert, delete         on public.matriculas_aula to authenticated;
grant select                         on public.aulas_detalhadas to authenticated;

grant usage, select on all sequences in schema public to authenticated;
