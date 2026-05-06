from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import Response, StreamingResponse
import httpx
from urllib.parse import urljoin, quote

app = FastAPI()

PROXY_PREFIX = "http://127.0.0.1:8080/proxy?url="

def proxify_url(original_url: str) -> str:
    return PROXY_PREFIX + quote(original_url, safe="")

def rewrite_m3u8(text: str, base_url: str) -> str:
    lines = []

    for line in text.splitlines():
        stripped = line.strip()

        if stripped == "":
            lines.append(line)
            continue

        # URI="something" unutar EXT-X-KEY, EXT-X-MAP itd.
        if 'URI="' in line:
            before, rest = line.split('URI="', 1)
            uri, after = rest.split('"', 1)
            absolute = urljoin(base_url, uri)
            line = before + 'URI="' + proxify_url(absolute) + '"' + after
            lines.append(line)
            continue

        # komentari ostaju isti
        if stripped.startswith("#"):
            lines.append(line)
            continue

        # obične linije su playlist ili segment URL
        absolute = urljoin(base_url, stripped)
        lines.append(proxify_url(absolute))

    return "\n".join(lines) + "\n"

@app.get("/proxy")
async def proxy(url: str, request: Request):
    if not url:
        raise HTTPException(status_code=400, detail="Missing url parameter")

    headers = dict(request.headers)
    headers.pop("host", None)
    headers.pop("content-length", None)

    async with httpx.AsyncClient(follow_redirects=True, timeout=None) as client:
        r = await client.get(url, headers=headers)

    content_type = r.headers.get("content-type", "")

    # Ako je HLS manifest, prepravi ga
    if (
        url.endswith(".m3u8")
        or "mpegurl" in content_type
        or "application/vnd.apple.mpegurl" in content_type
    ):
        original_text = r.text
        rewritten = rewrite_m3u8(original_text, url)

        return Response(
            content=rewritten,
            media_type="application/vnd.apple.mpegurl"
        )

    # Ako nije manifest, streamuj segment normalno
    async def stream():
        async with httpx.AsyncClient(follow_redirects=True, timeout=None) as client:
            async with client.stream("GET", url, headers=headers) as upstream:
                async for chunk in upstream.aiter_bytes():
                    yield chunk

    return StreamingResponse(
        stream(),
        media_type=content_type or "application/octet-stream"
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
