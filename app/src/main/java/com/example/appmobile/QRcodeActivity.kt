package com.example.appmobile

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.budiyev.android.codescanner.*
import com.example.appmobile.databinding.ActivityQrcodeBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.DateTimeException
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeParseException
import java.util.*
import kotlin.math.roundToInt

private const val CAMERA_REQUEST_CODE = 101

class QRcodeActivity : AppCompatActivity() {

    lateinit var session : SessionManager
    private lateinit var binding: ActivityQrcodeBinding

    lateinit var codeScanner: CodeScanner
    var isCodeScanner: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrcodeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        callBackend()

        /* Para remover um sombrado atraz do menu */
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.background = null
        /* Desativar o icon escondido que alinha o menu */
        bottomNavigationView.menu.getItem(2).isEnabled = false

        /* Executar botao QRCODE */
        val btnQrcode = findViewById<FloatingActionButton>(R.id.fab)
        btnQrcode.setOnClickListener {
            var i = Intent(applicationContext, QRcodeActivity::class.java)
            startActivity(i)
        }

        /* Botoes do Menu */
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.page_1 -> {
                    val iHome = Intent(applicationContext, Home::class.java)
                    startActivity(iHome)
                }
                R.id.page_2 -> {
                    val iPesquisa = Intent(applicationContext, pesquisaSalas::class.java)
                    startActivity(iPesquisa)
                }
                R.id.page_3 -> {
                    val iMinhas = Intent(applicationContext, MinhasReservas::class.java)
                    startActivity(iMinhas)
                }
                R.id.page_4 -> {
                    val Opcoes = Intent(applicationContext, Opcoes::class.java)
                    startActivity(Opcoes)
                }
            }
            return@setOnItemSelectedListener true
        }

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setOnMenuItemClickListener {menuItem ->
            when (menuItem.itemId) {
                R.id.notify -> {
                    var dialog = NotificacoesFragment()
                    dialog.show(supportFragmentManager, "TAG")
                    true
                }
                else -> false
            }

        }

        /* Voltar */
        val voltarBtn = findViewById<TextView>(R.id.goback)
        voltarBtn.setOnClickListener {
            try {
                finish()
            }catch (e:Exception){
                Log.e("QRCODE", "Já tinha saido da atividade")
            }
        }

        setupPermissions()
        /*
            Verificamos a versão do SDK, se for maior ou igual a 24 apresentamos o codeScanner
            que é uma livraria mais moderna e estavel, enquanto se for menor utilizamos a journeyApps.
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isCodeScanner = true
            codeScanner()
        }else journeyScanner()

    }

    private fun journeyScanner(){
        try{
            val scanner = IntentIntegrator(this)
            scanner.setBeepEnabled(false)
            scanner.setCameraId(0)
            scanner.setPrompt("Aponte o dispositivo ao QRCode.")
            scanner.setBarcodeImageEnabled(false)
            scanner.initiateScan()
        }catch (e: JSONException) {
            e.printStackTrace()
            infoDialog(getString(R.string.erro_qrcode_permissoes))
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                infoDialog(getString(R.string.erro_qrcode_vazio))
            } else {

                try {
                    // Converting the data to json format
                    val obj = JSONObject(result.contents)

                    if(!obj.has("nsala") || !obj.has("datareserva") || !obj.has("horainicio") || !obj.has("horafim")){
                        infoDialog(getString(R.string.erro_qrcode_formato))
                        return
                    }

                    if(!isValidDate(obj.getString("datareserva")) || !isValidTime(obj.getString("horainicio")) || !isValidTime(obj.getString("horafim"))){
                        infoDialog(getString(R.string.erro_qrcode_formato))
                        return
                    }

                    val url = appSettings.URLgetSala + obj.getString("nsala")
                    val requestQueue = Volley.newRequestQueue(this)
                    val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {
                        val sala = response.getJSONObject("data")
                        val alocacao = sala.getInt("alocacao")
                        val lugares = sala.getInt("lugares")
                        validarSobreposicao(obj.getString("nsala"), alocacao.toString(), lugares.toString(), obj.getString("horainicio"), obj.getString("horafim"), obj.getString("datareserva"))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        infoDialog(getString(R.string.erro_qrcode_sala))
                    }
                    }, { error -> error.printStackTrace().toString() })
                    requestQueue.add(request)


                } catch (e: JSONException) {
                    e.printStackTrace()
                    infoDialog(getString(R.string.erro_qrcode_formato))
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun codeScanner(){
        binding.mostrarScanner.visibility = View.VISIBLE
        codeScanner = CodeScanner(this, binding.scannerView)
        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.TWO_DIMENSIONAL_FORMATS

            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            isFlashEnabled = false

            errorCallback = ErrorCallback {
                runOnUiThread{
                    Log.i("QRCODE", it.message.toString())
                    infoDialog(getString(R.string.erro_geral))
                }
            }

        }

        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                Log.i("QRCODE", it.text)

                if(it.text == "" || it.text == null){
                    infoDialog(getString(R.string.erro_qrcode_vazio))
                    return@runOnUiThread
                }

                val obj = JSONObject(it.text.toString())

                if(!obj.has("nsala") || !obj.has("datareserva") || !obj.has("horainicio") || !obj.has("horafim")){
                    infoDialog(getString(R.string.erro_qrcode_formato))
                    return@runOnUiThread
                }

                if(!isValidDate(obj.getString("datareserva")) || !isValidTime(obj.getString("horainicio")) || !isValidTime(obj.getString("horafim"))){
                    infoDialog(getString(R.string.erro_qrcode_formato))
                    return@runOnUiThread
                }

                val url = appSettings.URLgetSala + obj.getString("nsala")
                val requestQueue = Volley.newRequestQueue(this)
                val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {
                    val sala = response.getJSONObject("data")
                    val alocacao = sala.getInt("alocacao")
                    val lugares = sala.getInt("lugares")
                    validarSobreposicao(obj.getString("nsala"), alocacao.toString(), lugares.toString(), obj.getString("horainicio"), obj.getString("horafim"), obj.getString("datareserva"))
                } catch (e: JSONException) {
                    e.printStackTrace()
                    infoDialog(getString(R.string.erro_qrcode_sala))
                }
                }, { error -> error.printStackTrace().toString() })
                requestQueue.add(request)
            }
        }

        binding.scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    override fun onResume() {
        super.onResume()
        if(isCodeScanner) codeScanner.startPreview()
    }

    override fun onPause() {
        super.onPause()
        if(isCodeScanner) codeScanner.releaseResources()
    }

    private fun setupPermissions(){
        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        if(permission != PackageManager.PERMISSION_GRANTED){
            makeRequest()
        }
    }

    private fun makeRequest(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_REQUEST_CODE -> {
                if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    infoDialog(getString(R.string.erro_qrcode_permissoes))
                }
            }
        }
    }

    private fun isValidDate(date : String) : Boolean{
        return try {
            val dateTime = LocalDate.parse(date)
            Log.i("QRCODE", dateTime.toString())
            true
        } catch (dtpe: DateTimeException) {
            Log.i("QRCODE", dtpe.toString())
            false
        }
    }

    private fun isValidTime(time : String) : Boolean{
        return try {
            val dateTime = LocalTime.parse(time)
            Log.i("QRCODE", dateTime.toString())
            true
        } catch (dtpe: DateTimeException) {
            Log.i("QRCODE", dtpe.toString())
            false
        }
    }

    /* Enviar informações da reserva e verificar disponibilidade */
    fun validarSobreposicao(nsala : String , alocacao : String, lugares : String, horainicio : String, horafim : String, datareserva : String){
        val url = appSettings.URL_validar_sobreposicao
        val params = HashMap<String, String>()
        params["horainicio"] = horainicio
        params["horafim"] = horafim
        params["datareserva"] = datareserva
        params["nsala"] = nsala
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {
            if(response.getBoolean("success")){
                val dialog = AlertDialog.Builder(this)
                val dialogView = layoutInflater.inflate(R.layout.dialog_pergunta, null)
                val btnVoltar = dialogView.findViewById<Button>(R.id.dialogVoltar)
                val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
                val txtCapacidade = dialogView.findViewById<TextView>(R.id.txtCapacidade)
                val alocacaoInt = alocacao.toInt()
                val lugaresFinal =
                    Math.floor((lugares.toInt() * alocacaoInt / 100).toDouble())
                        .roundToInt()
                if (alocacaoInt < 100) {
                    txtCapacidade.text = "A capacidade máxima da sala esta limitada a $lugaresFinal Lugares"
                } else txtCapacidade.text = "A capacidade máxima é de $lugaresFinal Lugares"

                dialog.setView(dialogView)
                dialog.setCancelable(true)
                val customDialog = dialog.create()
                customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                customDialog.show()
                btnVoltar.setOnClickListener {
                    customDialog.dismiss()
                    finish()
                }
                btnContinuar.setOnClickListener {
                    customDialog.dismiss()
                    fazerReserva(nsala, horainicio, horafim, datareserva)
                }

            }else{
                infoDialog(response.getString("message"))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace().toString() })
        requestQueue.add(request)
    }

    fun fazerReserva(nsala : String , horainicio : String, horafim : String, datareserva : String){
        val requestQueue = Volley.newRequestQueue(this)
        val strRequest: StringRequest = object : StringRequest(
            Method.POST, appSettings.URLaddReserva,
            Response.Listener { response ->
                if(response.toBoolean()){
                    infoDialog(getString(R.string.sucesso_add_reserva))
                }else{
                    infoDialog(getString(R.string.erro_add_reserva))
                }
            },
            Response.ErrorListener { error ->
                infoDialog(getString(R.string.erro_geral))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["nutilizador"] = session.getUser().toString();
                params["horainicio"] = horainicio
                params["horafim"] = horafim
                params["datareserva"] = datareserva
                params["nsala"] = nsala
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }

    private fun infoDialog(message: String){
        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_qrcode, null)
        val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
        val txtInfo = dialogView.findViewById<TextView>(R.id.txtInfo)
        txtInfo.text = message
        dialog.setView(dialogView)
        dialog.setCancelable(true)
        val customDialog = dialog.create()
        customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        customDialog.setCanceledOnTouchOutside(false)
        customDialog.show()
        btnContinuar.setOnClickListener {
            customDialog.dismiss()
            finish()
        }
    }

    /* Verificar se a backend esta online, caso contrario mostrar aviso de erro */
    fun callBackend(){
        val url = appSettings.callBackend
        val requestQueue = Volley.newRequestQueue(applicationContext)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error ->
            val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            val dialogView = layoutInflater.inflate(R.layout.dialog_connection, null)
            val btnVoltar = dialogView.findViewById<Button>(R.id.dialogVoltar)
            dialog.setView(dialogView)
            dialog.setCancelable(true)
            val customDialog = dialog.create()
            customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            customDialog.show()
            btnVoltar.setOnClickListener {
                finish()
                customDialog.dismiss()
            }
        })
        requestQueue.add(request)
    }

}