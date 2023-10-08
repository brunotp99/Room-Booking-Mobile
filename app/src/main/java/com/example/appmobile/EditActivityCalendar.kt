package com.example.appmobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.activitiesprojeto.ui.theme.Azul800
import com.example.activitiesprojeto.ui.theme.Danger
import com.example.activitiesprojeto.ui.theme.WeekScheduleTheme
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import com.jakewharton.threetenabp.AndroidThreeTen
import org.json.JSONException
import org.threeten.bp.LocalDateTime
import java.util.*

class EditActivityCalendar : AppCompatActivity() {

    lateinit var session : SessionManager
    data class Reservas(val nreserva: Int, val datareserva: String, val horainicio: String, val horafim: String, val estadoreserva: Int, val nomeuser: String, val estadouser: Int, val nomesala: String)
    var listReservas = ArrayList<Reservas>()
    data class Salas(val nsala : Int, val nestabelecimento : Int, val nestado : Int, val sala : String, val lugares : Int, val estadsala: Int, val descricao : String, val alocacao : Int, val intervalolimpeza : Int)
    var listSalas = ArrayList<Salas>()
    private lateinit var obterEventos : ArrayList<Event>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_calendar)
        AndroidThreeTen.init(this);

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

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

        /* Botao Notificacoes */
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
            finish()
        }

        /* Obter Salas */
        getSala(object: VolleyCallBack {
            override fun onSuccess() {
                getReservas(object: VolleyCallBack {
                    override fun onSuccess() {
                        val bundle = intent.extras
                        if(bundle != null){
                            val data = bundle.getString("data")
                            val delim = "-"
                            val dataSep = data?.split(delim)
                            val dataNova = dataSep!![2] + "-" + dataSep[1] + "-" + dataSep[0]
                            val nreserva = bundle.getString("nreserva")

                            val addReservas = ArrayList<Event>()
                            for(item in listReservas){
                                if(item.nreserva == nreserva?.toInt()) continue;
                                var hinicio = ""
                                var hfim = ""
                                if(item.horainicio.length == 4)
                                    hinicio = "0" + item.horainicio
                                else hinicio = item.horainicio

                                if(item.horafim.length == 4)
                                    hfim = "0" + item.horafim
                                else hfim = item.horafim

                                val Inicio = item.datareserva + "T" + hinicio + ":00"
                                val Fim = item.datareserva + "T" + hfim + ":00"

                                addReservas.add(Event(item.nomeuser, Azul800, LocalDateTime.parse(Inicio),  LocalDateTime.parse(Fim), item.nomesala))
                            }
                            obterEventos = addReservas

                            addComposeView(dataNova, listSalas[0].nsala.toString(), listSalas[0].alocacao.toString(), listSalas[0].lugares.toString(), listSalas[0].intervalolimpeza.toString())
                        }
                    }
                    override fun onFailure(error: String) {
                        Log.i("EditActivity", error)
                    }
                })
            }
            override fun onFailure(error: String) {
                Log.i("EditActivity", error)
            }
        })

    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error: String)
    }

    /* Obter reservas da backend */
    fun getReservas(callback: VolleyCallBack?) {
        val bundle = intent.extras
        val nsala = bundle?.getString("nsala")
        val url = appSettings.URLallReservas + nsala!!
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            listReservas.clear()
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)
                val userarray = distr.getJSONObject("utilizadores")
                val salaarray = distr.getJSONObject("sala")

                val getReserva = distr.getInt("nreserva")
                val getData = distr.getString("datareserva")
                val getHoraInicio = distr.getString("horainicio")
                val getHoraFim = distr.getString("horafim")
                val getEstadoReserva = distr.getInt("estadoreserva")
                val getUtilizador = userarray.getString("utilizador")
                val getEstadoUser = userarray.getInt("estadouser")
                val getSala = salaarray.getString("nomesala")

                listReservas.add(Reservas(getReserva, getData, getHoraInicio, getHoraFim, getEstadoReserva, getUtilizador, getEstadoUser, getSala))

            }

            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    /* Obter sala da backend */
    fun getSala(callback: VolleyCallBack?) {
        val bundle = intent.extras
        val nsala = bundle?.getString("nsala")
        val url = appSettings.URLgetSala + nsala!!

        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

            val distr = response.getJSONObject("data")

            val nsala = distr.getInt("nsala")
            val nestabelecimento = distr.getInt("nestabelecimento")
            val nestado = distr.getInt("nestado")
            val sala = distr.getString("sala")
            val lugares = distr.getInt("lugares")
            val estadosala = distr.getInt("estadosala")
            val descricao = distr.getString("descricao")
            val alocacao = distr.getInt("alocacao")
            val intervalolimpeza = distr.getInt("intervalolimpeza")

            listSalas.add(Salas(nsala, nestabelecimento, nestado, sala, lugares, estadosala, descricao, alocacao, intervalolimpeza))

            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    /* Carregar calendario atraves de JetPack Compose */
    private fun addComposeView(data: String, nsala : String, alocacao : String, lugares : String, intervalo : String){
        val pt = Locale("pt", "PT")
        val bundle = intent.extras
        val nreserva = bundle?.getString("nreserva")
        Locale.setDefault(pt);
        val newData = data.split("-")
        var dataEnviar = data
        if(newData[0].length != 4){
            //quer dizer que o ano nao esta ao inicio logo inverter
            dataEnviar = newData[2] + "-" + newData[1] + "-" + newData[0]
        }
        val view = findViewById<ComposeView>(R.id.composeCalendar).setContent{
            EditBoolean = true
            nReserva = nreserva!!
            WeekScheduleTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Schedule(events = obterEventos, dataEnviar, nsala, alocacao, lugares, intervalo, true)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val menuItem = bottomNavigationView.menu.getItem(3)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }
    }

}