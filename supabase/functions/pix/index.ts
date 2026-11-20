import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import {
  ErroUsuario,
  MP_API,
  json,
  paraApp,
  sincronizar,
  supabaseServidor,
  tokenMercadoPago,
} from "../_shared/mercadopago.ts";

const MINUTOS_VALIDADE = 30;

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ erro: "Use POST" }, 405);

  const sb = supabaseServidor();
  const jwt = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  const { data: auth } = await sb.auth.getUser(jwt);
  const usuario = auth?.user;
  if (!usuario) return json({ erro: "Sessão expirada. Faça login novamente." }, 401);

  const token = tokenMercadoPago();
  if (!token) {
    return json({ erro: "O pagamento por Pix ainda não foi configurado pela academia." }, 503);
  }

  let corpo: Record<string, any> = {};
  try {
    corpo = await req.json();
  } catch { }

  try {
    if (corpo.acao === "criar") return json(await criar(sb, usuario, token));
    if (corpo.acao === "consultar") return json(await consultar(sb, usuario.id, String(corpo.id ?? ""), token));
    return json({ erro: "Ação inválida" }, 400);
  } catch (e) {
    console.error(e);
    if (e instanceof ErroUsuario) return json({ erro: e.message }, 400);
    return json({ erro: "Não foi possível falar com o Mercado Pago. Tente novamente em instantes." }, 502);
  }
});

async function criar(sb: any, usuario: any, token: string) {
  const { data: plano, error: erroPlano } = await sb.from("planos")
    .select("user_id, nome_plano").eq("user_id", usuario.id).maybeSingle();
  if (erroPlano) throw erroPlano;
  if (!plano) throw new ErroUsuario("Somente alunos podem pagar um plano.");

  const { data: oferta, error: erroOferta } = await sb.from("planos_disponiveis")
    .select("*").eq("nome", plano.nome_plano).maybeSingle();
  if (erroOferta) throw erroOferta;
  if (!oferta) {
    throw new ErroUsuario(`O ${plano.nome_plano} não está disponível para pagamento pelo app. Fale com a academia.`);
  }
  const codigo = oferta.codigo;

  const daquiAPouco = new Date(Date.now() + 2 * 60_000).toISOString();
  const { data: aberto, error: erroAberto } = await sb.from("pagamentos").select("*")
    .eq("user_id", usuario.id).eq("plano_codigo", codigo).eq("status", "pendente")
    .gt("expira_em", daquiAPouco).not("qr_code", "is", null)
    .order("criado_em", { ascending: false }).limit(1).maybeSingle();
  if (erroAberto) throw erroAberto;
  if (aberto) return paraApp(await sincronizar(sb, aberto, token), oferta.nome);

  const { data: pag, error } = await sb.from("pagamentos")
    .insert({ user_id: usuario.id, plano_codigo: codigo, valor: oferta.valor })
    .select().single();
  if (error) throw error;

  const expira = new Date(Date.now() + MINUTOS_VALIDADE * 60_000);
  const expiraBrasilia = new Date(expira.getTime() - 3 * 3_600_000).toISOString().replace("Z", "-03:00");

  const resposta = await fetch(`${MP_API}/v1/payments`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      "X-Idempotency-Key": pag.id,
    },
    body: JSON.stringify({
      transaction_amount: Number(oferta.valor),
      description: `UniFIT - ${oferta.nome} (1 mês)`,
      payment_method_id: "pix",
      external_reference: pag.id,
      notification_url: `${Deno.env.get("SUPABASE_URL")}/functions/v1/webhook-mercadopago`,
      date_of_expiration: expiraBrasilia,
      payer: { email: usuario.email },
    }),
  });
  const mp = await resposta.json().catch(() => ({}));

  const dados = mp?.point_of_interaction?.transaction_data;
  if (!resposta.ok || !dados?.qr_code) {
    console.error("Mercado Pago recusou a cobrança", resposta.status, JSON.stringify(mp));
    await sb.from("pagamentos").update({ status: "erro" }).eq("id", pag.id);
    const msg = String(mp?.message ?? "");
    if (msg.toLowerCase().includes("key")) {
      throw new ErroUsuario("A conta da academia ainda não tem uma chave Pix cadastrada no Mercado Pago.");
    }
    throw new ErroUsuario("O Mercado Pago não conseguiu gerar o Pix agora. Tente novamente.");
  }

  const { data: salvo, error: erroSalvar } = await sb.from("pagamentos").update({
    mp_payment_id: String(mp.id),
    qr_code: dados.qr_code,
    qr_code_base64: dados.qr_code_base64,
    expira_em: expira.toISOString(),
  }).eq("id", pag.id).select().single();
  if (erroSalvar) throw erroSalvar;

  return paraApp(salvo, oferta.nome);
}

async function consultar(sb: any, userId: string, id: string, token: string) {
  const { data: pag, error } = await sb.from("pagamentos").select("*").eq("id", id).eq("user_id", userId).maybeSingle();
  if (error) throw error;
  if (!pag) throw new ErroUsuario("Cobrança não encontrada.");

  let atual = await sincronizar(sb, pag, token);

  if (atual.status === "pendente" && atual.expira_em && new Date(atual.expira_em).getTime() < Date.now() - 5 * 60_000) {
    await sb.from("pagamentos").update({ status: "expirado" }).eq("id", id).eq("status", "pendente");
    atual = { ...atual, status: "expirado" };
  }

  const { data: oferta } = await sb.from("planos_disponiveis").select("nome").eq("codigo", atual.plano_codigo).maybeSingle();
  return paraApp(atual, oferta?.nome);
}
