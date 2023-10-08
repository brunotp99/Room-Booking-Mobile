package com.example.appmobile

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.appmobile.databinding.ActivityHomeBinding
import com.example.appmobile.databinding.ActivityOutrosBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.picasso.Picasso
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import java.security.AccessController.getContext
import java.util.HashMap

class outrosActivity : AppCompatActivity() {

    lateinit var session : SessionManager
    private lateinit var binding: ActivityOutrosBinding

    data class Reservas(val nreserva: Int, val nsala: Int, val nutilizador: Int, val datareserva: String, val horainicio: String, val horafim: String, val utilizador: String, val cargo: String, val imagem: String, val nomesala: String, val descricao: String, val nestado: Int, val intervalo: Int)
    private var listReservasHoje = ArrayList<Reservas>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutrosBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        AndroidThreeTen.init(this)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        /* Botoes do Menu */
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.page_1 -> {
                    val OutrosActivity = Intent(applicationContext, outrosActivity::class.java)
                    startActivity(OutrosActivity)
                }
                R.id.page_2 -> {
                    val PedidosActivity = Intent(applicationContext, PedidosActivity::class.java)
                    startActivity(PedidosActivity)
                }
                R.id.page_3 -> {
                    session.LogoutUser()
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

    /* Obter lista de reservas backend e carregar para a recyclerview */
    private fun getReservas(){
        binding.progessBarHome.visibility = View.VISIBLE
        val swipe : SwipeRefreshLayout = findViewById(R.id.swipeRefresh)
                reservasHoje(object: VolleyCallBack {
                    override fun onSuccess() {
                        binding.progessBarHome.visibility = View.GONE
                        if(listReservasHoje.size != 0){
                            binding.nadaEncontrado.visibility = View.GONE
                            binding.mainListview.visibility = View.VISIBLE

                            val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(applicationContext)
                            binding.mainListview.layoutManager = layoutManager
                            val mainListAdapter = RecyclerOutros(listReservasHoje)
                            binding.mainListview.adapter = mainListAdapter

                            /* Callback para utilizar evento onclick fora do adaptador */
                            mainListAdapter.setOnClickListener(object :
                                RecyclerOutros.MyCallback {

                                override fun onListClick(horainicio: String, horafim: String, datareserva: String, sala: String, utilizador: String, cargo: String, imagem: String, descricao: String) {
                                    val inte = Intent(applicationContext, InfoReserva::class.java)
                                    inte.putExtra("horas", "$horainicio - $horafim");
                                    inte.putExtra("data", datareserva);
                                    inte.putExtra("sala", sala);
                                    inte.putExtra("requisitante", utilizador);
                                    inte.putExtra("cargo", cargo);
                                    inte.putExtra("imagem", imagem);
                                    inte.putExtra("descricao", descricao);
                                    startActivity(inte);
                                }

                            })

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


    /* Criação de uma recyclerview customizada */
    internal class RecyclerOutros(private val Reservas : ArrayList<Reservas>) : RecyclerView.Adapter<RecyclerOutros.MyViewHolder>() {

        internal interface MyCallback {
            fun onListClick(horainicio: String, horafim: String, datareserva: String, sala: String, utilizador: String, cargo: String, imagem: String, descricao: String)
        }

        var mItemClickListener: MyCallback? = null
        fun setOnClickListener(click: MyCallback) {
            mItemClickListener = click
        }

        internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var titleReuniao: TextView = view.findViewById(R.id.titleReuniao)
            var horario: TextView = view.findViewById(R.id.horario)
            var estadoSala: TextView = view.findViewById(R.id.estado)
            var userImg: ShapeableImageView = view.findViewById(R.id.userImg)
            var cardColor: LinearLayout = view.findViewById(R.id.cardColor)
            val cardbtn: MaterialCardView = view.findViewById(R.id.card)
            val limpeza: TextView = view.findViewById(R.id.limpeza)

        }
        @NonNull
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_outros, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

            holder.titleReuniao.text = "Reserva na " + Reservas[position].nomesala
            holder.horario.text = Reservas[position].horainicio + " - " + Reservas[position].horafim

            val intervalo = Reservas[position].intervalo
            if(intervalo != 0) holder.limpeza.text = "$intervalo minutos de limpeza"
            else holder.limpeza.text = "sem limpeza"

            Picasso.get().load(appSettings.URLfindImage + Reservas[position].imagem).resize(50, 50)
                .centerCrop()
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(holder.userImg)

            when(Reservas[position].nestado){
                1 -> {
                    holder.estadoSala.text = "Agendada"
                    holder.estadoSala.compoundDrawables[0]?.setTint(Color.parseColor("#b5b5b5")) //Disponivel
                    holder.cardColor.setBackgroundResource(R.drawable.border_left_grey)
                }
                4 -> {
                    holder.estadoSala.text = "Aguarda Limpeza"
                    holder.estadoSala.compoundDrawables[0]?.setTint(Color.parseColor("#17a2b8")) //Aguarda Limpeza
                    holder.cardColor.setBackgroundResource(R.drawable.border_left_orange)
                }
                5 -> {
                    holder.estadoSala.text = "Em curso"
                    holder.estadoSala.compoundDrawables[0]?.setTint(Color.parseColor("#d92929"))
                    holder.cardColor.setBackgroundResource(R.drawable.border_left_red)
                }
                else ->  holder.estadoSala.compoundDrawables[0]?.setTint(Color.parseColor("#6c757d")) //Indefinido
            }

            holder.cardbtn.setOnClickListener(View.OnClickListener { v ->
                mItemClickListener?.onListClick(Reservas[position].horainicio, Reservas[position].horafim, Reservas[position].datareserva, Reservas[position].nomesala, Reservas[position].utilizador, Reservas[position].cargo, Reservas[position].imagem, Reservas[position].descricao)
            })

        }
        override fun getItemCount(): Int {
            return Reservas.size
        }

    }

    /* Obter lista de reserva de hoje pela backend */
    private fun reservasHoje(callback: VolleyCallBack?) {
        val url = appSettings.URLreservasHoje + session.getEstabelecimento()
        Log.i("OutrosActivity", url)
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
                val nestado = salaarray.getInt("nestado")
                val intervalo = salaarray.getInt("intervalolimpeza")
                listReservasHoje.add(Reservas(nreserva, nsala, nutilizador, formatted, horainicio, horafim, utilizador, cargo, imagem, nomesala, descricao, nestado, intervalo))

            }
            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()); })
        requestQueue.add(request)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return false
    }

    override fun onResume() {
        super.onResume()
        getReservas()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val menuItem = bottomNavigationView.menu.getItem(0)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }
    }
}