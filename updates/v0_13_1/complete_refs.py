from pathlib import Path
import base64, urllib.request, io, math, struct
from PIL import Image
p=Path("android-app/app/src/main/assets/references_v7.txt")
rows=p.read_text().splitlines()
def append(idx,names,raw):
    n=rows[idx].split("|")
    assert not set(n)&set(names) and len(raw)==20*len(names)
    rows[idx]="|".join(n+names)
    rows[idx+1]=base64.b64encode(base64.b64decode(rows[idx+1],validate=True)+raw).decode()
append(0,"Arlott|Atlas|Aulus|Aurora".split("|"),base64.b64decode("rPVy+vaWVK1cO1lrbIdvL1qhiKQLTa7A7vfMMy1umB1VfkA3rVZLzChYW1FY1pahekA0fEE2r1dAsmRNe+4+NXl6M6WWjKzRwdW3psKnlrM="))
append(2,"Blade of Despair|Blade of the Heptaseas|Blood Wings|Book of Sages|Exotic Veil|Expert Gloves|Feather of Heaven|Flask of the Oasis|Regular Spear|Rogue Meteor|Rose Gold Meteor|Sea Halberd|Wishing Lantern".split("|"),base64.b64decode("+NrklrIqUiJPdCqFk1iAln9ihUaAQKCwy+TxdWV/oy1LjUY7g19gdZCSIhIKjZRyrWldWR4/pEpGfy8/s8W05PS81fKUgW9+YldJPENxXVpF29Gl1sqSoW1IS0soNzkvOy0fMujk4+ODw22MXWRsX01QTFqHNkll9e04iUTK3Ph5RH1VWJiQa6puYJfQ8nTovMx0mG9nKF5lK1BbImNvIVOg2M7g8urtfWxfPiYfTjo1aU9CsOLgpfa6aOR7WF5aNTk9FRxNHCTl4vLlxKRYYINdMXxeTW5tTnFSLrFk+du6powFUDZeh3F2f2KOXE1jy4pevHj75dtKPGlVTI1ISXyJa60="))
base="https://raw.githubusercontent.com/achedon12/mlbb/d0d96e8ecdd62553bfee83a7f75f8dbac4ad86c0/public/visuels/"
def descriptor(path):
    # Public artwork is read into memory only. Store only 20-byte non-reconstructive descriptor.
    with urllib.request.urlopen(base+path,timeout=25) as response:
        raw=response.read(1000001)
    assert len(raw)<=1000000
    image=Image.open(io.BytesIO(raw)).convert("RGB")
    w,h=image.size
    assert 20<=min(w,h)<=max(w,h)<=2000
    half=int(min(w,h)*.375)
    crop=image.crop((w//2-half,h//2-half,w//2+half,h//2+half))
    # Android-style center-mapped bilinear sample.
    def lum(x,y):
        u=(x+.5)*crop.width/9-.5;v=(y+.5)*crop.height/8-.5
        x0=max(0,int(math.floor(u)));y0=max(0,int(math.floor(v)))
        x1=min(x0+1,crop.width-1);y1=min(y0+1,crop.height-1)
        a=u-math.floor(u);b=v-math.floor(v)
        def L(px,py):
            r,g,b=crop.getpixel((px,py));return .299*r+.587*g+.114*b
        return (1-a)*(1-b)*L(x0,y0)+a*(1-b)*L(x1,y0)+(1-a)*b*L(x0,y1)+a*b*L(x1,y1)
    bits=bytes(sum((1<<(7-x)) if lum(x+1,y)>lum(x,y) else 0 for x in range(8)) for y in range(8))
    rgb=[]
    for y in (0,half):
        for x in (0,half):
            pixels=list(crop.crop((x,y,x+half,y+half)).getdata())
            rgb.extend(sum(v[i] for v in pixels)//len(pixels) for i in range(3))
    return bits+bytes(rgb)
heroes="Aamon|Akai|Aldous|Alice|Alpha|Alucard|Angela|Argus".split("|")
append(0,heroes,b"".join(descriptor("heros/"+n.lower()+"/icone.webp") for n in heroes))
append(2,["Twilight Armor"],descriptor("objets/twilight-armor.webp"))
# Keep old icon alias for current shield; strategy canonicalizes it.
catalog=[x.split("\t") for x in Path("android-app/app/src/main/assets/strategy13.tsv").read_text().splitlines()]
assert set(rows[0].split("|"))=={x[1] for x in catalog if x[0]=="H"}
assert len(rows[2].split("|"))==90
assert len(base64.b64decode(rows[1]))==133*20 and len(base64.b64decode(rows[3]))==90*20
p.write_text("\n".join(rows)+"\n")
print("STATIC REFERENCE COVERAGE: 133 heroes; 90 shop item labels (including Athena/Valkyrie alias); NOT LIVE ACCURACY")
