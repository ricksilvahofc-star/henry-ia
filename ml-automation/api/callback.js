const { ML_API, seal, setCookie, clearCookie, sendJson } = require("../lib/ml");

function readCookie(req, name) {
  const header = req.headers.cookie || "";
  const found = header.split(";").map(x=>x.trim()).find(x=>x.startsWith(name + "="));
  return found ? found.slice(name.length + 1) : null;
}

module.exports = async (req, res) => {
  try {
    const url = new URL(req.url, "https://example.invalid");
    const code = url.searchParams.get("code");
    const state = url.searchParams.get("state");
    const error = url.searchParams.get("error");

    if (error) return sendJson(res, 400, {error, message:"Autorização cancelada no Mercado Livre."});
    if (!code) return sendJson(res, 400, {error:"missing_code"});

    const stateCookie = readCookie(req, "ml_oauth_state");
    if (!state || !stateCookie) return sendJson(res, 400, {error:"invalid_state"});
    let saved;
    try { saved = JSON.parse(Buffer.from(stateCookie, "base64url").toString("utf8")); } catch { saved = null; }
    if (!saved || saved.state !== state || Date.now() - Number(saved.created_at || 0) > 10 * 60 * 1000) {
      return sendJson(res, 400, {error:"invalid_state"});
    }

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

    if (!r.ok || !data.access_token || !data.refresh_token) {
      return sendJson(res, r.status || 400, {error:"token_exchange_failed", details:data});
    }

    const session = {
      access_token:data.access_token,
      refresh_token:data.refresh_token,
      expires_at:Date.now() + Number(data.expires_in || 21600) * 1000
    };
    setCookie(res, seal(session), 60*60*24*30);\n    clearCookie(res, "ml_oauth_state");

    res.statusCode = 302;
    res.setHeader("Location", "/?connected=1");
    res.end();
  } catch (e) {
    sendJson(res, 500, {error:"callback_failed", message:e.message});
  }
};
