from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import Response, StreamingResponse
import httpx
from urllib.parse import urljoin, quote

app = FastAPI()

HOST = "127.0.0.1"
PORT = 8080

def make_proxy_url(original_url: str) -> str:
    return f"http://{HOST}:{PORT}/proxy?url={quote(original_url, safe='')}"

def rewrite_m3u8(manifest_text: str, base_url: str) -> str:
    rewritten_lines = []

    for line in manifest_text.splitlines():
        stripped = line.strip()

        if stripped == "":
            rewritten_lines.append(line)
            continue

        # Rewrite URI="..." inside tags like EXT-X-KEY or EXT-X-MAP
        if 'URI="' in line:
            start = line.find('URI="') + len('URI="')
            end = line.find('"', start)

            if end != -1:
                uri = line[start:end]
                absolute_uri = urljoin(base_url, uri)
                proxied_uri = make_proxy_url(absolute_uri)
                line = line[:start] + proxied_uri + line[end:]

            rewritten_lines.append(line)
            continue

        # Comments / HLS tags stay unchanged
        if stripped.startswith("#"):
            rewritten_lines.append(line)
            continue

        # Playlist or media segment URL
        absolute_url = urljoin(base_url, stripped)
        rewritten_lines.append(make_proxy_url(absolute_url))

    return "\n".join(rewritten_lines) + "\n"


@app.get("/proxy")
async def proxy(url: str, request: Request):
    if not url:
        raise HTTPException(status_code=400, detail="Missing url parameter")

    headers = dict(request.headers)
    headers.pop("host", None)
    headers.pop("content-length", None)
    headers.pop("accept-encoding", None)

    async with httpx.AsyncClient(follow_redirects=True, timeout=None) as client:
        response = await client.get(url, headers=headers)

    content_type = response.headers.get("content-type", "")

    is_manifest = (
        url.lower().endswith(".m3u8")
        or "mpegurl" in content_type.lower()
        or "application/vnd.apple.mpegurl" in content_type.lower()
        or "#EXTM3U" in response.text[:100]
    )

    if is_manifest:
        rewritten_manifest = rewrite_m3u8(response.text, url)

        return Response(
            content=rewritten_manifest,
            media_type="application/vnd.apple.mpegurl",
            headers={
                "Access-Control-Allow-Origin": "*",
                "Cache-Control": "no-cache"
            }
        )

    async def stream():
        async with httpx.AsyncClient(follow_redirects=True, timeout=None) as client:
            async with client.stream(
                "GET",
                url,
                headers=headers
            ) as upstream:
                async for chunk in upstream.aiter_bytes():
                    yield chunk

    safe_headers = {}
    for k, v in response.headers.items():
        if k.lower() not in (
            "content-length",
            "content-encoding",
            "transfer-encoding",
            "connection",
        ):
            safe_headers[k] = v

    return StreamingResponse(
        stream(),
        headers=safe_headers,
        media_type=content_type or "application/octet-stream"
    )


@app.get("/")
async def root():
    return {"status": "HLS proxy running"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=PORT)
