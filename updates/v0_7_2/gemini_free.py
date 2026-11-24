"""Gemini free-project adapter. No paid fallback in production. No request/secret logging."""
import base64,json,os,time,threading
import httpx
MODEL="gemini-3.5-flash-lite"
_lock=threading.Lock()
_day=-1
_calls=0
_block_until=0.0
def ready():
    return (os.getenv("MLBB_VISION_ENABLED")=="1"
        and os.getenv("MLBB_FREE_PROJECT_CONFIRMED")=="1"
        and bool(os.getenv("GEMINI_API_KEY","")))
def reserve():
    global _day,_calls,_block_until
    with _lock:
        now=time.time()
        day=int(now//86400)
        if day!=_day:
            _day=day;_calls=0
        maximum=min(20,max(1,int(os.getenv("MLBB_FREE_CALLS_DAY","10"))))
        if now<_block_until or _calls>=maximum:
            raise RuntimeError("Cota de visão gratuita esgotada; análise local")
        _calls+=1
async def read(image,content_type,prompt,schema,model_type):
    global _block_until
    if not ready():raise RuntimeError("Projeto gratuito não confirmado")
    if content_type not in ("image/jpeg","image/png") or not 0<len(image)<=2_500_000:
        raise ValueError("Imagem inválida")
    reserve()
    payload={"contents":[{"role":"user","parts":[{"text":prompt},
         {"inlineData":{"mimeType":content_type,"data":base64.b64encode(image).decode("ascii")}}]}],
         "generationConfig":{"responseMimeType":"application/json","responseJsonSchema":schema,
                             "maxOutputTokens":3000,"temperature":0.1}}
    async with httpx.AsyncClient(timeout=httpx.Timeout(30,connect=5),follow_redirects=False) as client:
        r=await client.post("https://generativelanguage.googleapis.com/v1beta/models/"+MODEL+":generateContent",
              headers={"x-goog-api-key":os.environ["GEMINI_API_KEY"]},json=payload)
        if r.status_code==429:
            with _lock:_block_until=time.time()+3600
            raise RuntimeError("Cota do provedor esgotada")
        if r.status_code!=200:raise RuntimeError("Provedor visual indisponível: HTTP "+str(r.status_code))
        body=r.json()
    candidates=body.get("candidates",[])
    if not candidates or candidates[0].get("finishReason") not in (None,"STOP"):
        raise ValueError("Observação incompleta")
    text="".join(p.get("text","") for p in candidates[0].get("content",{}).get("parts",[]) if not p.get("thought"))
    return model_type.model_validate(json.loads(text))
