
# Local HLS Proxy Server (Python / FastAPI)

This package contains a **Python-based HLS proxy server** that serves remote HLS playlists via `http://localhost`, allowing video players to load them as if they were local.

Supported playlists:

- http://cdnapi.kaltura.com/p/1878761/sp/187876100/playManifest/entryId/1_2xvajead/flavorIds/1_tl01409m,1_kptb3ez8,1_re3akioy,1_wuylsxwp/format/applehttp/protocol/http/a.m3u8
- http://cdnbakmi.kaltura.com/p/243342/sp/24334200/playManifest/entryId/0_uka1msg4/flavorIds/1_vqhfu6uy,1_80sohj7p/format/applehttp/protocol/http/a.m3u8
- http://playertest.longtailvideo.com/adaptive/captions/playlist.m3u8
- http://sample.vodobox.net/skate_phantom_flex_4k/skate_phantom_flex_4k.m3u8
- https://content.uplynk.com/channel/3353345690554b0e8de31bcd6b73fc37.m3u8

---

## Requirements

- Python 3.9+
- pip

---

## Installation

```bash
pip install fastapi uvicorn httpx
```

---

## Running the Proxy

```bash
python proxy.py
```

The proxy will listen on:

```
http://localhost:8080
```

---

## How to Use

Use the following localhost URLs in any HLS-compatible player (VLC, ffplay, Safari, hls.js, etc.):

```text
http://localhost:8080/proxy?url=http://cdnapi.kaltura.com/p/1878761/sp/187876100/playManifest/entryId/1_2xvajead/flavorIds/1_tl01409m,1_kptb3ez8,1_re3akioy,1_wuylsxwp/format/applehttp/protocol/http/a.m3u8

http://localhost:8080/proxy?url=http://cdnbakmi.kaltura.com/p/243342/sp/24334200/playManifest/entryId/0_uka1msg4/flavorIds/1_vqhfu6uy,1_80sohj7p/format/applehttp/protocol/http/a.m3u8

http://localhost:8080/proxy?url=http://playertest.longtailvideo.com/adaptive/captions/playlist.m3u8

http://localhost:8080/proxy?url=http://sample.vodobox.net/skate_phantom_flex_4k/skate_phantom_flex_4k.m3u8

http://localhost:8080/proxy?url=https://content.uplynk.com/channel/3353345690554b0e8de31bcd6b73fc37.m3u8
```

All nested playlists and media segments are automatically proxied.

---

## Testing

### VLC
Media → Open Network Stream → paste a localhost URL

### ffplay
```bash
ffplay "http://localhost:8080/proxy?url=https://content.uplynk.com/channel/3353345690554b0e8de31bcd6b73fc37.m3u8"
```

---

## Notes

- This proxy does **not** bypass DRM, authentication, or geo-restrictions
- Intended for local testing and development
- Do not expose publicly without security controls

---

## License

Provided as-is, for testing and development purposes only.
