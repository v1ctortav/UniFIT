import { createClient, SupabaseClient } from "npm:@supabase/supabase-js@2";

export const MP_API = "https://api.mercadopago.com";

export class ErroUsuario extends Error {}

export function json(dados: unknown, status = 200): Response {
  return new Response(JSON.stringify(dados), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

export function supabaseServidor(): SupabaseClient {
  return createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

export function tokenMercadoPago(): string | null {
  const t = Deno.env.get("MP_ACCESS_TOKEN")?.trim();
  return t ? t : null;
}

export async function buscarPagamentoMP(id: string, token: string): Promise<Record<string, any>> {
  const r = await fetch(`${MP_API}/v1/payments/${encodeURIComponent(id)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!r.ok) throw new Error(`Mercado Pago respondeu ${r.status}: ${await r.text()}`);
  return await r.json();
}

export function paraApp(p: Record<string, any>, nomePlano?: string) {
  return {
    id: p.id,
    plano: p.plano_codigo,
    nome_plano: nomePlano ?? null,
    valor: Number(p.valor),
    status: p.status,
    qr_code: p.qr_code,
    qr_code_base64: p.qr_code_base64,
    expira_em: p.expira_em,
    vencimento_novo: p.vencimento_novo,
  };
}

export async function sincronizar(
  sb: SupabaseClient,
  pag: Record<string, any>,
  token: string,
): Promise<Record<string, any>> {
  if (pag.status !== "pendente" || !pag.mp_payment_id) return pag;

  const mp = await buscarPagamentoMP(String(pag.mp_payment_id), token);
  const pago =
    mp.status === "approved" &&
    String(mp.id) === String(pag.mp_payment_id) &&
    mp.external_reference === pag.id &&
    mp.currency_id === "BRL" &&
    Number(mp.transaction_amount) >= Number(pag.valor);

  if (pago) {
    const { error } = await sb.rpc("confirmar_pagamento", { p_pagamento: pag.id });
    if (error) throw error;
  } else if (["cancelled", "rejected", "refunded", "charged_back"].includes(mp.status)) {
    const novo = mp.status === "cancelled" && mp.status_detail === "expired" ? "expirado" : "cancelado";
    await sb.from("pagamentos").update({ status: novo }).eq("id", pag.id).eq("status", "pendente");
  }

  const { data } = await sb.from("pagamentos").select("*").eq("id", pag.id).single();
  return data ?? pag;
}
