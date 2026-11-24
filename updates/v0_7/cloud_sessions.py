"""Opt-in short-lived cloud session credentials with process-local hard quotas.
Production requires HTTPS, a trusted reverse proxy and shared limiter for multiple workers.
Disabled by default. Never expose provider or administrator credentials.
"""
import os, time, secrets, threading
from collections import defaultdict
from fastapi import HTTPException
LOCK=threading.Lock()
TOKENS={}
ISSUANCE={}
day=int(time.time()//86400)
issued=0
used=0

def enabled():
    return os.getenv("MLBB_PUBLIC_SESSION_ENABLED","0")=="1"

def _reset(now):
    global day,issued,used
    d=int(now//86400)
    if d!=day:
        day=d;issued=0;used=0;ISSUANCE.clear()
    for token,entry in list(TOKENS.items()):
        if entry[0]<=now: TOKENS.pop(token,None)

def issue(ip):
    global issued
    if not enabled(): raise HTTPException(503,"Sessões automáticas desativadas")
    now=time.time()
    with LOCK:
        _reset(now)
        daily=min(100,max(1,int(os.getenv("MLBB_MAX_SESSIONS_DAY","5"))))
        if issued>=daily or ISSUANCE.get(ip,0)>=2:
            raise HTTPException(429,"Limite de sessões")
        token=secrets.token_urlsafe(32)
        TOKENS[token]=[now+7200,0,0.0]
        ISSUANCE[ip]=ISSUANCE.get(ip,0)+1;issued+=1
        return {"token":token,"expires_in":7200}

def authorize(header):
    global used
    if not enabled() or not header or not header.startswith("Bearer "): return False
    token=header[7:]
    now=time.time()
    with LOCK:
        _reset(now)
        record=TOKENS.get(token)
        if record is None:return False
        per=min(180,max(1,int(os.getenv("MLBB_MAX_REQUESTS_SESSION","100"))))
        total=min(1000,max(1,int(os.getenv("MLBB_MAX_REQUESTS_DAY","200"))))
        if record[1]>=per or used>=total or now-record[2]<10:
            raise HTTPException(429,"Limite de análises")
        record[1]+=1;record[2]=now;used+=1
        return True
