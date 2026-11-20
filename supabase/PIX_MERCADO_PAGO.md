# Pix de verdade no UniFIT (Mercado Pago)

## Como funciona

1. O aluno vê o plano dele (Comum R$ 1,00 ou Premium R$ 2,00) e toca em **Pix**. Ele não escolhe o plano: quem define é a academia, pelo script `supabase/alterar_plano_usuario.sql`.
2. O app chama a função **`pix`** do Supabase. Ela pede ao Mercado Pago um Pix de verdade (QR Code + copia e cola) que cai na **sua** conta Mercado Pago. O código vale 30 minutos.
3. Quando o Pix é pago, o Mercado Pago avisa a função **`webhook-mercadopago`**. Enquanto a tela está aberta, o app também confere a cada 5 segundos.
4. O servidor **confere direto no Mercado Pago** se o pagamento está aprovado e se o valor está certo. Só depois disso ativa o plano, coloca o vencimento 1 mês à frente (sempre no mesmo dia, mesmo pagando atrasado), atualiza o contrato e manda a notificação "Pagamento confirmado".

O token da sua conta fica só no Supabase. O app nunca vê esse token, e ninguém consegue ativar um plano sem pagar.

---

## Passo a passo

### 1. Conta Mercado Pago com chave Pix
1. Crie (ou entre em) uma conta no **Mercado Pago**. Pessoa física com CPF serve.
2. No app do Mercado Pago, vá em **Pix → Minhas chaves** e **cadastre uma chave Pix** (CPF, e-mail, celular ou aleatória).
   Sem chave cadastrada, o Mercado Pago não gera o QR Code e o app mostra um aviso.

### 2. Pegar o Access Token de produção
1. Acesse https://www.mercadopago.com.br/developers/panel/app e clique em **Criar aplicação**.
   - Nome: `UniFIT`
   - Tipo de solução: **Pagamentos online**
   - Produto: **Checkout Transparente** (API de pagamentos)
2. Abra a aplicação e vá em **Credenciais de produção**. Se pedir, preencha os dados do negócio e ative.
3. Copie o **Access Token** (começa com `APP_USR-`).
   ⚠️ Ele dá acesso à sua conta. Não mande para ninguém e não coloque no código do app.

### 3. Guardar o token no Supabase
1. Abra o painel do Supabase do projeto → **Edge Functions** → **Secrets** (ou *Project Settings → Edge Functions*).
2. Clique em **Add new secret**:
   - Name: `MP_ACCESS_TOKEN`
   - Value: o Access Token copiado (`APP_USR-...`)
3. Salve. Não precisa publicar nada de novo: as funções já leem o segredo.

### 4. Webhook (aviso automático de pagamento)
Cada cobrança já informa ao Mercado Pago o endereço do aviso, então o passo 4 é **opcional**. Ele deixa a configuração explícita no painel:
1. No painel da aplicação do Mercado Pago, vá em **Webhooks → Configurar notificações**.
2. Em **Modo produção**, use a URL:
   `https://oumwmakoduhavlirjudr.supabase.co/functions/v1/webhook-mercadopago`
3. Marque o evento **Pagamentos** e salve.
4. (Opcional) Copie a **assinatura secreta** e crie no Supabase o segredo `MP_WEBHOOK_SECRET` com ela. Com isso, avisos com assinatura errada são recusados.
   Se depois disso os planos pararem de ativar com o app fechado, apague esse segredo. A segurança principal continua: o servidor sempre confere o pagamento no Mercado Pago.

### 5. Testar com dinheiro de verdade (R$ 1,00)
1. No app, entre com uma conta de **aluno** → **Perfil → Realizar pagamento**.
2. Toque em **Pix**. Aparecem o QR Code e o código copia e cola do plano do aluno.
3. Pague com **outra conta bancária**. O Mercado Pago não deixa pagar Pix para a própria conta.
4. Em poucos segundos o app mostra **"Pagamento confirmado!"**. No perfil, o plano aparece **Ativo** com o novo vencimento.

### 6. Tirar o dinheiro do Mercado Pago
O valor cai no saldo da sua conta Mercado Pago. Para mandar ao seu banco: **app Mercado Pago → Transferir → Pix** para a sua chave no outro banco.
O Mercado Pago cobra uma pequena tarifa por Pix recebido via API. Confira em **Tarifas** no Mercado Pago.

---

## Mudar o plano ou o vencimento de um aluno
Supabase → **SQL Editor** → cole `supabase/alterar_plano_usuario.sql`, troque e-mail, plano (`comum`/`premium`), data e ativo/inativo → **Run**.

## Mudar preços ou descrições
Supabase → **Table Editor → `planos_disponiveis`**. Edite `valor`, `nome` ou `descricao`. O app lê daí na hora; não precisa gerar um app novo.

## Se algo der errado
- **"O pagamento por Pix ainda não foi configurado"**: o segredo `MP_ACCESS_TOKEN` não está salvo (passo 3).
- **"…não tem uma chave Pix cadastrada"**: falta a chave Pix na conta Mercado Pago (passo 1).
- Outros erros: Supabase → **Edge Functions → pix → Logs** mostra a resposta do Mercado Pago.
- Pagou e o plano não ativou: abra de novo **Realizar pagamento → mesmo plano → Pix** dentro dos 30 minutos de validade. O app reabre a mesma cobrança e confere no Mercado Pago.
- Histórico de cobranças: tabela **`pagamentos`** (status `pendente`, `aprovado`, `expirado`…).

## Arquivos
- `supabase/pagamentos_pix.sql`: tabelas, regra do vencimento e confirmação (já aplicado no banco)
- `supabase/functions/pix/index.ts`: gera e confere o Pix
- `supabase/functions/webhook-mercadopago/index.ts`: recebe o aviso do Mercado Pago
- `supabase/functions/_shared/mercadopago.ts`: código comum (conferência no Mercado Pago)
