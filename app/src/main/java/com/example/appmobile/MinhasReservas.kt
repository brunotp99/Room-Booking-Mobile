package com.example.appmobile

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.activitiesprojeto.ui.theme.*
import com.example.appmobile.databinding.ActivityMinhasReservasBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider
import com.himanshoe.kalendar.common.KalendarKonfig
import com.himanshoe.kalendar.common.KalendarSelector
import com.himanshoe.kalendar.common.KalendarStyle
import com.himanshoe.kalendar.common.data.KalendarEvent
import com.himanshoe.kalendar.common.theme.Grid
import com.himanshoe.kalendar.ui.Kalendar
import com.himanshoe.kalendar.ui.KalendarType
import com.jakewharton.threetenabp.AndroidThreeTen
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MinhasReservas : AppCompatActivity() {

    private lateinit var binding: ActivityMinhasReservasBinding
    lateinit var session : SessionManager
    private var listReservas = ArrayList<minhasReservasModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMinhasReservasBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        AndroidThreeTen.init(this);

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        /* Calendario Compose */
        composeHorziontal()

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

    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error: String)
    }

    /* Obter reservas da backend */
    private fun Reservas(callback: VolleyCallBack?, data : String) {
        val progessBarMinhasReservas = findViewById<LinearProgressIndicator>(R.id.progessBarMinhasReservas)
        progessBarMinhasReservas.visibility = View.VISIBLE

        val url = appSettings.URL_minhas_reservas + session.getEstabelecimento()
        val params = HashMap<String, String>()
        params["nutilizador"] = session.getUser().toString()

        listReservas.clear()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), {
                response ->try {

            val jsonArray = response.getJSONArray("" + "data")
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)
                val salaarray = distr.getJSONObject("sala")

                val nreserva = distr.getInt("nreserva")
                val datareserva = distr.getString("datareserva")
                val horainicio = distr.getString("horainicio")
                val horafim = distr.getString("horafim")
                val fimreserva = distr.getString("fimreserva")
                val estadoreserva = distr.getInt("estadoreserva")
                val current = LocalDate.parse(datareserva)
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                val formatted = current.format(formatter).toString()
                val nsala = salaarray.getInt("nsala")
                val sala = salaarray.getString("nomesala")
                val intervalo = salaarray.getInt("intervalolimpeza")
                val imagem = salaarray.getString("imagem")

                if(data == datareserva)
                    listReservas.add(minhasReservasModel(nreserva, formatted, horainicio, horafim, fimreserva, estadoreserva, nsala, sala, intervalo, imagem))

            }
            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    /* Obter as reservas na data selecionada */
    fun obterReservasData(datareserva : String){
        Reservas(object: VolleyCallBack {
            override fun onSuccess() {
                val progessBarMinhasReservas = findViewById<LinearProgressIndicator>(R.id.progessBarMinhasReservas)
                progessBarMinhasReservas.visibility = View.GONE

                /* Mostrar Imagem Vazio se nao ha reservas */
                if(listReservas.size == 0){
                    binding.nenhumaReserva.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                }else{
                    binding.nenhumaReserva.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }

                val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(applicationContext)
                recyclerView.layoutManager = layoutManager
                val mainListAdapter = RecyclerMinhasReservas(listReservas)
                recyclerView.adapter = mainListAdapter

                mainListAdapter.setOnClickListener(object :
                    RecyclerMinhasReservas.MyCallback {
                    override fun onListClick(position: Int, status: Int, intervalo : Int) {
                        Dialog(mainListAdapter, status, position, listReservas[position].nreserva, intervalo)
                    }

                    override fun onEditClick(nreserva: String, nsala : Int, sala : String, data : String, inicio : String, fim : String, status : Int, intervalo: Int, imagem: String) {
                        if(status == 0){
                            val i = Intent(applicationContext, EditActivity::class.java)
                            i.putExtra("nreserva", nreserva)
                            i.putExtra("sala", sala)
                            i.putExtra("nsala", nsala.toString())
                            i.putExtra("data", data)
                            i.putExtra("inicio", inicio)
                            i.putExtra("fim", fim)
                            i.putExtra("imagem", imagem)
                            startActivity(i)
                        }else{
                            val delim = "-"
                            val new = data.split(delim)
                            extenderReserva(nsala.toString(), nreserva,new[2]+"-"+new[1]+"-"+new[0], inicio, fim, intervalo)
                        }
                    }

                })
            }
            override fun onFailure(error: String) {
                val progessBarMinhasReservas = findViewById<LinearProgressIndicator>(R.id.progessBarMinhasReservas)
                progessBarMinhasReservas.visibility = View.GONE
                Log.i("MinhasReservas", error)
            }
        }, datareserva)
    }

    /* Carregar a livraria Kalendar atraves do JetPack Compose */
    private fun composeHorziontal(){
        val pt = Locale("pt", "PT")
        val listEvents = ArrayList<KalendarEvent>()
        val url = appSettings.URL_minhas_reservas + session.getEstabelecimento()
        val params = HashMap<String, String>()
        params["nutilizador"] = session.getUser().toString()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), {
                response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)
                val datareserva = distr.getString("datareserva")
                listEvents.add(KalendarEvent(LocalDate.parse(datareserva), "", ""))
            }

            val view = findViewById<ComposeView>(R.id.horizontalCalendar).setContent{
                Kalendar(
                    kalendarType = KalendarType.Oceanic(),
                    kalendarEvents = listEvents,
                    kalendarKonfig = KalendarKonfig(locale = pt),
                    kalendarStyle = KalendarStyle(hasRadius = false, elevation = Grid.Zero, kalendarBackgroundColor = Grey, kalendarColor = Grey, kalendarSelector = KalendarSelector.Dot(defaultColor = Color.White, selectedColor = Azul800, todayColor = Azul500, eventTextColor = Azul800, defaultTextColor = Smooth)),
                    onCurrentDayClick = { day, event ->

                        obterReservasData(day.toString())

                    }, errorMessage = {
                        //Handle the error if any
                    })
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace().toString() })
        requestQueue.add(request)
    }

    /* Abrir Dialogo para terminar uma reserva */
    private fun Dialog(adapter : RecyclerMinhasReservas, status : Int, position : Int, nreserva : Int, intervalo : Int){
        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_apagar, null)
        val btnVoltar = dialogView.findViewById<Button>(R.id.dialogVoltar)
        val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
        val txtRemover = dialogView.findViewById<TextView>(R.id.txtRemover)
        val txtCerteza = dialogView.findViewById<TextView>(R.id.txtCerteza)
        if(status == 1){
            txtRemover.text = "Terminar Reserva"
            txtCerteza.text = "Tem a certeza que deseja terminar a sua reserva?"
        }
        dialog.setView(dialogView)
        dialog.setCancelable(true)
        val customDialog = dialog.create()
        customDialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        customDialog.show()
        btnVoltar.setOnClickListener {
            customDialog.dismiss()
        }
        btnContinuar.setOnClickListener {
            customDialog.dismiss()
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Lisbon"))
            val sdf = SimpleDateFormat("HH:mm", Locale("pt", "PT"))
            val horas = sdf.format(Date())
            val horaslocal = LocalTime.parse(horas).plusMinutes(intervalo.toLong())
            if(status == 1) terminarReserva(nreserva.toString(), horas, horaslocal.toString())
            else apagarReserva(nreserva.toString())
            adapter.removeAt(position)
        }
    }

    /* Dialogo para extender uma reserva */
    private fun DialogSlider(nreserva : String, fim : String, max : Float, intervalo: Int){
        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_slider, null)
        val btnVoltar = dialogView.findViewById<Button>(R.id.dialogVoltar)
        val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
        val slider = dialogView.findViewById<Slider>(R.id.slider)
        val txtMax = dialogView.findViewById<TextView>(R.id.txtMax)
        val maxInt = max.toInt()
        val s = "Pode extender a reunião no máximo <b>$maxInt</b> minutos"
        txtMax.text = Html.fromHtml(s)
        slider.valueFrom = 0F
        slider.valueTo = max
        slider.value = 5F
        dialog.setView(dialogView)
        dialog.setCancelable(true)
        val customDialog = dialog.create()
        customDialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        customDialog.show()
        btnVoltar.setOnClickListener {
            customDialog.dismiss()
        }
        btnContinuar.setOnClickListener {
            customDialog.dismiss()
            var horasfim = ""
            if(fim.length == 4)
                horasfim = "0" + fim
            else horasfim = fim
            val horasFormat = LocalTime.parse(horasfim)
            val horasFinal = horasFormat.plusMinutes(slider.value.toLong())
            sendMinutes(nreserva, horasFinal.toString(), intervalo)
        }
    }

    /* Função para apagar uma reserva */
    fun apagarReserva(nreserva : String){
        val requestQueue = Volley.newRequestQueue(this)
        val strRequest: StringRequest = object : StringRequest(
            Method.POST, appSettings.URLdeleteReserva,
            Response.Listener { response ->
                if(response.toBoolean()){
                    composeHorziontal()
                    alertaSuccess(this@MinhasReservas, "Sucesso", getString(R.string.sucesso_remove_reserva))
                }else{
                    alertaDanger(this@MinhasReservas, "Atenção", getString(R.string.erro_remove_reserva))
                }
            },
            Response.ErrorListener { error ->
                Log.i("MinhasReservas", error.toString())
                alertaDanger(this@MinhasReservas, "Atenção", getString(R.string.erro_geral))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["nreserva"] = nreserva
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }

    /* Função para terminar uma reserva em curso */
    fun terminarReserva(nreserva : String, fim : String, fimintervalo : String){
        val requestQueue = Volley.newRequestQueue(this)
        val strRequest: StringRequest = object : StringRequest(
            Method.POST, appSettings.URLterminarReserva,
            Response.Listener { response ->
                if(response.toBoolean()){
                    alertaSuccess(this@MinhasReservas, "Sucesso", getString(R.string.sucesso_apagar_reserva))
                }else{
                    alertaDanger(this@MinhasReservas, "Atenção", getString(R.string.erro_apagar_reserva))
                }
            },
            Response.ErrorListener { error ->
                Log.i("MinhasReservas", error.toString())
                alertaDanger(this@MinhasReservas, "Atenção", getString(R.string.erro_geral))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["nreserva"] = nreserva
                params["fim"] = fim
                params["fimintervalo"] = fimintervalo
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }

    /* Função para extender a reserva selecionada */
    fun sendMinutes(nreserva : String, horas : String, intervalo : Int){
        val paginaMinhasReservas = findViewById<LinearLayout>(R.id.paginaMinhasReservas)
        val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
        val requestQueue = Volley.newRequestQueue(this)
        val strRequest: StringRequest = object : StringRequest(
            Method.PUT, appSettings.URLeditHoraReserva + nreserva,
            Response.Listener { response ->
                if(response.toBoolean()){
                    obterReservasHoje()
                    alertaSuccess(this@MinhasReservas, "Sucesso", getString(R.string.erro_apagar_reserva))
                }else{
                    alertaDanger(this@MinhasReservas, "Atenção", getString(R.string.erro_extender_reserva))
                }
            },
            Response.ErrorListener { error ->
                Log.i("MinhasReservas", error.toString())
                alertaDanger(this@MinhasReservas, "Atenção", getString(R.string.erro_geral))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["horafim"] =  horas
                params["intervalo"] = intervalo.toString()
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }

        }
        requestQueue.add(strRequest)
    }

    /* Função para saber quantos minutos posso extender uma reserva */
    fun extenderReserva(nsala : String, nreserva : String, data : String, inicio : String, fim : String, intervalo : Int){
        val url = appSettings.URLextenderReserva
        val params = HashMap<String, String>()
        params["nsala"] = nsala
        params["data"] = data
        params["inicio"] = inicio
        params["fim"] = fim
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), {
                response ->try {
            if(response.getBoolean("status")){
                    val minutos = response.getInt("limit")
                    DialogSlider(nreserva, fim, minutos.toFloat(), intervalo)
            }else{
                alertaDanger(this@MinhasReservas, "Atenção", getString(R.string.sobreposicao_reservas))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace().toString() })
        requestQueue.add(request)
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
            customDialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            customDialog.show()
            btnVoltar.setOnClickListener {
                customDialog.dismiss()
            }
        })
        requestQueue.add(request)
    }

    fun obterReservasHoje(){
        val current = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formatted = current.format(formatter).toString()

        Reservas(object: VolleyCallBack {
            override fun onSuccess() {
                val progessBarMinhasReservas = findViewById<LinearProgressIndicator>(R.id.progessBarMinhasReservas)
                progessBarMinhasReservas.visibility = View.GONE

                /* Mostrar Imagem Vazio se nao ha reservas */
                if(listReservas.size == 0){
                    binding.nenhumaReserva.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                }else{
                    binding.nenhumaReserva.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }

                val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(applicationContext)
                recyclerView.layoutManager = layoutManager
                val mainListAdapter = RecyclerMinhasReservas(listReservas)
                recyclerView.adapter = mainListAdapter

                mainListAdapter.setOnClickListener(object :
                    RecyclerMinhasReservas.MyCallback {
                    override fun onListClick(position: Int, status: Int, intervalo : Int) {
                        Dialog(mainListAdapter, status, position, listReservas[position].nreserva, intervalo)
                    }
                    override fun onEditClick(nreserva: String, nsala : Int, sala : String, data : String, inicio : String, fim : String, status : Int, intervalo : Int, imagem : String) {
                        if(status == 0){
                            val i = Intent(applicationContext, EditActivity::class.java)
                            i.putExtra("nreserva", nreserva)
                            i.putExtra("sala", sala)
                            i.putExtra("nsala", nsala.toString())
                            i.putExtra("data", data)
                            i.putExtra("inicio", inicio)
                            i.putExtra("fim", fim)
                            i.putExtra("imagem", imagem)
                            startActivity(i)
                        }else{
                            val delim = "-"
                            val new = data.split(delim)
                            extenderReserva(nsala.toString(), nreserva,new[2]+"-"+new[1]+"-"+new[0], inicio, fim, intervalo)
                        }
                    }

                })
            }
            override fun onFailure(error: String) {
                val progessBarMinhasReservas = findViewById<LinearProgressIndicator>(R.id.progessBarMinhasReservas)
                progessBarMinhasReservas.visibility = View.GONE
                Log.i("MinhasReservas", error)
            }
        }, formatted)
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val menuItem = bottomNavigationView.menu.getItem(3)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }

        callBackend()
        obterReservasHoje()
        composeHorziontal()

    }

}