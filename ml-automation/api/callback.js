const crypto = require("crypto");
const { ML_API, seal, setCookie, sendJson } = require("../lib/ml");

function validState(state) {
  try {
    const parts = String(state || "").split(".");
    if (parts.length !== 2) return false;
    const [payload, sig] = parts;
    const expected = crypto.createHmac("sha256", process.env.SESSION_KEY).update(payload).digest("base64url");
    if (sig.length !== expected.length) return false;
    if (!crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(expected))) return false;
    const saved = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
    return saved && Date.now() - Number(saved.created_at || 0) <= 10 * 60 * 1000;
  } catch {
    return false;
  }
}

module.exports = async (req, res) => {
  try {
    const url = new URL(req.url, "https://example.invalid");
    const code = url.searchParams.get("code");
    const state = url.searchParams.get("state");
    const error = url.searchParams.get("error");

    if (error) return sendJson(res, 400, {error, message:"Autorização cancelada no Mercado Livre."});
    if (!code) return sendJson(res, 400, {error:"missing_code"});
    if (!validState(state)) return sendJson(res, 400, {error:"invalid_state"});

    const body = new URLSearchParams({
      grant_type:"authorization_code",
      client_id:process.env.ML_CLIENT_ID,
      client_secret:process.env.ML_CLIENT_SECRET,
      code,
      redirect_uri:process.env.ML_REDIRECT_URI
    });

    const r = await fetch(`${ML_API}/oauth/token`, {
      method:"POST",
      headers:{"Content-Type":"application/x-www-form-urlencoded","Accept":"application/json"},
      body
    });
    const data = await r.json();

    if (!r.ok || !data.access_token) {
      return sendJson(res, r.status || 400, {error:"token_exchange_failed", details:data});
    }

    const session = {
      access_token:data.access_token,
      refresh_token:data.refresh_token || null,
      expires_at:Date.now() + Number(data.expires_in || 21600) * 1000
    };
    setCookie(res, seal(session), 60*60*24*30);

    res.statusCode = 302;
    res.setHeader("Location", "/?connected=1");
    res.end();
  } catch (e) {
    sendJson(res, 500, {error:"callback_failed", message:e.message});
  }
};
