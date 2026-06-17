package com.futaniumbox.players

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.AnimatorListenerAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.FileProvider
import androidx.core.view.MenuItemCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futaniumbox.players.databinding.ActivityMainBinding
import com.futaniumbox.players.ui.ChannelAdapter
import com.futaniumbox.players.model.Channel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.os.Bundle
import android.content.pm.PackageManager
import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.widget.ProgressBar
import android.widget.LinearLayout
import android.webkit.WebViewClient
import kotlin.math.max
import kotlin.math.min





class MainActivity : AppCompatActivity() {

    companion object {
        var navigatingInsideApp = false
    }


    private lateinit var vb: ActivityMainBinding
    private val client = OkHttpClient()
    private val adapter = ChannelAdapter()
		

    private val API_URL =
    "https://futaniumwebapp.vercel.app/api/buttons"

    private var refreshItem: MenuItem? = null
    private var refreshView: AppCompatImageView? = null

    private val SPIN_TAG_KEY = 0x13572468
    private var spinCompletedOne = false
    private var spinPendingStop = false
    private var spinAnimator: ObjectAnimator? = null
    private var spinRepeats: Int = 0

    private var pendingApkUri: Uri? = null
    private var downloadingDialog: AlertDialog? = null

		



    private val unknownSourcesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        pendingApkUri?.let { uri ->
            if (canInstallUnknownSources()) startApkInstall(uri)
            else Toast.makeText(this, "Permita instalar apps deste fonte para atualizar.", Toast.LENGTH_LONG).show()
        }
    }

    

    

    

    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        

        setSupportActionBar(vb.toolbar)
        window.statusBarColor = Color.parseColor("#10131C")
        vb.toolbar.elevation = 6f
        vb.toolbar.contentInsetEndWithActions = dp(44)
        vb.toolbar.post {
            for (i in 0 until vb.toolbar.childCount) {
                val child = vb.toolbar.getChildAt(i)
                if (child is TextView) {
                    child.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    child.typeface = Typeface.DEFAULT_BOLD
                    break
                }
            }
        }

        

        vb.rvChannels.layoutManager = LinearLayoutManager(this)
vb.rvChannels.adapter = adapter

var painelAberto = false

vb.webViewPanel.settings.javaScriptEnabled = true
vb.webViewPanel.settings.domStorageEnabled = true
vb.webViewPanel.webViewClient = WebViewClient()

vb.btnPainel.setOnClickListener {

    painelAberto = !painelAberto

    if (painelAberto) {

        vb.btnPainel.text = "CANAIS"

        vb.searchLayout.visibility = View.GONE
        vb.tvCount.visibility = View.GONE
        vb.swipe.visibility = View.GONE

        vb.webViewPanel.visibility = View.VISIBLE

        if (vb.webViewPanel.url == null) {
    vb.webViewPanel.loadUrl(
        "https://futaniumwebapp.vercel.app/painel/home.html"
    )
}

    } else {

        vb.btnPainel.text = "PAINEL"

        vb.searchLayout.visibility = View.VISIBLE
        vb.tvCount.visibility = View.VISIBLE
        vb.swipe.visibility = View.VISIBLE

        vb.webViewPanel.visibility = View.GONE
    }
}

vb.swipe.isEnabled = false


loadChannelsCache()?.let { cachedJson ->

    try {

        val cachedChannels = parseChannels(cachedJson)

        adapter.submit(cachedChannels)

        vb.tvCount.text =
            "${cachedChannels.size} canais"

    } catch (_: Exception) {
    }
}


vb.etSearch.addTextChangedListener { text ->
    adapter.filter(text?.toString() ?: "")
}

vb.searchLayout.setEndIconOnClickListener {

    vb.etSearch.setText("")
    vb.etSearch.clearFocus()

    adapter.filter("")
    vb.rvChannels.scrollToPosition(0)
}
        

// 🔧 Evita que o RecyclerView feche ou anime cards ao atualizar
vb.rvChannels.setHasFixedSize(false)
vb.rvChannels.itemAnimator = null


        // ======= Largura máx. 600dp em telas grandes / TV =======
        val isTv = packageManager.hasSystemFeature("android.software.leanback") ||
                   packageManager.hasSystemFeature("android.hardware.type.television")
        val maxCardPx = (600 * resources.displayMetrics.density).toInt()

        fun isLargeNow(): Boolean {
            val dm = resources.displayMetrics
            val widthDpNow = dm.widthPixels / dm.density
            return widthDpNow >= 600f || isTv
        }

        fun applyCardWidthConstraint() {
            val w = vb.rvChannels.width
            if (w <= 0) return
            if (isLargeNow()) {
                vb.rvChannels.clipToPadding = false
                val target = min(w, maxCardPx)
                val side = max(0, (w - target) / 2)
                vb.rvChannels.setPadding(side, vb.rvChannels.paddingTop, side, vb.rvChannels.paddingBottom)
                for (i in 0 until vb.rvChannels.childCount) {
                    val child = vb.rvChannels.getChildAt(i)
                    val lp = child.layoutParams as RecyclerView.LayoutParams
                    lp.width = target
                    child.layoutParams = lp
                }
            } else {
                vb.rvChannels.setPadding(0, vb.rvChannels.paddingTop, 0, vb.rvChannels.paddingBottom)
                for (i in 0 until vb.rvChannels.childCount) {
                    val child = vb.rvChannels.getChildAt(i)
                    val lp = child.layoutParams as RecyclerView.LayoutParams
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                    child.layoutParams = lp
                }
            }
        }

        vb.rvChannels.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyCardWidthConstraint() }

        vb.rvChannels.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                // ajusta largura do item
                if (isLargeNow()) {
                    val target = min(vb.rvChannels.width, maxCardPx)
                    (view.layoutParams as RecyclerView.LayoutParams).apply {
                        width = target
                        view.layoutParams = this
                    }
                } else {
                    (view.layoutParams as RecyclerView.LayoutParams).apply {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                        view.layoutParams = this
                    }
                }

                if (isTv) {
                    // o CARD é focável
                    view.isFocusable = true
                    view.isFocusableInTouchMode = true

                    // botões internos focáveis + UP/DOWN mudam de card
                    makeButtonsFocusableForTv(view)

                    // OK no CARD → expande e foca 1º botão visível; LEFT/RIGHT entra nos botões
                    view.setOnKeyListener { v, key, ev ->
                        if (ev.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

                        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                            v.performClick()
                            v.postDelayed({
                                findFirstVisibleFocusableButton(v)?.requestFocus()
                            }, 80)
                            return@setOnKeyListener true
                        }

                        if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            val target = if (key == KeyEvent.KEYCODE_DPAD_RIGHT)
                                findFirstVisibleFocusableButton(v)
                            else
                                findLastVisibleFocusableButton(v)
                            if (target != null) {
                                target.requestFocus()
                                return@setOnKeyListener true
                            }
                        }
                        false
                    }
                }
            }
            override fun onChildViewDetachedFromWindow(view: View) {}
        })

        // UP/DOWN quando o foco estiver em um botão interno muda de card
        if (isTv) {
            vb.rvChannels.isFocusable = true
            vb.rvChannels.isFocusableInTouchMode = true
            vb.rvChannels.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                if (keyCode != KeyEvent.KEYCODE_DPAD_UP && keyCode != KeyEvent.KEYCODE_DPAD_DOWN) return@setOnKeyListener false
                val focused = currentFocus ?: return@setOnKeyListener false
                val holder = vb.rvChannels.findContainingViewHolder(focused) ?: return@setOnKeyListener false
                val pos = holder.bindingAdapterPosition
                val nextPos = if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) pos + 1 else pos - 1
                if (nextPos < 0 || nextPos >= (vb.rvChannels.adapter?.itemCount ?: 0)) return@setOnKeyListener true
                (vb.rvChannels.layoutManager as LinearLayoutManager).scrollToPosition(nextPos)
                vb.rvChannels.post {
                    vb.rvChannels.findViewHolderForAdapterPosition(nextPos)?.itemView?.requestFocus()
                }
                true
            }
        }
        // ===========================================================

        

				// 🔹 Espaço extra no final da lista (sem afetar os cards)
val bottomSpace = (40 * resources.displayMetrics.density).toInt()
vb.rvChannels.setPadding(
    vb.rvChannels.paddingLeft,
    vb.rvChannels.paddingTop,
    vb.rvChannels.paddingRight,
    bottomSpace
)
vb.rvChannels.clipToPadding = false



        vb.swipe.setOnRefreshListener {

    vb.etSearch.setText("")
vb.etSearch.clearFocus()

adapter.filter("")
vb.rvChannels.scrollToPosition(0)

    if (!vb.swipe.isRefreshing)
        vb.swipe.isRefreshing = true

    startRefreshSpin()

    fetchGames(onFinally = {
        stopRefreshSpin()
    })
}

        if (adapter.itemCount == 0) {

    if (isOnline()) {

        fetchGames()

        // checkAppUpdateExternal(
        //     metaUrl = "https://raw.githubusercontent.com/galaxyplay1234/futanium-box-3.1/refs/heads/main/update.json",
        //     showNoUpdateToast = false
        // )

    } else {

        showOfflineDialog {
            vb.swipe.isRefreshing = true
            fetchGames()
        }

    }
}


} // onCreate

   override fun onPause() {
    super.onPause()
    
}

override fun onResume() {
    super.onResume()
    navigatingInsideApp = false
}
   

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        refreshItem = menu.findItem(R.id.action_refresh)

        val size = obtainActionBarSize()
        val iv = AppCompatImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(size, size)
            setImageDrawable(refreshItem!!.icon)
            scaleType = ImageView.ScaleType.CENTER

            val rippleColor = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
            val inset = dp(5)
            val mask = InsetDrawable(ShapeDrawable(OvalShape()), inset, inset, inset, inset)
            background = RippleDrawable(rippleColor, null, mask)
            setPadding(dp(8), dp(8), dp(8), dp(8))

            contentDescription = refreshItem!!.title
            isClickable = true
            isFocusable = true

            setOnClickListener { onOptionsItemSelected(refreshItem!!) }
        }
        MenuItemCompat.setActionView(refreshItem, iv)
        refreshView = iv

        refreshView?.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            postOnAnimation { setLayerType(View.LAYER_TYPE_NONE, null) }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {

    vb.etSearch.setText("")
vb.etSearch.clearFocus()

adapter.filter("")
vb.rvChannels.scrollToPosition(0)

    startRefreshSpin()

    if (!vb.swipe.isRefreshing)
        vb.swipe.isRefreshing = true

    fetchGames(onFinally = {
        stopRefreshSpin()
    })

    true
}
            else -> super.onOptionsItemSelected(item)
        }
    }



    private fun fetchGames(onFinally: (() -> Unit)? = null) {
        if (!isOnline()) {
            vb.swipe.isRefreshing = false
            showOfflineDialog {
                vb.swipe.isRefreshing = true
                fetchGames(onFinally)
            }
            return
        }

      



        if (!vb.swipe.isRefreshing) vb.swipe.isRefreshing = true

        Thread {
    try {
        val req = Request.Builder().url(API_URL).build()
        val res = client.newCall(req).execute()
        val body = res.body?.string() ?: "[]"
        
				saveChannelsCache(body)

        val channels = parseChannels(body)

        runOnUiThread {

    adapter.submit(channels)

    vb.tvCount.text = "${channels.size} canais"

    if (vb.etSearch.text?.isNotEmpty() == true) {
        vb.etSearch.setText("")
        vb.etSearch.clearFocus()
    }

    vb.rvChannels.scrollToPosition(0)
}

    } catch (_: Exception) {
        // silêncio para não expor a API
    } finally {
        runOnUiThread {
            vb.swipe.isRefreshing = false
            onFinally?.invoke()
        }
    }
}.start()
}

   



private fun parseChannels(json: String): List<Channel> {

    val arr = JSONArray(json)
    val list = mutableListOf<Channel>()

    for (i in 0 until arr.length()) {

        val obj = arr.getJSONObject(i)

        list.add(
            Channel(
                name = obj.optString("name"),
                link = obj.optString("link")
            )
        )
    }

    return list
}


    private fun checkAppUpdateExternal(metaUrl: String, showNoUpdateToast: Boolean = false) {
        Thread {
            try {
                val req = Request.Builder().url(metaUrl).build()
                val res = OkHttpClient().newCall(req).execute()
                val body = res.body?.string().orEmpty()
                if (body.isBlank()) return@Thread

                val obj = JSONObject(body)
                val remoteCode = obj.optInt("versionCode", -1)
                val apkUrl     = obj.optString("apkUrl", "")
                val title      = obj.optString("title", "Nova versão disponível")
                val changelog  = obj.optString("changelog", "")

                if (remoteCode > currentVersionCode()) {
                    runOnUiThread { showUpdateAvailableDialog(title, changelog, apkUrl) }
                } else if (showNoUpdateToast) {
                    runOnUiThread {
                        Toast.makeText(this, "Você já está na última versão.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                if (showNoUpdateToast) {
                    runOnUiThread {
                        Toast.makeText(this, "Falha ao checar atualização.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun showUpdateAvailableDialog(title: String, changelog: String, apkUrl: String) {
        val msg = if (changelog.isNotBlank()) changelog else "Há uma nova versão disponível."

        val dialog = AlertDialog.Builder(this)
            .setTitle(title.ifBlank { "Nova versão disponível" })
            .setMessage(msg)
            .setIcon(applicationInfo.icon)
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("BAIXAR") { _, _ -> downloadAndPromptInstall(apkUrl) }
            .create()

        dialog.setOnShowListener {
            val c = getColor(R.color.menuColor)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(c)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(c)
        }
        dialog.show()
    }

    private fun showDownloadingDialog(): AlertDialog {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(8))
        }
        val pb = ProgressBar(this).apply { isIndeterminate = true }
        val tv = TextView(this).apply {
            text = "Baixando atualização…"
            setPadding(0, dp(12), 0, 0)
        }
        container.addView(pb)
        container.addView(tv)

        return AlertDialog.Builder(this)
            .setTitle("Atualizando")
            .setView(container)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun downloadAndPromptInstall(apkUrl: String) {
        downloadingDialog?.dismiss()
        downloadingDialog = showDownloadingDialog()

        Thread {
            try {
                val client = OkHttpClient()
                val res = client.newCall(Request.Builder().url(apkUrl).build()).execute()
                val body = res.body ?: throw IllegalStateException("Sem corpo na resposta")

                val dir = File(cacheDir, "apks").apply { mkdirs() }
                val file = File(dir, "update.apk")
                body.byteStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }

                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                runOnUiThread {
                    downloadingDialog?.dismiss()
                    downloadingDialog = null
                    prepareInstall(uri)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    downloadingDialog?.dismiss()
                    downloadingDialog = null
                    Toast.makeText(this, "Erro ao baixar atualização.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun prepareInstall(uri: Uri) {
        pendingApkUri = uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!canInstallUnknownSources()) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:$packageName")
                    )
                    unknownSourcesLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    i.data = Uri.parse("package:$packageName")
                    startActivity(i)
                    Toast.makeText(this, "Habilite 'Apps desconhecidos' para atualizar.", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
        startApkInstall(uri)
    }

    private fun startApkInstall(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { startActivity(intent) }
        catch (_: Exception) { Toast.makeText(this, "Não foi possível iniciar a instalação.", Toast.LENGTH_LONG).show() }
    }

    private fun canInstallUnknownSources(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { packageManager.canRequestPackageInstalls() } catch (_: SecurityException) { false }
        } else true
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun showOfflineDialog(onRetry: (() -> Unit)? = null) {
        val d = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Sem conexão")
            .setMessage("Verifique sua internet e tente novamente.")
            .setNegativeButton("CONFIGURAR WI-FI") { _, _ ->
                startActivity(android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
            }
            .setPositiveButton("TENTAR NOVAMENTE") { _, _ ->
                if (isOnline()) onRetry?.invoke() else showOfflineDialog(onRetry)
            }
            .create()

        d.setOnShowListener {
            val c = getColor(R.color.menuColor)
            d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(c)
            d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(c)
        }
        d.show()
    }

		

    // ===== refresh icon spin (inalterado) =====
    private fun startRefreshSpin() {
        val v = refreshView ?: return
        if (v.getTag(SPIN_TAG_KEY) == true) return

        v.animate().cancel()
        spinAnimator?.cancel()
        spinAnimator = null
        spinRepeats = 0
        spinCompletedOne = false
        spinPendingStop = false

        v.setTag(SPIN_TAG_KEY, true)
        v.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val base = ((v.rotation % 360f) + 360f) % 360f
        v.rotation = base

        spinAnimator = ObjectAnimator.ofFloat(v, View.ROTATION, base, base + 360f).apply {
            duration = 1100
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationRepeat(animation: android.animation.Animator) {
                    spinRepeats++
                    spinCompletedOne = true
                    if (spinPendingStop && spinRepeats >= 1) {
                        animation.cancel()
                        finishToSnap(v)
                    }
                }
            })
            start()
        }
    }

    private fun stopRefreshSpin() {
        val v = refreshView ?: return
        if (v.getTag(SPIN_TAG_KEY) != true) return
        if (!spinCompletedOne) { spinPendingStop = true; return }

        spinAnimator?.cancel()
        spinAnimator = null
        finishToSnap(v)
    }

    private fun finishToSnap(v: View) {
        v.setTag(SPIN_TAG_KEY, false)
        val current = ((v.rotation % 360f) + 360f) % 360f
        val remaining = if (current == 0f) 0f else 360f - current
        if (remaining > 0f) {
            val dur = (remaining / 360f * 240).toLong().coerceAtLeast(100L)
            v.animate()
                .rotationBy(remaining)
                .setDuration(dur)
                .setInterpolator(LinearInterpolator())
                .withEndAction { v.rotation = 0f; v.setLayerType(View.LAYER_TYPE_NONE, null) }
                .start()
        } else {
            v.rotation = 0f
            v.setLayerType(View.LAYER_TYPE_NONE, null)
        }
        spinRepeats = 0
        spinCompletedOne = false
        spinPendingStop = false
    }

    private fun obtainActionBarSize(): Int {
        val tv = TypedValue()
        var size = dp(48)
        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            size = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
        return size
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun currentVersionCode(): Int {
        return try {
            val pm = packageManager
            val pi = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.getPackageInfo(packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt() else @Suppress("DEPRECATION") pi.versionCode
        } catch (_: Exception) { 0 }
    }

    // ========= Helpers de foco/TV =========
    private fun makeButtonsFocusableForTv(root: View) {
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val v = root.getChildAt(i)
                if (v is ViewGroup) makeButtonsFocusableForTv(v)
                if (v.isClickable) {
                    v.isFocusable = true
                    v.isFocusableInTouchMode = true
                    // quando o foco está num botão, UP/DOWN mudam de card
                    v.setOnKeyListener { btn, keyCode, ev ->
                        if (ev.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            val holder = vb.rvChannels.findContainingViewHolder(btn) ?: return@setOnKeyListener false
                            val pos = holder.bindingAdapterPosition
                            val nextPos = if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) pos + 1 else pos - 1
                            if (nextPos < 0 || nextPos >= (vb.rvChannels.adapter?.itemCount ?: 0)) return@setOnKeyListener true
                            (vb.rvChannels.layoutManager as LinearLayoutManager).scrollToPosition(nextPos)
                            vb.rvChannels.post {
                                vb.rvChannels.findViewHolderForAdapterPosition(nextPos)?.itemView?.requestFocus()
                            }
                            return@setOnKeyListener true
                        }
                        false // LEFT/RIGHT/OK seguem padrão (navega pelos botões e clica)
                    }
                }
            }
        }
    }

    private fun findFirstVisibleFocusableButton(root: View): View? {
        if (root !is ViewGroup) return null
        for (i in 0 until root.childCount) {
            val c = root.getChildAt(i)
            if (c.visibility == View.VISIBLE && c.isClickable && c.isFocusable) return c
            val deeper = findFirstVisibleFocusableButton(c)
            if (deeper != null) return deeper
        }
        return null
    }

    private fun findLastVisibleFocusableButton(root: View): View? {
        if (root !is ViewGroup) return null
        for (i in root.childCount - 1 downTo 0) {
            val c = root.getChildAt(i)
            if (c.visibility == View.VISIBLE && c.isClickable && c.isFocusable) return c
            val deeper = findLastVisibleFocusableButton(c)
            if (deeper != null) return deeper
        }
        return null
    }

private fun saveChannelsCache(json: String) {

    getSharedPreferences("channels_cache", Context.MODE_PRIVATE)
        .edit()
        .putString("channels_json", json)
        .apply()
}

private fun loadChannelsCache(): String? {

    return getSharedPreferences(
        "channels_cache",
        Context.MODE_PRIVATE
    ).getString("channels_json", null)
}

}
