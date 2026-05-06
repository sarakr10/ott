from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import Response, StreamingResponse
import httpx
from urllib.parse import urljoin, quote

app = FastAPI()

# 👇 VAŽNO: koristimo relativni proxy path
def make_proxy_url(original_url: str) -> str:
    return "/proxy?url=" + quote(original_url, safe="")

def rewrite_m3u8(manifest_text: str, base_url: str) -> str:
    base = base_url.rsplit("/", 1)[0] + "/"
    lines = []

    for line in manifest_text.splitlines():
        stripped = line.strip()

        if stripped == "":
            lines.append(line)
            continue

        # 👇 rewrite URI="..." (KEY, MAP, itd)
        if 'URI="' in line:
            start = line.find('URI="') + len('URI="')
            end = line.find('"', start)

            if end != -1:
                uri = line[start:end]
                absolute = urljoin(base, uri)
                proxied = make_proxy_url(absolute)
                line = line[:start] + proxied + line[end:]

            lines.append(line)
            continue

        # komentari ostaju
        if stripped.startswith("#"):
            lines.append(line)
            continue

        # 👇 segment ili child playlist
        absolute = urljoin(base, stripped)
        lines.append(make_proxy_url(absolute))

    return "\n".join(lines) + "\n"


@app.get("/proxy")
async def proxy(url: str, request: Request):
    if not url:
        raise HTTPException(status_code=400, detail="Missing url")

    async with httpx.AsyncClient(follow_redirects=True, timeout=None) as client:
        resp = await client.get(url)

    content_type = resp.headers.get("content-type", "")

    # 👇 da li je m3u8
    is_m3u8 = (
        url.endswith(".m3u8")
        or "mpegurl" in content_type.lower()
        or "#EXTM3U" in resp.text[:100]
    )

    # =====================
    # 🎯 M3U8 (REWRITE)
    # =====================
    if is_m3u8:
        rewritten = rewrite_m3u8(resp.text, url)

        return Response(
            content=rewritten,
            media_type="application/vnd.apple.mpegurl",
            headers={
                "Cache-Control": "no-cache",
                "Access-Control-Allow-Origin": "*"
            }
        )

    # =====================
    # 🎯 SEGMENTI (.ts, .mp4...)
    # =====================
    async def stream():
        async with httpx.AsyncClient(follow_redirects=True, timeout=None) as client:
            async with client.stream("GET", url) as upstream:
                async for chunk in upstream.aiter_bytes():
                    yield chunk

    headers = {}
    for k, v in resp.headers.items():
        if k.lower() not in (
            "content-length",
            "transfer-encoding",
            "connection",
            "content-encoding",
        ):
            headers[k] = v

    return StreamingResponse(
        stream(),
        media_type=content_type or "application/octet-stream",
        headers=headers
    )


@app.get("/")
async def root():
    return {"status": "OK"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
