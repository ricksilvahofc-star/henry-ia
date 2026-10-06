const crypto = require("crypto");

const ML_AUTH = "https://auth.mercadolivre.com.br/authorization";
const ML_API = "https://api.mercadolibre.com";
const COOKIE = "ml_session";

function keyBytes() {
  const raw = process.env.SESSION_KEY || "";
  if (!raw) throw new Error("SESSION_KEY não configurada");
  return crypto.createHash("sha256").update(raw).digest();
}

function seal(value) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv("aes-256-gcm", keyBytes(), iv);
  const encrypted = Buffer.concat([cipher.update(JSON.stringify(value), "utf8"), cipher.final()]);
  const tag = cipher.getAuthTag();
  return Buffer.concat([iv, tag, encrypted]).toString("base64url");
}

function open(value) {
  const buf = Buffer.from(value, "base64url");
  const iv = buf.subarray(0, 12);
  const tag = buf.subarray(12, 28);
  const encrypted = buf.subarray(28);
  const decipher = crypto.createDecipheriv("aes-256-gcm", keyBytes(), iv);
  decipher.setAuthTag(tag);
  return JSON.parse(Buffer.concat([decipher.update(encrypted), decipher.final()]).toString("utf8"));
}

function setCookie(res, value, maxAge = 60 * 60 * 24 * 30, name = COOKIE) {
  res.setHeader("Set-Cookie", `${name}=${value}; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=${maxAge}`);
}

function clearCookie(res, name = COOKIE) {
  res.setHeader("Set-Cookie", `${name}=; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=0`);
}

function readCookie(req) {
  const header = req.headers.cookie || "";
  const found = header.split(";").map(x => x.trim()).find(x => x.startsWith(COOKIE + "="));
  return found ? found.slice(COOKIE.length + 1) : null;
}

function getSession(req) {
  const raw = readCookie(req);
  if (!raw) return null;
  try { return open(raw); } catch { return null; }
}

async function tokenFromSession(req, res) {
  const session = getSession(req);
  if (!session) return null;

  if (session.expires_at && Date.now() < session.expires_at - 60_000) {
    return session.access_token;
  }

  const body = new URLSearchParams({
    grant_type: "refresh_token",
    client_id: process.env.ML_CLIENT_ID,
    client_secret: process.env.ML_CLIENT_SECRET,
    refresh_token: session.refresh_token
  });

  const r = await fetch(`${ML_API}/oauth/token`, {
    method:"POST",
    headers:{"Content-Type":"application/x-www-form-urlencoded","Accept":"application/json"},
    body
  });
  const data = await r.json();

  if (!r.ok || !data.access_token || !data.refresh_token) {
    clearCookie(res);
    return null;
  }

  const next = {
    access_token: data.access_token,
    refresh_token: data.refresh_token,
    expires_at: Date.now() + Number(data.expires_in || 21600) * 1000
  };
  setCookie(res, seal(next));
  return next.access_token;
}

async function mlFetch(req, res, path, options = {}) {
  const token = await tokenFromSession(req, res);
  if (!token) {
    const err = new Error("Não conectado ao Mercado Livre");
    err.status = 401;
    throw err;
  }

  const headers = {
    "Authorization": `Bearer ${token}`,
    ...(options.headers || {})
  };

  const r = await fetch(`${ML_API}${path}`, {...options, headers});
  const text = await r.text();
  let data;
  try { data = JSON.parse(text); } catch { data = {raw:text}; }

  return {status:r.status, data};
}

function sendJson(res, status, data) {
  res.statusCode = status;
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.end(JSON.stringify(data));
}

module.exports = {
  ML_AUTH, ML_API, COOKIE, seal, setCookie, clearCookie, getSession,
  tokenFromSession, mlFetch, sendJson
};
