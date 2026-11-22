# Votacao: bloqueio de votos invalidos (US-19)

As mesmas regras de elegibilidade sao aplicadas no servico de registro e de confirmacao.
O eleitor e identificado pelo token de login; o cliente nao escolhe um eleitorId.

| Situacao | Resultado |
| --- | --- |
| Sem autenticacao | 401 |
| Perfil CHEFE_SECAO ou CANDIDATO | 403 |
| Conta ELEITOR sem cadastro de eleitor | 403 |
| Eleicao AGENDADA ou ENCERRADA | 422 |
| Segundo registro no mesmo cargo e eleicao/turno | 409 |
| Novo registro ou confirmacao para cargo ja confirmado | 409 |

Uma eleicao identifica um turno. Outros cargos e outras eleicoes/turnos continuam
independentes. Registros rejeitados preservam a escolha original e nao alteram o placar.
Depois da confirmacao, a verificacao de voto confirmado ocorre antes de consultar
um novo candidato, inclusive se esse candidato nao existir.

## Verificacao manual no Swagger

1. Autorize como eleitor e registre em `POST /api/votacao/{eleicaoId}/votos`
   um candidato valido de uma eleicao ABERTA: espere 201.
2. Repita o registro com o mesmo candidato ou outro do mesmo cargo: espere 409.
3. Confirme em `POST /api/votacao/{eleicaoId}/votos/{cargo}/confirmacao`: espere 204.
4. Repita a confirmacao ou tente registrar outra escolha no cargo: espere 409.
5. Autorize como chefe ou candidato e tente os dois endpoints: espere 403.
6. Como chefe, encerre a eleicao. Autorize novamente como eleitor e tente os dois
   endpoints: espere 422.

Os testes de integracao verificam o banco e o placar apos as rejeicoes, para todos
os cinco cargos. Nenhum endpoint adicional e necessario para US-19.

## Sigilo do voto (US-20)

- `comparecimento` registra eleitor, eleicao/turno e cargo, sem candidato ou id do voto.
- `voto` guarda apenas um id aleatorio, eleicao, cargo e candidato; nao guarda eleitor,
  usuario, comparecimento ou horario de confirmacao. Nao ha chave estrangeira entre
  `comparecimento` e `voto`.
- `voto_pendente` associa temporariamente o eleitor a escolha durante o fluxo em duas
  etapas. A confirmacao remove esse registro na mesma transacao que grava o voto e
  o comparecimento. Uma falha desfaz as tres operacoes e permite tentar novamente.
- Registro e confirmacao retornam corpos vazios, sem id de voto, candidato ou recibo
  que ligue eleitor a escolha. Nenhum perfil, incluindo o chefe, possui endpoint
  de consulta ao voto ou pendente de um eleitor. A consulta de candidatos continua disponivel.
- O pedido de voto tem representacao textual redigida. A configuracao padrao desativa
  detalhes de corpos HTTP e SQL, inclusive parametros, para evitar expor escolhas em logs.

A separacao e uma garantia do modelo de dados confirmado e da API. O pendente ainda
contem a escolha antes da confirmacao; acesso direto ao banco, backups e correlacao
externa nao sao anonimizados por esse mecanismo.

### Verificacao

No Swagger, registrar e confirmar devem retornar 201 e 204 com corpo vazio.
Nao existe operacao GET para consultar escolhas individuais. Os testes
`SigiloVotoIntegrationTest` verificam a estrutura SQLite real, a ausencia de consultas,
as respostas vazias e a remocao dos pendentes apos duas confirmacoes de eleitores diferentes.
`SigiloVotoTest` verifica que a representacao textual do pedido nao revela a escolha.
