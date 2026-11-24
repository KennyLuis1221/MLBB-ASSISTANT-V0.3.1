"""V0.7 integration. Keep capture guard; add diagnostics and explicit online consent."""
from pathlib import Path
import os,json
base=Path("android-app/app/src/main/java/com/mlbb/assistant")
def replace(p,old,new):
    s=p.read_text(encoding="utf-8")
    if old not in s: raise RuntimeError(f"{p.name}: missing {old[:90]}")
    p.write_text(s.replace(old,new,1),encoding="utf-8")
u=base/"UploadClient.kt"
replace(u,"data class AnalysisResult(","""data class ObservedPlayer(
    val hero:String,val team:String,val items:List<String>,
    val gold:String?=null,val kda:String?=null,
    val portrait:android.graphics.Bitmap?=null,
    val itemImages:Map<String,android.graphics.Bitmap> = emptyMap()
)

data class AnalysisResult(""")
replace(u,"    val visionStatus: String\n)","""    val visionStatus: String,
    val players: List<ObservedPlayer> = emptyList(),
    val draftPicks: List<String> = emptyList(),
    val alternatives: List<String> = emptyList(),
    val threatSummary: String = "",
    val matchTime: String? = null,
    val observedAt: Long = 0L,
    val frameCount: Int = 0
)""")
s=base/"ProjectionService.kt"
replace(s,"    private var paused = false","    @Volatile private var paused = false\n    private var capturedFrames=0\n    private var completedFrames=0\n    private var foregroundPackageInfo=\"não verificado\"")
replace(s,'        url = prefs.getString("url", "") ?: ""','''        url = if(intent.getBooleanExtra("cloud_allowed",false)) {
            prefs.getString("url","").orEmpty().ifBlank { BuildConfig.DEFAULT_API_URL }
        } else ""''')
replace(s,'        intervalMs = prefs.getInt("interval", 12).toLong().coerceIn(10, 60) * 1000L','        intervalMs = if(url.isBlank()) 4_000L else 12_000L')
replace(s,'            if (paused || uploadInFlight.get()) return','            capturedFrames++\n            if (paused || uploadInFlight.get()) return')
replace(s,'                showStatus("Envio bloqueado: abra o MLBB")','                showStatus("Captura recebida: $capturedFrames quadros. Análise bloqueada: MLBB não confirmado em foco ($foregroundPackageInfo).")')
replace(s,'                    if (paused || !isGameForeground()) return@execute','''                    if (paused || !isGameForeground()) {
                        showStatus("Quadro descartado: MLBB não confirmado em foco")
                        return@execute
                    }
                    showStatus("Analisando quadro • recebidos: $capturedFrames • concluídos: $completedFrames")''')
replace(s,'                            UploadClient.analyze(url, token, jpeg, sessionId, heroHint, armor, healing, magic)','''                            if(token.isBlank()) token=CloudSession.connect(url)
                            if(paused || !isGameForeground()) return@execute
                            UploadClient.analyze(url, token, jpeg, sessionId, heroHint, armor, healing, magic)''')
replace(s,'                    ui.post { overlay?.showResult(result) }','''                    completedFrames++
                    val enriched=result.copy(frameCount=completedFrames)
                    ui.post { overlay?.showResult(enriched) }
                    getSharedPreferences("settings",MODE_PRIVATE).edit()
                        .putString("last_diagnostic",enriched.status+" | quadros: "+completedFrames).apply()''')
replace(s,'                    showStatus("Servidor: $detail")','                    showStatus("Falha na análise: $detail")')
replace(s,'        return foregroundPackage == gamePackage','        foregroundPackageInfo=if(foregroundPackage==gamePackage)"MLBB" else if(foregroundPackage==null)"sem evento de uso" else "outro pacote"\n        return foregroundPackage == gamePackage')
replace(s,'        ui.post { overlay?.showStatus(message) }','''        getSharedPreferences("settings",MODE_PRIVATE).edit().putString("last_diagnostic",message.take(220)).apply()
        ui.post { overlay?.showStatus(message) }''')
m=base/"MainActivity.kt"
replace(m,'    private var consentBox: CheckBox? = null','    private var consentBox: CheckBox? = null\n    private var cloudConsent: CheckBox? = null\n    private var sessionCloudAllowed=false')
replace(m,'Autorizo a captura durante esta sessão. Imagens só serão enviadas se configurar um servidor em Avançadas.','Autorizo analisar localmente as capturas do MLBB durante esta sessão.')
replace(m,'        ui.addView(consentBox)','''        ui.addView(consentBox)
        cloudConsent=CheckBox(this).apply {
            text="Permito enviar quadros do MLBB ao servidor configurado e ao provedor de visão. Opcional."
            setTextColor(white);textSize=12f;isChecked=false
        }
        ui.addView(cloudConsent)
        ui.addView(label("Último diagnóstico: "+prefs.getString("last_diagnostic","Nenhuma sessão iniciada"),12,muted))''')
replace(m,'        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager','        sessionCloudAllowed=cloudConsent?.isChecked==true\n        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager')
replace(m,'                .putExtra(ProjectionService.EXTRA_RESULT_CODE, resultCode)','                .putExtra("cloud_allowed",sessionCloudAllowed)\n                .putExtra(ProjectionService.EXTRA_RESULT_CODE, resultCode)')
replace(m,'            toast("Captura iniciada. Abra o MLBB.")','''            toast("Captura autorizada. Iniciando MLBB.")
            val pkg=prefs.getString("game_package","com.mobile.legends")?:"com.mobile.legends"
            val launch=packageManager.getLaunchIntentForPackage(pkg)
            if(launch!=null)startActivity(launch) else toast("Abra o MLBB manualmente.")''')
replace(m,'Versão 0.6.1 de diagnóstico. O reconhecimento local lê textos visíveis em telas do MLBB. Ícones do placar ainda exigem visão online treinada. Não acessa informações ocultas do jogo.','Versão 0.7 experimental. Observações repetidas, diagnóstico de captura e painel redesenhado. Não acessa informações ocultas.')
replace(m,'Autorize o envio de capturas para esta sessão','Autorize a captura para esta sessão')
manifest=Path("android-app/app/src/main/AndroidManifest.xml")
replace(manifest,'    <application','    <queries><package android:name="com.mobile.legends" /></queries>\n    <application')
gradle=Path("android-app/app/build.gradle.kts")
replace(gradle,'versionCode = 8','versionCode = 9')
replace(gradle,'versionName = "0.6.1"','versionName = "0.7.0"')
endpoint=os.getenv("MLBB_DEFAULT_API_URL","").strip().rstrip("/")
if endpoint and not endpoint.startswith("https://"): raise RuntimeError("Endpoint deve usar HTTPS")
# JSON string as Kotlin string literal. Reject unexpected characters.
if endpoint and not all(c.isalnum() or c in "://.-_" for c in endpoint): raise RuntimeError("Endpoint inválido")
replace(gradle,'versionName = "0.7.0"','versionName = "0.7.0"\n        buildConfigField("String", "DEFAULT_API_URL", '+json.dumps(json.dumps(endpoint))+')')
# Prevent stale legacy initial result from masquerading as observation.
replace(s,'                    visionStatus = "Dados inimigos não detectados. Abra o placar durante o MLBB."','                    visionStatus = "Dados inimigos não detectados. Abra o placar durante o MLBB.", frameCount=0')
print("V0.7 integrated")
