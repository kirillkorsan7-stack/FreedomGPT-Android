const http = require("node:http");

const PORT = Number(process.env.PORT || 8080);
const API_KEY = process.env.OPENAI_API_KEY;
const MODEL = process.env.OPENAI_MODEL || "gpt-5.5";
const WINDOW_MS = 60_000;
const MAX_REQUESTS_PER_WINDOW = 20;
const clients = new Map();

function send(res, status, body) {
  res.writeHead(status, { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" });
  res.end(JSON.stringify(body));
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", chunk => {
      raw += chunk;
      if (raw.length > 64_000) {
        reject(new Error("Request too large"));
        req.destroy();
      }
    });
    req.on("end", () => {
      try { resolve(JSON.parse(raw || "{}")); } catch { reject(new Error("Invalid JSON")); }
    });
    req.on("error", reject);
  });
}

const server = http.createServer(async (req, res) => {
  if (req.method === "GET" && req.url === "/health") return send(res, 200, { ok: true });
  if (req.method !== "POST" || req.url !== "/chat") return send(res, 404, { error: "Not found" });
  if (!API_KEY) return send(res, 503, { error: "Server is missing OPENAI_API_KEY." });

  const ip = req.headers["x-forwarded-for"]?.split(",")[0]?.trim() || req.socket.remoteAddress || "unknown";
  const now = Date.now();
  const previous = clients.get(ip) || { start: now, count: 0 };
  if (now - previous.start > WINDOW_MS) { previous.start = now; previous.count = 0; }
  previous.count += 1;
  clients.set(ip, previous);
  if (previous.count > MAX_REQUESTS_PER_WINDOW) return send(res, 429, { error: "Too many requests. Try again in a minute." });

  try {
    const body = await readJson(req);
    if (!Array.isArray(body.messages) || body.messages.length === 0 || body.messages.length > 30) {
      return send(res, 400, { error: "messages must be an array containing 1 to 30 items." });
    }
    const input = body.messages.map(item => {
      if (!["user", "assistant"].includes(item.role) || typeof item.content !== "string" || item.content.length > 12_000) {
        throw new Error("Invalid message format.");
      }
      return { role: item.role, content: item.content };
    });
    const upstream = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: { "Authorization": `Bearer ${API_KEY}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        model: MODEL,
        instructions: "You are FreedomGPT, a helpful and honest assistant. Reply in the user's language. Be direct, clear, and respectful. Do not claim to have performed actions you have not performed.",
        input,
        max_output_tokens: 1200
      })
    });
    const data = await upstream.json();
    if (!upstream.ok) {
      console.error("AI provider error:", data.error?.message || upstream.status);
      return send(res, 502, { error: "AI provider request failed. Check server configuration and provider account." });
    }
    const reply = (data.output || [])
      .filter(item => item.type === "message")
      .flatMap(item => item.content || [])
      .filter(part => part.type === "output_text")
      .map(part => part.text)
      .join("\n")
      .trim();
    if (!reply) return send(res, 502, { error: "AI provider returned no text." });
    return send(res, 200, { reply });
  } catch (error) {
    console.error(error.message);
    return send(res, 400, { error: error.message === "Request too large" ? error.message : "Invalid request." });
  }
});

server.listen(PORT, () => console.log(`FreedomGPT backend listening on ${PORT}`));
