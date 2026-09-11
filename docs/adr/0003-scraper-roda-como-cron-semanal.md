# Scraper roda como cron job semanal, não como serviço contínuo

**Status**: accepted

O scraping não precisa de dado em tempo real — eventos não mudam de hora em hora. A raspagem (Sympla e futuras fontes) roda como um job agendado semanal, não um processo sempre ativo escutando ou fazendo polling contínuo.

## Consequências

- Reduz o que precisa ficar sempre rodando na hospedagem (#33) a só a API e, se confirmado o caminho não oficial do WhatsApp (#30), o processo de envio — o scraper em si pode rodar sob demanda (cron do próprio host, GitHub Actions agendado, etc.).
- Eventos podem ficar até uma semana desatualizados entre execuções; se isso se provar curto demais, é decisão fácil de reverter (mudar a frequência do cron), diferente da linguagem ou do banco.
