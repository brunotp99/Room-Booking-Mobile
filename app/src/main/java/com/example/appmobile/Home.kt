package com.example.appmobile

import android.app.AlertDialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.appmobile.databinding.ActivityHomeBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.picasso.Picasso
import org.apache.http.conn.ConnectTimeoutException
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import org.xmlpull.v1.XmlPullParserException
import java.net.ConnectException
import java.net.MalformedURLException
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.math.roundToInt


class Home : AppCompatActivity() {

    lateinit var session : SessionManager
    private lateinit var binding: ActivityHomeBinding

    data class Reservas(val type: Int, val nreserva: Int, val nsala: Int, val nutilizador: Int, val datareserva: String, val horainicio: String, val horafim: String, val utilizador: String, val cargo: String, val imagem: String, val nomesala: String, val descricao: String, val estado: String)
    private var listReservasHoje = ArrayList<Reservas>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        AndroidThreeTen.init(this)

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

        val swipe : SwipeRefreshLayout = findViewById(R.id.swipeRefresh)

        swipe.setOnRefreshListener {
            getReservas()
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

    /* Obter lista de reservas da backend (Union Hoje, Amanha, Esta semana) */
    private fun reservasHoje(callback: VolleyCallBack?) {
        val url = appSettings.URLunionReservas + session.getEstabelecimento()
        Log.i("Home", url)
        listReservasHoje.clear()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, {
                response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)
                val userarray = distr.getJSONObject("utilizadores")
                val salaarray = distr.getJSONObject("sala")

                //val nreserva: Int, val nsala: Int, val nutilizador: Int, val datareserva: String, val horainicio: String, val horafim: String, val utilizador: String, val nomesala: String
                val type = distr.getString("tipolista").toInt()
                val nreserva = distr.getString("nreserva").toString().toInt()
                val nsala = distr.getString("nsala").toString().toInt()
                val nutilizador = distr.getString("nutilizador").toString().toInt()
                val datareserva = distr.getString("datareserva").toString()
                var formatted = ""
                val current = LocalDate.parse(datareserva)
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                formatted = current.format(formatter).toString()
                val horainicio = distr.getString("horainicio").toString()
                val horafim = distr.getString("fimreserva").toString()
                val utilizador = userarray.getString("utilizador").toString()
                val cargo = userarray.getString("cargo").toString()
                val imagem = userarray.getString("imagem").toString()
                val nomesala = salaarray.getString("nomesala").toString()
                val descricao = salaarray.getString("descricao").toString()
                val estado = distr.getString("estado")
                listReservasHoje.add(Reservas(type, nreserva, nsala, nutilizador, formatted, horainicio, horafim, utilizador, cargo, imagem, nomesala, descricao, estado))

            }
            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    /* Criação de uma ListView com adaptador customizado */
    private class MyCustomAdapter(context: Context, val Reservas : ArrayList<Reservas>): BaseAdapter() {
        private val mContext: Context
        init {
            mContext = context
        }
        override fun getCount(): Int {
            return Reservas.size
        }
        override fun getItemId(posicao: Int): Long {
            return posicao.toLong()
        }
        override fun getItem(posicao: Int): Any {
            return "Item"
        }

        fun dateFormatter(date: String) : String{

            val current = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val formatted = current.format(formatter).toString()

            if(formatted == date){
                return "Hoje"
            }

            val data = date.split("-")
            var dia = data[0]
            var mes = ""
            when(data[1].toInt()){
                1 -> mes = "Janeiro"
                2 -> mes = "Fevereiro"
                3 -> mes = "Março"
                4 -> mes = "Abril"
                5 -> mes = "Maio"
                6 -> mes = "Junho"
                7 -> mes = "Julho"
                8 -> mes = "Agosto"
                9 -> mes = "Setembro"
                10 -> mes = "Outubro"
                11 -> mes = "Novembri"
                12 -> mes = "Dezembro"
                else -> ""
            }

            return "$dia de $mes"

        }

        private var Tipo = -1
        override fun getView(posicao: Int, convertView: View?, viewGroup: ViewGroup?): View {

                val layoutInflater = LayoutInflater.from(mContext)
                val rowMain = layoutInflater.inflate(R.layout.card_home, viewGroup, false)

                if(Tipo != Reservas[posicao].type){
                    Tipo = Reservas[posicao].type
                    val titleList = rowMain.findViewById<TextView>(R.id.titleList)
                    titleList.visibility = View.VISIBLE
                    when(Tipo){
                        1 -> titleList.text = "Hoje"
                        2 -> titleList.text = "Amanhã"
                        3 -> titleList.text = "Outras"
                        else -> titleList.text = ""
                    }
                }

                val titleTextView = rowMain.findViewById<TextView>(R.id.titleReuniao)
                titleTextView.text = "Reserva na " + Reservas[posicao].nomesala

                val hourTextView = rowMain.findViewById<TextView>(R.id.horario)
                hourTextView.text = Reservas[posicao].horainicio + " as " + Reservas[posicao].horafim

                val dateTextView = rowMain.findViewById<TextView>(R.id.data)
                dateTextView.text = dateFormatter(Reservas[posicao].datareserva)

                val current = LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                val formatted = current.format(formatter).toString()

                /* Verificar se a reserva vai ser feita hoje */
                if(Reservas[posicao].datareserva == formatted){
                    if(Reservas[posicao].estado == "3"){
                        val cardColor = rowMain.findViewById<LinearLayout>(R.id.cardColor)
                        cardColor.setBackgroundResource(R.drawable.border_left_red)
                    }
                }

                /* Carregar a imagem do utilizador atraves do Picasso */
                val userImg = rowMain.findViewById<ShapeableImageView>(R.id.userImg)
                Log.i("Home", appSettings.URLfindImage + Reservas[posicao].imagem)
                Picasso.get().load(appSettings.URLfindImage + Reservas[posicao].imagem).resize(50, 50)
                    .centerCrop()
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(userImg);
                Log.i("Home", appSettings.URLfindImage + Reservas[posicao].imagem)

                /* Intent para pagina de informações enviando todos os dados */
                val cardbtn = rowMain.findViewById<MaterialCardView>(R.id.card)
                cardbtn.setOnClickListener {
                    val inte = Intent(mContext, InfoReserva::class.java)
                    inte.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    inte.putExtra("horas", Reservas[posicao].horainicio + " - " + Reservas[posicao].horafim);
                    inte.putExtra("data", Reservas[posicao].datareserva);
                    inte.putExtra("sala", Reservas[posicao].nomesala);
                    inte.putExtra("requisitante", Reservas[posicao].utilizador);
                    inte.putExtra("cargo", Reservas[posicao].cargo);
                    inte.putExtra("imagem", Reservas[posicao].imagem);
                    inte.putExtra("descricao", Reservas[posicao].descricao);
                    mContext.startActivity(inte);
                }
                this.notifyDataSetChanged();
                return rowMain
        }
    }

    /* Obter reservas da backend e carregar os dados para a lista, se esta estiver vazia mostrar ecra informativo */
    private fun getReservas(){
        binding.progessBarHome.visibility = View.VISIBLE
        val swipe : SwipeRefreshLayout = findViewById(R.id.swipeRefresh)
        reservasHoje(object: VolleyCallBack {
            override fun onSuccess() {
                binding.progessBarHome.visibility = View.GONE
                if(listReservasHoje.size != 0){
                    binding.nadaEncontrado.visibility = View.GONE
                    binding.mainListview.visibility = View.VISIBLE
                    binding.mainListview.adapter = MyCustomAdapter(applicationContext, listReservasHoje)
                }else{
                    binding.mainListview.visibility = View.GONE
                    binding.nadaEncontrado.visibility = View.VISIBLE
                }
                swipe.isRefreshing = false
            }
            override fun onFailure(error: String) {
                Log.i("Home", error)
                binding.progessBarHome.visibility = View.GONE
                binding.mainListview.visibility = View.GONE
                binding.nadaEncontrado.visibility = View.VISIBLE
                swipe.isRefreshing = false
            }
        })

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
                customDialog.dismiss()
            }
        })
        requestQueue.add(request)
    }

    override fun onResume() {
        super.onResume()
        getReservas()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val menuItem = bottomNavigationView.menu.getItem(0)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }
        callBackend()
    }

}