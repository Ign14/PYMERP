export function normalizeRut(value: string): string {
  const cleaned = value.replace(/\./g, "").replace(/-/g, "").trim().toUpperCase();
  if (cleaned.length <= 1) {
    return cleaned;
  }
  const body = cleaned.slice(0, -1);
  const dv = cleaned.slice(-1);
  return `${body}-${dv}`;
}

export function isValidRut(value: string): boolean {
  if (!value) {
    return false;
  }
  const cleaned = value.replace(/\./g, "").replace(/-/g, "").trim().toUpperCase();
  if (!/^\d{7,8}[0-9K]$/.test(cleaned)) {
    return false;
  }
  const body = cleaned.slice(0, -1);
  const dv = cleaned.slice(-1);
  return calculateCheckDigit(body) === dv;
}

function calculateCheckDigit(body: string): string {
  let sum = 0;
  let factor = 2;
  for (let i = body.length - 1; i >= 0; i -= 1) {
    sum += parseInt(body[i], 10) * factor;
    factor = factor === 7 ? 2 : factor + 1;
  }
  const modulus = 11 - (sum % 11);
  if (modulus === 11) {
    return "0";
  }
  if (modulus === 10) {
    return "K";
  }
  return String(modulus);
}
