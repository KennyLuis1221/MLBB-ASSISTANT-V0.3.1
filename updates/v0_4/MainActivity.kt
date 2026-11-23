package com.mlbb.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

/** V0.4: home simples, draft e dicas offline multi-heroi.
 * Analise automatica so com backend HTTPS realmente configurado.
 */
class MainActivity : Activity() {
    private val requestProjection = 7301
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private var statusLabel: TextView? = null
    private var permissionLabel: TextView? = null
    private var consentBox: CheckBox? = null
    private var page = "home"
    private val navy = 0xFF091525.toInt()
    private val slate = 0xFF13263B.toInt()
    private val white = 0xFFF3F7FA.toInt()
    private val muted = 0xFF9DB2C7.toInt()
    private val gold = 0xFFE9BF70.toInt()
    private val cyan = 0xFF32C6F7.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        showHome()
    }
    override fun onResume() {
        super.onResume()
        refreshStatus()
    }
    @Deprecated("Back used for small built-in screens")
    override fun onBackPressed() {
        if (page != "home") showHome() else super.onBackPressed()
    }

    private fun root(): LinearLayout {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(38))
            setBackgroundColor(navy)
        }
        setContentView(ScrollView(this).apply {
            fillViewport = true
            setBackgroundColor(navy)
            addView(content)
        })
        return content
    }
    private fun showHome() {
        page = "home"
        val ui = root()
        ui.addView(label("MLBB", 15, cyan, true))
        ui.addView(label("ASSISTANT PRO", 29, gold, true))
        ui.addView(label("Sua estratégia, antes e durante a partida", 13, muted))
        ui.addView(space(19))
        val stateCard = card()
        stateCard.addView(label("STATUS DO ASSISTENTE", 12, gold, true))
        statusLabel = label("", 14, white)
        permissionLabel = label("", 12, muted)
        stateCard.addView(space(7))
        stateCard.addView(statusLabel)
        stateCard.addView(space(5))
        stateCard.addView(permissionLabel)
        ui.addView(stateCard)
        refreshStatus()
        ui.addView(space(18))
        ui.addView(button("▶  INICIAR ASSISTÊNCIA AO VIVO", true) { startCapture() })
        ui.addView(space(11))
        ui.addView(button("♜  SUGESTÃO DE PERSONAGEM") { showDraft() })
        ui.addView(space(9))
        ui.addView(button("⚔  DICAS DE BUILD PARA MEU HERÓI") { showBuild() })
        ui.addView(space(15))

        ui.addView(label("PRIMEIRO ACESSO", 12, gold, true))
        ui.addView(label("Autorize somente as permissões necessárias para o jogo. A captura de tela sempre exige confirmação do Android.", 12, muted))
        ui.addView(space(7))
        ui.addView(button("Permissões e privacidade") { showPermissions() })
        ui.addView(space(9))
        consentBox = CheckBox(this).apply {
            text = "Autorizo a captura e o envio de imagens do MLBB durante esta sessão, para o servidor de análise configurado."
            textSize = 12f
            setTextColor(white)
            buttonTintList = android.content.res.ColorStateList.valueOf(cyan)
            isChecked = false
        }
        ui.addView(consentBox)
        ui.addView(space(18))
        ui.addView(button("■  Encerrar assistência") {
            startService(Intent(this, ProjectionService::class.java).setAction(ProjectionService.ACTION_STOP))
            toast("Encerramento solicitado")
        })
        ui.addView(space(12))
        ui.addView(button("Configurações avançadas  ⚙") { showAdvanced() })
        ui.addView(space(14))
        ui.addView(label("Versão 0.4 de testes. A seleção e as dicas offline usam heurísticas por classe; a análise visual exige um servidor online implantado e calibrado. Nenhuma informação oculta do jogo é acessada.", 11, muted))
    }

    private fun showDraft() {
        page = "draft"
        val ui = root()
        back(ui, "ESCOLHA INTELIGENTE DE HERÓIS")
        ui.addView(label("Smart Hero Pick", 24, gold, true))
        ui.addView(label("Sugestões preliminares por função, composição e favoritos. Não são counters validados por IA.", 12, muted))
        ui.addView(space(15))
        val lanes = arrayOf("Selva", "EXP", "Meio", "Ouro", "Roam")
        val lane = Spinner(this)
        lane.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lanes)
        ui.addView(label("Qual posição você vai jogar?", 13, white, true))
        ui.addView(lane)
        val allies = input("Aliados selecionados (separe por vírgulas)", "")
        val enemies = input("Inimigos revelados (separe por vírgulas)", "")
        val favorites = input("Seus heróis favoritos (opcional)", "")
        ui.addView(space(12))
        for (f in listOf(allies, enemies, favorites)) {
            ui.addView(f)
            ui.addView(space(9))
        }
        val result = card()
        result.addView(label("Preencha os campos e toque em sugerir.", 13, muted))
        ui.addView(button("SUGERIR 3 HERÓIS", true) {
            result.removeAllViews()
            val picks = HeroCatalog.suggest(lane.selectedItem.toString(), allies.text.toString(), enemies.text.toString(), favorites.text.toString())
            picks.forEachIndexed { i, pick ->
                result.addView(label((i + 1).toString() + ".  " + pick.hero, 18, gold, true))
                result.addView(label(pick.reason, 12, white))
                result.addView(space(9))
            }
            if (picks.isEmpty()) result.addView(label("Nenhuma escolha válida encontrada.", 13, muted))
        })
        ui.addView(space(12))
        ui.addView(result)
        ui.addView(space(10))
        ui.addView(label("Catálogo local: " + HeroCatalog.heroes.size + " heróis cadastrados. Se algum nome não for encontrado, confira a grafia. Heróis banidos não devem ser considerados elegíveis.", 11, muted))
    }

    private fun showBuild() {
        page = "build"
        val ui = root()
        back(ui, "DICAS DE EQUIPAMENTOS")
        ui.addView(label("Meu herói e minha build", 23, gold, true))
        ui.addView(label("Apoio offline por classe; não substitui a análise visual da partida.", 12, muted))
        ui.addView(space(14))
        val choice = AutoCompleteTextView(this).apply {
            setText(prefs.getString("hero_hint", "") ?: "")
            hint = "Digite ou selecione seu herói"
            threshold = 1
            setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, HeroCatalog.heroes))
            setSingleLine(true)
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(14), dp(15), dp(14), dp(15))
            background = background(slate, slate, 12)
        }
        ui.addView(choice)
        ui.addView(space(13))
        val armor = check("Inimigos com defesa física elevada")
        val healing = check("Inimigos com muita cura")
        val magic = check("Ameaça de dano mágico explosivo")
        for (c in listOf(armor, healing, magic)) ui.addView(c)
        ui.addView(space(9))
        val result = card()
        result.addView(label("Selecione um herói para consultar dicas.", 13, muted))
        ui.addView(button("ANALISAR OPÇÕES", true) {
            val hero = HeroCatalog.find(choice.text.toString())
            if (hero == null) {
                toast("Herói não encontrado no catálogo")
                return@button
            }
            prefs.edit().putString("hero_hint", hero).apply()
            result.removeAllViews()
            result.addView(label(hero + "  •  " + HeroCatalog.rolesOf(hero).joinToString(" / "), 17, gold, true))
            result.addView(space(7))
            HeroCatalog.itemSuggestions(hero, armor.isChecked, healing.isChecked, magic.isChecked).forEach { tip ->
                result.addView(label(tip.item, 16, cyan, true))
                result.addView(label(tip.reason, 12, white))
                result.addView(space(10))
            }
            result.addView(label("Dicas experimentais. Confirme os efeitos dos itens no patch atual antes de comprar.", 11, muted))
        })
        ui.addView(space(12))
        ui.addView(result)
    }

    private fun showPermissions() {
        page = "permissions"
        val ui = root()
        back(ui, "PERMISSÕES E PRIVACIDADE")
        ui.addView(label("Você controla o acesso", 23, gold, true))
        ui.addView(space(15))
        ui.addView(button("1  Permitir painel flutuante") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName)))
        })
        ui.addView(space(10))
        ui.addView(button("2  Permitir identificar MLBB aberto") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        if (Build.VERSION.SDK_INT >= 33) {
            ui.addView(space(10))
            ui.addView(button("3  Autorizar notificações") {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
                else toast("Notificações já permitidas")
            })
        }
        ui.addView(space(15))
        ui.addView(label("A captura de tela só começa com sua autorização. Quando a análise online estiver conectada, capturas poderão ser enviadas ao servidor e ao provedor de IA. Não abra informações pessoais enquanto estiver capturando.", 13, muted))
    }

    private fun showAdvanced() {
        page = "advanced"
        val ui = root()
        back(ui, "CONFIGURAÇÕES AVANÇADAS")
        ui.addView(label("Conexão e desenvolvimento", 22, gold, true))
        ui.addView(label("Para o usuário final, estes campos só serão dispensáveis depois da publicação de um servidor confiável. Não coloque chave secreta de IA no celular.", 12, muted))
        ui.addView(space(12))
        val url = input("URL do servidor HTTPS", prefs.getString("url", "") ?: "")
        val token = input("Token de acesso ao servidor", prefs.getString("token", "") ?: "").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val game = input("Pacote MLBB", prefs.getString("game_package", "com.mobile.legends") ?: "com.mobile.legends")
        val interval = input("Intervalo entre capturas (15 a 60 segundos)", prefs.getInt("interval", 25).toString()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        listOf(url, token, game, interval).forEach { ui.addView(it); ui.addView(space(9)) }
        ui.addView(button("SALVAR CONFIGURAÇÃO", true) {
            val intValue = interval.text.toString().toIntOrNull()
            if (intValue == null || intValue !in 15..60) {
                toast("Use um intervalo de 15 a 60 segundos")
                return@button
            }
            val address = url.text.toString().trim().trimEnd('/')
            if (address.isNotBlank() && !(address.startsWith("https://") ||
                (BuildConfig.DEBUG && address.startsWith("http://")))) {
                toast("Use uma URL HTTPS (HTTP permitido somente em debug)")
                return@button
            }
            prefs.edit().putString("url", address).putString("token", token.text.toString().trim())
                .putString("game_package", game.text.toString().trim())
                .putInt("interval", intValue).apply()
            toast("Configurações salvas")
            showHome()
        })
    }

    private fun refreshStatus() {
        val connected = !prefs.getString("url", "").isNullOrBlank() && !prefs.getString("token", "").isNullOrBlank()
        statusLabel?.text = if (connected) "●  Servidor configurado (não verificado)" else "●  IA online ainda não conectada"
        permissionLabel?.text = "Painel: " + if (Settings.canDrawOverlays(this)) "permitido" else "aguarda permissão"
        permissionLabel?.append("   •   Acesso de uso: " + if (hasUsageAccess()) "permitido" else "aguarda permissão")
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    private fun startCapture() {
        val url = prefs.getString("url", "")?.trim().orEmpty()
        val token = prefs.getString("token", "")?.trim().orEmpty()
        if (url.isBlank() || token.isBlank()) {
            AlertDialog.Builder(this).setTitle("IA online ainda não disponível")
                .setMessage("O app precisa de um servidor de visão implantado para analisar partidas ao vivo. Enquanto isso, a seleção de heróis e as dicas offline funcionam sem servidor. Configuração manual disponível em Avançadas.")
                .setPositiveButton("Entendi", null).show()
            return
        }
        if (consentBox?.isChecked != true) return toast("Autorize o envio de capturas para esta sessão")
        if (!Settings.canDrawOverlays(this)) return toast("Autorize o painel flutuante nas permissões")
        if (!hasUsageAccess()) return toast("Autorize o acesso de uso nas permissões")
        val gamePackage = prefs.getString("game_package", "com.mobile.legends") ?: "com.mobile.legends"
        if (!gamePackage.contains('.')) return toast("Pacote do jogo inválido nas configurações")
        if (!(url.startsWith("https://") || (BuildConfig.DEBUG && url.startsWith("http://"))))
            return toast("Endereço do servidor inválido")
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), requestProjection)
    }

    @Deprecated("Activity result retained to keep this APK dependency-free")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestProjection && resultCode == RESULT_OK && data != null) {
            val intent = Intent(this, ProjectionService::class.java)
                .setAction(ProjectionService.ACTION_START)
                .putExtra(ProjectionService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(ProjectionService.EXTRA_RESULT_DATA, data)
            startForegroundService(intent)
            toast("Captura iniciada. Abra o MLBB.")
        } else if (requestCode == requestProjection) toast("Captura não autorizada")
    }

    private fun back(ui: LinearLayout, name: String) {
        ui.addView(button("‹  Voltar") { showHome() })
        ui.addView(space(12))
        ui.addView(label(name, 12, cyan, true))
        ui.addView(space(5))
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(16), dp(15), dp(16))
        background = background(slate, 0xFF192E45.toInt(), 16, 0xFF244C6A.toInt())
    }
    private fun input(hintText: String, value: String): EditText = EditText(this).apply {
        setText(value)
        hint = hintText
        textSize = 14f
        setTextColor(white)
        setHintTextColor(muted)
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = background(slate, slate, 12)
    }
    private fun check(value: String): CheckBox = CheckBox(this).apply {
        text = value
        textSize = 13f
        setTextColor(white)
        buttonTintList = android.content.res.ColorStateList.valueOf(cyan)
    }
    private fun button(value: String, primary: Boolean = false, click: () -> Unit): Button = Button(this).apply {
        text = value
        textSize = 14f
        isAllCaps = false
        setTextColor(if (primary) navy else white)
        background = background(
            if (primary) gold else 0xFF17415E.toInt(),
            if (primary) 0xFFF6D797.toInt() else 0xFF164A6B.toInt(),
            13
        )
        setOnClickListener { click() }
    }
    private fun label(value: String, size: Int, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun background(start: Int, end: Int, radius: Int, stroke: Int? = null): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(start, end)).apply {
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }
    private fun space(height: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
}
