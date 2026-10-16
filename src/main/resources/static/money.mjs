export function parseAmountToCents(value) {
  let normalized = String(value).trim();
  if (/^\d{1,3}(?:\.\d{3})+,\d{1,2}$/.test(normalized)) normalized = normalized.replace(/\./g, '');
  if (!/^\d+(?:[,.]\d{1,2})?$/.test(normalized)) {
    throw new Error('Informe um valor como 12,50 ou 1.250,00.');
  }
  const [whole, fraction = ''] = normalized.replace(',', '.').split('.');
  const cents = BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'));
  if (cents <= 0n) throw new Error('O valor precisa ser maior que zero.');
  if (cents > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error('O valor informado é muito alto.');
  return Number(cents);
}

export function formatCurrencyInput(value) {
  const digits = String(value).replace(/\D/g, '');
  if (!digits) return '';
  const cents = BigInt(digits).toString().padStart(3, '0');
  const whole = cents.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${whole},${cents.slice(-2)}`;
}

export function formatCurrencyPaste(value) {
  const text = String(value).trim().replace(/^R\$\s*/, '');
  if (/^\d+$/.test(text)) return formatCurrencyInput(text);
  return formatCurrencyInput(String(parseAmountToCents(text)));
}

export function formatMoney(cents) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(cents / 100);
}
