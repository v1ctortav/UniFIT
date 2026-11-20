import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { buscarPagamentoMP, sincronizar, supabaseServidor, tokenMercadoPago } from "../_shared/mercadopago.ts";

const ok = (texto = "ok") => new Response(texto, { status: 200 });

Deno.serve(async (req) => {
  const url = new URL(req.url);
  let corpo: Record<string, any> = {};
  try {
    corpo = await req.json();
  } catch { }

  const tipo = corpo.type ?? corpo.topic ?? url.searchParams.get("type") ?? url.searchParams.get("topic");
  const idPagamento = String(corpo?.data?.id ?? url.searchParams.get("data.id") ?? url.searchParams.get("id") ?? "");
  if (tipo !== "payment" || !idPagamento) return ok("ignorado");

  const segredo = Deno.env.get("MP_WEBHOOK_SECRET")?.trim();
  if (segredo && !(await assinaturaValida(req, url, idPagamento, segredo))) {
    return new Response("assinatura inválida", { status: 401 });
  }

  const token = tokenMercadoPago();
  if (!token) return ok("sem token");

  try {
    const mp = await buscarPagamentoMP(idPagamento, token);
    const referencia = mp.external_reference;
    if (!referencia) return ok("sem referência");

    const sb = supabaseServidor();
    const { data: pag } = await sb.from("pagamentos").select("*").eq("id", referencia).maybeSingle();
    if (!pag) return ok("cobrança desconhecida");
    if (pag.mp_payment_id && String(pag.mp_payment_id) !== String(mp.id)) return ok("outro pagamento");

    await sincronizar(sb, { ...pag, mp_payment_id: pag.mp_payment_id ?? String(mp.id) }, token);
    return ok();
  } catch (e) {
    console.error(e);
    return new Response("erro", { status: 500 });
  }
});

async function assinaturaValida(req: Request, url: URL, idPagamento: string, segredo: string): Promise<boolean> {
  const cabecalho = req.headers.get("x-signature") ?? "";
  const partes = Object.fromEntries(
    cabecalho.split(",").map((p) => p.split("=").map((s) => s.trim())).filter((p) => p.length === 2),
  );
  const ts = partes["ts"];
  const v1 = partes["v1"];
  if (!ts || !v1) return false;

  const idDaUrl = (url.searchParams.get("data.id") ?? idPagamento).toLowerCase();
  const requestId = req.headers.get("x-request-id");
  let manifesto = `id:${idDaUrl};`;
  if (requestId) manifesto += `request-id:${requestId};`;
  manifesto += `ts:${ts};`;

  const chave = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(segredo),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const assinatura = await crypto.subtle.sign("HMAC", chave, new TextEncoder().encode(manifesto));
  const hex = Array.from(new Uint8Array(assinatura)).map((b) => b.toString(16).padStart(2, "0")).join("");
  return hex === v1;
}
