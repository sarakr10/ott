from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import StreamingResponse
import httpx

app = FastAPI()

@app.get("/proxy")
async def proxy(url: str, request: Request):
    if not url:
        raise HTTPException(status_code=400, detail="Missing url parameter")

    headers = dict(request.headers)
    headers.pop("host", None)
    headers.pop("content-length", None)

    async def stream():
        async with httpx.AsyncClient(follow_redirects=True) as client:
            async with client.stream(
                method=request.method,
                url=url,
                headers=headers,
                timeout=None
            ) as upstream:
                async for chunk in upstream.aiter_bytes():
                    yield chunk

    async with httpx.AsyncClient(follow_redirects=True) as client:
        upstream_headers = {}
        async with client.stream("GET", url, timeout=None) as r:
            for k, v in r.headers.items():
                if k.lower() not in (
                    "content-length",
                    "content-encoding",
                    "transfer-encoding",
                    "connection",
                ):
                    upstream_headers[k] = v

    return StreamingResponse(
        stream(),
        headers=upstream_headers,
        media_type=upstream_headers.get("content-type")
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
