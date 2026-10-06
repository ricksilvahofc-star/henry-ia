const { mlFetch, sendJson } = require("../../lib/ml");

module.exports = async (req, res) => {
  try {
    const id = req.query?.id || (req.url.match(/items\/([^/?]+)/) || [])[1];
    if (!id) return sendJson(res, 400, {error:"missing_item_id"});

    if (req.method === "GET") {
      const r = await mlFetch(req, res, `/items/${encodeURIComponent(id)}`);
      return sendJson(res, r.status, r.data);
    }

    if (req.method === "PUT") {
      let raw = "";
      for await (const chunk of req) raw += chunk;
      const body = JSON.parse(raw || "{}");

      const r = await mlFetch(req, res, `/items/${encodeURIComponent(id)}`, {
        method:"PUT",
        headers:{"Content-Type":"application/json","Accept":"application/json"},
        body:JSON.stringify(body)
      });
      return sendJson(res, r.status, r.data);
    }

    res.setHeader("Allow","GET, PUT");
    sendJson(res, 405, {error:"method_not_allowed"});
  } catch (e) {
    sendJson(res, e.status || 500, {error:e.message});
  }
};
