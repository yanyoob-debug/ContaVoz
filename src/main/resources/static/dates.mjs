export function todayISO(now = new Date()) {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(now);
  const part = (type) => parts.find((item) => item.type === type).value;
  return `${part('year')}-${part('month')}-${part('day')}`;
}

export function formatDate(date) {
  if (!date) return 'Sem data';
  const [year, month, day] = date.split('-');
  return `${day}/${month}/${year}`;
}

export function formatMonth(month) {
  return new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric', timeZone: 'UTC' })
    .format(new Date(`${month}-01T00:00:00Z`));
}

export function shiftMonth(month, step) {
  const [year, number] = month.split('-').map(Number);
  const index = (year - 1) * 12 + number - 1 + step;
  if (index < 0 || index >= 9999 * 12) return month;
  return `${String(Math.floor(index / 12) + 1).padStart(4, '0')}-${String(index % 12 + 1).padStart(2, '0')}`;
}
