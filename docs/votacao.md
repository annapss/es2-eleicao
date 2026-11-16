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
