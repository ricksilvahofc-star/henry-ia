# Henry Dropshipping — Mercado Livre

Integração pessoal para OAuth 2.0 e gerenciamento de publicações do Mercado Livre.

## Deploy

Importe este repositório na Vercel e defina **Root Directory** como `ml-automation`.

Variáveis de ambiente:
- `ML_CLIENT_ID` — APP ID do Mercado Livre.
- `ML_CLIENT_SECRET` — Secret Key do Mercado Livre.
- `ML_REDIRECT_URI` — exatamente a URL pública `https://SEU-PROJETO.vercel.app/api/callback`.
- `SESSION_KEY` — uma senha aleatória longa, somente na Vercel.

Depois do deploy, cadastre a mesma `ML_REDIRECT_URI` na aplicação do Mercado Livre.

O código mantém os tokens somente em cookie HttpOnly/Secure criptografado com `SESSION_KEY`; nenhum token deve ser colocado no GitHub.

## Observação

A API do Mercado Livre exige permissões adequadas para publicar/alterar anúncios. A publicação pode exigir atributos adicionais conforme a categoria.
