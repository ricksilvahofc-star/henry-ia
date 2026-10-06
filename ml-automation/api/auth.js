const crypto = require("crypto");
const { ML_AUTH } = require("../lib/ml");

function makeState() {
  const payload = Buffer.from(JSON.stringify({
    nonce: crypto.randomBytes(24).toString("hex"),
    created_at: Date.now()
  })).toString("base64url");
  const sig = crypto.createHmac("sha256", process.env.SESSION_KEY).update(payload).digest("base64url");
  return payload + "." + sig;
}

module.exports = async (req, res) => {
  if (!process.env.ML_CLIENT_ID || !process.env.ML_CLIENT_SECRET || !process.env.ML_REDIRECT_URI || !process.env.SESSION_KEY) {
    res.statusCode = 500;
    return res.end("Configure ML_CLIENT_ID, ML_CLIENT_SECRET, ML_REDIRECT_URI e SESSION_KEY na Vercel.");
  }

  const state = makeState();

  const params = new URLSearchParams({
    response_type:"code",
    client_id:process.env.ML_CLIENT_ID,
    redirect_uri:process.env.ML_REDIRECT_URI,
    state,
    scope:"offline_access read write"
  });

  res.statusCode = 302;
  res.setHeader("Location", `${ML_AUTH}?${params.toString()}`);
  res.end();
};
