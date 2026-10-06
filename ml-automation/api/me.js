const { mlFetch, sendJson } = require("../lib/ml");

module.exports = async (req, res) => {
  try {
    const r = await mlFetch(req, res, "/users/me");
    sendJson(res, r.status, r.data);
  } catch (e) {
    sendJson(res, e.status || 500, {error:e.message});
  }
};
