from pathlib import Path
import base64,json
p=Path("android-app/app/src/main/assets/references_v7.txt")
rows=p.read_text().splitlines()
names="Lolita|Lukas|Lunox|Luo Yi|Lylia|Marcel|Martis|Masha|Mathilda|Melissa|Minotaur|Minsitthar|Miya|Moskov|Nana|Natalia|Natan|Nolan|Novaria|Obsidia|Sun|Suyou|Terizla|Thamuz|Yve|Zetian|Zhask|Zhuxin|Zilong".split("|")
new=base64.b64decode("4Xlxs7Ohs7yLametf2eyhYTBmYScDkbQEg6GRsOUgbOAedeojsWGaQgSYGCAkJDYlHlsOy41xqKKnnZ02ODw8fDw6OiHZZGOV6VqUmdtUGaAgODz8Mzs7HpEf0QwRI1oiqh+mMDAweD4fn48VFh3KDdgc217rpqi7xg87PT29PRzYYaPfqhzW4CIcqDj6fmZPOzm/m1aRKZ6YVI2MqF5bVJs5LjZ3dz4YD5JWDZCoGVpo2Zp8NDY7MOAguLFmIyloo/BkIWFWVYwedjS5vddfVojLmAmMVEkLn8tOHNTm/ImPL2Ll2tPp3xUsHdi2a2HzJwnczY8nHiKiKqPe5ufi7m9n8Dsx1nc/Lz8eFpflWVgpWZon7XA38zccOJjYeCYfSRgh0l7tIuafVFqY/Pi8Pjs/PpJNkpmPVBdNEaAZYOHn48Xk4+ODJ11ZqBiTc6mlMJ0TmRjwcGJ83d8wZaHrINvmnNqqnt9LmoeWnr42PGaecmfec6klc+vqtoOTmZgsrDw+YNyknRcWpd6oJeKnmJJzJhw1FgAs3FUtmxWvo6Js31wuMziYng6njyKfZR2hZSOgZO1j5WMOHDYwFBgcEM7OjxFOFd0b01US8jCk5uzI2VNajw8dkczxZNTuYI/+HzM8qKwxG1eP1RvQn9oS4lmSJegoEBAwOJweLVyZjoiK8qFbqNpVbK0oKTo8LDxX16DX050YVZ+YU5whOx8fPx4ePoxIzROP0hVOESpgpnns83n7uRt63JMS4SAgZ+AdruTkA==",validate=True)
old=base64.b64decode(rows[1],validate=True)
existing=rows[0].split("|")
assert len(new)==20*len(names)==580
assert len(existing)==92 and len(old)==1840
assert not set(existing)&set(names)
catalog=json.loads(Path("server/heroes_catalog.json").read_text())
assert set(existing+names)<=set(catalog)
rows[0]="|".join(existing+names)
rows[1]=base64.b64encode(old+new).decode()
p.write_text("\n".join(rows)+"\n")
assert len(rows[0].split("|"))==121
print("Referências de heróis: 121 de 133; itens mantidos: 76.")
