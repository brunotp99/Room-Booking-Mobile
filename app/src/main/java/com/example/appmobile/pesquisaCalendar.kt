package com.example.appmobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.compose.material.Surface
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.activitiesprojeto.ui.theme.*
import com.example.appmobile.databinding.ActivityPesquisaCalendarBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.himanshoe.kalendar.common.KalendarKonfig
import com.himanshoe.kalendar.common.KalendarSelector
import com.himanshoe.kalendar.common.KalendarStyle
import com.himanshoe.kalendar.ui.Kalendar
import com.himanshoe.kalendar.ui.KalendarType
import com.jakewharton.threetenabp.AndroidThreeTen
import org.json.JSONException
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import java.util.*

class pesquisaCalendar : AppCompatActivity() {

    private lateinit var binding: ActivityPesquisaCalendarBinding
    lateinit var session : SessionManager
    data class Reservas(val nreserva: Int, val datareserva: String, val horainicio: String, val horafim: String, val estadoreserva: Int, val nomeuser: String, val estadouser: Int, val nomesala: String)
    var listReservas = ArrayList<Reservas>()
    private lateinit var obterEventos : ArrayList<Event>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPesquisaCalendarBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
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


    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error: String)
    }

    /* Obter lista de reservas */
    fun getReservas(callback: VolleyCallBack?) {
        val bundle = intent.extras
        val nsala = bundle?.getString("sala")
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

                listReservas.add(
                    Reservas(
                        getReserva,
                        getData,
                        getHoraInicio,
                        getHoraFim,
                        getEstadoReserva,
                        getUtilizador,
                        getEstadoUser,
                        getSala
                    )
                )

            }

            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    /* Carregar a livraria Kalendar atraves do JetPack Compose */
    private fun composeHorziontal(){
        val pt = Locale("pt", "PT")
        val view = findViewById<ComposeView>(R.id.horizontalCalendar).setContent{
            Kalendar(
                kalendarType = KalendarType.Oceanic(startDate = LocalDate.now()),
                kalendarKonfig = KalendarKonfig(locale = pt),
                kalendarStyle = KalendarStyle(kalendarBackgroundColor = Branco,kalendarSelector = KalendarSelector.Rounded(selectedColor = Azul800, todayColor = Azul500)),
                onCurrentDayClick = { day, event ->
                    if(day.dayOfWeek.toString() == "SATURDAY" || day.dayOfWeek.toString() == "SUNDAY")
                        naoPode()
                    else addComposeView(day.toString())
                }, errorMessage = {
                    //Handle the error if any
                })
        }
    }

    /* Carregar o calendario atraves do JetPack Compose */
    private fun addComposeView(data: String){
        val pt = Locale("pt", "PT")
        Locale.setDefault(pt);
        val bundle = intent.extras
        val nsala = bundle?.getString("sala")
        val alocacao = bundle?.getString("alocacao")
        val lugares = bundle?.getString("lugares")
        val intervalo = bundle?.getString("intervalo")
        val view = findViewById<ComposeView>(R.id.composeCalendar).setContent{
            WeekScheduleTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Schedule(events = obterEventos, data, nsala!!, alocacao!!, lugares!!, intervalo!!, false)
                }
            }
        }
    }

    private fun naoPode(){
        val view = findViewById<ComposeView>(R.id.composeCalendar).setContent{
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val image: Painter = painterResource(id = R.drawable.undraw_events)
                    Image(painter = image,contentDescription = "", modifier = Modifier.width(200.dp))
                    Text(
                        text = getString(R.string.erro_fins_de_semana),
                        color = Color.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(16.dp)
                    )
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        getReservas(object: VolleyCallBack {
            override fun onSuccess() {
                val addReservas = ArrayList<Event>()
                for(item in listReservas){
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
                composeHorziontal()
                if(LocalDate.now().dayOfWeek.toString() == "SATURDAY" || LocalDate.now().dayOfWeek.toString() == "SUNDAY")
                    naoPode()
                else addComposeView(LocalDate.now().toString())
            }

            override fun onFailure(error: String) {
                val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
                Log.i("Volley", error)
                alertaDanger(this@pesquisaCalendar, "Atenção", getString(R.string.erro_mostrar_calendario))
            }

        })
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val menuItem = bottomNavigationView.menu.getItem(1)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }
    }

}